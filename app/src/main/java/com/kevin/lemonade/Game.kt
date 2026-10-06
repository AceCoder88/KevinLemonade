package com.kevin.lemonade

import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.random.Random

enum class Face { HAPPY, CHEER, WORRY, GASP }
enum class Screen { STAND, SHOP, CATCH, TRAVEL, CLOSET, DATE, WAR, SPACE, DIG, PIRATE, RIDE, TOWER, LOTTO, RACE, TROPHY, REBIRTH, CODES, SETTINGS, THERAPY }

/** Floating text on the stage. x/y are fractions of the stage box. */
data class Pop(val id: Long, val text: String, val x: Float, val y: Float, val color: Color, val ms: Long = 1500)

fun <T> List<T>.pick(): T = this[Random.nextInt(size)]
fun fmt(n: Long): String = "\$" + "%,d".format(n)

class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("kevin_save", Context.MODE_PRIVATE)

    // ---------------- saved game state ----------------
    var name by mutableStateOf("Kevin")
    var money by mutableLongStateOf(0L)
    var lemonsLeft by mutableIntStateOf(20)
    var glasses by mutableIntStateOf(0)
    /** lemons squeezed since Kevin was last happy */
    var sad by mutableDoubleStateOf(0.0)
    var inPitcher by mutableIntStateOf(0)
    val lvl = mutableStateMapOf<String, Int>()
    val lemonLvl = mutableStateMapOf<String, Int>()

    // ---------------- what's on screen ----------------
    var screen by mutableStateOf(Screen.STAND)
    var bubble by mutableStateOf("Hi! I'm Kevin. What are we making today?")
    var bubbleBump by mutableIntStateOf(0)
    var face by mutableStateOf(Face.HAPPY)
        private set
    var look by mutableStateOf(Offset(170f, 276f))
    var juiceLevel by mutableFloatStateOf(0f)
    var dropVisible by mutableStateOf(false)
    var dropY by mutableFloatStateOf(0f)
    var dropSx by mutableFloatStateOf(1f)
    var dropSy by mutableFloatStateOf(1f)
    var dropColor by mutableStateOf(PEEL)
    var dropGlow by mutableStateOf<Color?>(null)
    var plungerY by mutableFloatStateOf(0f)
    var crankAngle by mutableFloatStateOf(0f)
    var streamOn by mutableStateOf(false)
    var splash by mutableFloatStateOf(0f)
    var splashColor by mutableStateOf(JUICE)
    var kevinDx by mutableFloatStateOf(0f)
    var kevinDy by mutableFloatStateOf(0f)
    var kevinRot by mutableFloatStateOf(0f)
    var armRot by mutableFloatStateOf(0f)
    var lid by mutableFloatStateOf(0f)
    var holdProgress by mutableFloatStateOf(0f)
    var busy by mutableStateOf(false)
    var cranking by mutableStateOf(false)
    var crankN by mutableIntStateOf(0)
    var crankNeed by mutableIntStateOf(0)
    var catchNudge by mutableIntStateOf(0)
    var moneyBump by mutableIntStateOf(0)
    var zestSays by mutableStateOf(Lines.zestHello.first())
    var partyUntil by mutableLongStateOf(0L)
    val pops = mutableStateListOf<Pop>()
    val hearts = mutableStateListOf<Pop>()

    private var popId = 0L
    private var taps = 0
    private val pokeTimes = ArrayList<Long>()
    private var holdJob: Job? = null
    private var holdStart = 0L
    private var crankDone: CompletableDeferred<Unit>? = null
    private var crankStart = 0L
    private var bag = ArrayList<Relative>()
    private var current = Lines.relatives[0]
    private var toastJob: Job? = null

    // ---------------- the rest of the game, one object per chunk ----------------
    // (declared before init so load() can fill them)
    val closet = ClosetState(this)
    val dating = DatingState(this)
    val travel = TravelState(this)
    val war = WarState(this)
    val space = SpaceState(this)
    val dig = DigState(this)
    val pirate = PirateState(this)
    val ride = RideState(this)
    val tower = TowerState(this)
    val therapy = TherapyState(this)
    val fun_ = FunState(this)
    val realm = RealmState(this)
    val features: List<Feature> = listOf(closet, dating, travel, war, space, dig, pirate, ride, tower, therapy, fun_, realm)

    /** a short message at the bottom of the screen (the original's toast()) */
    var toastText by mutableStateOf<String?>(null)
    fun toast(text: String) {
        toastText = named(text)
        toastJob?.cancel()
        toastJob = viewModelScope.launch { delay(3200); toastText = null }
    }

    /** true while Kevin is in the Death Realm or the Nightmare Realm (the original's realm || nightmare) */
    fun inRealm(): Boolean = realm.active || realm.nightmare

    /** limes: a lemon stand-in that cheers Kevin up instead of making him sad (from Be a Pirate) */
    var limesLeft by mutableIntStateOf(0)
    /** true while a finger is down on Kevin (so checkGray doesn't turn him evil mid-tap) */
    var holdingKevin by mutableStateOf(false)
    /** which variant of War/Dig is open: "normal", or themed from a Death Realm / Nightmare Realm extra */
    var warTheme by mutableStateOf("normal")
    var mineTheme by mutableStateOf("normal")

    // ---------------- shared state the original kept at the top of its script ----------------
    // Several features set these (surprise events, codes, the admin panel, the traveling shop, the
    // wizard tower...) and others read them (the squeeze, earn(), war). Same names as the original.
    // Timers are SystemClock.uptimeMillis() deadlines, like partyUntil; "now < xUntil" means active.
    var frenzyUntil by mutableLongStateOf(0L)
    var megaUntil by mutableLongStateOf(0L)
    var freeLemonsUntil by mutableLongStateOf(0L)
    var robotRushUntil by mutableLongStateOf(0L)
    var happyUntil by mutableLongStateOf(0L)
    var stormUntil by mutableLongStateOf(0L)
    var moneyTreeUntil by mutableLongStateOf(0L)
    var bondPrinterUntil by mutableLongStateOf(0L)
    var goldBondsUntil by mutableLongStateOf(0L)
    var superTowersUntil by mutableLongStateOf(0L)
    /** the original's window.__rainbow: double money until then */
    var rainbowUntil by mutableLongStateOf(0L)
    /** the original's window.__speedUntil / __speedMult: squeezing speed boost */
    var speedUntil by mutableLongStateOf(0L)
    var speedMult by mutableDoubleStateOf(2.0)
    /** the next this-many squeezes are guaranteed golden */
    var goldenNext by mutableIntStateOf(0)
    /** extra wall health for the war */
    var wallBonus by mutableIntStateOf(0)
    /** archer windows filled on the war wall (0..6) */
    var wallArchers by mutableIntStateOf(0)
    /** Death Realm visits so far (doom() grows with these) */
    var visits by mutableIntStateOf(0)
    var gender by mutableStateOf("boy")
    /** Settings: what Kevin talks about */
    val talk = mutableStateMapOf("lemons" to true, "upgrades" to true, "outfits" to true, "phone" to true, "poke" to true, "bubble" to true)
    /** looks: cute mode, OG mode (looks like the very first version), and the disco / upside-down codes */
    var cute by mutableStateOf(false)
    var og by mutableStateOf(false)
    var disco by mutableStateOf(false)
    var upside by mutableStateOf(false)
    /** admin panel: open every tool lock without owning the tool */
    var bypassLocks by mutableStateOf(false)
    fun luck() = 0.04 * (lv("clover") + lv("horseshoe") + lv("rabbitfoot"))
    fun dodge() = 0.06 * lv("dodgeluck")
    fun now() = SystemClock.uptimeMillis()

    fun lv(id: String) = lvl[id] ?: 0
    fun llv(id: String) = lemonLvl[id] ?: 0

    // ---------------- the original game's formulas ----------------
    fun lemonCap() = 20 + 20 * llv("cap")
    fun price(): Long = ((2 + lv("sugar") + lv("zest") + 5 * lv("truck") + 10 * lv("register") +
            (if (closet.wearing("tie")) 3 else 0) + (if (closet.wearing("pirateboots")) 2 else 0)) *
            2.0.pow(lv("ice")) * (1 + 0.25 * lv("sign")) * (1 + 0.5 * lv("franchise")) * (1 + 0.1 * lv("awning")) *
            (if (closet.wearing("tophat")) 1.25 else 1.0) * (if (closet.wearing("tuxedo")) 1.2 else 1.0) *
            (if (closet.wearing("piratecoat")) 1.1 else 1.0) * (if (closet.wearingSet("fancy")) 1.5 else 1.0) *
            (if (closet.wearingSet("summer")) 1.75 else 1.0) *
            (1 + 0.15 * lv("bigcups")) * (1 + 0.2 * lv("neon"))).roundToLong()
    fun need() = max(1, 5 - lv("juicy") - (if (closet.wearing("chef")) 1 else 0) - (if (closet.wearingSet("baker")) 1 else 0))
    fun speed() = 1.3.pow(lv("fast")) * 1.15.pow(lv("turbo")) * (if (closet.wearing("sneakers")) 1.2 else 1.0) *
            (if (now() < speedUntil) speedMult else 1.0)
    private fun sd(ms: Double) = ms / speed()
    fun autoEvery(): Double = listOf(0, 4000, 3000, 2000, 1400, 900, 650, 450)[lv("auto")] /
            (if (closet.wearing("skates")) 2.0 else 1.0) / (if (closet.wearingSet("cowboy")) 1.3 else 1.0) /
            (1 + 0.25 * lv("hydraulic"))
    fun doom() = (30.0 + 12 * lv("therapy") + 6 * (1 + lv("snacks")) * min(visits, 5)) *
            (if (closet.wearing("shades")) 2.0 else 1.0) * (1 + 0.1 * lv("chair")) *
            (if (closet.wearing("sweater")) 1.3 else 1.0) * (if (closet.wearingSet("winter")) 3.0 else 1.0) *
            (1 + 0.15 * lv("games"))
    fun sadFrac() = min(1.0, sad / doom())
    fun stage() = if (sad / doom() < 0.34) 0 else if (sad / doom() < 0.67) 1 else 2
    fun partyOn() = SystemClock.uptimeMillis() < partyUntil
    /** the price a pitcher sells for right now, before Kevin's mood, rebirth, Lemy, party or rainbow bonuses */
    fun earnBase(): Long {
        if (now() < megaUntil) return price() * 10
        if (now() < frenzyUntil) return price() * 3
        if (realm.kevinGone) return price()
        val full = price()
        return when (stage()) { 0 -> full; 1 -> max(1L, Math.round(full / 2.0)); else -> 1L }
    }
    fun earn(): Long = Math.round(
        fun_.rebirthMult() * earnBase() * (if (dating.momoWith) 2 else 1) *
            (if (partyOn()) 3 else 1) * (if (now() < rainbowUntil) 2 else 1)
    )
    fun named(s: String) = s.replace("{N}", name)

    init {
        load()
        if (bubble.contains("Kevin") && name != "Kevin") bubble = "Hi! I'm $name. What are we making today?"
        viewModelScope.launch { ticker() }
        viewModelScope.launch { blinker() }
    }

    // ---------------- talking & faces ----------------
    fun say(text: String) { bubble = named(text); bubbleBump++ }

    fun showFace(f: Face) {
        face = if (f == Face.CHEER && stage() >= 1) Face.HAPPY else f
    }

    private fun story(part: String): String {
        val st = stage()
        val r = current
        val tpl = when {
            part == "full" -> Lines.full[st].pick()
            st == 0 -> return when (part) { "drop" -> r.drop; "squish" -> r.squish; else -> r.after }
            part == "drop" -> Lines.sadDrop[st - 1].pick()
            part == "squish" -> Lines.sadSquish[st - 1].pick()
            else -> Lines.sadAfter[st - 1].pick()
        }
        return tpl.replace("{Rc}", r.n.replaceFirstChar { it.uppercase() }).replace("{R}", r.n)
    }

    private fun nextRelative() {
        if (bag.isEmpty()) bag = ArrayList(Lines.relatives.shuffled())
        current = bag.removeAt(bag.size - 1)
    }

    fun popAt(text: String, x: Float = 0.68f, y: Float = 0.5f, color: Color = Color(0xFF3E9B4F), ms: Long = 1500) {
        val p = Pop(popId++, text, x, y, color, ms)
        pops.add(p)
        viewModelScope.launch { delay(ms); pops.remove(p) }
    }

    private fun heart() {
        val h = Pop(popId++, "\u2665", 0.14f + Random.nextFloat() * 0.14f, 0.42f + Random.nextFloat() * 0.15f, Color(0xFFFF4D8D), 1000)
        hearts.add(h)
        viewModelScope.launch { delay(1000); hearts.remove(h) }
    }

    // ---------------- little animations ----------------
    private suspend fun tween(ms: Double, block: (Float) -> Unit) {
        val total = max(1.0, ms)
        val start = SystemClock.uptimeMillis()
        while (true) {
            val t = ((SystemClock.uptimeMillis() - start) / total).toFloat().coerceIn(0f, 1f)
            block(t)
            if (t >= 1f) break
            delay(16)
        }
    }
    private fun easeIn(t: Float) = t * t
    private fun easeOut(t: Float) = 1 - (1 - t) * (1 - t)
    /** evenly spaced keyframes */
    private fun kf(t: Float, vararg v: Float): Float {
        val seg = (v.size - 1) * t
        val i = min(v.size - 2, floor(seg).toInt())
        val u = seg - i
        return v[i] + (v[i + 1] - v[i]) * u
    }

    internal fun jump() {
        if (stage() >= 1) return
        viewModelScope.launch { tween(450.0) { kevinDy = -22f * sin(PI.toFloat() * easeOut(it)) } ; kevinDy = 0f }
        viewModelScope.launch { tween(600.0) { armRot = kf(it, 0f, -30f, 10f, 0f) } }
    }
    internal fun shiver() {
        viewModelScope.launch { repeat(3) { tween(180.0) { kevinDx = kf(it, 0f, -4f, 4f, 0f) } }; kevinDx = 0f }
    }
    private fun dance() {
        viewModelScope.launch {
            tween(1400.0) {
                kevinRot = kf(it, 0f, -14f, 14f, -14f, 14f, 0f)
                kevinDy = kf(it, 0f, -14f, 0f, -14f, 0f, 0f)
                armRot = kf(it, 0f, -50f, 20f, -50f, 0f)
            }
            kevinRot = 0f; kevinDy = 0f; armRot = 0f
        }
    }
    private suspend fun blinker() {
        while (true) {
            delay(2500L + Random.nextLong(3500))
            tween(220.0) { lid = kf(it, 0f, 1f, 0f) }
            lid = 0f
        }
    }
    private suspend fun setLevel(target: Float, ms: Double) {
        val start = juiceLevel
        tween(ms) { juiceLevel = start + (target - start) * it }
    }

    fun lookAtTouch(p: Offset) { if (!busy) look = p }

    // ---------------- squeezing ----------------
    fun mainButton() {
        if (cranking) { crankTap(); return }
        if (realm.nightmare) { realm.noEscape(); return }
        if (realm.active) { realm.leaveRealm(); return }
        squeeze(true)
    }

    fun squeeze(byHand: Boolean) {
        if (busy) return
        if (realm.nightmare) { realm.noEscape(); return }
        if (realm.active) { realm.leaveRealm(); return }
        val isLime = limesLeft > 0
        if (!isLime && lemonsLeft <= 0) {
            say(Lines.outOfLemons.pick())
            catchNudge++
            return
        }
        busy = true
        viewModelScope.launch {
            try { squeezeFlow(byHand, isLime) } finally { busy = false; cranking = false }
        }
    }

    private suspend fun squeezeFlow(byHand: Boolean, isLime: Boolean) {
        if (isLime) limesLeft--
        else if (lv("lucky") > 0 && Random.nextDouble() < 0.1 * lv("lucky")) popAt("Lucky! Free lemon", 0.30f, 0.20f)
        else lemonsLeft--
        // the Mega hopper squeezes extra lemons (or limes) at the same time
        var extra = 0; var extraLimes = 0
        repeat(lv("multisq")) {
            if (limesLeft > 0) { limesLeft--; extra++; extraLimes++ }
            else if (lemonsLeft > 0) { lemonsLeft--; extra++ }
        }
        if (extra > 0) popAt("${extra + 1} lemons at once!", 0.52f, 0.20f)
        // after a rebirth, a second full-size squeezer squeezes another lemon (or lime) at the same time
        var twinLime = false; var twin = false
        if (fun_.rebirths >= 1) {
            if (limesLeft > 0) { limesLeft--; twin = true; twinLime = true }
            else if (lemonsLeft > 0) { lemonsLeft--; twin = true }
            if (twin) popAt("Extra squeeze!", 0.46f, 0.22f)
        }

        look = Offset(420f, 60f)
        nextRelative()
        val golden = (lv("golden") > 0 && Random.nextDouble() < 0.05 * lv("golden")) ||
                (lv("goldluck") > 0 && Random.nextDouble() < 0.03 * lv("goldluck"))
        val diamond = !golden && !isLime && lv("diamondluck") > 0 && Random.nextDouble() < 0.015 * lv("diamondluck")
        dropColor = if (golden) Color(0xFFFFC400) else if (diamond) Color(0xFFA8EEFF) else if (isLime) Color(0xFF7BC043) else PEEL
        dropGlow = if (golden) Color(0xFFFFE066) else if (diamond) Color(0xFFDFF8FF) else null
        if (isLime) { showFace(Face.CHEER); say(Lines.LimeLines.drop.pick()) }
        else { showFace(Face.WORRY); say(story("drop")); shiver() }

        // the lemon falls in and bounces
        dropSx = 1f; dropSy = 1f; dropVisible = true
        tween(sd(800.0)) { t ->
            dropY = when {
                t < 0.75f -> -280f + 280f * easeIn(t / 0.75f)
                t < 0.88f -> -14f * easeOut((t - 0.75f) / 0.13f)
                else -> -14f * (1 - easeIn((t - 0.88f) / 0.12f))
            }
        }
        dropY = 0f
        look = Offset(420f, 218f)
        delay(sd(250.0).toLong())

        showFace(if (isLime) Face.CHEER else Face.GASP)
        say(when {
            isLime -> Lines.LimeLines.squish.pick()
            golden -> "Whoa, ${current.n} is GOLDEN?!"
            diamond -> "${current.n} is made of DIAMOND?! Since WHEN?!"
            else -> story("squish")
        })
        val handCrank = byHand && lv("motor") == 0
        if (handCrank) crankByHand()

        val pressMs = sd(if (handCrank) 80.0 else 220.0)
        val p0 = plungerY; val sx0 = dropSx; val sy0 = dropSy
        tween(pressMs) { t ->
            val e = easeIn(t)
            plungerY = p0 + (40f - p0) * e
            dropSx = sx0 + (1.35f - sx0) * e
            dropSy = sy0 + (0.3f - sy0) * e
        }
        juiceSplash(if (golden) Color(0xFFFFC400) else if (diamond) Color(0xFFDFF8FF) else JUICE)

        look = Offset(518f, 360f)
        streamOn = true
        val per = 1 + lv("press")
        if (lv("doubleplunge") > 0 && Random.nextDouble() < 0.12 * lv("doubleplunge")) {
            inPitcher += per; popAt("Double squeeze!", 0.48f, 0.25f)
        }
        if (isLime) sad = max(0.0, sad - 1.0) else if (now() >= happyUntil) sad = min(doom(), sad + 1.0)
        inPitcher += per
        if (extra > 0) {
            inPitcher += per * extra
            sad = max(0.0, sad - extraLimes)
            if (now() >= happyUntil) sad = min(doom(), sad + (extra - extraLimes))
        }
        if (twin) {
            inPitcher += per
            if (twinLime) sad = max(0.0, sad - 1.0) else if (now() >= happyUntil) sad = min(doom(), sad + 1.0)
        }
        if (diamond) { val b = price() * 30; money += b; popAt("+${fmt(b)} DIAMOND!", 0.55f, 0.30f, Color(0xFF2E9BD6), 1800) }
        if (golden) { val b = price() * 10; money += b; popAt("+${fmt(b)} GOLDEN!", 0.55f, 0.30f, Color(0xFFE9A800)) }
        setLevel(min(MAX_H, MAX_H * inPitcher / need()), sd(700.0))
        streamOn = false

        tween(sd(300.0)) { plungerY = 40f * (1 - easeOut(it)) }
        plungerY = 0f
        dropVisible = false; dropSx = 1f; dropSy = 1f; dropY = 0f; dropGlow = null

        val sold = inPitcher >= need()
        if (sold) {
            inPitcher = 0
            showFace(Face.CHEER); say(story("full")); jump()
            sellPitcher()
            delay(sd(900.0).toLong())
            setLevel(0f, sd(600.0))
            delay(sd(300.0).toLong())
        }
        showFace(Face.HAPPY)
        if (!sold) say(if (isLime) Lines.LimeLines.after.pick() else if (golden) "We're RICH! ...I still feel weird about it." else story("after"))
        else realm.newGhostMsg?.let { say("Wait... I think $it just showed up in the Death Realm."); realm.newGhostMsg = null }
    }

    /** juice from anywhere other than the main squeezer (the extra squeezers upgrade); sells a full pitcher */
    private fun addJuice(n: Int) {
        inPitcher += n
        juiceLevel = min(MAX_H, MAX_H * inPitcher / need())
        if (inPitcher >= need() && !busy) {
            inPitcher = 0
            sellPitcher()
            viewModelScope.launch { delay(300); if (!busy && inPitcher == 0) setLevel(0f, 400.0) }
        }
    }

    private fun juiceSplash(c: Color) {
        splashColor = c
        viewModelScope.launch { tween(600.0) { splash = it }; splash = 0f }
    }

    /** without the motor, YOU crank the squeezer by hand: tap fast! */
    private suspend fun crankByHand() {
        crankNeed = max(2, 8 - lv("arms"))
        crankN = 0
        crankStart = SystemClock.uptimeMillis()
        val done = CompletableDeferred<Unit>()
        crankDone = done
        cranking = true
        done.await()
    }

    fun crankTap() {
        if (!cranking) return
        crankN++
        val f = crankN.toFloat() / crankNeed
        plungerY = 40f * f
        crankAngle = crankN * 75f
        dropSx = 1 + 0.35f * f
        dropSy = 1 - 0.7f * f
        if (crankN >= crankNeed) {
            cranking = false
            if (SystemClock.uptimeMillis() - crankStart < crankNeed * 220L) {
                inPitcher += 1
                popAt("SUPER SQUEEZE! Extra juice!", 0.5f, 0.22f)
            }
            crankDone?.complete(Unit)
        }
    }

    private fun sellPitcher() {
        glasses++
        if (glasses % 10 == 0) popAt("$glasses pitchers sold! WOO!", 0.5f, 0.12f, Color(0xFFE8483B), 2000)
        if (lv("music") > 0) sad = max(0.0, sad - 1.5 * lv("music"))
        var p = earn()
        if (Random.nextDouble() < 0.06 * lv("vip") + 0.04 * lv("vipluck")) { p *= 5; popAt("VIP customer! x5", 0.6f, 0.3f) }
        money += p
        if (Random.nextDouble() < 0.12 * lv("tips") + 0.06 * lv("tipluck")) {
            val tip = max(1L, Math.round(p * 1.5))
            money += tip
            viewModelScope.launch { delay(350); popAt("+${fmt(tip)} tip!", 0.82f, 0.5f) }
        }
        popAt("+" + fmt(p))
        moneyBump++
        realm.maybeSpawnGhost()
        save()
    }

    // ---------------- poking & holding Kevin ----------------
    private fun therapyTap(): Boolean {
        if (realm.nightmare || realm.active || realm.kevinGone || sad <= 0) return false
        taps++
        sad = max(0.0, sad - TAP_HEAL * (if (closet.wearing("clownnose")) 2 else 1))
        heart()
        if (!busy && taps % 6 == 1) say(if (sad <= 0) "I feel GREAT! Thanks, buddy." else Lines.therapyTap.pick())
        if (!busy) showFace(Face.HAPPY)
        return true
    }

    private fun poke() {
        // tap Kevin 6 times fast for his secret dance!
        val now = SystemClock.uptimeMillis()
        pokeTimes.removeAll { now - it >= 2500 }
        pokeTimes.add(now)
        if (pokeTimes.size >= 6 && !busy && !realm.active && !realm.nightmare) {
            pokeTimes.clear()
            dance()
            sad = max(0.0, sad - 3)
            showFace(Face.CHEER)
            say(Lines.dance.pick())
            return
        }
        if (therapyTap()) return
        if (busy) return
        if (realm.nightmare) return
        if (realm.active) {
            showFace(Face.CHEER); say(Lines.realmPoke.pick()); jump()
            viewModelScope.launch { delay(700); showFace(Face.HAPPY) }
            return
        }
        showFace(Face.CHEER)
        say(Lines.poke[stage()].pick())
        jump()
        viewModelScope.launch { delay(700); if (!busy) showFace(Face.HAPPY) }
    }

    /** the clown outfit set earns Kevin a tip every time you tap or hold him */
    private fun clownTip() {
        if (!closet.wearingSet("clown")) return
        val tip = max(2L, Math.round(price() / 2.0))
        money += tip
        popAt("Honk! +${fmt(tip)}", 0.22f, 0.30f)
    }

    fun kevinDown() {
        clownTip()
        if (busy && !realm.nightmare && !realm.active) { therapyTap(); return }
        if (busy || realm.nightmare || holdStart != 0L) return
        holdStart = SystemClock.uptimeMillis()
        holdingKevin = true
        holdJob = viewModelScope.launch {
            var lastLine = -1
            while (true) {
                val held = SystemClock.uptimeMillis() - holdStart
                if (held > TAP_MS) {
                    val t = min(1f, (held - TAP_MS) / HOLD_MS.toFloat())
                    holdProgress = t
                    val idx = min(4, floor(t * 5).toInt())
                    if (idx != lastLine) {
                        lastLine = idx
                        showFace(if (idx < 2) Face.WORRY else Face.GASP)
                        say(Lines.hold[idx]); shiver()
                    }
                    if (t >= 1f) {
                        holdStart = 0L; holdingKevin = false; holdJob = null; holdProgress = 0f
                        realm.squeezeFriend()
                        return@launch
                    }
                }
                delay(16)
            }
        }
    }

    fun kevinUp() {
        if (holdStart == 0L) return
        val held = SystemClock.uptimeMillis() - holdStart
        holdStart = 0L
        holdingKevin = false
        holdJob?.cancel(); holdJob = null
        holdProgress = 0f
        if (held <= TAP_MS) poke()
        else { showFace(Face.HAPPY); say(Lines.letGo.pick()) }
    }

    // ---------------- shop ----------------
    fun upgradeVisible(u: Upgrade): Boolean {
        // spooky upgrades are only for sale while visiting the Death Realm
        if (u.cat == Cat.SPOOKY) return realm.active
        if (u.id in Upgrades.needMotor && lv("motor") == 0) return false
        if (u.id == "arms" && lv("motor") > 0) return false
        return true
    }

    fun upgradeDesc(u: Upgrade): String =
        if (u.id == "press") Upgrades.pressDesc.getOrElse(lv("press")) { "The best squeezer there is" } else named(u.desc)

    fun buy(u: Upgrade) {
        val c = u.cost(lv(u.id))
        if (money < c || lv(u.id) >= u.max) return
        money -= c
        lvl[u.id] = lv(u.id) + 1
        say(u.say)
        if (u.id == "therapy") showFace(Face.HAPPY)
        if (u.id == "girlfriend" && !realm.kevinGone && !realm.nightmare) dating.momoWith = true
        zestSays = Lines.zestBuy.pick()
        save()
    }

    fun buyLemon(u: LemonUpgrade) {
        val c = u.cost(llv(u.id))
        if (money < c || llv(u.id) >= u.max) return
        if (u.id == "hspeed" && llv("helper") == 0) return
        money -= c
        lemonLvl[u.id] = llv(u.id) + 1
        save()
    }

    fun addLemons(n: Int): Int {
        val before = lemonsLeft
        lemonsLeft = min(lemonCap(), lemonsLeft + n)
        return lemonsLeft - before
    }

    fun openShop() { zestSays = Lines.zestHello.pick(); screen = Screen.SHOP }
    fun pokeZest() { zestSays = Lines.zestPoke.pick() }

    // ---------------- the puppy, and clouds that rain lemons ----------------
    private var puppyPetCd = 0L
    fun petPuppy() {
        if (now() < puppyPetCd) return
        puppyPetCd = now() + 2500
        sad = max(0.0, sad - 0.5)
        popAt("Woof! Woof!", 0.54f, 0.68f)
        if (talk["poke"] == true && !busy) say(listOf(
            "Who's a good boy? ZESTY is!", "Zesty! Did you just lick the squeezer? Gross. Good boy.",
            "Zesty, sit! ...Zesty, stand! He does whatever he wants.", "My puppy is the best puppy in the whole galaxy.",
        ).pick())
    }

    private val cloudCooldown = LongArray(3)
    fun rainCloud(index: Int) {
        if (realm.active || realm.nightmare || index !in cloudCooldown.indices || now() < cloudCooldown[index]) return
        cloudCooldown[index] = now() + 20000
        if (lemonsLeft < lemonCap()) { lemonsLeft++; popAt("The cloud rained a lemon! +1", 0.5f, 0.18f) }
        else popAt("Pitter patter! (lemon storage is full)", 0.5f, 0.18f)
    }

    // ---------------- things that happen over time ----------------
    /** screens where the original paused the auto-squeezer and the gray-out countdown */
    private fun inAnotherWorld() = screen in setOf(Screen.CATCH, Screen.WAR, Screen.SPACE, Screen.PIRATE, Screen.DIG, Screen.RIDE)

    private suspend fun ticker() {
        var lastAuto = SystemClock.uptimeMillis()
        var farmAcc = 0L
        var secAcc = 0L
        var sec = 0L
        var factoryT = 0; var bankT = 0; var fanT = 0; var rainT = 0; var hugT = 0; var partyT = 0
        var deliveryT = 0; var napT = 0; var billT = 0
        val squeezerT = DoubleArray(3)
        while (true) {
            delay(100)
            val now = SystemClock.uptimeMillis()
            if (inAnotherWorld()) { lastAuto += 100 }
            else {
                if (lv("auto") > 0 && !busy && holdStart == 0L && !realm.active && !realm.nightmare &&
                    (lemonsLeft > 0 || limesLeft > 0) && now - lastAuto >= autoEvery()
                ) { lastAuto = now; squeeze(false) }
                // the Extra squeezer upgrade: 1-3 little squeezers that work all by themselves
                if (lv("squeezers") > 0 && !realm.active && !realm.nightmare) {
                    for (i in 0 until lv("squeezers")) {
                        squeezerT[i] += 0.1
                        if (squeezerT[i] < 5.5 / speed()) continue
                        if (limesLeft <= 0 && lemonsLeft <= 0) continue
                        squeezerT[i] = 0.0
                        val miniLime = limesLeft > 0
                        if (miniLime) limesLeft-- else lemonsLeft--
                        if (miniLime) sad = max(0.0, sad - 0.5) else sad = min(doom(), sad + 0.5)
                        addJuice(1 + lv("press"))
                    }
                }
            }
            // lemon farm
            if (lv("farm") > 0) {
                farmAcc += 100
                if (farmAcc >= (12 - 2 * min(lv("farm"), 5)) * 1000 / (1 + 0.5 * lv("greenhouse"))) {
                    farmAcc = 0
                    if (lemonsLeft < lemonCap()) lemonsLeft++
                }
            }
            if (!inAnotherWorld()) realm.checkGray(now)
            secAcc += 100
            if (secAcc < 1000) continue
            secAcc = 0; sec++

            if (lv("billboard") > 0 && ++billT >= 5) { billT = 0; money += max(1L, Math.round(price() * 0.3 * lv("billboard"))) }
            if (lv("factory") > 0 && ++factoryT >= 32 - 5 * lv("factory")) { factoryT = 0; money += price() }
            if (lv("bank") > 0 && ++bankT >= 10) { bankT = 0; money += min(100000L, (money * 0.01 * lv("bank")).toLong()) }
            if (lv("fanclub") > 0 && stage() == 0 && ++fanT >= 4) { fanT = 0; money += 5L * lv("fanclub") * max(1L, Math.round(price() / 4.0)) }
            if (lv("lemonrain") > 0 && ++rainT >= 90) {
                rainT = 0
                if (addLemons(10 * lv("lemonrain")) > 0) popAt("Lemon rain! +${10 * lv("lemonrain")} lemons", 0.5f, 0.15f)
            }
            if (lv("hugs") > 0 && ++hugT >= 20) { hugT = 0; sad = max(0.0, sad - 1.5 * lv("hugs")); popAt("Hug!", 0.22f, 0.40f, Color(0xFFFF4D8D)) }
            if (lv("party") > 0 && ++partyT >= 180 - 30 * (lv("party") - 1)) {
                partyT = 0; partyUntil = SystemClock.uptimeMillis() + 30000; popAt("PARTY! Triple money!", 0.5f, 0.15f, Color(0xFFE8483B), 2500)
            }
            if (lv("delivery") > 0 && ++deliveryT >= 30) {
                deliveryT = 0
                val got = addLemons(5 * lv("delivery"))
                if (got > 0) popAt("Lemon delivery! +$got", 0.5f, 0.15f)
            }
            if (lv("puppy") > 0 && sad > 0) sad = max(0.0, sad - 0.08 * lv("puppy"))
            if (lv("nap") > 0 && ++napT >= 60) { napT = 0; sad = max(0.0, sad - 3 * lv("nap")) }
            for (f in features) f.tick()
            if (sec % 5 == 0L) save()
        }
    }

    fun sadText(): String {
        val pct = sadFrac()
        return when (stage()) {
            0 -> if (pct < 0.05) "$name is super happy!" else "$name is happy"
            1 -> "$name is a little sad"
            else -> "$name is SUPER sad"
        }
    }

    fun priceText(): String {
        val mood = when (stage()) {
            0 -> "$name is happy, so you get full price"
            1 -> "$name is a little sad, so you only get half"
            else -> "$name is super sad, so you only get \$1"
        }
        val n = need()
        return "A pitcher sells for ${fmt(earn())} and takes $n lemon${if (n > 1) "s" else ""}. $mood."
    }

    // ---------------- save / load / restart ----------------
    fun save() {
        val j = JSONObject()
        j.put("name", name); j.put("money", money); j.put("lemonsLeft", lemonsLeft); j.put("glasses", glasses)
        j.put("sad", sad); j.put("inPitcher", inPitcher)
        j.put("visits", visits); j.put("limesLeft", limesLeft); j.put("gender", gender)
        j.put("cute", cute); j.put("og", og)
        j.put("talk", JSONObject(talk.toMap()))
        j.put("lvl", JSONObject(lvl.toMap())); j.put("lemonLvl", JSONObject(lemonLvl.toMap()))
        for (f in features) j.put(f.key, JSONObject().also { f.save(it) })
        prefs.edit().putString("save", j.toString()).apply()
    }

    private fun load() {
        val s = prefs.getString("save", null) ?: return
        try {
            val j = JSONObject(s)
            name = j.optString("name", "Kevin")
            money = j.optLong("money", 0); lemonsLeft = j.optInt("lemonsLeft", 20); glasses = j.optInt("glasses", 0)
            sad = j.optDouble("sad", 0.0); inPitcher = j.optInt("inPitcher", 0)
            visits = j.optInt("visits", 0); limesLeft = j.optInt("limesLeft", 0); gender = j.optString("gender", "boy")
            cute = j.optBoolean("cute", false); og = j.optBoolean("og", false)
            j.optJSONObject("talk")?.let { o -> o.keys().forEach { talk[it] = o.optBoolean(it, true) } }
            j.optJSONObject("lvl")?.let { o -> o.keys().forEach { lvl[it] = o.getInt(it) } }
            j.optJSONObject("lemonLvl")?.let { o -> o.keys().forEach { lemonLvl[it] = o.getInt(it) } }
            for (f in features) j.optJSONObject(f.key)?.let { o -> try { f.load(o) } catch (_: Exception) { } }
            juiceLevel = min(MAX_H, MAX_H * inPitcher / need())
        } catch (_: Exception) { }
    }

    fun restart() {
        name = "Kevin"; money = 0; lemonsLeft = 20; glasses = 0; sad = 0.0; inPitcher = 0
        visits = 0; limesLeft = 0; warTheme = "normal"; mineTheme = "normal"; bypassLocks = false
        lvl.clear(); lemonLvl.clear(); juiceLevel = 0f; partyUntil = 0
        for (f in features) f.reset()
        showFace(Face.HAPPY)
        say("Hi! I'm Kevin. What are we making today?")
        screen = Screen.STAND
        save()
    }

    override fun onCleared() { save() }

    companion object {
        const val MAX_H = 92f
        const val TAP_HEAL = 0.2
        const val TAP_MS = 350L
        const val HOLD_MS = 5000L
        val PEEL = Color(0xFFFFD21F)
        val JUICE = Color(0xFFFFE94A)
    }
}
