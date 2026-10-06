package com.kevin.lemonade

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** [how] may contain {N} for Kevin's name, applied at render time via g.named(). */
class TrophyDef(val name: String, val how: String, val done: (GameViewModel, FunState) -> Boolean)

val TROPHIES = listOf(
    TrophyDef("First pitcher", "Sell your first pitcher") { g, _ -> g.glasses >= 1 },
    TrophyDef("Lemonade legend", "Sell 100 pitchers") { g, _ -> g.glasses >= 100 },
    TrophyDef("Rich lemon", "Have \$10,000 at once") { g, _ -> g.money >= 10000 },
    TrophyDef("Family visit", "Visit the Death Realm") { g, _ -> g.visits >= 1 },
    // INTEGRATION: needs a `kevinGone` flag once the Death/Nightmare Realm core state exists on GameViewModel.
    TrophyDef("Nightmare escapee", "Escape the Nightmare Realm") { _, _ -> false },
    // INTEGRATION: needs WarState.maxWave (the highest wave reached).
    TrophyDef("War hero", "Reach wave 10 in war") { _, _ -> false },
    TrophyDef("Pirate captain", "Reach voyage 3 as a pirate") { g, _ -> g.pirate.voyage >= 3 },
    TrophyDef("Diamond digger", "Dig up a diamond") { g, _ -> g.dig.dugDiamond },
    TrophyDef("Lucky ticket", "Win the lottery JACKPOT") { _, f -> f.wonJackpot },
    TrophyDef("Tool collector", "Buy every tool") { g, _ -> listOf("bow", "pager", "telescope", "map", "closetkey", "shovel").all { g.lv(it) > 0 } },
    TrophyDef("True love", "Get {N} a girlfriend") { _, f -> f.lemyEver },
)

val SECRETS = listOf(
    TrophyDef("I know you're cheating", "You used the secret hall code. Busted!") { _, f -> f.usedHall },
    TrophyDef("The chosen lemon", "Defeat Darth Lime at the Lime Star") { g, _ -> g.space.darthDefeated },
    // INTEGRATION: needs a day/night cycle plus a `wokeKevin` flag; nothing like it exists yet.
    TrophyDef("Sleepy head", "Wake {N} up when he falls asleep at night") { _, _ -> false },
    // INTEGRATION: needs a `rescuedPartner` flag from the Underworld-rescue flow (War's Furious Kevin fight / Death Realm).
    TrophyDef("Calm down, Kevin!", "Rescue Lemy (or Lenny) from the Underworld") { _, _ -> false },
)

@Composable
fun TrophyScreenImpl(g: GameViewModel) {
    val f = g.fun_
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ChunkyButton("Back to lemonade", color = CARD) { g.screen = Screen.STAND }
        Spacer(Modifier.height(10.dp))
        OutlinedTitle("Trophies")
        Spacer(Modifier.height(10.dp))
        for ((i, t) in TROPHIES.withIndex()) {
            val got = i in f.gotTrophy
            TrophyRow(if (got) "🏆" else "🔒", t.name, g.named(t.how), got)
        }
        Spacer(Modifier.height(12.dp))
        Text("Secret trophies (${f.gotSecret.size}/${SECRETS.size})", fontFamily = Lilita, fontSize = 18.sp)
        Spacer(Modifier.height(6.dp))
        for ((i, t) in SECRETS.withIndex()) {
            val got = i in f.gotSecret
            if (got) TrophyRow("🌟", t.name, g.named(t.how), true)
            else TrophyRow("❓", "???", "It's a secret! Keep playing to find it.", false)
        }
    }
}

@Composable
private fun TrophyRow(icon: String, name: String, how: String, got: Boolean) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(shape)
            .background(if (got) androidx.compose.ui.graphics.Color(0xFFE6F4D9) else CARD)
            .border(3.dp, LINE, shape).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(icon, fontSize = 20.sp, modifier = Modifier.padding(end = 10.dp))
        Column {
            Text(name, fontWeight = FontWeight.Black, fontSize = 14.sp)
            Text(how, fontSize = 12.sp)
        }
    }
}
