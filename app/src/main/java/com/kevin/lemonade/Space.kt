package com.kevin.lemonade

import androidx.compose.runtime.Composable
import org.json.JSONObject

/** Space: the planets, the other Kevins, missions, the Space Station. */
class SpaceState(val g: GameViewModel) : Feature {
    override val key = "space"
    override fun save(j: JSONObject) {}
    override fun load(j: JSONObject) {}
    override fun reset() {}
}

@Composable
fun SpaceScreen(g: GameViewModel) = ComingSoon(g, "Space")
