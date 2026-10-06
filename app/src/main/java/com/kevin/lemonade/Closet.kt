package com.kevin.lemonade

import androidx.compose.runtime.Composable
import org.json.JSONObject

/** Kevin's Closet: outfits with special powers. */
class ClosetState(val g: GameViewModel) : Feature {
    override val key = "closet"
    override fun save(j: JSONObject) {}
    override fun load(j: JSONObject) {}
    override fun reset() {}

    /** is Kevin wearing this piece? (the original's wearing(id)) */
    fun wearing(id: String): Boolean = false
    /** is Kevin wearing every piece of this set? (the original's wearingSet(id)) */
    fun wearingSet(id: String): Boolean = false
}

@Composable
fun ClosetScreen(g: GameViewModel) = ComingSoon(g, "Kevin's Closet")
