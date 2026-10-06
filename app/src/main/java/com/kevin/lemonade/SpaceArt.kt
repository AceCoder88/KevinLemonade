package com.kevin.lemonade

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

// ----- the 800x500 space canvas, same layout as the original's #spaceCanvas -----
const val SW = 800f
const val SH = 500f
private const val SUN_X = 400f
private const val SUN_Y = 250f
private const val SUN_R = 50f
private const val TILT = 0.52f
private const val STATION_X = 92f
private const val STATION_Y = 66f
private const val LIMESTAR_X = 712f
private const val LIMESTAR_Y = 430f
private const val BLACKHOLE_X = 64f
private const val BLACKHOLE_Y = 436f

private class SpAsteroid(var a: Float, var gone: Float = 0f)
private class SpComet(var x: Float, var y: Float, val vx: Float, val vy: Float)
private class SpWhale(var x: Float, val y0: Float, var t: Float = 0f)
private class SpChest(var x: Float, var y: Float, val vy: Float)
private class SpFloat(val text: String, val x: Float, var y: Float, var life: Float)
private class SpAlien(var t: Float, val color: Color)
/** state of a Darth Lime duel; "talk" -> "fight" -> "won"/"lost". Public so SpaceDuelPanel can read [phase]. */
class SpDuel(var phase: String, var taps: Int = 0, var time: Float = 12f, var swing: Float = 0f, var hit: Float = 0f, var boom: Float = 0f)

/**
 * Everything that happens on the Space screen while it's open: the solar system map, flying to a
 * world and squeezing its fruit family, the Space Station shop, and the Darth Lime duel. Ported
 * from the original's spaceMode/sLoop/worlds/stuff/duel globals. Nothing here is saved - only the
 * persisted bits live on [SpaceState] ([SpaceSim.sp]); this class is recreated each time you visit.
 */
class SpaceSim(val g: GameViewModel, val scope: CoroutineScope, val onLeave: () -> Unit) {
    val sp get() = g.space

    /** "map" | "station" | "duel" | a world id like "sun"/"moon"/"mars"/... */
    var mode by mutableStateOf("map")
    var busy by mutableStateOf(false)

    /** the shared speech bubble used for worlds / the station droid / Darth Lime (the original's #sBubble) */
    var bubble by mutableStateOf("")
    var bubbleOn by mutableStateOf(false)
    /** Kevin's co-pilot chatter, shown only on the map (the original's #sKevin) */
    var kevinQuip by mutableStateOf("")
    var kevinQuipOn by mutableStateOf(false)
    private var kQuipT = 0f
    private var chatterT = 10f

    val starsXY = List(120) { Triple(Random.nextFloat() * SW, Random.nextFloat() * SH, Random.nextFloat() * 1.6f + 0.3f) }
    private val orbitA = HashMap<String, Float>().apply { for (o in SPACE_ORBIT) put(o.id, o.startAngle) }
    private var moonA = 0f

    // ---- squeezing a world's fruit family ----
    var kFace = "happy"
    var kLookX = 420f; var kLookY = 218f
    var squashY = 1f; var dropY = -80f; var pressY = 0f
    var streamOn = false
    var sLevel = 0f
    private val bags = HashMap<String, MutableList<SpaceFamily>>()
    private val inPitcher = HashMap<String, Int>()
    private var robotT = 0f
    private val aliens = ArrayList<SpAlien>()

    // ---- asteroids, a comet, a space whale, floating treasure, a black hole ----
    private val asteroids = (0 until 7).map { SpAsteroid(it * 0.9f) }
    private var comet: SpComet? = null
    private var whale: SpWhale? = null
    private val chests = ArrayList<SpChest>()
    private val sFloat = ArrayList<SpFloat>()
    private var stuffComet = 12f
    private var stuffWhale = 40f
    private var stuffChest = 20f
    private var holeCd = 0f

    // ---- Darth Lime ----
    var duel: SpDuel? = null

    // ---------------- per-frame update, mirrors the original's sLoop branches ----------------
    fun update(dt: Float) {
        if (kQuipT > 0) { kQuipT -= dt; if (kQuipT <= 0) kevinQuipOn = false }
        when {
            mode == "map" -> {
                for (o in SPACE_ORBIT) orbitA[o.id] = (orbitA[o.id] ?: o.startAngle) + o.speed * dt
                moonA += 0.5f * dt
                stuffUpdate(dt)
                chatterT += dt
                if (chatterT > 15f) { chatterT = 0f; kSay(SPACE_CHATTER.pick()) }
            }
            mode == "duel" -> duel?.let { duelTick(it, dt) }
            SPACE_WORLDS.containsKey(mode) -> {
                for (al in aliens) al.t += dt
                aliens.removeAll { it.t >= 4.2f }
                if (sp.lv("robot") > 0 && !busy) {
                    robotT += dt
                    if (robotT >= max(1f, 6f - sp.lv("robot"))) { robotT = 0f; scope.launch { spaceSqueeze() } }
                }
            }
        }
    }

    private fun stuffUpdate(dt: Float) {
        for (a in asteroids) { a.a += 0.025f * dt; if (a.gone > 0) a.gone -= dt }
        stuffComet -= dt; stuffWhale -= dt; stuffChest -= dt
        if (holeCd > 0) holeCd -= dt
        if (comet == null && stuffComet <= 0f) { comet = SpComet(-40f, 40f + Random.nextFloat() * 120f, 260f, 70f); stuffComet = 25f + Random.nextFloat() * 15f }
        comet?.let { it.x += it.vx * dt; it.y += it.vy * dt; if (it.x > SW + 60) comet = null }
        if (whale == null && stuffWhale <= 0f) { whale = SpWhale(SW + 120f, 150f + Random.nextFloat() * 60f); stuffWhale = 60f + Random.nextFloat() * 30f }
        whale?.let { it.x -= 45f * dt; it.t += dt; if (it.x < -160f) whale = null }
        if (stuffChest <= 0f && chests.size < 3) { chests.add(SpChest(100f + Random.nextFloat() * 600f, SH + 20f, -22f - Random.nextFloat() * 12f)); stuffChest = 30f + Random.nextFloat() * 20f }
        for (c in chests) c.y += c.vy * dt
        chests.removeAll { it.y < -30f }
        for (f in sFloat) { f.y -= 30f * dt; f.life -= dt }
        sFloat.removeAll { it.life <= 0f }
    }

    private fun sPop(text: String, x: Float, y: Float) { sFloat.add(SpFloat(text, x, y, 1.6f)) }

    fun kSay(t: String, secs: Float = 3.5f) { kevinQuip = "${g.name}: $t"; kevinQuipOn = true; kQuipT = secs }
    private fun sSay(t: String) { bubble = t; bubbleOn = true }

    // ---------------- the map ----------------
    private fun planetPos(o: OrbitBody): Offset {
        val a = orbitA[o.id] ?: o.startAngle
        return Offset(SUN_X + cos(a) * o.r, SUN_Y + sin(a) * o.r * TILT)
    }
    private fun earthAngle() = orbitA["earth"] ?: SPACE_ORBIT[0].startAngle
    private fun moonPos(): Offset {
        val e = planetPos(SPACE_ORBIT[0])
        return Offset(e.x + cos(moonA) * 34f, e.y + sin(moonA) * 18f)
    }
    private fun astPos(a: SpAsteroid, i: Int): Offset {
        val r = 286f + (i % 3) * 9f
        return Offset(SUN_X + cos(a.a) * r, SUN_Y + sin(a.a) * r * TILT)
    }

    fun mapHit(px: Float, py: Float): String? {
        if (hypot(px - STATION_X, py - STATION_Y) < 40) return "station"
        stuffHit(px, py)?.let { return it }
        if (!sp.darthDefeated && hypot(px - LIMESTAR_X, py - LIMESTAR_Y) < 40) return "limestar"
        if (hypot(px - SUN_X, py - SUN_Y) < SUN_R + 10) return "sun"
        val m = moonPos(); if (hypot(px - m.x, py - m.y) < 22) return "moon"
        for (o in SPACE_ORBIT) { val q = planetPos(o); if (hypot(px - q.x, py - q.y) < o.size + 16) return o.id }
        return null
    }

    private fun stuffHit(px: Float, py: Float): String? {
        comet?.let { if (hypot(px - it.x, py - it.y) < 26) return "comet" }
        whale?.let { val wy = it.y0 + sin(it.t * 1.5f) * 8f; if (abs(px - it.x) < 80 && abs(py - wy) < 40) return "whale" }
        for (i in chests.indices) if (hypot(px - chests[i].x, py - chests[i].y) < 22) return "chest:$i"
        for (i in asteroids.indices) { if (asteroids[i].gone > 0) continue; val p = astPos(asteroids[i], i); if (hypot(px - p.x, py - p.y) < 16) return "ast:$i" }
        if (hypot(px - BLACKHOLE_X, py - BLACKHOLE_Y) < 34) return "blackhole"
        return null
    }

    private fun useStuff(id: String): Boolean {
        when {
            id == "comet" -> {
                val c = comet ?: return false
                val v = max(100L, g.price() * 15)
                g.money += v; sPop("Comet caught! +${fmt(v)}", c.x, c.y); comet = null
                kSay(listOf("We caught a COMET! It's so sparkly!", "Comet in the jar! I mean... rocket.").pick())
                return true
            }
            id == "whale" -> {
                val wh = whale ?: return false
                val v = max(200L, g.price() * 25)
                g.money += v; g.sad = 0.0
                sPop("Big Bertha sings! +${fmt(v)}", wh.x, wh.y0 - 20)
                kSay("Big Bertha is SINGING! It's the most beautiful thing I've ever heard. *sniff*")
                wh.x -= 1f
                return true
            }
            id.startsWith("chest:") -> {
                val idx = id.removePrefix("chest:").toIntOrNull() ?: return false
                if (idx < 0 || idx >= chests.size) return false
                val c = chests.removeAt(idx)
                val loot = listOf<() -> String>(
                    { val v = max(150L, g.price() * 12); g.money += v; "+${fmt(v)}!" },
                    // TODO(integration): the original grants +12 limes here (Pirate's currency). PirateState
                    // doesn't expose a lime counter yet - wire this to it once Pirate.kt is ported.
                    { "+12 limes!" },
                    { g.war.bonds += 80; "+80 war bonds!" },
                    { g.addLemons(25); "+25 lemons!" },
                    { g.goldenNext += 3; "3 golden lemons!" },
                ).pick()()
                sPop("Space treasure: $loot", c.x, c.y)
                kSay(listOf("A treasure chest floating in SPACE! Finders keepers!", "Ooh, space loot!").pick())
                return true
            }
            id.startsWith("ast:") -> {
                val idx = id.removePrefix("ast:").toIntOrNull() ?: return false
                val a = asteroids.getOrNull(idx) ?: return false
                val p = astPos(a, idx)
                a.gone = 20f
                val v = 20L + Random.nextInt(60) + g.price()
                g.money += v
                sPop("Space gems! +${fmt(v)}", p.x, p.y)
                return true
            }
            id == "blackhole" -> {
                if (holeCd > 0) { kSay("The black hole is still burping. Wait ${ceil(holeCd).toInt()} seconds!"); return true }
                holeCd = 20f
                val r = Random.nextDouble()
                when {
                    r < 0.3 -> { val v = max(500L, g.price() * 40); g.money += v; sPop("The black hole spit out +${fmt(v)}!", BLACKHOLE_X + 60, BLACKHOLE_Y - 50); kSay("It spit out MONEY! Thanks, black hole!") }
                    // TODO(integration): the original grants +30 limes here too - see the chest TODO above.
                    r < 0.5 -> { sPop("30 limes came out!", BLACKHOLE_X + 60, BLACKHOLE_Y - 50); kSay("Limes from another dimension!") }
                    r < 0.7 -> { g.war.bonds += 200; sPop("+200 war bonds!", BLACKHOLE_X + 60, BLACKHOLE_Y - 50); kSay("War bonds from the void!") }
                    r < 0.9 -> { sPop("Kevin got stretched like spaghetti!", BLACKHOLE_X + 80, BLACKHOLE_Y - 50); kSay("I'm SPAGHETTI! I'm lemon spaghetti! ...Okay, I'm back. That was weird.") }
                    else -> { val v = (g.money * 0.05).toLong(); g.money -= v; sPop("It ate ${fmt(v)}! Burp.", BLACKHOLE_X + 60, BLACKHOLE_Y - 50); kSay("It ATE some of our money! Bad black hole!") }
                }
                return true
            }
            else -> return false
        }
    }

    fun checkMissions() {
        while (sp.missionIdx < SPACE_MISSIONS.size && SPACE_MISSIONS[sp.missionIdx].done(sp)) {
            val m = SPACE_MISSIONS[sp.missionIdx]
            g.money += m.reward
            sp.missionIdx++
            val msg = when {
                sp.missionIdx == 5 -> "Mission complete! +${fmt(m.reward)}... Wait, what's that dark thing near the bottom? The LIME STAR?!"
                sp.missionIdx >= SPACE_MISSIONS.size -> "We did it! I'm a LEMON JEDI now! +${fmt(m.reward)}"
                else -> "Mission complete! +${fmt(m.reward)}. On to the next one!"
            }
            kSay(msg, 5f)
        }
    }

    fun visit(id: String) {
        if (id == "earth") { onLeave(); return }
        if (id == "station") {
            mode = "station"; sp.stationVisited = true
            sSay(SPACE_DROID_LINES[0])
            scope.launch { delay(1500); kSay("A space station! It's like a mall, but in SPACE.") }
            checkMissions()
            return
        }
        if (useStuff(id)) return
        if (id == "limestar" && sp.missionIdx < 5) {
            val left = 5 - sp.missionIdx
            kSay("Darth Lime's shield is up! We have to finish $left more mission${if (left == 1) "" else "s"} first. Then we can get in!", 5f)
            return
        }
        if (id == "limestar") { mode = "duel"; bubbleOn = false; scope.launch { startDuel() }; return }
        val w = SPACE_WORLDS[id] ?: return
        sp.visited = sp.visited + id
        scope.launch { delay(50); checkMissions() }
        SPACE_ARRIVE[id]?.let { line -> scope.launch { delay(1200); if (mode == id) kSay(line) } }
        mode = id
        val ip = inPitcher[id] ?: 0
        sLevel = 92f * ip / sp.need()
        dropY = -80f; pressY = 0f; kFace = "happy"
        sSay(w.hello)
    }

    fun showMapView() { mode = "map"; bubbleOn = false; duel = null }

    fun tapStuffOrKevin(px: Float, py: Float) {
        when {
            mode == "map" -> mapHit(px, py)?.let { visit(it) }
            mode == "station" -> if (hypot(px - 250f, py - 240f) < 80) sSay(SPACE_DROID_LINES.pick())
            SPACE_WORLDS.containsKey(mode) -> if (!busy && hypot(px - 170f, py - 290f) < 110) {
                kFace = "cheer"; sSay(SPACE_WORLDS[mode]!!.poke.pick())
                scope.launch { delay(700); if (!busy && mode != "map") kFace = "happy" }
            }
        }
    }

    // ---------------- squeezing a world's fruit family ----------------
    private suspend fun tween(ms: Double, block: (Float) -> Unit) {
        val total = max(1.0, ms)
        val start = SystemClock.uptimeMillis()
        while (true) {
            val t = ((SystemClock.uptimeMillis() - start) / total).toFloat().coerceIn(0f, 1f)
            block(t)
            if (t >= 1f) break
            delay(16)
        }
    }

    suspend fun spaceSqueeze() {
        if (mode == "duel") {
            val d = duel
            if (d != null && d.phase == "lost") startDuel() else duelTap()
            return
        }
        val w = SPACE_WORLDS[mode] ?: return
        if (busy) return
        busy = true
        try {
            val bag = bags.getOrPut(w.id) { ArrayList() }
            if (bag.isEmpty()) bag.addAll(w.family.shuffled())
            val rel = bag.removeAt(bag.size - 1)
            kFace = "worry"; kLookX = 420f; kLookY = 60f; sSay(rel.drop)
            squashY = 1f
            tween(800.0) { k -> dropY = -60f + (238f - -60f) * (k * k); kLookY = dropY }
            delay(250)
            kFace = "gasp"; sSay(rel.squish)
            tween(220.0) { k -> pressY = 40f * k; squashY = 1f - 0.7f * k }
            streamOn = true; kLookX = 518f; kLookY = 360f
            val ip = (inPitcher[w.id] ?: 0) + 1 + sp.lv("rocket")
            inPitcher[w.id] = ip
            val need = sp.need()
            val from = sLevel; val to = min(92f, 92f * ip / need)
            tween(700.0) { k -> sLevel = from + (to - from) * k }
            streamOn = false
            tween(300.0) { k -> pressY = 40f * (1 - k) }
            dropY = -80f; squashY = 1f
            if (ip >= need) {
                inPitcher[w.id] = 0
                sp.sold[w.id] = (sp.sold[w.id] ?: 0) + 1
                aliens.add(SpAlien(0f, listOf(Color(0xFF7BC043), Color(0xFFB79CFF), Color(0xFF2EC4B6), Color(0xFFFF7FB0)).pick()))
                if (Random.nextDouble() < 0.5) scope.launch { delay(1600); kSay(SPACE_ALIEN_TALK.pick()) }
                if (Random.nextDouble() < 0.4) scope.launch { delay(1200); kSay(SPACE_SQUEEZE_TALK.pick()) }
                scope.launch { delay(100); checkMissions() }
                val alien = sp.lv("moonjuice") > 0 && Random.nextDouble() < 0.12 * sp.lv("moonjuice")
                val got = sp.price(w) * (if (alien) 2 else 1)
                g.money += got
                kFace = "cheer"; sSay(if (alien) "An alien paid DOUBLE! +${fmt(got)}" else "${w.full.pick()} +${fmt(got)}")
                delay(900)
                val f = sLevel; tween(600.0) { k -> sLevel = f * (1 - k) }
            } else {
                kFace = "happy"; sSay(rel.after)
            }
            kLookX = 420f; kLookY = 218f
        } finally {
            busy = false
        }
    }

    // ---------------- the Space Station ----------------
    fun buyUpgrade(u: SpaceUpgrade) {
        val c = u.cost(sp.lv(u.id))
        if (g.money < c || sp.lv(u.id) >= u.max) return
        g.money -= c; sp.lvl[u.id] = sp.lv(u.id) + 1
        sp.stationBought = true
        checkMissions()
        sSay(SPACE_DROID_BUY_LINES.pick())
    }

    fun buyPaint(p: RocketPaint) {
        if (!sp.paintsOwned.contains(p.color)) {
            if (g.money < p.cost) return
            g.money -= p.cost
            sp.paintsOwned = sp.paintsOwned + p.color
        }
        sp.rocketColor = p.color
        sSay("LM-3PO: ${p.name}! A splendid choice. Your rocket looks positively dashing.")
    }

    // ---------------- Darth Lime ----------------
    suspend fun startDuel() {
        val d = SpDuel("talk"); duel = d
        for ((who, line) in SPACE_DUEL_TALK) {
            if (mode != "duel") return
            sSay("${who.replace("{N}", g.name)}: ${line.replace("{N}", g.name)}")
            delay(2300)
        }
        if (mode != "duel") return
        d.phase = "fight"; d.time = 12f
        sSay("Tap \"Swing\" as fast as you can! ${sp.duelNeed()} hits to win!")
    }

    private fun duelTap() {
        val d = duel ?: return
        if (d.phase != "fight") return
        d.taps++; d.swing = 0.12f; d.hit = 0.1f
        if (d.taps >= sp.duelNeed()) {
            d.phase = "won"
            sp.darthDefeated = true
            sSay("Darth Lime: NOOOO! ...You were the chosen lemon, ${g.name}!")
            scope.launch { delay(1500); kSay("We beat Darth Lime! The galaxy is SAFE! Lemon Jedi for life!", 5f); checkMissions() }
        }
    }

    private fun duelTick(d: SpDuel, dt: Float) {
        if (d.swing > 0) d.swing -= dt
        if (d.hit > 0) d.hit -= dt
        if (d.phase == "won") d.boom = min(1.2f, d.boom + dt)
        if (d.phase == "fight") {
            d.time -= dt
            if (d.time <= 0) { d.phase = "lost"; sSay("Darth Lime: The SOUR side is too strong! Come back when you're ready, little lemon.") }
        }
    }

    // ---------------- drawing (reads the fields above; called every frame) ----------------
    fun draw(ds: DrawScope, nowMs: Long) = with(ds) {
        when {
            mode == "map" -> drawMap(nowMs)
            mode == "station" -> drawStation(nowMs)
            mode == "duel" -> drawDuel(nowMs)
            else -> SPACE_WORLDS[mode]?.let { drawWorld(it, nowMs) }
        }
    }

    private val labelPaint = android.graphics.Paint().apply {
        isAntiAlias = true; textAlign = android.graphics.Paint.Align.CENTER; textSize = 15f
    }
    private fun DrawScope.spLabel(text: String, x: Float, y: Float, size: Float = 15f, fill: Int = 0xFFF4EFD0.toInt(), outline: Int = 0xFF0B0F2A.toInt()) {
        val c = drawContext.canvas.nativeCanvas
        labelPaint.textSize = size
        labelPaint.style = android.graphics.Paint.Style.STROKE; labelPaint.strokeWidth = 4f; labelPaint.color = outline
        c.drawText(text, x, y, labelPaint)
        labelPaint.style = android.graphics.Paint.Style.FILL; labelPaint.color = fill
        c.drawText(text, x, y, labelPaint)
    }

    private fun DrawScope.drawMap(t: Long) {
        drawRect(Color(0xFF0B0F2A), Offset.Zero, Size(SW, SH))
        for ((sxp, syp, r) in starsXY) {
            val a = 0.5f + 0.5f * sin(t / 600f + sxp)
            drawCircle(Color.White.copy(alpha = a.coerceIn(0f, 1f)), r, Offset(sxp, syp))
        }
        for (o in SPACE_ORBIT) drawOval(Color.White.copy(alpha = 0.15f), Offset(SUN_X - o.r, SUN_Y - o.r * TILT), Size(o.r * 2, o.r * 2 * TILT), style = Stroke(1.5f))
        val back = SPACE_ORBIT.filter { sin(orbitA[it.id] ?: it.startAngle) < 0 }
        val front = SPACE_ORBIT.filter { sin(orbitA[it.id] ?: it.startAngle) >= 0 }
        for (o in back) drawPlanet(o)
        drawCircle(Brush.radialGradient(0f to Color(0xE6FFDC5A), 1f to Color(0x008C1E), center = Offset(SUN_X, SUN_Y), radius = SUN_R * 1.8f), SUN_R * 1.8f, Offset(SUN_X, SUN_Y))
        rotate(t / 4000f * (180f / PI.toFloat()), Offset(SUN_X, SUN_Y)) {
            for (i in 0 until 12) rotate(i * 30f, Offset(SUN_X, SUN_Y)) {
                drawCircle(Color(0x59FFC850), 3f, Offset(SUN_X, SUN_Y - SUN_R - 14f - 6f * sin(t / 300f + i)))
            }
        }
        drawCircle(Brush.radialGradient(0f to Color(0xFFFFF7C2), 0.5f to Color(0xFFFFC53D), 1f to Color(0xFFF08A1C), center = Offset(SUN_X - 15, SUN_Y - 15), radius = SUN_R), SUN_R, Offset(SUN_X, SUN_Y))
        drawCircle(Color(0xFFE2731B), SUN_R, Offset(SUN_X, SUN_Y), style = Stroke(4f))
        spLabel("Sun: pineapples", SUN_X, SUN_Y + SUN_R + 22)
        for (o in front) drawPlanet(o)
        drawStationIcon(t)
        drawStuff(t)
        if (!sp.darthDefeated) drawLimeStarIcon(t)
        drawMapRocket(t)
    }

    private fun DrawScope.drawPlanet(o: OrbitBody) {
        val p = planetPos(o)
        if (o.id == "saturn") drawOval(Color(0xFFE8D9A8), Offset(p.x - o.size * 1.9f, p.y - o.size * 0.6f), Size(o.size * 3.8f, o.size * 1.2f), style = Stroke(4f))
        drawCircle(Brush.radialGradient(0f to Color.White, 0.18f to o.color, 1f to Color(0xFF10142E), center = Offset(p.x - o.size * 0.4f, p.y - o.size * 0.4f), radius = o.size * 1.1f), o.size, p)
        drawCircle(Color.Black.copy(alpha = 0.4f), o.size, p, style = Stroke(2f))
        if (o.id == "earth") { drawCircle(Color(0xFF4FA35A), 6f, Offset(p.x - 4, p.y - 3)); drawCircle(Color(0xFF4FA35A), 4f, Offset(p.x + 6, p.y + 5)) }
        if (o.id == "jupiter") for (dy in listOf(-8f, 2f, 10f)) drawLine(Color(0xFFA8693A), Offset(p.x - o.size + 4, p.y + dy), Offset(p.x + o.size - 4, p.y + dy), 3f)
        spLabel(o.label, p.x, p.y + o.size + 20)
        if (o.id == "earth") {
            val m = moonPos()
            drawCircle(Color(0xFFD6D8DE), 8f, m)
            spLabel("Moon: blueberries", m.x, m.y - 14)
        }
    }

    private fun DrawScope.drawStationIcon(t: Long) {
        translate(STATION_X, STATION_Y) {
            for (dx in listOf(-46f, 26f)) { drawRect(Color(0xFF2E6BD6), Offset(dx, -8f), Size(20f, 16f)); drawRect(Color(0xFFC0C8D0), Offset(dx, -8f), Size(20f, 16f), style = Stroke(2f)) }
            drawLine(Color(0xFF9AA7B1), Offset(-26f, 0f), Offset(26f, 0f), 3f)
            drawCircle(Brush.radialGradient(0f to Color.White, 1f to Color(0xFF8796A0), center = Offset(-5f, -5f), radius = 18f), 16f, Offset.Zero)
            drawCircle(Color(0xFF3A3A4A), 16f, Offset.Zero, style = Stroke(2f))
            drawCircle(if (sin(t / 300f) > 0) Color(0xFFE8483B) else Color(0xFF5A1A1A), 3f, Offset(0f, -18f))
            drawCircle(Color(0xFF9ED8F5), 3f, Offset(-5f, 2f)); drawCircle(Color(0xFF9ED8F5), 3f, Offset(5f, 2f))
        }
        spLabel("Space Station", STATION_X, STATION_Y + 32)
    }

    private fun DrawScope.drawLimeStarIcon(t: Long) {
        drawCircle(Brush.radialGradient(0f to Color(0xFF9FD86A), 0.6f to Color(0xFF3E6B2E), 1f to Color(0xFF1A2E12), center = Offset(LIMESTAR_X - 8, LIMESTAR_Y - 8), radius = 30f), 28f, Offset(LIMESTAR_X, LIMESTAR_Y))
        drawCircle(Color(0xFF0B140A), 28f, Offset(LIMESTAR_X, LIMESTAR_Y), style = Stroke(2f))
        drawCircle(Color(0xFF1A2E12), 7f, Offset(LIMESTAR_X - 9, LIMESTAR_Y - 10))
        drawCircle(Color(0xFFE8483B).copy(alpha = (0.5f + 0.5f * sin(t / 200f)).coerceIn(0f, 1f)), 3f, Offset(LIMESTAR_X - 9, LIMESTAR_Y - 10))
        if (sp.missionIdx < 5) {
            drawCircle(Color(0xFF7BC043).copy(alpha = (0.5f + 0.3f * sin(t / 300f)).coerceIn(0f, 1f)), 36f, Offset(LIMESTAR_X, LIMESTAR_Y), style = Stroke(3f))
            spLabel("Lime Star (shield up: ${sp.missionIdx}/5)", LIMESTAR_X - 60, LIMESTAR_Y - 46)
        } else spLabel("Lime Star: Darth Lime is here!", LIMESTAR_X - 70, LIMESTAR_Y - 46)
    }

    private fun DrawScope.drawMapRocket(t: Long) {
        val a = t / 2500f
        val rx = SUN_X + cos(a) * 160f; val ry = SUN_Y + sin(a) * 160f * TILT
        translate(rx, ry) {
            rotate((a + PI.toFloat() / 2) * (180f / PI.toFloat()), Offset.Zero) {
                drawRocketBody(sp.rocketColor)
            }
        }
        spLabel("${g.name} (co-pilot)", rx, ry - 26)
    }

    private fun DrawScope.drawRocketBody(color: Color) {
        drawCircle(Color(0xFFE8483B), 5f, Offset(-7f, 10f))
        drawCircle(Color(0xFFE8483B), 5f, Offset(7f, 10f))
        drawOval(color, Offset(-8f, -18f), Size(16f, 34f))
        drawOval(Color(0xFF3A2E12), Offset(-8f, -18f), Size(16f, 34f), style = Stroke(2f))
        drawCircle(Color(0xFFFFD21F), 5f, Offset(0f, -4f))
        drawCircle(Color(0xFF3A2E12), 5f, Offset(0f, -4f), style = Stroke(2f))
    }

    private fun DrawScope.drawStuff(t: Long) {
        for ((i, a) in asteroids.withIndex()) {
            if (a.gone > 0) continue
            val p = astPos(a, i)
            rotate((a.a * 3f + i) * (180f / PI.toFloat()), p) {
                translate(p.x, p.y) {
                    drawCircle(Color(0xFF8A7F76), 9f, Offset.Zero)
                    drawCircle(Color(0xFF3A3430), 9f, Offset.Zero, style = Stroke(1.5f))
                    drawCircle(listOf(Color(0xFF7FE3FF), Color(0xFFFF7FB0), Color(0xFFFFD21F))[i % 3], 2.5f, Offset(1f, -1f))
                }
            }
        }
        for (k in 4 downTo 0) drawOval(Color(0xFFA06BFF).copy(alpha = 0.15f + k * 0.08f), Offset(BLACKHOLE_X - 26 - k * 6, BLACKHOLE_Y - (26 + k * 6) * 0.45f), Size((26 + k * 6) * 2f, (26 + k * 6) * 0.9f), style = Stroke(3f))
        drawCircle(Color.Black, 16f, Offset(BLACKHOLE_X, BLACKHOLE_Y))
        drawCircle(Color(0xFFFFB86B), 16f, Offset(BLACKHOLE_X, BLACKHOLE_Y), style = Stroke(2f))
        spLabel(if (holeCd > 0) "Black hole (${ceil(holeCd).toInt()}s)" else "Black hole", BLACKHOLE_X + 18, BLACKHOLE_Y - 34)
        comet?.let { c ->
            drawLine(Color(0xE6E6F8FF), Offset(c.x - 70, c.y - 19), Offset(c.x, c.y), 8f, StrokeCap.Round)
            drawCircle(Color.White, 7f, Offset(c.x, c.y))
        }
        whale?.let { w ->
            val wy = w.y0 + sin(w.t * 1.5f) * 8f
            translate(w.x, wy) {
                drawOval(Color(0xFF5B7FC9), Offset(-70f, -30f), Size(140f, 60f))
                drawOval(Color(0xFF1E2E5A), Offset(-70f, -30f), Size(140f, 60f), style = Stroke(3f))
                drawCircle(Color.White, 6f, Offset(-44f, -8f)); drawCircle(Color(0xFF111111), 3f, Offset(-45f, -8f))
            }
            spLabel("Big Bertha the space whale", w.x, wy - 42)
        }
        for (c in chests) {
            translate(c.x, c.y) {
                drawRect(Color(0xFF8B5A2B), Offset(-14f, -8f), Size(28f, 18f))
                drawRect(Color(0xFF3A2414), Offset(-14f, -8f), Size(28f, 18f), style = Stroke(2f))
                drawRect(Color(0xFFA8733E), Offset(-14f, -14f), Size(28f, 7f))
                drawRect(Color(0xFFE9B630), Offset(-3f, -9f), Size(6f, 7f))
            }
        }
        for (f in sFloat) {
            val a = max(0f, f.life).coerceAtMost(1f)
            spLabel(f.text, f.x, f.y, 15f, Color(0xFFFFD21F).copy(alpha = a).toArgb(), Color(0xFF05081C).copy(alpha = a).toArgb())
        }
    }

    // ---------------- a fruit world ----------------
    private fun DrawScope.drawWorld(w: SpaceWorld, t: Long) {
        drawRect(Brush.verticalGradient(listOf(w.sky0, w.sky1), startY = 0f, endY = SH), Offset.Zero, Size(SW, SH))
        if (w.stars) for ((sxp, syp, r) in starsXY) if (syp <= 400f) drawCircle(Color.White.copy(alpha = 0.7f), r, Offset(sxp, syp))
        if (w.bands) { drawRect(Color(0x59A8693A), Offset(0f, 60f), Size(SW, 30f)); drawRect(Color(0x59A8693A), Offset(0f, 230f), Size(SW, 30f)); drawRect(Color(0x59A8693A), Offset(0f, 320f), Size(SW, 30f)) }
        if (w.rings) drawOval(Color(0x99E8D9A8), Offset(-120f, 50f), Size(1040f, 140f), style = Stroke(14f))
        drawRect(w.ground, Offset(0f, 450f), Size(SW, 50f))
        if (w.swamp) { drawOval(Color(0xFF5FB0A8), Offset(-10f, 458f), Size(240f, 44f)); drawOval(Color(0xFF5FB0A8), Offset(610f, 458f), Size(220f, 36f)) }
        // table + squeezer + pitcher (same layout as the original's drawWorld)
        drawRect(Color(0xFFC98B4E), Offset(300f, 400f), Size(400f, 18f)); drawRect(Color(0xFF3A2E12), Offset(300f, 400f), Size(400f, 18f), style = Stroke(4f))
        drawRect(Color(0xFF8796A0), Offset(354f, 130f), Size(16f, 242f))
        drawRect(Color(0xFFCFD8DE), Offset(405f, 290f), Size(30f, 82f))
        drawRect(Color(0xFFCFD8DE), Offset(413f, 140f + pressY), Size(14f, 34f))
        drawCircle(Color(0xFFE8483B), 12f, Offset(420f, 116f + pressY))
        if (dropY > -70f) translate(420f, 238f) { scale(1.3f - 0.3f * squashY, squashY, Offset.Zero) { translate(0f, dropY - 238f - 20f) { drawWorldFruit(w, 0.22f, null) } } }
        drawPath(androidx.compose.ui.graphics.Path().apply { moveTo(368f, 232f); lineTo(472f, 232f); lineTo(456f, 292f); lineTo(384f, 292f); close() }, Color(0xFFCFD8DE))
        if (streamOn) drawRect(w.juice, Offset(514f, 289f), Size(8f, 108f))
        drawRect(Color.White.copy(alpha = 0.3f), Offset(500f, 300f), Size(120f, 100f))
        drawRect(w.juice, Offset(500f, 400f - sLevel), Size(120f, sLevel))
        drawRect(Color(0xFF3A2E12), Offset(500f, 300f), Size(120f, 100f), style = Stroke(4f))
        for (al in aliens) {
            val k = al.t / 4.2f
            val ax = if (k < 0.4f) 840f - (k / 0.4f) * 140f else if (k < 0.55f) 700f else 700f + ((k - 0.55f) / 0.45f) * 160f
            val ay = 410f - abs(sin(al.t * 8f)) * 4f
            translate(ax, ay) {
                drawOval(al.color, Offset(-16f, -20f), Size(32f, 40f))
                drawOval(Color(0xFF3A2E12), Offset(-16f, -20f), Size(32f, 40f), style = Stroke(2.5f))
                for (ex in listOf(-7f, 0f, 7f)) { drawCircle(Color.White, 4f, Offset(ex, -4f)); drawCircle(Color(0xFF111111), 1.6f, Offset(ex, -4f)) }
                if (k > 0.5f) drawRect(w.juice, Offset(14f, -2f), Size(9f, 13f))
            }
        }
        // the player's own rocket, parked
        translate(752f, 400f) { drawRocketBody(sp.rocketColor) }
        spLabel("${g.name} landed!", 762f, 352f, 14f)
        // Kevin, as this world's fruit
        translate(170f, (if (w.fruit == "turtle") 345f else 290f) + (if (kFace == "cheer") -abs(sin(t / 80f)) * 10f else 0f)) {
            drawWorldFruit(w, 1f, if (kFace == "cheer") "happy" else kFace)
        }
    }

    private fun DrawScope.drawWorldFruit(w: SpaceWorld, s: Float, face: String?) {
        scale(s, s, Offset.Zero) {
            drawCircle(w.juice, 90f, Offset.Zero)
            drawCircle(Color(0xFF3A2E12), 90f, Offset.Zero, style = Stroke(5f))
            when (w.fruit) {
                "pineapple" -> for (i in -2..2) drawLine(Color(0xFF4FA34A), Offset(i * 14f, -84f), Offset(i * 10f, -150f + abs(i) * 14f), 10f, StrokeCap.Round)
                "strawberry", "watermelon", "cherry" -> drawLine(Color(0xFF4FA34A), Offset(0f, -84f), Offset(10f, -118f), 8f, StrokeCap.Round)
                "pepper" -> drawLine(Color(0xFF4FA34A), Offset(0f, -84f), Offset(14f, -116f), 8f, StrokeCap.Round)
                "grape" -> drawLine(Color(0xFF4FA34A), Offset(0f, -84f), Offset(-6f, -116f), 8f, StrokeCap.Round)
                "turtle" -> { drawOval(Color(0xFF6FB34A), Offset(-78f - 22f, 70f - 14f), Size(44f, 28f)); drawOval(Color(0xFF6FB34A), Offset(78f - 22f, 70f - 14f), Size(44f, 28f)) }
            }
            if (face != null) {
                val worried = face == "worry" || face == "gasp"
                for (ex in listOf(-30f, 30f)) {
                    drawCircle(Color.White, 19f, Offset(ex, -12f)); drawCircle(Color(0xFF3A2E12), 19f, Offset(ex, -12f), style = Stroke(3f))
                    drawCircle(Color(0xFF3A2E12), 8f, Offset(ex, -12f))
                }
                drawLine(Color(0xFF3A2E12), Offset(-46f, if (worried) -40f else -38f), Offset(-16f, if (worried) -48f else -42f), 5f, StrokeCap.Round)
                drawLine(Color(0xFF3A2E12), Offset(16f, if (worried) -48f else -42f), Offset(46f, if (worried) -40f else -38f), 5f, StrokeCap.Round)
                when (face) {
                    "gasp" -> drawOval(Color(0xFF8C2F2F), Offset(-13f, 25f), Size(26f, 34f))
                    "worry" -> drawLine(Color(0xFF3A2E12), Offset(-24f, 50f), Offset(24f, 50f), 5f, StrokeCap.Round)
                    else -> drawLine(Color(0xFF3A2E12), Offset(-28f, 34f), Offset(28f, 34f), 5f, StrokeCap.Round)
                }
            }
        }
    }

    // ---------------- the Space Station ----------------
    private fun DrawScope.drawStation(t: Long) {
        drawRect(Color(0xFF2A2E3A), Offset.Zero, Size(SW, SH))
        drawRect(Color(0xFF05081C), Offset(420f, 40f), Size(340f, 220f))
        drawRect(Color(0xFF9AA7B1), Offset(420f, 40f), Size(340f, 220f), style = Stroke(8f))
        for ((sxp, syp, r) in starsXY) if (syp < 200f) drawCircle(Color.White.copy(alpha = (0.5f + 0.5f * sin(t / 500f + sxp)).coerceIn(0f, 1f)), r, Offset(420f + sxp * 0.42f, 40f + syp * 1.05f))
        drawCircle(Color(0xFF3D7BE0), 50f, Offset(640f, 200f))
        drawCircle(Color(0xFF4FA35A), 18f, Offset(625f, 185f)); drawCircle(Color(0xFF4FA35A), 12f, Offset(655f, 215f))
        drawRect(Color(0xFF3A3F4E), Offset(0f, 380f), Size(SW, 120f))
        drawRect(Color(0xFF8796A0), Offset(40f, 330f), Size(360f, 60f)); drawRect(Color(0xFF2A2E3A), Offset(40f, 330f), Size(360f, 60f), style = Stroke(4f))
        drawRect(Color(0xFF9ED8F5), Offset(56f, 342f), Size(80f, 30f))
        val bob = sin(t / 400f) * 3f; val dx = 250f; val dy = 240f + bob
        drawOval(Brush.horizontalGradient(listOf(Color(0xFFB8860B), Color(0xFFFFE07A), Color(0xFFB8860B)), startX = dx - 52f, endX = dx + 52f), Offset(dx - 52f, dy - 64f), Size(104f, 128f))
        drawOval(Color(0xFF3A2E12), Offset(dx - 52f, dy - 64f), Size(104f, 128f), style = Stroke(4f))
        drawRect(Color(0xFF3A2E12), Offset(dx - 34f, dy - 20f), Size(68f, 22f))
        drawCircle(Color(0xFFFFE36B), 7f, Offset(dx - 15f, dy - 9f)); drawCircle(Color(0xFFFFE36B), 7f, Offset(dx + 15f, dy - 9f))
        spLabel("LM-3PO", dx, dy + 88f, 15f, Color(0xFFFFE07A).toArgb())
    }

    // ---------------- Darth Lime ----------------
    private fun DrawScope.drawLemonFighter(x: Float, y: Float, lime: Boolean, saberColor: Color, swing: Boolean, hurt: Boolean) {
        translate(x, y) {
            if (lime) drawPath(androidx.compose.ui.graphics.Path().apply {
                moveTo(-50f, -40f); quadraticTo(-80f, 60f, -60f, 110f); lineTo(60f, 110f); quadraticTo(80f, 60f, 50f, -40f); close()
            }, Color(0xFF1A1A1A))
            val body: Brush = if (hurt) SolidColor(Color.White) else Brush.radialGradient(
                0f to (if (lime) Color(0xFFC8F2A8) else Color(0xFFFFF6B8)),
                0.55f to (if (lime) Color(0xFF7BC043) else Color(0xFFFFD21F)),
                1f to (if (lime) Color(0xFF3E6B2E) else Color(0xFFE3A600)),
                center = Offset(-15f, -20f), radius = 70f,
            )
            drawOval(body, Offset(-55f, -64f), Size(110f, 128f))
            drawOval(Color(0xFF1A1A1A), Offset(-55f, -64f), Size(110f, 128f), style = Stroke(4f))
            for (ex in listOf(-18f, 18f)) { drawCircle(Color.White, 10f, Offset(ex, -12f)); drawCircle(Color(0xFF1A1A1A), 4.5f, Offset(ex + 3f, -12f)) }
            // saber
            val hiltX = if (lime) -50f else 50f
            rotate((if (swing) 1.1f else 0.4f) * (if (lime) -1f else 1f) * (180f / PI.toFloat()), Offset(hiltX, 10f)) {
                translate(hiltX, 10f) {
                    drawRect(Color(0xFFC0C8D0), Offset(-4f, -4f), Size(8f, 22f))
                    drawLine(saberColor, Offset(0f, -6f), Offset(0f, -110f), 10f, StrokeCap.Round)
                    drawLine(Color.White, Offset(0f, -8f), Offset(0f, -108f), 4f, StrokeCap.Round)
                }
            }
        }
    }

    private fun DrawScope.drawDuel(t: Long) {
        drawRect(Color(0xFF0A0A14), Offset.Zero, Size(SW, SH))
        for ((sxp, syp, r) in starsXY) drawCircle(Color.White.copy(alpha = 0.4f), r, Offset(sxp, syp))
        drawRect(Color(0xFF1E2A1A), Offset(0f, 400f), Size(SW, 100f))
        val d = duel ?: return
        val sw = d.swing > 0
        if (d.phase != "won") drawLemonFighter(560f + (if (sw) -10f else 0f), 300f, true, Color(0xFFFF2A2A), !sw, d.hit > 0)
        else drawCircle(Color(0xFF7BC043).copy(alpha = max(0f, 1f - d.boom)), 40f + d.boom * 200f, Offset(560f, 300f))
        drawLemonFighter(240f + (if (sw) 14f else 0f), 300f, false, Color(0xFFFFE36B), sw, false)
        if (d.phase == "fight") {
            drawRect(Color(0xFF330000), Offset(250f, 30f), Size(300f, 16f))
            drawRect(Color(0xFF7BC043), Offset(250f, 30f), Size(300f * max(0f, 1f - d.taps / sp.duelNeed().toFloat()), 16f))
            drawRect(Color.White, Offset(250f, 30f), Size(300f, 16f), style = Stroke(2f))
            spLabel("Darth Lime  ·  ${ceil(d.time).toInt()}s left", 400f, 66f, 16f)
        }
    }
}
