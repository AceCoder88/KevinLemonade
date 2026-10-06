package com.kevin.lemonade

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A race lemon: color and odds (faster racers pay less, like the original). */
data class Racer(val name: String, val color: Color, val odds: Int)

val RACERS = listOf(
    Racer("Zippy", Color(0xFFFFD21F), 2),
    Racer("Sourpuss", Color(0xFF7BC043), 3),
    Racer("Pinky", Color(0xFFFF7FB0), 4),
    Racer("Blue Bolt", Color(0xFF4AA8FF), 6),
)
const val RACE_TRACK = 300f
val RACE_BETS = listOf(10L, 100L, 1000L, 10000L)

@Composable
fun RaceScreenImpl(g: GameViewModel) {
    val f = g.fun_
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ChunkyButton("Back to lemonade", color = CARD) { g.screen = Screen.STAND }
        Spacer(Modifier.height(10.dp))
        OutlinedTitle("Lemon Races")
        Spacer(Modifier.height(6.dp))
        MoneyText(g, 24)
        Spacer(Modifier.height(10.dp))

        // the track: one lane per racer, position as a fraction of RACE_TRACK
        Column(Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFE6F4D9)).border(3.dp, LINE, RoundedCornerShape(12.dp)).padding(8.dp)) {
            for ((i, r) in RACERS.withIndex()) {
                val frac = (f.racePos.getOrElse(i) { 0f } / RACE_TRACK).coerceIn(0f, 1f)
                Box(Modifier.fillMaxWidth().weight(1f).padding(vertical = 2.dp)) {
                    Box(
                        Modifier.fillMaxWidth(frac.coerceAtLeast(0.001f)).fillMaxHeight()
                            .clip(RoundedCornerShape(50)),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        Box(Modifier.size(26.dp).clip(CircleShape).background(r.color).border(2.dp, LINE, CircleShape))
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(f.raceMsg, fontWeight = FontWeight.Black, fontSize = 15.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))

        Text("Pick your lemon", fontWeight = FontWeight.Black, fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for ((i, r) in RACERS.withIndex()) {
                ChunkyButton(
                    "${r.name} (${r.odds}x)",
                    color = if (i == f.racePick) PEEL else CARD,
                    enabled = !f.racing,
                ) { f.racePick = i }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text("Bet", fontWeight = FontWeight.Black, fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (b in RACE_BETS) {
                ChunkyButton("Bet ${fmt(b)}", color = if (f.raceBet == b) PEEL else CARD, enabled = !f.racing) { f.raceBet = b }
            }
        }
        Spacer(Modifier.height(10.dp))
        ChunkyButton("Start the race!", big = true, enabled = !f.racing && g.money >= f.raceBet) { f.startRace() }
        Spacer(Modifier.height(20.dp))
        BettorsPanel(g)
    }
}
