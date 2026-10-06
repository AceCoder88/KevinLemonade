package com.kevin.lemonade

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject

/**
 * A chunk of the game with its own saved state (war, space, the closet...).
 * GameViewModel owns one instance of each, saves/loads them under [key],
 * and calls [tick] once a second.
 */
interface Feature {
    val key: String
    /** write this feature's saved fields into [j] */
    fun save(j: JSONObject)
    /** read them back; [j] may be missing fields from older saves */
    fun load(j: JSONObject)
    /** back to a brand new game */
    fun reset()
    /** what Rebirth does to this feature. The original keeps everything except money and Mr. Zest's regular/advanced upgrades, so by default nothing happens */
    fun onRebirth() {}
    /** the MAX OUT secret code: set every upgrade in this feature to its max */
    fun maxOut() {}
    /** once a second, on the main thread, wherever the player is */
    fun tick() {}
}

/** placeholder until a screen is ported */
@Composable
fun ComingSoon(g: GameViewModel, title: String) {
    Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ChunkyButton("Back to lemonade", color = CARD) { g.screen = Screen.STAND }
        Spacer(Modifier.height(40.dp))
        OutlinedTitle(title)
        Spacer(Modifier.height(16.dp))
        Text("Coming soon!", fontSize = 20.sp, textAlign = TextAlign.Center)
    }
}
