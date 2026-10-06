package com.kevin.lemonade

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** key, title, description - same as the original's TALK_OPTIONS */
private val TALK_OPTIONS = listOf(
    Triple("lemons", "Lemons and relatives", "What Kevin says while lemons get squeezed, running out of lemons, and new relatives showing up in the Death Realm"),
    Triple("upgrades", "Upgrades", "Kevin talking about things you buy in the shop"),
    Triple("outfits", "Outfits", "Kevin talking about his clothes, treasure, and outfits"),
    Triple("phone", "Dr. Rind phone calls", "The therapy chair phone calls"),
    Triple("poke", "Poke chatter", "What Kevin says when you tap him"),
    Triple("bubble", "Speech bubble", "Turn this off to hide the speech bubble completely"),
)

@Composable
fun SettingsScreenImpl(g: GameViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ChunkyButton("Back to lemonade", color = CARD) { g.screen = Screen.STAND }
        Spacer(Modifier.height(10.dp))
        OutlinedTitle("Settings")
        Spacer(Modifier.height(6.dp))
        Text("Choose what ${g.name} talks about", fontSize = 14.sp)
        Spacer(Modifier.height(10.dp))
        for ((k, title, desc) in TALK_OPTIONS) {
            TalkRow(title, desc, checked = g.talk[k] ?: true) { g.talk[k] = it }
        }
        Spacer(Modifier.height(10.dp))
        TalkRow(
            "Cute mode",
            "Pastel colors, pink lemonade, a flower crown for Kevin, a smiling sun, butterflies, bunny and kitten customers, and hearts everywhere.",
            checked = g.cute,
        ) { g.cute = it }
        TalkRow(
            "OG mode",
            "Make everything look like the very first version of the game: no night, no rainbows, no new buttons, just Kevin, the squeezer, and the lemonade.",
            checked = g.og,
        ) { g.og = it }
    }
}

@Composable
private fun TalkRow(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(shape).background(CARD).border(3.dp, LINE, shape)
            .clickable { onChange(!checked) }.padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onChange, colors = CheckboxDefaults.colors(checkedColor = PEEL, checkmarkColor = LINE))
        Column(Modifier.padding(start = 6.dp)) {
            Text(title, fontWeight = FontWeight.Black, fontSize = 14.sp)
            Text(desc, fontSize = 12.sp)
        }
    }
}
