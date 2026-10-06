package com.kevin.lemonade

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min

/**
 * Space: the planets, the other Kevins, space missions, the Space Station, and Darth Lime.
 * Ported from the original's SPACE / SPACE ADVENTURE / SPACE STUFF sections (lines 2986-3948).
 * Only the persisted bits (upgrade levels, pitchers sold, missions, Darth Lime, the rocket's paint
 * job) live here; the live map/world/station/duel simulation is [SpaceSim], recreated each visit.
 */
class SpaceState(val g: GameViewModel) : Feature {
    override val key = "space"

    /** the Space Station's upgrade levels (the original's sLvl) */
    val lvl = mutableStateMapOf<String, Int>()
    /** pitchers sold on each world, keyed by world id (the original's worlds[id].sold) */
    val sold = mutableStateMapOf<String, Int>()
    var visited by mutableStateOf<Set<String>>(emptySet())
    var stationVisited by mutableStateOf(false)
    var stationBought by mutableStateOf(false)
    /** true once Darth Lime is defeated at the Lime Star - also a trophy condition */
    var darthDefeated by mutableStateOf(false)
    var missionIdx by mutableIntStateOf(0)
    var rocketColor by mutableStateOf(SPACE_PAINTS[0].color)
    var paintsOwned by mutableStateOf(setOf(SPACE_PAINTS[0].color))

    fun lv(id: String) = lvl[id] ?: 0
    fun totalSold() = sold.values.sum()
    /** fruit needed to fill a space pitcher (the original's spaceNeed) */
    fun need() = max(1, 5 - lv("hyper"))
    /** lemon-saber swings needed to beat Darth Lime (the original's duelNeed) */
    fun duelNeed() = max(10, 32 - 5 * lv("shield"))
    /** the "Lemon saber" upgrade: War's arrows and Sgt. Kevin hit 50% harder. Cross-feature value for War.kt. */
    fun saberMult() = if (lv("saber") > 0) 1.5 else 1.0
    /** a world's pitcher price (the original's spacePrice) */
    fun price(w: SpaceWorld): Long {
        var m = (1.0 + 0.5 * lv("galaxy")) * (1.0 + 0.25 * lv("tractor"))
        if (g.closet.wearing("space")) m *= 1.5
        if (g.closet.wearing("spacesuit")) m *= 1.25
        if (g.closet.wearingSet("astronaut")) m *= 2.0
        return Math.round(w.price * m)
    }

    override fun save(j: JSONObject) {
        j.put("lvl", JSONObject(lvl.toMap()))
        j.put("sold", JSONObject(sold.toMap()))
        j.put("visited", JSONArray(visited.toList()))
        j.put("stationVisited", stationVisited)
        j.put("stationBought", stationBought)
        j.put("darthDefeated", darthDefeated)
        j.put("missionIdx", missionIdx)
        j.put("rocketColor", rocketColor.toArgb())
        j.put("paintsOwned", JSONArray(paintsOwned.map { it.toArgb() }))
    }

    override fun load(j: JSONObject) {
        j.optJSONObject("lvl")?.let { o -> o.keys().forEach { lvl[it] = o.getInt(it) } }
        j.optJSONObject("sold")?.let { o -> o.keys().forEach { sold[it] = o.getInt(it) } }
        j.optJSONArray("visited")?.let { a -> visited = (0 until a.length()).map { a.getString(it) }.toSet() }
        stationVisited = j.optBoolean("stationVisited", false)
        stationBought = j.optBoolean("stationBought", false)
        darthDefeated = j.optBoolean("darthDefeated", false)
        missionIdx = j.optInt("missionIdx", 0)
        rocketColor = Color(j.optInt("rocketColor", SPACE_PAINTS[0].color.toArgb()))
        j.optJSONArray("paintsOwned")?.let { a -> paintsOwned = (0 until a.length()).map { Color(a.getInt(it)) }.toSet() }
        if (paintsOwned.isEmpty()) paintsOwned = setOf(SPACE_PAINTS[0].color)
    }

    override fun reset() {
        lvl.clear(); sold.clear(); visited = emptySet()
        stationVisited = false; stationBought = false; darthDefeated = false; missionIdx = 0
        rocketColor = SPACE_PAINTS[0].color; paintsOwned = setOf(SPACE_PAINTS[0].color)
    }

    // Rebirth keeps Space entirely (the original only resets money + Mr. Zest's shop on rebirth),
    // so onRebirth() is deliberately left at Feature's default - do not override it here.

    /** the MAX OUT code: every Space Station upgrade maxed (the original's maxEverything, 8502-8526) */
    override fun maxOut() {
        for (u in SPACE_SHOP) lvl[u.id] = u.max
    }

    /** the "Finish every space mission" secret code does the work here; Fun/Codes triggers it */
    fun finishAllMissions() {
        visited = ALL_WORLDS.toSet(); stationVisited = true; stationBought = true; darthDefeated = true
        missionIdx = SPACE_MISSIONS.size
    }
}

@Composable
fun SpaceScreen(g: GameViewModel) {
    val scope = rememberCoroutineScope()
    val sim = remember { SpaceSim(g, scope) { g.screen = Screen.STAND } }
    var frame by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) withFrameNanos { now ->
            val dt = if (last == 0L) 0f else min(0.05f, (now - last) / 1e9f)
            last = now
            sim.update(dt)
            frame = now
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ChunkyButton("Back to Earth", color = CARD, enabled = !sim.busy) { g.screen = Screen.STAND }
            Spacer(Modifier.weight(1f))
            MoneyText(g, 24)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTitle(
            when {
                sim.mode == "map" -> "Solar system"
                sim.mode == "station" -> "The Space Station"
                sim.mode == "duel" -> "The Lime Star"
                else -> SPACE_WORLDS[sim.mode]?.let { "${it.place}: ${it.fruit.removeSuffix("y")}${if (it.fruit.endsWith("y")) "ies" else "s"}" } ?: "Space"
            }, 24, Color(0xFF9ED8F5),
        )
        Spacer(Modifier.height(6.dp))
        val shape = RoundedCornerShape(24.dp)
        Box(Modifier.fillMaxWidth().background(LINE, shape).padding(bottom = 6.dp).aspectRatio(SW / SH).clip(shape).border(4.dp, LINE, shape)) {
            Canvas(Modifier.fillMaxSize().pointerInput(Unit) {
                awaitEachGesture {
                    val sc = size.width / SW
                    val down = awaitFirstDown()
                    down.consume()
                    sim.tapStuffOrKevin(down.position.x / sc, down.position.y / sc)
                    while (true) {
                        val ev = awaitPointerEvent()
                        val c = ev.changes.firstOrNull() ?: break
                        c.consume()
                        if (!c.pressed) break
                    }
                }
            }) {
                frame.let { }
                scale(size.width / SW, size.width / SW, Offset.Zero) { sim.draw(this, System.currentTimeMillis() % 1_000_000L) }
            }
            if (sim.mode == "map" && sim.kevinQuipOn) {
                Box(Modifier.align(Alignment.TopCenter).padding(top = 10.dp).background(LINE, CircleShape).padding(horizontal = 14.dp, vertical = 5.dp)) {
                    Text(sim.kevinQuip, color = PEEL, fontWeight = FontWeight.Black, fontSize = 13.sp, textAlign = TextAlign.Center)
                }
            }
            if (sim.mode != "map" && sim.bubbleOn) {
                Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp).background(Color.White, RoundedCornerShape(14.dp))
                    .border(3.dp, LINE, RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(sim.bubble, color = LINE, fontSize = 13.sp, textAlign = TextAlign.Center)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        when {
            sim.mode == "map" -> SpaceMissionsPanel(g)
            sim.mode == "station" -> SpaceStationPanel(g, sim)
            sim.mode == "duel" -> SpaceDuelPanel(sim, scope)
            else -> SpaceWorldPanel(g, sim, scope)
        }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun SpaceBackToMapRow(sim: SpaceSim) {
    ChunkyButton("Back to the map", color = CARD, enabled = !sim.busy) { sim.showMapView() }
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun SpaceMissionsPanel(g: GameViewModel) {
    val sp = g.space
    Text(
        "Tap a planet to visit it. The Space Station is top-left; the Lime Star is bottom-right.",
        fontSize = 13.sp, textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(10.dp))
    Text("Space missions", fontFamily = Lilita, fontSize = 20.sp)
    Spacer(Modifier.height(4.dp))
    for ((i, m) in SPACE_MISSIONS.withIndex()) {
        val st = if (i < sp.missionIdx) "done" else if (i == sp.missionIdx) "now" else "later"
        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (st == "done") "✔" else if (st == "now") "★" else "•",
                fontWeight = FontWeight.Black,
                color = when (st) { "done" -> Color(0xFF4FA35A); "now" -> PEEL; else -> LINE.copy(alpha = 0.4f) },
            )
            Spacer(Modifier.width(8.dp))
            Text(m.text, Modifier.weight(1f), fontSize = 14.sp)
            Text(fmt(m.reward), fontWeight = FontWeight.Black, fontSize = 13.sp)
        }
    }
    if (sp.missionIdx >= SPACE_MISSIONS.size) {
        Spacer(Modifier.height(6.dp))
        Text("You did every mission. ${g.name} is a true LEMON JEDI!", fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SpaceWorldPanel(g: GameViewModel, sim: SpaceSim, scope: CoroutineScope) {
    val w = SPACE_WORLDS[sim.mode] ?: return
    SpaceBackToMapRow(sim)
    Text("Pitchers sold here: ${g.space.sold[w.id] ?: 0}. Each one sells for ${fmt(g.space.price(w))}.", fontSize = 13.sp, textAlign = TextAlign.Center)
    Spacer(Modifier.height(10.dp))
    ChunkyButton("Squeeze a ${w.fruit}", big = true, enabled = !sim.busy) { scope.launch { sim.spaceSqueeze() } }
}

@Composable
private fun SpaceStationPanel(g: GameViewModel, sim: SpaceSim) {
    SpaceBackToMapRow(sim)
    Text("Space Station shop", fontFamily = Lilita, fontSize = 22.sp)
    for (u in SPACE_SHOP) {
        val level = g.space.lv(u.id)
        UpgradeCard(
            name = u.name, desc = u.desc, level = level, max = u.max, cost = u.cost(level),
            canBuy = g.money >= u.cost(level), levelText = "Level $level of ${u.max}",
        ) { sim.buyUpgrade(u) }
    }
    Spacer(Modifier.height(16.dp))
    Text("Paint ${g.name}'s rocket", fontFamily = Lilita, fontSize = 20.sp)
    Spacer(Modifier.height(4.dp))
    for (p in SPACE_PAINTS) {
        val owned = g.space.paintsOwned.contains(p.color)
        val on = g.space.rocketColor == p.color
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                .clickable(enabled = owned || g.money >= p.cost) { sim.buyPaint(p) }
                .padding(vertical = 6.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(20.dp).background(p.color, CircleShape).border(2.dp, LINE, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(p.name, Modifier.weight(1f), fontSize = 14.sp)
            Text(if (on) "Using it!" else if (owned) "Owned" else fmt(p.cost), fontWeight = FontWeight.Black, fontSize = 13.sp)
        }
    }
}

@Composable
private fun SpaceDuelPanel(sim: SpaceSim, scope: CoroutineScope) {
    SpaceBackToMapRow(sim)
    when (sim.duel?.phase) {
        "fight" -> ChunkyButton("Swing your lemon saber!", big = true, color = Color(0xFFFFE36B)) { scope.launch { sim.spaceSqueeze() } }
        "lost" -> ChunkyButton("Try again!", big = true) { scope.launch { sim.spaceSqueeze() } }
        else -> {}
    }
}
