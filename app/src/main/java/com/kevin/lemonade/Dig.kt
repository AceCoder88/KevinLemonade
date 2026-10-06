package com.kevin.lemonade

import androidx.compose.runtime.Composable
import org.json.JSONObject

/** Digging: the hole, ores, and the Underworld. */
class DigState(val g: GameViewModel) : Feature {
    override val key = "dig"
    override fun save(j: JSONObject) {}
    override fun load(j: JSONObject) {}
    override fun reset() {}
}

@Composable
fun DigScreen(g: GameViewModel) = ComingSoon(g, "Dig")
