package com.kevin.lemonade

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sign
import kotlin.math.sin
import kotlin.random.Random

/**
 * Digging: the hole next to the stand, ores, hired miners, and (if you dig too deep) falling
 * into the Underworld. Ported from original/kevin.html lines ~6989-7293 (mineShop/ml/ORES/world)
 * plus the "Horrible Hole" variant at ~7251-7268 / 7335-7343.
 */
enum class DigTheme { NORMAL, HORRIBLE }

data class DigUpgrade(
    val id: String, val name: String, val desc: String,
    val base: Double, val grow: Double, val max: Int, val war: Boolean = false,
) {
    fun cost(level: Int): Long = (base * grow.pow(level)).roundToLong()
}

/** Note: unlike shop/lemonShop/warShop/pirateShop/spaceShop, the original never adds +2 to mineShop's maxes. */
object DigShop {
    val all = listOf(
        DigUpgrade("power", "Sharper shovel", "Dig 40% faster", 60.0, 1.7, 10),
        DigUpgrade("lantern", "Brighter lantern", "Your circle of light gets bigger (it's pitch black down there!)", 50.0, 1.8, 7),
        DigUpgrade("detector", "Ore detector", "Ores sparkle through the darkness", 120.0, 2.0, 6),
        DigUpgrade("lucky", "Lucky dirt", "More ores show up when you dig", 100.0, 1.9, 7),
        DigUpgrade("polish", "Ore polish", "Ores sell for 25% more", 80.0, 1.8, 10),
        DigUpgrade("drill", "Power drill", "Dig a 3-wide tunnel with every dig", 800.0, 3.0, 3),
        DigUpgrade("miners", "Hire a lemon miner", "A miner digs for ores down here all by himself, AND joins the war to fight moles in the tunnel", 150.0, 2.2, 6, war = true),
        DigUpgrade("pick", "Golden pickaxes", "Your miners dig faster, hit harder in war, and take 2 more hits", 120.0, 1.9, 8, war = true),
    )
}

private class OreInfo(val name: String, val value: Long, val color: Color, val spark: Color)
private val ORES = linkedMapOf(
    "coal" to OreInfo("Coal", 5, Color(0xFF2A2A2A), Color(0xFF9AA7B1)),
    "iron" to OreInfo("Iron", 15, Color(0xFFC9A27A), Color(0xFFE8D2B8)),
    "gold" to OreInfo("Gold", 40, Color(0xFFFFD21F), Color(0xFFFFF3A0)),
    "diamond" to OreInfo("Diamond", 120, Color(0xFF7FE3FF), Color(0xFFE6FCFF)),
    "crystal" to OreInfo("Lemon crystal", 300, Color(0xFFE9FF5C), Color(0xFFFFFFC0)),
)

private const val DCOLS = 20
private const val DROWS = 60
private const val DTILE = 40f
private const val DW = 800f
private const val DH = 500f
private const val FALL_ROW = 48

class DigCell(var type: String, var ore: String? = null)
class DigMiner(var r: Int = 0, var c: Int = 10, var digT: Float = 0f, var moveT: Float = 0f, var face: Int = 1, var falling: Boolean = false)
class DigDigger(var r: Int, var c: Int, val i: Int) {
    var digT = 0f; var moveT = 0f; var face = 1; var dr = 1; var dc = 0; var steps = 0
}
class DigFloat(val x: Float, var y: Float, val text: String, var life: Float)
private class DigWorld {
    var cells: Array<Array<DigCell>> = emptyArray()
    var miner = DigMiner()
}

private val TILE_COLORS = mapOf(
    "dirt" to (Color(0xFF8B5A2B) to Color(0xFF7A4E24)),
    "stone" to (Color(0xFF7E7E86) to Color(0xFF6E6E76)),
    "hard" to (Color(0xFF4E4A58) to Color(0xFF433F4C)),
    "soul" to (Color(0xFF4A2A6E) to Color(0xFF3E2260)),
)
private val HORRIBLE_COLORS = mapOf(
    "dirt" to (Color(0xFF5A1418) to Color(0xFF4E1014)),
    "stone" to (Color(0xFF4A1A2A) to Color(0xFF401624)),
    "hard" to (Color(0xFF2E0A14) to Color(0xFF260810)),
    "soul" to (Color(0xFF1A0006) to Color(0xFF140004)),
)

class DigState(val g: GameViewModel) : Feature {
    override val key = "dig"

    /** mine shop levels, PLUS "miners" and "pick" - the original keeps those in the war's level
     * map (they're mineShop items with war:true) so miners also fight moles in War.
     * INTEGRATION: War doesn't exist yet in this port. When it does, it should read
     * g.dig.lv("miners") for how many lemon miners join the wall to fight moles, and
     * g.dig.lv("pick") for their war bonus (original: miners hit harder and take 2 more hits). */
    val lvl = mutableStateMapOf<String, Int>()
    fun lv(id: String) = lvl[id] ?: 0

    /** Public mode. Default (NORMAL) is what the Hub's "Dig" place and the stage's dig hole should
     * use. To open the Nightmare Realm's "Horrible Hole" variant instead, set
     * `g.dig.theme = DigTheme.HORRIBLE` *before* setting `g.screen = Screen.DIG`. Cursed ores are
     * worth 3x and the tunnel is colored red/black; everything else plays the same. */
    var theme by mutableStateOf(DigTheme.NORMAL)

    private val worlds = HashMap<DigTheme, DigWorld>()
    val diggers = ArrayList<DigDigger>()
    val floats = ArrayList<DigFloat>()
    var banner by mutableStateOf("")
    var bannerT = 0f
    var bannerOn by mutableStateOf(false)
    private var warned = false
    /** where the player is currently holding on the canvas, in world pixels (null = not holding) */
    var heldPoint: Offset? = null
    /** set once you've ever dug up a diamond - for the Trophies screen (the original's dugDiamond) */
    var dugDiamond by mutableStateOf(false)

    private fun luckMult() = 1.0 + 0.25 * lv("lucky") + 0.2 * g.lv("oreluck")

    private fun generate(w: DigWorld) {
        val cells = Array(DROWS) { r ->
            Array(DCOLS) { _ ->
                if (r == 0) DigCell("air")
                else {
                    val t = if (r < 12) "dirt" else if (r < 30) "stone" else if (r < 48) "hard" else "soul"
                    val luck = luckMult()
                    val roll = Random.nextDouble()
                    val ore = when {
                        r >= 40 && roll < 0.015 * luck -> "crystal"
                        r >= 30 && roll < 0.035 * luck -> "diamond"
                        r >= 18 && roll < 0.07 * luck -> "gold"
                        r >= 8 && roll < 0.12 * luck -> "iron"
                        r >= 3 && roll < 0.2 * luck -> "coal"
                        else -> null
                    }
                    DigCell(t, ore)
                }
            }
        }
        w.cells = cells
        w.miner = DigMiner()
    }

    private fun activeWorld(): DigWorld = worlds.getOrPut(theme) { DigWorld().also { generate(it) } }
    fun miner() = activeWorld().miner

    fun mMsg(t: String, secs: Float = 2f) { banner = g.named(t); bannerT = secs; bannerOn = true }

    /** the original's "Climb to the top" button */
    fun climbUp() {
        val m = activeWorld().miner
        m.r = 0; m.digT = 0f; m.moveT = 0f
        mMsg("Back up at the surface!", 1.5f)
    }

    /** the original's "Dig a new spot" button */
    fun newPatch() {
        generate(activeWorld())
        mMsg("A brand new patch of ground to dig!", 1.8f)
    }

    private fun hardness(cell: DigCell): Double =
        (when (cell.type) { "dirt" -> 0.25; "stone" -> 0.5; "hard" -> 0.9; "soul" -> 0.7; else -> 0.0 }) +
            (if (cell.ore != null) 0.2 else 0.0)

    private fun collect(cell: DigCell, r: Int, c: Int) {
        val oreId = cell.ore ?: return
        val info = ORES[oreId] ?: return
        if (oreId == "diamond") dugDiamond = true
        val horrible = theme == DigTheme.HORRIBLE
        val v = Math.round(info.value * (1 + 0.25 * lv("polish")) * (if (horrible) 3.0 else 1.0))
        g.money += v
        floats.add(DigFloat(c * DTILE + DTILE / 2, r * DTILE, "${if (horrible) "Cursed " else ""}${info.name}! +${fmt(v)}", 1.3f))
    }

    private fun digCell(w: DigWorld, r: Int, c: Int) {
        if (r < 1 || r >= DROWS || c < 0 || c >= DCOLS) return
        val cell = w.cells[r][c]
        if (cell.type == "air") return
        collect(cell, r, c)
        cell.type = "air"; cell.ore = null
    }

    private suspend fun fallIntoUnderworld(w: DigWorld) {
        if (theme == DigTheme.HORRIBLE) {
            mMsg("BLARGH! The Horrible Hole spits you back up to the top!", 2.4f)
            w.miner = DigMiner()
            return
        }
        mMsg("AAAAH! You dug too deep...", 2f)
        delay(1600)
        w.miner = DigMiner()
        // INTEGRATION: the original falls on into the Death Realm here (enterRealm()). That screen
        // isn't ported yet (no Screen.REALM / owning feature). For now we just surface you with a line.
        g.say("Whoa! We fell all the way down into the Underworld!")
        g.screen = Screen.STAND
    }

    private fun stepToward(w: DigWorld, dr: Int, dc: Int, dt: Float) {
        val m = w.miner
        val nr = m.r + dr; val nc = m.c + dc
        if (nc < 0 || nc >= DCOLS || nr < 0 || nr >= DROWS) return
        if (dc != 0) m.face = dc
        val cell = w.cells[nr][nc]
        if (cell.type == "air") {
            if (m.moveT <= 0f) { m.r = nr; m.c = nc; m.moveT = 0.12f; m.digT = 0f }
            return
        }
        if (cell.type == "soul" || nr >= FALL_ROW) {
            if (m.falling) return
            m.falling = true; m.r = nr; m.c = nc
            g.viewModelScope.launch { fallIntoUnderworld(w); m.falling = false }
            return
        }
        m.digT += dt
        if (m.digT >= hardness(cell) / (1 + 0.4 * lv("power"))) {
            digCell(w, nr, nc)
            val drill = lv("drill")
            if (drill > 0) {
                for (k in 1..drill) {
                    if (dr != 0) { digCell(w, nr, nc - k); digCell(w, nr, nc + k) }
                    else { digCell(w, nr - k, nc); digCell(w, nr + k, nc) }
                }
            }
            m.r = nr; m.c = nc; m.digT = 0f; m.moveT = 0.1f
        }
    }

    private fun syncDiggers() {
        while (diggers.size < lv("miners")) diggers.add(DigDigger(0, 2 + Random.nextInt(16), diggers.size))
    }

    private fun diggerStep(w: DigWorld, d: DigDigger, dt: Float) {
        if (d.moveT > 0f) { d.moveT -= dt; return }
        if (d.steps <= 0) {
            val opts = listOf(1 to 0, 1 to 0, 0 to 1, 0 to -1)
            val (dr, dc) = opts[Random.nextInt(opts.size)]
            d.dr = dr; d.dc = dc; d.steps = 2 + Random.nextInt(5)
        }
        var dr = d.dr; var dc = d.dc
        if (d.r + dr >= FALL_ROW - 2) { dr = 0; dc = if (Random.nextDouble() < 0.5) 1 else -1; d.dr = 0; d.dc = dc }
        val nr = d.r + dr; val nc = d.c + dc
        if (nc < 0 || nc >= DCOLS) { d.steps = 0; d.dc = -dc; return }
        if (dc != 0) d.face = dc
        val cell = w.cells[nr][nc]
        if (cell.type == "air") { d.r = nr; d.c = nc; d.moveT = 0.18f; d.steps--; return }
        if (cell.type == "soul") { d.steps = 0; return }
        d.digT += dt
        if (d.digT >= hardness(cell) * 1.6 / (1 + 0.4 * lv("power")) / (1 + 0.3 * lv("pick"))) {
            digCell(w, nr, nc); d.r = nr; d.c = nc; d.digT = 0f; d.moveT = 0.12f; d.steps--
        }
    }

    private fun camTop(w: DigWorld): Float = max(0f, min(DROWS * DTILE - DH, w.miner.r * DTILE - 200f))

    /** stepped every frame while DigScreen is on screen (like the original's requestAnimationFrame loop) */
    fun update(dt: Float) {
        val w = activeWorld()
        syncDiggers()
        for (d in diggers) diggerStep(w, d, dt)
        if (bannerT > 0f) { bannerT -= dt; if (bannerT <= 0f) bannerOn = false }
        val m = w.miner
        if (m.moveT > 0f) m.moveT -= dt
        val held = heldPoint
        if (held != null && !m.falling) {
            val camY = camTop(w)
            val mxp = m.c * DTILE + DTILE / 2; val myp = m.r * DTILE + DTILE / 2 - camY
            val dx = held.x - mxp; val dy = held.y - myp
            if (abs(dx) > DTILE / 2 || abs(dy) > DTILE / 2) {
                if (abs(dx) > abs(dy)) stepToward(w, 0, sign(dx).toInt(), dt) else stepToward(w, sign(dy).toInt(), 0, dt)
            }
        } else if (!m.falling) m.digT = 0f
        if (m.r >= 44 && !warned) {
            warned = true
            mMsg("It's getting HOT... the purple Underworld rock is just below. You can't dig it, and if you touch it you'll fall in!", 3.5f)
        }
        if (m.r < 40) warned = false
        for (f in floats) { f.y -= 30f * dt; f.life -= dt }
        floats.removeAll { it.life <= 0f }
    }

    fun camTopNow(): Float = camTop(activeWorld())
    fun worldRows(): Array<Array<DigCell>> = activeWorld().cells

    override fun save(j: JSONObject) {
        j.put("lvl", JSONObject(lvl.toMap()))
        j.put("dugDiamond", dugDiamond)
    }
    override fun load(j: JSONObject) {
        j.optJSONObject("lvl")?.let { o -> o.keys().forEach { lvl[it] = o.getInt(it) } }
        dugDiamond = j.optBoolean("dugDiamond", false)
    }
    override fun reset() {
        lvl.clear(); theme = DigTheme.NORMAL; worlds.clear(); diggers.clear(); floats.clear()
        bannerOn = false; warned = false; heldPoint = null; dugDiamond = false
    }
    /** the secret MAX OUT code: matches the original's maxEverything() for the mineShop (8502-8526) */
    override fun maxOut() {
        for (u in DigShop.all) lvl[u.id] = u.max
        syncDiggers()
    }
}

@Composable
fun DigScreen(g: GameViewModel) {
    val dig = g.dig
    var frame by remember { mutableLongStateOf(0L) }
    LaunchedEffect(dig.theme) {
        var last = 0L
        while (true) withFrameNanos { now ->
            val dt = if (last == 0L) 0f else min(0.05f, (now - last) / 1e9f)
            last = now
            dig.update(dt)
            frame = now
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ChunkyButton("Back to lemonade", color = CARD) { g.screen = Screen.STAND }
            Spacer(Modifier.weight(1f))
            MoneyText(g, 26)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTitle(if (dig.theme == DigTheme.HORRIBLE) "The Horrible Hole" else "Digging", 30, Color(0xFFE9B630))
        Text("Depth: ${dig.miner().r} m", fontWeight = FontWeight.Black, fontSize = 15.sp)
        Spacer(Modifier.height(8.dp))
        val shape = RoundedCornerShape(24.dp)
        Box(Modifier.fillMaxWidth().background(LINE, shape).padding(bottom = 6.dp).aspectRatio(DW / DH).clip(shape).border(4.dp, LINE, shape)) {
            Canvas(Modifier.fillMaxSize().pointerInput(Unit) {
                awaitEachGesture {
                    val sc = size.width / DW
                    val down = awaitFirstDown()
                    down.consume()
                    dig.heldPoint = Offset(down.position.x / sc, down.position.y / sc + dig.camTopNow())
                    while (true) {
                        val ev = awaitPointerEvent()
                        val c = ev.changes.firstOrNull() ?: break
                        c.consume()
                        dig.heldPoint = Offset(c.position.x / sc, c.position.y / sc + dig.camTopNow())
                        if (!c.pressed) break
                    }
                    dig.heldPoint = null
                }
            }) {
                frame.let { }
                scale(size.width / DW, size.width / DW, Offset.Zero) { drawMine(dig) }
            }
            if (dig.bannerOn) {
                Box(Modifier.align(Alignment.TopCenter).padding(top = 10.dp).background(LINE, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)) {
                    Text(dig.banner, color = PEEL, fontWeight = FontWeight.Black, fontSize = 14.sp, textAlign = TextAlign.Center)
                }
            }
        }
        Text("Tap and hold where you want to dig. Find ores, but don't dig too deep!", fontSize = 13.sp, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(8.dp))
        Row {
            ChunkyButton("Climb to the top", color = CARD) { dig.climbUp() }
            Spacer(Modifier.width(8.dp))
            ChunkyButton("Dig a new spot", color = CARD) { dig.newPatch() }
        }
        Spacer(Modifier.height(10.dp))
        Text("Dig shop", fontFamily = Lilita, fontSize = 22.sp)
        for (u in DigShop.all) {
            val level = dig.lv(u.id)
            val locked = u.id == "pick" && dig.lv("miners") == 0
            UpgradeCard(
                name = u.name,
                desc = if (locked) "Hire a lemon miner first" else u.desc,
                level = level, max = u.max, cost = u.cost(level), canBuy = g.money >= u.cost(level),
                levelText = if (u.id == "miners") "Miners: $level of ${u.max}" else null,
                locked = locked,
            ) {
                if (locked) return@UpgradeCard
                val c = u.cost(level)
                if (g.money < c || level >= u.max) return@UpgradeCard
                g.money -= c
                dig.lvl[u.id] = level + 1
                if (u.id == "miners") dig.mMsg("A lemon miner joins you! He'll fight moles in the war too.", 2f)
            }
        }
        Spacer(Modifier.height(30.dp))
    }
}

private fun DrawScope.diggerFigure(x: Float, y: Float, face: Int, digging: Boolean, t: Long, seed: Int, hatColor: Color, pickGold: Boolean, scaleF: Float = 1f) {
    translate(x, y) {
        scale(scaleF, scaleF, Offset.Zero) {
            oval(0f, 2f, 12f, 15f, Color(0xFFFFD21F))
            drawArc(hatColor, 180f, 180f, true, Offset(-13f, -16f), Size(26f, 14f))
            drawArc(LINE, 180f, 180f, false, Offset(-13f, -16f), Size(26f, 14f), style = androidx.compose.ui.graphics.drawscope.Stroke(2.5f))
            drawCircle(Color(0xFFFFF6A0), 3f, Offset(face * 6f, -11f))
            drawCircle(LINE, 2f, Offset(face * 4f, 0f))
            val swing = if (digging) -0.8f + sin(t / 60f + seed) * 0.8f else -0.4f
            rotate(swing * 57.3f, Offset(face * 10f, 2f)) {
                drawLine(Color(0xFF6B4A1E), Offset(face * 10f, 2f), Offset(face * 10f, -14f), 3f, StrokeCap.Round)
                val pc = if (pickGold) Color(0xFFE9B630) else Color(0xFF9AA7B1)
                drawPath(androidx.compose.ui.graphics.Path().apply {
                    moveTo(face * 10f - 6f, -14f); lineTo(face * 10f + 6f, -14f); lineTo(face * 10f, -22f); close()
                }, pc)
            }
        }
    }
}

private fun DrawScope.drawMine(dig: DigState) {
    val w = dig.worldRows()
    val m = dig.miner()
    val camY = dig.camTopNow()
    val horrible = dig.theme == DigTheme.HORRIBLE
    val tileColors = if (horrible) HORRIBLE_COLORS else TILE_COLORS
    drawRect(Color(0xFF1A1020), Offset.Zero, Size(DW, DH))
    if (camY < DTILE) drawRect(Color(0xFF9ED8F5), Offset(0f, 0f), Size(DW, DTILE - camY))
    val t = System.currentTimeMillis() % 1_000_000L
    val r0 = (camY / DTILE).toInt().coerceAtLeast(0)
    val r1 = min(DROWS - 1, r0 + (DH / DTILE).toInt() + 1)
    for (r in r0..r1) for (c in 0 until DCOLS) {
        val cell = w[r][c]
        val x = c * DTILE; val y = r * DTILE - camY
        if (cell.type == "air") { if (r > 0) drawRect(Color(0xFF2A1A10), Offset(x, y), Size(DTILE, DTILE)); continue }
        val (c0, c1) = tileColors[cell.type] ?: (Color.Gray to Color.DarkGray)
        drawRect(if ((r + c) % 2 == 0) c1 else c0, Offset(x, y), Size(DTILE, DTILE))
        if (r == 1) drawRect(Color(0xFF6DBA58), Offset(x, y), Size(DTILE, 8f))
        drawRect(Color.Black.copy(alpha = 0.12f), Offset(x + 6f, y + 10f), Size(5f, 4f))
        drawRect(Color.Black.copy(alpha = 0.12f), Offset(x + 24f, y + 26f), Size(6f, 4f))
        cell.ore?.let { oreId ->
            val info = ORES[oreId]!!
            for ((ox, oy, s) in listOf(Triple(10f, 12f, 6f), Triple(26f, 18f, 7f), Triple(16f, 28f, 5f))) {
                val p = androidx.compose.ui.graphics.Path().apply {
                    moveTo(x + ox, y + oy - s); lineTo(x + ox + s, y + oy); lineTo(x + ox, y + oy + s); lineTo(x + ox - s, y + oy); close()
                }
                drawPath(p, info.color)
                drawPath(p, Color.Black.copy(alpha = 0.45f), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f))
            }
        }
        if (cell.type == "soul") drawRect(Color(0xFFC8A0FF).copy(alpha = 0.15f + 0.1f * sin(t / 300f + c + r)), Offset(x, y), Size(DTILE, DTILE))
    }

    val mx = m.c * DTILE + DTILE / 2; val my = m.r * DTILE + DTILE / 2 - camY
    diggerFigure(mx, my, m.face, m.digT > 0f, t, 0, Color(0xFFF2C200), false, 1f)
    val digHats = listOf(Color(0xFFE8483B), Color(0xFF2E9BD6), Color(0xFF7B5CFF), Color(0xFF4FA34A), Color(0xFFFF7FB0), Color(0xFFE9B630))
    for (d in dig.diggers) {
        val dx = d.c * DTILE + DTILE / 2; val dy = d.r * DTILE + DTILE / 2 - camY
        if (dy < -DTILE || dy > DH + DTILE) continue
        diggerFigure(dx, dy, d.face, d.digT > 0f, t, d.i, digHats[d.i % digHats.size], dig.lv("pick") > 0, 0.8f)
    }

    // pitch black underground except circles of light around the miner and hired diggers
    if (m.r > 1 || dig.diggers.any { it.r > 1 }) {
        val canvas = drawContext.canvas.nativeCanvas
        val save = canvas.saveLayer(0f, 0f, DW, DH, null)
        val baseColor = if (horrible) android.graphics.Color.argb(255, 0x14, 0x00, 0x03) else android.graphics.Color.BLACK
        val basePaint = android.graphics.Paint().apply { color = baseColor }
        canvas.drawRect(0f, max(0f, DTILE - camY), DW, DH, basePaint)
        val holePaint = android.graphics.Paint().apply {
            xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_OUT)
        }
        fun hole(x: Float, y: Float, light: Float) {
            holePaint.shader = android.graphics.RadialGradient(
                x, y, light,
                intArrayOf(android.graphics.Color.BLACK, android.graphics.Color.TRANSPARENT),
                floatArrayOf(0f, 1f), android.graphics.Shader.TileMode.CLAMP,
            )
            canvas.drawCircle(x, y, light, holePaint)
        }
        hole(mx, my, if (m.r <= 1) 2000f else 64f + 30f * dig.lv("lantern"))
        for (d in dig.diggers) hole(d.c * DTILE + DTILE / 2, d.r * DTILE + DTILE / 2 - camY, 46f + 10f * dig.lv("lantern"))
        canvas.restoreToCount(save)
    }

    if (dig.lv("detector") > 0) {
        val rad = 2 + 2 * dig.lv("detector")
        for (r in r0..r1) for (c in 0 until DCOLS) {
            val cell = w[r][c]
            val oreId = cell.ore ?: continue
            if (abs(r - m.r) > rad || abs(c - m.c) > rad) continue
            val s = 3f + 2f * abs(sin(t / 250f + r * 3 + c))
            drawCircle(ORES[oreId]!!.spark, s, Offset(c * DTILE + DTILE / 2, r * DTILE + DTILE / 2 - camY))
        }
    }

    val uwY = FALL_ROW * DTILE - camY
    if (uwY < DH) drawRect(Color(0xFF9B6CFF).copy(alpha = 0.25f), Offset(0f, uwY), Size(DW, DH - uwY))

    for (f in dig.floats) {
        val a = max(0f, f.life)
        drawTextOutlined(f.text, f.x, f.y - camY, 16f, PEEL, Color(0xFF1A1020), a)
    }
}

private val digLabelPaint = android.graphics.Paint().apply { isAntiAlias = true; textAlign = android.graphics.Paint.Align.CENTER; isFakeBoldText = true }
private fun DrawScope.drawTextOutlined(text: String, x: Float, y: Float, size: Float, fill: Color, outline: Color, alpha: Float = 1f) {
    val c = drawContext.canvas.nativeCanvas
    digLabelPaint.textSize = size
    digLabelPaint.style = android.graphics.Paint.Style.STROKE; digLabelPaint.strokeWidth = 4f
    digLabelPaint.color = android.graphics.Color.argb((alpha * 255).toInt().coerceIn(0, 255), (outline.red * 255).toInt(), (outline.green * 255).toInt(), (outline.blue * 255).toInt())
    c.drawText(text, x, y, digLabelPaint)
    digLabelPaint.style = android.graphics.Paint.Style.FILL
    digLabelPaint.color = android.graphics.Color.argb((alpha * 255).toInt().coerceIn(0, 255), (fill.red * 255).toInt(), (fill.green * 255).toInt(), (fill.blue * 255).toInt())
    c.drawText(text, x, y, digLabelPaint)
}
