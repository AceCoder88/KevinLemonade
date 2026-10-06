package com.kevin.lemonade

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

const val DATE_COST = 10L

/** one profile in the original's PROFILES list. [color] stands in for her/his portrait art (see report). */
data class DateProfile(
    val name: String,
    val she: Boolean?,
    val bio: String,
    val kevinLine: String,
    val match: Boolean = false,
    val sister: Boolean = false,
    val forcedLine: String? = null,
    val talk: List<String>? = null,
    val color: Color,
)

val PROFILES: List<DateProfile> = listOf(
    DateProfile("Kory", null, "Lemon. Loves volleyball and bossing her little brother around.",
        "EWW! Kory is my SISTER! Next!", sister = true, color = Color(0xFFFFD21F)),
    DateProfile("Coco", true, "Coconut. Tough on the outside, sweet on the inside.",
        "Coco is SO tough. She'd crack ME. Next!",
        forcedLine = "WHAT?! ...Okay, okay. Coco IS kinda sweet on the inside.",
        talk = listOf("Coco: I'm tough on the outside, but Kevin melts my heart.", "Coco: Want some coconut water? ...No? Okay."),
        color = Color(0xFF7A5230)),
    DateProfile("A Cow", true, "Moo.", "Wait... this is just a cow. Hi, cow. ...Next!",
        forcedLine = "I'm dating... a COW?! ...Moo. I mean, hi.",
        talk = listOf("A Cow: Moo.", "A Cow: Moo moo.", "A Cow: ...Moo? (She seems very happy.)"),
        color = Color.White),
    DateProfile("Bana", true, "Banana. Loves to split.", "Bana the Banana. She'd just split on me. Next!",
        forcedLine = "Fine! Bana, just promise you won't split on me.",
        talk = listOf("Bana: I promised Kevin I'd never split.", "Bana: Kevin is so a-PEEL-ing!"),
        color = Color(0xFFFFE36B)),
    DateProfile("Hendrix", false, "Broccoli. Plays guitar. Has a band.", "Wait, is that Hendrix? He's cool, but I'm not gay. Next!",
        forcedLine = "Huh?! ...Okay, okay. Hendrix, you can teach me guitar, I guess!",
        talk = listOf("Hendrix: Kevin and me are starting a band!", "Hendrix: Wanna hear a guitar solo? *strum strum*"),
        color = Color(0xFF3E9B4F)),
    DateProfile("Lemy", true, "Lemon. Loves sunsets, cuddles, and lemonade (from a store).",
        "...Lemy. WOW. She's PERFECT. My heart is going boing-boing!", match = true,
        talk = listOf("Lemy: Kevin is SO sweet.", "Lemy: Do you like my bow?", "Lemy: I love lemonade. Just not THAT way.",
            "Lemy: Kevin wrote me a poem! It was about lemons.", "Lemy: Please be nice to Kevin's family!"),
        color = Color(0xFFFFE45C)),
)
val LEMY: DateProfile = PROFILES.last()
/** the original's LENNY: never appears in the swipe deck, only arrives via the 'girlfriend' upgrade when {N} is a girl */
val LENNY = DateProfile("Lenny", false, "", "", match = true,
    talk = listOf("Lenny: Your stand is the coolest in town!", "Lenny: Want to go stargazing tonight?",
        "Lenny: I wrote you a song! It's about lemons.", "Lenny: You're the zestiest lemon I know!"),
    color = Color(0xFF2E9BD6))

/** LemonLove, Kevin's dating app. */
class DatingState(val g: GameViewModel) : Feature {
    override val key = "dating"

    /** Lemy (or whoever Kevin matched with) is at the stand with him: DOUBLE money. The original's momoWith. */
    var momoWith by mutableStateOf(false)
    var partner by mutableStateOf<DateProfile?>(null)

    // ---- the swipe loop's visible state ----
    var dating by mutableStateOf(false)
    var curProfile by mutableStateOf<DateProfile?>(null)
    var stamp by mutableStateOf("")
    var kevinSays by mutableStateOf("")
    var forceVisible by mutableStateOf(false)
    var viewingCouple by mutableStateOf(false)
    var breakingUp by mutableStateOf(false)
    private var forceRequested: DateProfile? = null

    fun pn(): String = partner?.name ?: "Lemy"
    fun friendWord(): String = if (partner?.she == false) "BOYFRIEND" else "GIRLFRIEND"

    /**
     * What the main stage needs to draw Lemy/the partner (drawing her is the core agent's job -
     * see the final report). "lemon-pink-bow" is Lemy herself (the default look), "lemon-blue-cap"
     * is Lenny (the original describes him as "a lemon in a blue cap"), and "profile:<name>" is one
     * of the swipeable [PROFILES] (Coco, A Cow, Bana, Hendrix) - look up [PROFILES] by name for her/his
     * [DateProfile.color] as a simple stand-in appearance.
     */
    fun partnerLook(): String = when (partner?.name ?: "Lemy") {
        "Lemy" -> "lemon-pink-bow"
        "Lenny" -> "lemon-blue-cap"
        else -> "profile:${partner!!.name}"
    }

    fun openDating() {
        if (momoWith) { viewingCouple = true; return }
        if (dating || g.money < DATE_COST) return
        g.money -= DATE_COST
        dating = true
        forceRequested = null
        g.viewModelScope.launch { swipeLoop() }
    }

    fun requestForce() {
        val p = curProfile
        if (p != null && !p.match) forceRequested = p
    }

    fun closeDating() { dating = false; viewingCouple = false }

    private suspend fun swipeLoop() {
        for (p in PROFILES) {
            curProfile = p; stamp = ""; kevinSays = "${g.name} is scrolling..."
            forceVisible = !p.match
            delay(1100)
            if (forceRequested == null) kevinSays = "${g.name}: ${p.kevinLine}"
            if (p.match && forceRequested == null) {
                forceVisible = false; stamp = "IT'S A MATCH!"
                delay(3200)
                finishDating(p)
                return
            }
            if (forceRequested == null) delay(1700)
            val f = forceRequested
            if (f != null && (f.name == "Kory" || f.name == "Hendrix")) {
                // these two don't work out: they just leave, no matter how hard you force it
                forceVisible = false
                if (f.name == "Kory") {
                    stamp = "UH OH"
                    kevinSays = "Kory: WHY DID YOU CLICK ON MINE?! I'm your SISTER, ${g.name}! I'm telling Mom!"
                    delay(3000)
                    kevinSays = "${g.name}: I didn't do it! Somebody else clicked it!"
                } else {
                    stamp = "OOPS"
                    kevinSays = "Hendrix: Oh! Uh... I should tell you, I'm actually gay. Oops, that just slipped out!"
                    delay(3000)
                    kevinSays = "${g.name}: Oh! That's cool, Hendrix! Still friends?"
                    delay(2200)
                    kevinSays = "Hendrix: Totally! See ya, ${g.name}!"
                }
                delay(2400)
                forceRequested = null
                stamp = "GONE"
                delay(600)
                continue
            }
            if (f != null) {
                // you overrode Kevin's choice!
                forceVisible = false
                kevinSays = "${g.name}: ${f.forcedLine}"
                stamp = "FORCED MATCH!"
                delay(3400)
                finishDating(f)
                return
            }
            stamp = "NOPE"
            delay(600)
        }
        dating = false; curProfile = null
    }

    private fun finishDating(p: DateProfile) {
        dating = false; forceRequested = null; curProfile = null
        partner = p; momoWith = true
        g.say(if (p.name == "Lemy") "${p.name} is here! I'm the happiest lemon in the WORLD." else "Well... ${p.name} is here. I guess we're dating now!")
        g.save()
    }

    /** the 'girlfriend' Mr. Zest upgrade: an instant match with Lemy (or Lenny if {N} is a girl).
     * Game.kt's buy() needs to call this - see the final report's integration notes. */
    fun getGirlfriend() {
        partner = if (g.gender == "girl") LENNY else LEMY
        momoWith = true
        if (g.gender == "girl") g.say("Lenny?! He's... PERFECT. My heart is going boing-boing!")
        g.save()
    }

    fun breakUp() {
        if (!momoWith || breakingUp) return
        breakingUp = true
        val who = pn()
        g.viewModelScope.launch {
            stamp = "BROKEN UP"
            kevinSays = "${g.name}: We're... breaking up? ...Okay. I'll miss you, $who."
            delay(2200)
            kevinSays = if (who == "A Cow") "A Cow: Moo. (She walks away sadly.)" else "$who: Bye, ${g.name}. Take care of yourself."
            delay(2000)
            momoWith = false; partner = null
            g.sad = minOf(g.doom(), g.sad + g.doom() * 0.4)
            g.say("I'm single again. ...I'm gonna need a minute.")
            breakingUp = false; viewingCouple = false; stamp = ""
            g.save()
        }
    }

    override fun save(j: JSONObject) {
        j.put("momoWith", momoWith)
        partner?.let { j.put("partner", it.name) }
    }

    override fun load(j: JSONObject) {
        momoWith = j.optBoolean("momoWith", false)
        val name = j.optString("partner", "")
        partner = when (name) { "Lemy" -> LEMY; "Lenny" -> LENNY; "" -> null; else -> PROFILES.find { it.name == name } }
    }

    override fun reset() {
        momoWith = false; partner = null; dating = false; curProfile = null
        stamp = ""; kevinSays = ""; breakingUp = false; viewingCouple = false; forceRequested = null
    }

    /** Rebirth doesn't touch dating in the original - a match (and momoWith's double money) is kept. */
    override fun onRebirth() {}

    override fun maxOut() {
        if (!momoWith) { partner = LEMY; momoWith = true }
    }
}

// ======================= LemonLove screen =======================
@Composable
fun DateScreen(g: GameViewModel) {
    val d = g.dating
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        ChunkyButton("Back to lemonade", color = CARD) { g.screen = Screen.STAND }
        Spacer(Modifier.height(16.dp))
        OutlinedTitle("LemonLove", 30, Color(0xFFFF4D8D))
        Spacer(Modifier.height(10.dp))
        when {
            d.momoWith && !d.dating -> CoupleCard(g, d)
            d.dating -> SwipeCard(g, d)
            else -> {
                Text("Find ${g.name} a match!", fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                ChunkyButton("Open the dating app (\$$DATE_COST)", big = true, enabled = g.money >= DATE_COST,
                    modifier = Modifier.align(Alignment.CenterHorizontally)) { d.openDating() }
            }
        }
    }
}

@Composable
private fun SwipeCard(g: GameViewModel, d: DatingState) {
    val p = d.curProfile
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CARD).border(3.dp, LINE, RoundedCornerShape(20.dp)).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (p != null) {
            Box(Modifier.size(100.dp).clip(CircleShape).background(p.color).border(3.dp, LINE, CircleShape))
            Spacer(Modifier.height(8.dp))
            Text(p.name, fontFamily = Lilita, fontSize = 22.sp)
            Text(p.bio, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
        if (d.stamp.isNotEmpty()) Text(d.stamp, fontFamily = Lilita, fontSize = 20.sp, color = RED)
        Spacer(Modifier.height(8.dp))
        Text(d.kevinSays, fontSize = 14.sp, textAlign = TextAlign.Center)
        if (d.forceVisible) {
            Spacer(Modifier.height(10.dp))
            ChunkyButton("Make him date them! ♥", color = Color(0xFFFF4D8D)) { d.requestForce() }
        }
    }
}

@Composable
private fun CoupleCard(g: GameViewModel, d: DatingState) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(CARD).border(3.dp, LINE, RoundedCornerShape(20.dp)).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val p = d.partner
        Box(Modifier.size(100.dp).clip(CircleShape).background(p?.color ?: Color(0xFFFFE45C)).border(3.dp, LINE, CircleShape))
        Spacer(Modifier.height(8.dp))
        Text(d.pn(), fontFamily = Lilita, fontSize = 22.sp)
        Text("Dating ${g.name}", fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        Text("${g.name} and ${d.pn()} are together. So cute!", fontSize = 14.sp, textAlign = TextAlign.Center)
        if (d.stamp.isNotEmpty()) { Spacer(Modifier.height(6.dp)); Text(d.stamp, fontFamily = Lilita, fontSize = 18.sp, color = RED) }
        Spacer(Modifier.height(10.dp))
        ChunkyButton("Break up", color = RED, enabled = !d.breakingUp) { d.breakUp() }
    }
}
