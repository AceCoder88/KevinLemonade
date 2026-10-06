package com.kevin.lemonade

import androidx.compose.runtime.Composable
import org.json.JSONObject

/** Wanda's traveling shop and the traveling helpers. */
class TravelState(val g: GameViewModel) : Feature {
    override val key = "travel"
    override fun save(j: JSONObject) {}
    override fun load(j: JSONObject) {}
    override fun reset() {}
}

@Composable
fun TravelScreen(g: GameViewModel) = ComingSoon(g, "Traveling Shop")
