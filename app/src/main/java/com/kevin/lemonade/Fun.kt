package com.kevin.lemonade

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/** Lottery, races, trophies, rebirth, surprise events, secret codes, settings. */
class FunState(val g: GameViewModel) : Feature {
    override val key = "fun"

    // =================== REBIRTH ===================
    var rebirths by mutableIntStateOf(0)
    fun rebirthMult() = 1 + 0.25 * rebirths
    fun rebirthCost(): Long = (10000.0 * 2.0.pow(rebirths)).roundToLong()

    /** everything a rebirth keeps (the shop ids that are NOT reset to 0) - same list as the original's `keep` */
    private val rebirthKeep = setOf(
        "bow", "pager", "telescope", "map", "closetkey", "shovel", "motor", "wand", "saddle", "lottopass", "racepass",
        "girlfriend", "thappy", "tquick", "tchair", "clover", "horseshoe", "rabbitfoot", "goldluck", "diamondluck",
        "dodgeluck", "tipluck", "vipluck", "critluck", "oreluck", "ghostpay", "pass", "reunion", "snacks",
        "nightlight", "loot", "brave",
    )

    fun doRebirth() {
        val c = rebirthCost()
        if (g.money < c) return
        if (g.busy) { rebirthMsg = "Wait for the squeeze to finish!"; return }
        rebirths++
        g.money = 0
        for (id in g.lvl.keys.toList()) if (id !in rebirthKeep) g.lvl[id] = 0
        g.inPitcher = 0
        g.juiceLevel = 0f
        // every other feature decides for itself what a rebirth resets (most things are kept, per the original)
        for (f in g.features) if (f !== this) f.onRebirth()
        g.showFace(Face.CHEER)
        g.say(
            if (rebirths == 1) "I've been REBORN! And look, a SECOND squeezer! ...Wait, that's twice the squeezing. Oh no."
            else "Rebirth number $rebirths! I feel brand new! Money bonus: x${"%.2f".format(rebirthMult())}!",
        )
        rebirthMsg = "Rebirth $rebirths complete! Every pitcher now pays ${"%.2f".format(rebirthMult())}x."
        g.save()
    }

    var rebirthMsg by mutableStateOf("")

    // =================== LEMON LOTTERY ===================
    val lottoCells = mutableStateListOf<LottoSymbol>()
    val lottoOpen = mutableStateListOf<Boolean>()
    var hasCard by mutableStateOf(false)
    var lottoDone by mutableStateOf(true)
    var lottoMsg by mutableStateOf("Buy a ticket and scratch it!")
    var wonJackpot by mutableStateOf(false)

    /** limes won from the lottery/bettors. INTEGRATION: this should feed Pirate's limesLeft (squeezable limes)
     * once PirateState exists; for now it's just held here so nothing crashes or silently vanishes. */
    var strandedLimes by mutableIntStateOf(0)

    private fun rollSymbol(): LottoSymbol {
        var r = Random.nextDouble() * 100
        for (i in SYMBOLS.indices) { r -= LOTTO_WEIGHTS[i]; if (r <= 0) return SYMBOLS[i] }
        return SYMBOLS[0]
    }

    fun newLottoCard() {
        val cells = MutableList(9) { rollSymbol() }
        if (Random.nextDouble() < 0.4 + g.luck()) {
            val w = rollSymbol()
            for (i in (0..8).shuffled().take(3)) cells[i] = w
        } else {
            for (i in 0..8) {
                val counts = HashMap<String, Int>()
                cells.forEach { counts[it.id] = (counts[it.id] ?: 0) + 1 }
                if ((counts[cells[i].id] ?: 0) > 2) {
                    val idx = (SYMBOLS.indexOf(cells[i]) + 1 + i) % SYMBOLS.size
                    cells[i] = SYMBOLS[idx]
                }
            }
        }
        lottoCells.clear(); lottoCells.addAll(cells)
        lottoOpen.clear(); repeat(9) { lottoOpen.add(false) }
        hasCard = true; lottoDone = false
    }

    fun buyTicket() {
        if (g.money < LOTTO_TICKET || (hasCard && !lottoDone)) return
        g.money -= LOTTO_TICKET
        newLottoCard()
        lottoMsg = "Tap the squares to scratch them! Get 3 of the same to win."
    }

    fun scratch(i: Int) {
        if (!hasCard || lottoDone || lottoOpen[i]) return
        lottoOpen[i] = true
        if (lottoOpen.all { it }) finishLottoCard()
    }

    fun scratchAll() {
        if (!hasCard || lottoDone) return
        for (i in lottoOpen.indices) lottoOpen[i] = true
        finishLottoCard()
    }

    private fun finishLottoCard() {
        lottoDone = true
        val counts = HashMap<String, Int>()
        lottoCells.forEach { counts[it.id] = (counts[it.id] ?: 0) + 1 }
        val win = SYMBOLS.firstOrNull { (counts[it.id] ?: 0) >= 3 }
        lottoMsg = when {
            win == null -> "No match. ${g.name}: \"Aww. Maybe next time!\""
            win.id == "skull" -> "3 skulls! Darth Lime laughs at you. Nothing this time!"
            win.limes > 0 -> { strandedLimes += win.limes; "3 limes! You win ${win.limes} limes to squeeze!" }
            else -> {
                g.money += win.prize
                if (win.id == "kevin") wonJackpot = true
                "${win.name}! You win ${fmt(win.prize)}!" + if (win.id == "kevin") " ${g.name}: \"That's ME on the ticket! I'm LUCKY!\"" else ""
            }
        }
        checkTrophies()
    }

    // =================== LEMON RACES ===================
    var racePick by mutableIntStateOf(0)
    var raceBet by mutableLongStateOf(10L)
    var racing by mutableStateOf(false)
    var raceMsg by mutableStateOf("")
    val racePos = mutableStateListOf(0f, 0f, 0f, 0f)
    private var raceJob: Job? = null

    fun startRace() {
        if (racing || g.money < raceBet) return
        g.money -= raceBet
        racing = true
        raceMsg = "And they're OFF!"
        for (i in racePos.indices) racePos[i] = 0f
        raceJob = g.viewModelScope.launch {
            val base = RACERS.map { 1.0 / sqrt(it.odds.toDouble()) }
            var winner = -1
            var last = g.now()
            val start = last
            while (winner < 0) {
                delay(16)
                val now = g.now()
                val dt = min(0.05, (now - last) / 1000.0); last = now
                val tSec = (now - start) / 1000.0
                for (i in RACERS.indices) {
                    val speed = (60 + Random.nextDouble() * 120) * base[i] * 1.6 * dt *
                        (1 + 0.4 * sin(tSec * 1000 / 300 + i * 2)) * (if (i == racePick) 1 + g.luck() * 0.8 else 1.0)
                    racePos[i] = racePos[i] + speed.toFloat()
                    if (winner < 0 && racePos[i] >= RACE_TRACK) winner = i
                }
            }
            racing = false
            val w = RACERS[winner]
            if (winner == racePick) {
                val prize = raceBet * w.odds
                g.money += prize
                raceMsg = "${w.name} WINS! You won ${fmt(prize)}!"
            } else {
                raceMsg = "${w.name} wins! Your racer lost. ${g.name}: \"There's always next race!\""
            }
        }
    }

    // =================== BETTORS ===================
    class Bettor(val name: String, val icon: String) {
        var hired by mutableStateOf(false)
        var t by mutableIntStateOf(0)
        var log by mutableStateOf("Ready to bet!")
    }

    val bettors = listOf(Bettor("Betty", "🎩"), Bettor("Gus", "🧢"), Bettor("Lucky Lou", "🍀"), Bettor("Dice", "🎲"))
    fun bettorCost(): Long = (250.0 * 2.2.pow(bettors.count { it.hired })).roundToLong()

    fun hireBettor(i: Int) {
        val b = bettors[i]
        if (b.hired || g.money < bettorCost()) return
        g.money -= bettorCost()
        b.hired = true
    }

    private fun simTicket(): Pair<String, Long> {
        if (Random.nextDouble() >= 0.4 + g.luck()) return "lost" to 0L
        val r = Random.nextDouble() * 100
        val id = when { r < 33 -> "lemon"; r < 52 -> "lime"; r < 77 -> "gold"; r < 88 -> "diamond"; r < 92 -> "kevin"; else -> "skull" }
        val s = SYMBOLS.first { it.id == id }
        if (s.limes > 0) { strandedLimes += s.limes; return "won ${s.limes} limes" to 0L }
        if (s.id == "kevin") wonJackpot = true
        return (if (s.prize > 0) "won ${fmt(s.prize)}" else "got 3 skulls") to s.prize
    }

    private fun simRace(bet: Long): Pair<String, Long> {
        val pickI = Random.nextInt(RACERS.size)
        val weights = RACERS.mapIndexed { i, r -> (1.0 / r.odds) * (if (i == pickI) 1 + g.luck() * 2 else 1.0) }
        val total = weights.sum()
        var r = Random.nextDouble() * total
        var win = 0
        for (i in weights.indices) { r -= weights[i]; if (r <= 0) { win = i; break } }
        val picked = RACERS[pickI]
        return if (win == pickI) "bet on ${picked.name} and WON ${fmt(bet * picked.odds)}" to bet * picked.odds
        else "bet on ${picked.name} and lost ${fmt(bet)}" to 0L
    }

    private fun tickBettors() {
        for (b in bettors) {
            if (!b.hired) continue
            b.t++
            if (b.t < 30) continue
            b.t = 0
            val games = buildList { if (g.lv("lottopass") > 0) add("lotto"); if (g.lv("racepass") > 0) add("race") }
            if (games.isEmpty()) { b.log = "Unlock the lottery or the races first!"; continue }
            if (games.pick() == "lotto") {
                if (g.money < LOTTO_TICKET) { b.log = "Can't afford a ticket right now."; continue }
                g.money -= LOTTO_TICKET
                val (txt, won) = simTicket(); g.money += won
                b.log = "Bought a ticket and $txt."
            } else {
                val bet = max(10L, min(1000L, (g.money * 0.05).toLong()))
                if (g.money < bet) { b.log = "Can't afford a bet right now."; continue }
                g.money -= bet
                val (txt, won) = simRace(bet); g.money += won
                b.log = txt.replaceFirstChar { it.uppercase() } + "."
            }
        }
    }

    // =================== TROPHIES ===================
    val gotTrophy = mutableStateListOf<Int>()
    val gotSecret = mutableStateListOf<Int>()
    var usedHall by mutableStateOf(false)
    var lemyEver by mutableStateOf(false)

    fun checkTrophies() {
        if (g.dating.momoWith) lemyEver = true
        TROPHIES.forEachIndexed { i, t -> if (i !in gotTrophy && t.done(g, this)) { gotTrophy.add(i); g.toast("🏆 Trophy unlocked: ${t.name}!") } }
        SECRETS.forEachIndexed { i, t -> if (i !in gotSecret && t.done(g, this)) { gotSecret.add(i); g.toast("🌟 SECRET trophy unlocked: ${t.name}!") } }
    }

    fun trophyLabel(): String = "Trophies (${gotTrophy.size}/${TROPHIES.size})" + if (gotSecret.isNotEmpty()) " +${gotSecret.size} secret" else ""

    // =================== SURPRISE EVENTS ===================
    private var eventT = 0
    var custQueue by mutableIntStateOf(0)

    private val EVENTS: List<() -> Unit> = listOf(
        ::eventUfo, ::eventFamous, ::eventRainbow, ::eventBird, ::eventMeteor, ::eventSnow, ::eventWindy, ::eventBirthday,
    )

    fun runEvent(i: Int) { EVENTS.getOrNull(i)?.invoke() }

    fun startRainbow(ms: Long) {
        g.rainbowUntil = max(g.now(), g.rainbowUntil) + ms
        g.popAt("RAINBOW! Double money for ${if (ms >= 120000) "${ms / 60000} minutes" else "1 minute"}!", 0.5f, 0.15f)
    }

    private fun eventUfo() {
        if (g.talk["lemons"] == true) g.say("A UFO?! Are they... taking my cousins?! Or buying them?")
        val pay = max(50L, g.price() * 8)
        g.money += pay
        g.popAt("Aliens paid ${fmt(pay)}!", 0.60f, 0.20f)
    }

    private fun eventFamous() {
        val who = listOf("a famous movie star", "the Lemon King", "a pop singer", "an astronaut", "the mayor").pick()
        val pay = max(30L, g.price() * 10)
        g.money += pay
        g.popAt("${who.replaceFirstChar { it.uppercase() }} bought 10 pitchers! +${fmt(pay)}", 0.5f, 0.2f)
        if (g.talk["lemons"] == true) g.say("Whoa! That was $who! They LOVE our lemonade!")
    }

    private fun eventRainbow() {
        startRainbow(60000)
        if (g.talk["lemons"] == true) g.say("A rainbow! Everything is twice as nice right now!")
    }

    private fun eventBird() {
        if (g.lemonsLeft > 0) g.lemonsLeft--
        if (g.talk["lemons"] == true) g.say("HEY! That bird stole a lemon! That was my cousin, you THIEF!")
    }

    private fun eventMeteor() {
        val before = g.lemonsLeft
        g.addLemons(6 + Random.nextInt(10))
        g.popAt(if (g.lemonsLeft > before) "Lemon meteor shower! +${g.lemonsLeft - before} lemons" else "Lemon meteor shower! (storage is full)", 0.5f, 0.15f)
        if (g.talk["lemons"] == true) g.say("Lemons falling from SPACE?! Is that Pineapple Kevin's doing?")
    }

    private fun eventSnow() {
        val pay = max(40L, g.price() * 8)
        g.money += pay
        g.popAt("SNOW DAY! Everyone wants hot lemonade: +${fmt(pay)}", 0.5f, 0.15f)
        if (g.talk["lemons"] == true) g.say("SNOW?! Brrr! Hot lemonade, anyone? ...Is that even a thing?")
    }

    private fun eventWindy() {
        val before = g.lemonsLeft
        g.addLemons(8)
        g.popAt(if (g.lemonsLeft > before) "WINDY! ${g.lemonsLeft - before} lemons blew in!" else "WINDY! (lemon storage is full)", 0.5f, 0.15f)
        if (g.talk["lemons"] == true) g.say("Whoa, it's WINDY! Hold onto your leaves, everybody!")
    }

    private fun eventBirthday() {
        val gift = max(300L, g.price() * 30)
        g.money += gift
        strandedLimes += 10
        g.popAt("HAPPY BIRTHDAY, ${g.name.uppercase()}! Presents: +${fmt(gift)} and 10 limes!", 0.5f, 0.12f)
        g.sad = 0.0
        g.showFace(Face.CHEER)
        g.say("It's my BIRTHDAY?! You remembered! ...I didn't even remember!")
    }

    private fun tickSurpriseEvents() {
        // INTEGRATION: the original also pauses events while realm/nightmare/kevinGone/atWar/atSpace/atPirate/
        // atMine/atTherapy; g.screen != STAND already covers most of those (being on another screen), but
        // realm/nightmare/kevinGone don't exist in Game.kt yet - add them to this condition once they do.
        if (g.screen != Screen.STAND || g.inRealm()) return
        eventT++
        if (eventT < 75) return
        eventT = 0
        val weights = doubleArrayOf(3.0, 3.0, 1.0, 3.0, 3.0, 2.0, 2.0, 0.6)
        var r = Random.nextDouble() * weights.sum()
        for (i in weights.indices) { r -= weights[i]; if (r <= 0) { runEvent(i); break } }
    }

    private fun tickCustomerQueue() {
        if (custQueue <= 0) return
        val n = min(25, custQueue)
        custQueue -= n
        repeat(n) { g.money += g.price() }
    }

    // =================== SETTINGS ===================
    fun setGender(kind: String) {
        g.gender = kind
        val want = if (kind == "girl") "Kevia" else "Kevin"
        val other = if (kind == "girl") "Kevin" else "Kevia"
        if (g.name == other) { g.name = want; g.say(if (kind == "girl") "Hi! I'm $want! Like my bow?" else "Hi! I'm $want!") }
    }

    // =================== SECRET CODES ===================
    fun tryHallCode(input: String): Boolean {
        val ok = input.trim().lowercase() == "hall"
        if (ok) { usedHall = true; checkTrophies() }
        return ok
    }

    fun giveCash(kind: String, amt: Long): String {
        if (amt < 1) return "Type a number, like 1000."
        when (kind) {
            "both" -> { g.war.bonds += amt; g.money += amt; return "You got ${fmt(amt)} AND ${amt} war bonds! Wow!" }
            "bonds" -> { g.war.bonds += amt; return "You got $amt war bonds! Go win that war." }
            else -> { g.money += amt; return "You got ${fmt(amt)}! Spend it wisely." }
        }
    }

    /** the MAX OUT secret code: the core parts (every regular + lemon-catching upgrade), then every
     * feature's own maxOut(). INTEGRATION: most features are still stubs, so their maxOut() is a no-op -
     * see the final report for what each one still needs to do (war/space/pirate/dig shops, traveling shop,
     * closet, wall archers, etc., matching the original's maxEverything()). */
    fun maxEverything() {
        for (u in Upgrades.all) g.lvl[u.id] = u.max
        for (u in LemonUpgrades.all) g.lemonLvl[u.id] = u.max
        g.lemonsLeft = g.lemonCap()
        for (f in g.features) if (f !== this) f.maxOut()
        g.sad = 0.0
        g.showFace(Face.CHEER)
        g.say("WHOA. Everything is MAXED?! This stand is UNSTOPPABLE!")
        g.save()
    }

    // =================== Feature ===================
    override fun save(j: JSONObject) {
        j.put("rebirths", rebirths)
        j.put("wonJackpot", wonJackpot)
        j.put("usedHall", usedHall)
        j.put("lemyEver", lemyEver)
        j.put("gotTrophy", JSONArray(gotTrophy))
        j.put("gotSecret", JSONArray(gotSecret))
        j.put("bettors", JSONArray(bettors.map { it.hired }))
        j.put("cute", g.cute); j.put("og", g.og); j.put("gender", g.gender)
        val talkJ = JSONObject(); for ((k, v) in g.talk) talkJ.put(k, v)
        j.put("talk", talkJ)
    }

    override fun load(j: JSONObject) {
        rebirths = j.optInt("rebirths", 0)
        wonJackpot = j.optBoolean("wonJackpot", false)
        usedHall = j.optBoolean("usedHall", false)
        lemyEver = j.optBoolean("lemyEver", false)
        j.optJSONArray("gotTrophy")?.let { a -> gotTrophy.clear(); for (i in 0 until a.length()) gotTrophy.add(a.getInt(i)) }
        j.optJSONArray("gotSecret")?.let { a -> gotSecret.clear(); for (i in 0 until a.length()) gotSecret.add(a.getInt(i)) }
        j.optJSONArray("bettors")?.let { a -> for (i in 0 until min(a.length(), bettors.size)) bettors[i].hired = a.getBoolean(i) }
        g.cute = j.optBoolean("cute", false)
        g.og = j.optBoolean("og", false)
        g.gender = j.optString("gender", "boy")
        j.optJSONObject("talk")?.let { o -> o.keys().forEach { k -> g.talk[k] = o.getBoolean(k) } }
    }

    /** a brand new game: wipe everything Fun owns */
    override fun reset() {
        rebirths = 0
        wonJackpot = false; usedHall = false; lemyEver = false
        gotTrophy.clear(); gotSecret.clear()
        for (b in bettors) { b.hired = false; b.t = 0; b.log = "Ready to bet!" }
        lottoCells.clear(); lottoOpen.clear(); hasCard = false; lottoDone = true
        lottoMsg = "Buy a ticket and scratch it!"
        strandedLimes = 0
        racing = false; raceMsg = ""; for (i in racePos.indices) racePos[i] = 0f
        custQueue = 0
        g.cute = false; g.og = false; g.gender = "boy"
        for (k in g.talk.keys.toList()) g.talk[k] = true
    }

    /** a rebirth keeps everything Fun owns (trophies, rebirths count, lottery/race history, settings) */
    override fun onRebirth() {}

    override fun maxOut() {}

    override fun tick() {
        checkTrophies()
        tickBettors()
        tickCustomerQueue()
        tickSurpriseEvents()
    }
}

@Composable
fun LottoScreen(g: GameViewModel) = LottoScreenImpl(g)

@Composable
fun RaceScreen(g: GameViewModel) = RaceScreenImpl(g)

@Composable
fun TrophyScreen(g: GameViewModel) = TrophyScreenImpl(g)

@Composable
fun RebirthScreen(g: GameViewModel) = RebirthScreenImpl(g)

@Composable
fun CodesScreen(g: GameViewModel) = CodesScreenImpl(g)

@Composable
fun SettingsScreen(g: GameViewModel) = SettingsScreenImpl(g)
