package com.kevin.lemonade

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class HubTab(val title: String) { SHOPS("Shops"), WORLDS("Worlds"), FUN("Fun"), KEVIN("Kevin"), HELP("Help") }

private class Place(val label: String, val color: Color, val text: Color = Color.White, val show: Boolean, val go: () -> Unit)

/** "Places to go": the original's hub, in tabs. A place only shows up once you own its tool. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Hub(g: GameViewModel) {
    var tab by rememberSaveable { mutableStateOf(HubTab.SHOPS) }
    val realm = g.inRealm()
    val places: List<Place> = when (tab) {
        HubTab.SHOPS -> listOf(
            Place("🛒 Open the Shop", RED, show = true) { g.openShop() },
            Place("🎒 Traveling Shop", Color(0xFF2E8B9E), show = true) { g.screen = Screen.TRAVEL },
            Place("👚 ${g.name}'s Closet", Color(0xFFFF7FB0), LINE, show = g.lv("closetkey") > 0) { g.screen = Screen.CLOSET },
        )
        HubTab.WORLDS -> listOf(
            Place("🍋 Catch the Lemons", Color(0xFFE8B90F), LINE, show = true) { g.screen = Screen.CATCH },
            Place("⚔️ Go to War", Color(0xFF6B7A3A), show = g.lv("bow") > 0) { g.screen = Screen.WAR },
            Place("🚀 Space", Color(0xFF141A3A), PEEL, show = g.lv("telescope") > 0 && !realm) { g.screen = Screen.SPACE },
            Place("⛏️ Dig", Color(0xFF6B4A1E), Color(0xFFFFF6E0), show = g.lv("shovel") > 0 && !realm) { g.screen = Screen.DIG },
            Place("🏴‍☠️ Be a Pirate", Color(0xFF1E4E6E), PEEL, show = g.lv("map") > 0 && !realm) { g.screen = Screen.PIRATE },
            Place("🐎 Ride the Horse", Color(0xFF8B5A2B), show = g.lv("saddle") > 0 || g.closet.wearingSet("cowboy")) { g.screen = Screen.RIDE },
            Place("🧙 Wizard Tower", Color(0xFF4B2E9E),
                show = g.lv("wand") > 0 || g.closet.wearingSet("wizard") || g.closet.wearingSet("witch")) { g.screen = Screen.TOWER },
        )
        HubTab.FUN -> listOf(
            Place("🎟️ Lemon Lottery", Color(0xFF4FA34A), show = g.lv("lottopass") > 0) { g.screen = Screen.LOTTO },
            Place("🏁 Lemon Races", RED, show = g.lv("racepass") > 0) { g.screen = Screen.RACE },
            Place("🏆 Trophies", Color(0xFFE9B630), LINE, show = true) { g.screen = Screen.TROPHY },
            Place("✨ Rebirth", Color(0xFF7B5CFF), show = true) { g.screen = Screen.REBIRTH },
        )
        HubTab.KEVIN -> listOf(
            Place("🛋️ Send ${g.name} to therapy (\$50)", Color(0xFF7B5CFF), show = g.lv("pager") > 0) { g.screen = Screen.THERAPY },
            Place("💘 Dating app", Color(0xFFFF4D8D), show = g.dating.momoWith || g.lv("girlfriend") > 0) { g.screen = Screen.DATE },
        )
        HubTab.HELP -> emptyList()
    }

    val shape = RoundedCornerShape(24.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(Color(0xFFFFF6D6)).border(4.dp, LINE, shape).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Places to go", fontFamily = Lilita, fontSize = 22.sp)
        Spacer(Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (t in HubTab.entries) {
                val pill = RoundedCornerShape(999.dp)
                Text(t.title, fontWeight = FontWeight.Black, fontSize = 15.sp,
                    modifier = Modifier.clip(pill).background(if (t == tab) PEEL else Color.White)
                        .border(3.dp, LINE, pill).clickable { tab = t }
                        .padding(horizontal = 14.dp, vertical = 6.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        if (tab == HubTab.HELP) HubHelp(g)
        else {
            val shown = places.filter { it.show }
            for (p in shown) {
                ChunkyButton(p.label, color = p.color, textColor = p.text,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), onClick = p.go)
            }
            if (shown.isEmpty()) Text("Nothing here yet! Buy tools in the Shop (under Tools) to unlock more.",
                fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.alpha(0.75f))
        }
    }
}

@Composable
private fun HubHelp(g: GameViewModel) {
    val n = g.name
    val topics = listOf(
        "Shops" to "Mr. Zest the shopkeeper sells all your lemonade upgrades, the advanced upgrades, and the super traveling deals.",
        "Catch the Lemons" to "Out of lemons? Shake the lemon tree and catch more in your basket. Spend money on a bigger basket, more storage, and helpers.",
        "Therapy" to "$n goes to see Dr. Rind for one minute. You can check on him while he's there. When he comes back he's completely happy, " +
            "and he stays happy for a whole minute.\n\nMr. Zest sells therapy upgrades: longer happiness, quicker sessions, a therapy chair " +
            "so Dr. Rind can help $n right at the stand, and even a girlfriend for $n named Lemy.",
        "War" to "Defend the wall from bugs, worms, rats, snails, bees, crows, beetles, frogs and sneaky moles that dig under the ground. " +
            "Hire soldiers to fight on the front line.\n\nEvery 5th wave is a boss: Giant Bird, King Rat, Queen Bee, Giant Snail, Worm King, " +
            "and Mega Frog.\n\nEarn war bonds and trade them for cash.",
    )
    var open by remember { mutableStateOf<String?>(null) }
    for ((title, body) in topics) {
        Text((if (open == title) "▴ " else "▾ ") + title, fontWeight = FontWeight.Black, fontSize = 16.sp,
            modifier = Modifier.fillMaxWidth().clickable { open = if (open == title) null else title }.padding(8.dp))
        if (open == title) Text(body, fontSize = 14.sp, modifier = Modifier.padding(start = 12.dp, end = 8.dp, bottom = 8.dp))
    }
}

/** the original's toast: a little note at the bottom of the screen */
@Composable
fun Toast(g: GameViewModel, modifier: Modifier) {
    val t = g.toastText ?: return
    val shape = RoundedCornerShape(16.dp)
    Text(t, color = Color(0xFFFFF6B8), fontWeight = FontWeight.Black, textAlign = TextAlign.Center,
        modifier = modifier.padding(16.dp).clip(shape).background(LINE).border(3.dp, PEEL, shape)
            .padding(horizontal = 16.dp, vertical = 10.dp))
}
