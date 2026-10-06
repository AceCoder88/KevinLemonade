package com.kevin.lemonade

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RebirthScreenImpl(g: GameViewModel) {
    val f = g.fun_
    val cost = f.rebirthCost()
    LaunchedEffect(Unit) { f.rebirthMsg = "" }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ChunkyButton("Back to lemonade", color = CARD) { g.screen = Screen.STAND }
        Spacer(Modifier.height(10.dp))
        OutlinedTitle("Rebirth")
        Spacer(Modifier.height(10.dp))
        Text(
            "You have rebirthed ${f.rebirths} time${if (f.rebirths == 1) "" else "s"}. The next rebirth costs ${fmt(cost)} (the price doubles every time).",
            textAlign = TextAlign.Center, fontSize = 14.sp,
        )
        Spacer(Modifier.height(12.dp))
        Text("• Money bonus: every pitcher pays ${"%.2f".format(f.rebirthMult() + 0.25)}x (right now: ${"%.2f".format(f.rebirthMult())}x)", fontSize = 14.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            if (f.rebirths == 0) "• A second full-size squeezer! It's already electric. Every squeeze uses 2 lemons and makes twice the juice."
            else "• You already have your second squeezer.",
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(6.dp))
        Text("• What resets: your money goes back to \$0, and Mr. Zest's regular and advanced upgrades start over.", fontSize = 14.sp)
        Spacer(Modifier.height(6.dp))
        Text("• What you keep: tools, therapy, luck, outfits, Lemy, war, space, pirates, digging, and everything else.", fontSize = 14.sp)
        Spacer(Modifier.height(16.dp))
        ChunkyButton(
            text = if (g.money < cost) "Need ${fmt(cost)} to rebirth" else "Rebirth for ${fmt(cost)}!",
            big = true, enabled = g.money >= cost,
        ) { f.doRebirth() }
        if (f.rebirthMsg.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(f.rebirthMsg, textAlign = TextAlign.Center, fontSize = 14.sp)
        }
    }
}
