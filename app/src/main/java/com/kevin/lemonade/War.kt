package com.kevin.lemonade

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableLongStateOf
import org.json.JSONObject

/** War: the wall, the bugs, soldiers, bosses, war bonds, the General's shop. */
class WarState(val g: GameViewModel) : Feature {
    override val key = "war"
    override fun save(j: JSONObject) {}
    override fun load(j: JSONObject) {}
    override fun reset() {}

    /** war bonds, the war's money; other shops take them too */
    var bonds by mutableLongStateOf(0L)
}

@Composable
fun WarScreen(g: GameViewModel) = ComingSoon(g, "War")
