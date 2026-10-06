package com.kevin.lemonade

import androidx.compose.runtime.Composable
import org.json.JSONObject

/** Therapy with Dr. Rind. */
class TherapyState(val g: GameViewModel) : Feature {
    override val key = "therapy"
    override fun save(j: JSONObject) {}
    override fun load(j: JSONObject) {}
    override fun reset() {}
}

@Composable
fun TherapyScreen(g: GameViewModel) = ComingSoon(g, "Therapy")
