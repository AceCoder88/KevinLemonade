package com.kevin.lemonade

import androidx.compose.runtime.Composable
import org.json.JSONObject

/** Cowboy Realm horse ride and horse companions. */
class RideState(val g: GameViewModel) : Feature {
    override val key = "ride"
    override fun save(j: JSONObject) {}
    override fun load(j: JSONObject) {}
    override fun reset() {}
}

@Composable
fun RideScreen(g: GameViewModel) = ComingSoon(g, "Ride the Horse")
