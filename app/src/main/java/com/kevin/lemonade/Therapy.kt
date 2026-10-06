package com.kevin.lemonade

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import org.json.JSONObject

/**
 * Therapy with Dr. Rind: send Kevin away from the stand for a bit, or (with the `tchair`
 * upgrade) let Dr. Rind call him on speakerphone right at the stand instead.
 */
class TherapyState(val g: GameViewModel) : Feature {
    override val key = "therapy"

    /** Kevin is away from the stand, getting a full session */
    var atTherapy by mutableStateOf(false)
    private var therapyEnd by mutableLongStateOf(0L)
    private var therapyLen by mutableLongStateOf(60000L)
    private var chairTalk = 0

    override fun save(j: JSONObject) {
        j.put("atTherapy", atTherapy); j.put("therapyEnd", therapyEnd); j.put("therapyLen", therapyLen)
    }
    override fun load(j: JSONObject) {
        atTherapy = j.optBoolean("atTherapy", false)
        therapyEnd = j.optLong("therapyEnd", 0L)
        therapyLen = j.optLong("therapyLen", 60000L)
        // a session that was running when the app was last closed just finishes next tick
    }
    override fun reset() { atTherapy = false; therapyEnd = 0L; therapyLen = 60000L; chairTalk = 0 }

    fun therapyTime(): Long = max(20000L, 60000L - 8000L * g.lv("tquick"))
    fun happyTime(): Long = 60000L + 30000L * g.lv("thappy")
    fun therapyLeft(): Long = max(0L, therapyEnd - g.now())

    fun canSend(): Boolean = !g.realm.kevinGone && !g.realm.active && !g.realm.nightmare && g.money >= THERAPY_COST

    fun send() {
        if (atTherapy) { g.screen = Screen.THERAPY; return }
        if (!canSend()) return
        g.money -= THERAPY_COST
        g.say("Therapy? ...Okay. Maybe I DO need to talk to someone.")
        therapyLen = therapyTime()
        atTherapy = true
        therapyEnd = g.now() + therapyLen
        g.realm.cancelGray()
        g.bubble = "${g.name} is at therapy. Back in a minute!"
        g.screen = Screen.THERAPY
    }

    private fun finish() {
        atTherapy = false
        g.sad = 0.0
        g.happyUntil = max(g.happyUntil, g.now() + happyTime())
        g.showFace(Face.CHEER)
        g.say("I'm BACK! Therapy was AMAZING. I'm totally happy!")
    }

    override fun tick() {
        if (atTherapy && therapyLeft() <= 0) finish()
        // therapy chair: Dr. Rind calls him on the phone and slowly cheers him up without leaving the stand
        if (g.lv("tchair") > 0 && !atTherapy && !g.realm.kevinGone && !g.realm.active && !g.realm.nightmare && g.sad > 0) {
            g.sad = max(0.0, g.sad - 0.25 * g.lv("tchair"))
            chairTalk++
            if (chairTalk >= 15 && !g.busy && g.talk["phone"] == true) { chairTalk = 0; g.say(Lines.phoneLines.pick()) }
        }
    }

    /** the chair shows up at the stand once bought, as long as Kevin isn't away or gone */
    fun chairVisible(): Boolean = g.lv("tchair") > 0 && !atTherapy && !g.realm.kevinGone

    /** 0..1 through this session, for the progress bar / talk line */
    fun progress(): Float = (1f - therapyLeft().toFloat() / therapyLen.toFloat()).coerceIn(0f, 1f)

    companion object { const val THERAPY_COST = 50L }
}

@Composable
fun TherapyScreen(g: GameViewModel) {
    val t = g.therapy
    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth()) {
            ChunkyButton("Back to lemonade", color = CARD) { g.screen = Screen.STAND }
        }
        Spacer(Modifier.height(24.dp))
        OutlinedTitle("Dr. Rind's office")
        Spacer(Modifier.height(20.dp))
        if (!t.atTherapy) {
            Text("${g.name} finished the session and went back to the stand, totally happy.", fontSize = 16.sp, textAlign = TextAlign.Center)
        } else {
            val k = t.progress()
            val idx = min(Lines.therapyTalk.size - 1, floor(k * Lines.therapyTalk.size).toInt())
            Box(
                Modifier.fillMaxWidth(0.9f).background(Color.White, RoundedCornerShape(16.dp))
                    .border(3.dp, LINE, RoundedCornerShape(16.dp)).padding(16.dp)
            ) {
                Text(g.named(Lines.therapyTalk[idx]), fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(20.dp))
            Box(Modifier.fillMaxWidth(0.8f).height(16.dp).clip(CircleShape).background(Color(0xFFB9B5A0)).border(3.dp, LINE, CircleShape)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(k).background(Color(0xFF8FCB5A)))
            }
            Spacer(Modifier.height(8.dp))
            Text("${ceil(t.therapyLeft() / 1000.0).toInt()} seconds left", fontSize = 15.sp)
        }
    }
}
