package com.kevin.lemonade

import androidx.compose.runtime.Composable
import org.json.JSONObject

/** Wizard Tower: spells, potions, apprentices. */
class TowerState(val g: GameViewModel) : Feature {
    override val key = "tower"
    override fun save(j: JSONObject) {}
    override fun load(j: JSONObject) {}
    override fun reset() {}
}

@Composable
fun TowerScreen(g: GameViewModel) = ComingSoon(g, "Wizard Tower")
