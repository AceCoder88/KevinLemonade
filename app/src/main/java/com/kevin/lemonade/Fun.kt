package com.kevin.lemonade

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import org.json.JSONObject

/** Lottery, races, trophies, rebirth, surprise events, secret codes. */
class FunState(val g: GameViewModel) : Feature {
    override val key = "fun"
    override fun save(j: JSONObject) {}
    override fun load(j: JSONObject) {}
    override fun reset() {}

    var rebirths by mutableIntStateOf(0)
    fun rebirthMult() = 1 + 0.25 * rebirths
}

@Composable
fun LottoScreen(g: GameViewModel) = ComingSoon(g, "Lemon Lottery")

@Composable
fun RaceScreen(g: GameViewModel) = ComingSoon(g, "Lemon Races")

@Composable
fun TrophyScreen(g: GameViewModel) = ComingSoon(g, "Trophies")

@Composable
fun RebirthScreen(g: GameViewModel) = ComingSoon(g, "Rebirth")

@Composable
fun CodesScreen(g: GameViewModel) = ComingSoon(g, "Secret codes")

@Composable
fun SettingsScreen(g: GameViewModel) = ComingSoon(g, "Settings")
