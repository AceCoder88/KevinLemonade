package com.kevin.lemonade

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import org.json.JSONObject

/** LemonLove, Kevin's dating app. */
class DatingState(val g: GameViewModel) : Feature {
    override val key = "dating"
    override fun save(j: JSONObject) {}
    override fun load(j: JSONObject) {}
    override fun reset() {}

    /** Lemy (or whoever Kevin matched with) is at the stand with him: DOUBLE money. The original's momoWith. */
    var momoWith by mutableStateOf(false)
}

@Composable
fun DateScreen(g: GameViewModel) = ComingSoon(g, "Dating app")
