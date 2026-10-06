package com.kevin.lemonade

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A lottery scratch-card symbol. `prize` is money won (0 if it wins something else, like limes). */
data class LottoSymbol(val id: String, val icon: String, val prize: Long, val name: String, val limes: Int = 0)

val SYMBOLS = listOf(
    LottoSymbol("lemon", "🍋", 60, "3 lemons"),
    LottoSymbol("lime", "🟢", 0, "3 limes", limes = 15),
    LottoSymbol("gold", "🦙", 250, "3 gold coins"),
    LottoSymbol("diamond", "💎", 1000, "3 diamonds"),
    LottoSymbol("kevin", "😊", 5000, "3 Kevins: JACKPOT"),
    LottoSymbol("skull", "💀", 0, "3 skulls"),
)
val LOTTO_WEIGHTS = doubleArrayOf(30.0, 14.0, 18.0, 8.0, 3.0, 27.0)
const val LOTTO_TICKET = 20L

@Composable
fun LottoScreenImpl(g: GameViewModel) {
    val f = g.fun_
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ChunkyButton("Back to lemonade", color = CARD) { g.screen = Screen.STAND }
        Spacer(Modifier.height(10.dp))
        OutlinedTitle("Lemon Lottery")
        Spacer(Modifier.height(6.dp))
        MoneyText(g, 24)
        Spacer(Modifier.height(10.dp))
        Text(f.lottoMsg, textAlign = TextAlign.Center, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 8.dp))
        Spacer(Modifier.height(10.dp))
        if (f.hasCard) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.width(220.dp).height(220.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(9) { i ->
                    val open = f.lottoOpen.getOrElse(i) { false }
                    val shape = RoundedCornerShape(10.dp)
                    Box(
                        Modifier.size(66.dp).clip(shape)
                            .background(if (open) CARD else PEEL)
                            .border(3.dp, LINE, shape)
                            .clickable(enabled = !f.lottoDone) { f.scratch(i) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(if (open) f.lottoCells[i].icon else "?", fontSize = 26.sp)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            if (!f.lottoDone) ChunkyButton("Scratch it all", color = CARD) { f.scratchAll() }
        }
        Spacer(Modifier.height(10.dp))
        ChunkyButton(
            text = "Buy a ticket (${fmt(LOTTO_TICKET)})",
            color = RED, textColor = androidx.compose.ui.graphics.Color.White,
            enabled = g.money >= LOTTO_TICKET && (!f.hasCard || f.lottoDone),
        ) { f.buyTicket() }
        Spacer(Modifier.height(20.dp))
        BettorsPanel(g)
    }
}

/** shared by the Lottery and Race screens (the original's #bettorsL / #bettorsR) */
@Composable
fun BettorsPanel(g: GameViewModel) {
    val f = g.fun_
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.fillMaxWidth(0.9f).clip(shape).background(CARD).border(3.dp, LINE, shape).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Bettors", fontWeight = FontWeight.Black, fontSize = 16.sp)
        Spacer(Modifier.height(6.dp))
        val firstUnhired = f.bettors.indexOfFirst { !it.hired }
        for ((i, b) in f.bettors.withIndex()) {
            if (!b.hired && i != firstUnhired) continue
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${b.icon} ${b.name}", fontWeight = FontWeight.Black, fontSize = 14.sp, modifier = Modifier.weight(1f))
                if (b.hired) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(b.log, fontSize = 11.sp)
                        Text("${30 - b.t}s", fontSize = 11.sp)
                    }
                } else {
                    Column(Modifier.weight(1f)) { Text("Bets for you every 30 seconds", fontSize = 11.sp) }
                    ChunkyButton("Hire (${fmt(f.bettorCost())})", color = PEEL, enabled = g.money >= f.bettorCost()) { f.hireBettor(i) }
                }
            }
        }
    }
}
