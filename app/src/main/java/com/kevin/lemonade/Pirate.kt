package com.kevin.lemonade

import androidx.compose.runtime.Composable
import org.json.JSONObject

/** Pirate adventure: the ship, limes, allies. */
class PirateState(val g: GameViewModel) : Feature {
    override val key = "pirate"
    override fun save(j: JSONObject) {}
    override fun load(j: JSONObject) {}
    override fun reset() {}
}

@Composable
fun PirateScreen(g: GameViewModel) = ComingSoon(g, "Be a Pirate")
