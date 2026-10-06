package com.kevin.lemonade

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

/**
 * The Death Realm (visit Kevin's family of squeezed relatives, or bring Kevin back once he's
 * gone) and the Nightmare Realm (what happens if you hold Kevin too long, or let him go all
 * gray for too long without a visit). Mirrors the original's `realm`/`nightmare`/`kevinGone`
 * globals and the functions around them.
 */
class RealmState(val g: GameViewModel) : Feature {
    override val key = "realm"

    /** in the Death Realm right now */
    var active by mutableStateOf(false)
    /** in the Nightmare Realm right now (evil Kevin) */
    var nightmare by mutableStateOf(false)
    /** Kevin has been squeezed away entirely; the stand runs without him until you bring him back */
    var kevinGone by mutableStateOf(false)
    /** 0 = not counting down; otherwise the uptime millis when Kevin started turning gray */
    var grayAt by mutableLongStateOf(0L)
    /** how many times you've squeezed the shadow-Kevin button down, for the "no escape" button text */
    private var tries = 0
    var lemyMetFamily by mutableStateOf(false)

    // ---- ghosts in the Death Realm ----
    /** how many of the 10 named relatives have shown up so far */
    var famCount by mutableLongStateOf(0L)
    /** every relative beyond the 10 named ones, as an uncounted crowd */
    var famExtra by mutableLongStateOf(0L)
    val famDone = mutableStateListOf(false, false, false, false, false, false, false, false, false, false)
    var newGhostMsg by mutableStateOf<String?>(null)

    // ---- the dead closet (Death Realm only outfits) ----
    val deadOwned = mutableStateMapOf<String, Boolean>()
    var deadWearing by mutableStateOf<String?>(null)

    // ---- holding dark Kevin in the Nightmare Realm ----
    var darkProgress by mutableFloatStateOf(0f)
    private var darkHoldStart = 0L
    private var darkJob: Job? = null

    override fun save(j: JSONObject) {
        j.put("kevinGone", kevinGone); j.put("famCount", famCount); j.put("famExtra", famExtra)
        j.put("lemyMetFamily", lemyMetFamily); j.put("deadWearing", deadWearing ?: "")
        j.put("famDone", JSONArray(famDone))
        j.put("deadOwned", JSONObject(deadOwned.toMap()))
    }
    override fun load(j: JSONObject) {
        kevinGone = j.optBoolean("kevinGone", false)
        famCount = j.optLong("famCount", 0L); famExtra = j.optLong("famExtra", 0L)
        lemyMetFamily = j.optBoolean("lemyMetFamily", false)
        deadWearing = j.optString("deadWearing", "").ifEmpty { null }
        j.optJSONArray("famDone")?.let { a -> for (i in 0 until min(a.length(), famDone.size)) famDone[i] = a.optBoolean(i, false) }
        j.optJSONObject("deadOwned")?.let { o -> o.keys().forEach { deadOwned[it] = o.optBoolean(it, false) } }
        // a realm/nightmare visit in progress never survives a restart - that's a safe place to resume
        active = false; nightmare = false; grayAt = 0L
    }
    override fun reset() {
        active = false; nightmare = false; kevinGone = false; grayAt = 0L; tries = 0; lemyMetFamily = false
        famCount = 0L; famExtra = 0L; for (i in famDone.indices) famDone[i] = false
        deadOwned.clear(); deadWearing = null; darkProgress = 0f; darkHoldStart = 0L; darkJob?.cancel(); darkJob = null
        newGhostMsg = null
    }
    // rebirth keeps the relationship with the Death Realm (visits, the dead closet) going, same as the original

    // ---------------- visiting ----------------
    fun visitCost(): Long = max(1L, 10L - 2L * g.lv("pass"))
    fun ghostPay(): Long = ((50 + 25 * g.lv("ghostpay")) * (if (active && deadWearing != null) 1.5 else 1.0)).roundToLong()
    fun famTotal(): Long = famCount + famExtra

    fun visitLabel(): String = when {
        active || nightmare -> ""
        kevinGone -> "Visit the Death Realm to bring Kevin back (\$$BRING_BACK)"
        else -> "Visit the Death Realm (\$${visitCost()})"
    }
    fun canVisit(): Boolean = !g.busy && !active && !nightmare && !g.therapy.atTherapy &&
        g.money >= (if (kevinGone) BRING_BACK else visitCost())

    fun visit() {
        if (!canVisit()) return
        if (kevinGone) g.viewModelScope.launch { bringBack() } else g.viewModelScope.launch { goVisit() }
    }

    private suspend fun goVisit() {
        val vc = visitCost()
        g.money -= vc
        g.busy = true
        enterRealm()
        g.busy = false
    }

    private suspend fun enterRealm() {
        active = true
        val withLemy = g.dating.momoWith && !lemyMetFamily
        g.showFace(Face.WORRY)
        g.say(
            if (withLemy) "Come on, Lemy! It's time you met my family."
            else if (g.dating.momoWith) "Let's go say hi to the family again, Lemy!"
            else "Okay... let's go check on the family."
        )
        delay(1600)
        g.showFace(Face.GASP); g.say("Whoa. So THIS is the Death Realm.")
        delay(1500)
        g.showFace(Face.CHEER); g.jump(); g.say("GARY?! GRANDMA?! You guys look GREAT!")
        delay(1800)
        g.showFace(Face.HAPPY); g.say("They have clouds AND snacks! My family is totally fine!")
        if (withLemy && g.dating.momoWith) {
            lemyMetFamily = true
            delay(2000)
            val meet = listOf(
                "Everyone! This is Lemy. She's my girlfriend!" to true,
                "Grandma: Oh, she's LOVELY, dear! Bring her to Sunday dinner!" to false,
                "Gary: Oooooh! ${g.name} has a girlfriend!" to false,
                "Lemy: Hi, everybody! You're all so... floaty!" to false,
                "Mom: She's wonderful, ${g.name}. Keep her FAR away from that squeezer." to false,
                "I'm so happy you all get to meet her!" to true,
            )
            for ((line, kev) in meet) {
                if (!active) break
                if (kev) { g.showFace(Face.CHEER); g.jump() }
                g.say(line)
                delay(2200)
            }
            if (active) g.showFace(Face.HAPPY)
        }
    }

    fun leaveRealm() {
        g.viewModelScope.launch {
            g.busy = true
            g.say("Bye everybody! Save me a cloud!")
            delay(1200)
            active = false; g.sad = 0.0; g.visits++
            g.showFace(Face.CHEER); g.jump()
            g.say("I'm back! My family is okay. I feel SO much better!")
            g.busy = false
        }
    }

    private suspend fun bringBack() {
        g.money -= BRING_BACK
        g.busy = true
        kevinGone = false; active = true; g.sad = 0.0
        for (i in famDone.indices) famDone[i] = false
        g.showFace(Face.WORRY); g.say("...Hello? Is somebody there?")
        delay(1800)
        g.showFace(Face.GASP); g.say("You came to get ME? After I trapped you?!")
        delay(2400)
        g.showFace(Face.CHEER); g.jump(); g.say("Okay. I promise to be good. Let's go home!")
        delay(2000)
        active = false
        g.showFace(Face.CHEER); g.jump(); g.say("I'm BACK! Did you miss me?")
        g.busy = false
    }

    // ---------------- squeezing family (don't!) ----------------
    fun squeezeGhost(i: Int) {
        if (!active || g.busy || kevinGone || i >= famCount || famDone.getOrElse(i) { true }) return
        g.viewModelScope.launch {
            g.busy = true
            val f = Lines.famList[i]
            g.showFace(Face.GASP); g.say(f.plea)
            delay(800)
            delay(200)
            famDone[i] = true
            val gp = ghostPay(); g.money += gp; g.moneyBump++
            g.popAt("+${fmt(gp)}", 0.55f, 0.3f)
            delay(300)
            g.showFace(Face.GASP); g.say(f.react)
            delay(2000)
            val doneCount = famDone.count { it }
            if (doneCount.toLong() >= famCount && famExtra <= 0) {
                g.busy = false
                turnEvilInner("You squeezed my WHOLE family. Every. Single. One.", "You took my family. Now I'm taking YOU.")
                return@launch
            }
            g.showFace(Face.WORRY)
            val left = famCount - doneCount + famExtra
            g.say(if (left == 1L) "There's only ONE left. Don't you DARE." else if (left > 50) "I still have ${left} relatives up here. ...but STOP IT!" else Lines.angry.pick())
            g.busy = false
        }
    }

    fun squeezeCrowd() {
        if (!active || g.busy || kevinGone || famExtra <= 0) return
        g.viewModelScope.launch {
            g.busy = true
            val who = crowdName()
            g.say("$who: Hi! I'm one of your ${famTotal()} relatives! Wait, what are you d—")
            delay(900)
            famExtra--
            val gp = ghostPay(); g.money += gp; g.moneyBump++
            g.popAt("+${fmt(gp)}", 0.55f, 0.3f)
            g.showFace(Face.GASP)
            g.say(listOf("Not $who! ...Wait, who was that?", "I have SO many relatives. That was $who, I think.",
                "$who! I didn't even know you!", "${famTotal()} relatives left. This could take a while.").pick())
            delay(1500)
            g.busy = false
        }
    }

    fun crowdName(): String = "${Lines.crowdPrefix.pick()} ${Lines.crowdName.pick()}"

    fun maybeSpawnGhost() {
        if (g.glasses % max(1, 3 - g.lv("reunion")) != 0) return
        if (famCount < min(Lines.famList.size.toLong(), famDone.size.toLong())) {
            newGhostMsg = Lines.famList[famCount.toInt()].who
            famCount++
            return
        }
        val add = max(1L, ceil(famTotal() * 0.12).toLong())
        famExtra += add
        newGhostMsg = if (add == 1L) crowdName() else "${add} more relatives"
    }

    // ---------------- turning gray, then evil ----------------
    fun warningText(): String? {
        if (grayAt == 0L) return null
        val left = max(0L, ceil((EVIL_TIME + 10000L * g.lv("brave") - (g.now() - grayAt)) / 1000.0).toLong())
        return "${g.name} turns evil in $left! Visit the Death Realm or tap him a lot!"
    }

    fun cancelGray() { grayAt = 0L }

    /** called once a second from the main ticker */
    fun checkGray(now: Long) {
        val safe = if (grayAt != 0L) g.sad <= g.doom() - 3 else g.sad < g.doom()
        if (kevinGone || active || nightmare || g.therapy.atTherapy || safe) { grayAt = 0L; return }
        if (grayAt == 0L) { grayAt = now; if (!g.busy) g.say("I'm all gray... I need my family... or I'll turn EVIL.") }
        val left = EVIL_TIME + 10000L * g.lv("brave") - (now - grayAt)
        if (left <= 0 && !g.busy && !g.holdingKevin && darkHoldStart == 0L) turnEvil()
    }

    fun turnEvil(first: String? = null, last: String? = null) {
        g.viewModelScope.launch { turnEvilInner(first, last) }
    }
    private suspend fun turnEvilInner(first: String?, last: String?) {
        g.busy = true; grayAt = 0L
        g.showFace(Face.WORRY); g.say(first ?: "You never took me to see my family...")
        delay(2000)
        g.showFace(Face.WORRY); g.say("Now... you'll stay with ME. FOREVER.")
        delay(2000)
        active = false; nightmare = true
        g.juiceLevel = GameViewModel.MAX_H
        g.say("Welcome... to the Nightmare Realm.")
        delay(2200)
        g.say(last ?: "You let me turn gray. Now you can NEVER leave.")
        g.busy = false
    }

    // ---------------- holding Kevin too long, the normal way in ----------------
    fun squeezeFriend() { g.viewModelScope.launch { squeezeFriendInner() } }
    private suspend fun squeezeFriendInner() {
        g.busy = true
        g.showFace(Face.GASP); g.say("I TOLD YOU NOT TO!!!")
        delay(900)
        g.say("NOOOOOOOOO!!!")
        delay(300)
        delay(200)
        nightmare = true; active = false
        g.juiceLevel = GameViewModel.MAX_H
        delay(700)
        g.say("...")
        delay(1300)
        g.say("Welcome... to the Nightmare Realm.")
        delay(2200)
        g.say("You squeezed me. Now you can NEVER leave.")
        g.busy = false
    }

    // ---------------- the nightmare: hold dark Kevin for ~30s to escape ----------------
    fun darkHoldMs(): Long = max(6000L, 30000L - 4000L * g.lv("nightlight"))
    fun shadowDown() {
        if (!nightmare || g.busy || darkHoldStart != 0L) return
        darkHoldStart = g.now()
        darkJob = g.viewModelScope.launch {
            var lastLine = -1
            while (true) {
                val held = g.now() - darkHoldStart
                val need = darkHoldMs()
                val t = min(1f, held / need.toFloat())
                darkProgress = t
                val idx = min(Lines.darkLines.size - 1, (t * Lines.darkLines.size).toInt())
                if (idx != lastLine) { lastLine = idx; g.say(Lines.darkLines[idx]) }
                if (t >= 1f) { darkHoldStart = 0L; darkProgress = 0f; squeezeDarkInner(); return@launch }
                delay(16)
            }
        }
    }
    fun shadowUp() {
        if (darkHoldStart == 0L) return
        darkHoldStart = 0L; darkJob?.cancel(); darkJob = null; darkProgress = 0f
        g.say(Lines.darkLetGo.pick())
    }
    private suspend fun squeezeDarkInner() {
        g.busy = true
        g.say("NOOOOOOOOOOOOOOOO!!!")
        delay(1100)
        delay(200)
        delay(500)
        nightmare = false; active = false; kevinGone = true
        g.bubble = ""
        g.juiceLevel = 0f
        val loot = 100L * (1 + g.lv("loot"))
        g.money += loot
        g.popAt("+${fmt(loot)}", 0.5f, 0.4f)
        g.busy = false
    }

    /** squeeze() while in the nightmare realm: the button just taunts you */
    fun noEscape() {
        tries++
        g.say(Lines.noEscape.pick())
    }
    /** the main button's label while in the nightmare realm */
    fun escapeButtonLabel(): String = if (tries % 2 == 1) "There is no escape" else "Escape the nightmare"

    // ---------------- the dead closet: Death Realm only outfits ----------------
    fun buyOrWearDead(id: String) {
        val item = DEAD.find { it.id == id } ?: return
        if (deadOwned[id] != true) {
            if (g.money < item.price) return
            g.money -= item.price; deadOwned[id] = true
        }
        deadWearing = if (deadWearing == id) null else id
        if (deadWearing == id) g.say(item.line)
    }

    companion object {
        const val BRING_BACK = 50L
        const val EVIL_TIME = 20000L
        data class DeadItem(val id: String, val name: String, val price: Long, val line: String)
        val DEAD = listOf(
            DeadItem("sheet", "Bedsheet ghost", 40, "Boooo! I'm a ghost wearing a ghost costume. Ghost-ception!"),
            DeadItem("skeleton", "Skeleton suit", 40, "Look at my bones! ...Lemons don't have bones. Look at my FAKE bones!"),
            DeadItem("vampire", "Vampire cape", 40, "I vant to drink your... lemonade. Bleh!"),
        )
    }
}
