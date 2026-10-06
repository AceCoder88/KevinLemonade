package com.kevin.lemonade

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

// =================== COWBOY REALM: the Horse Ride (kevin.html 7627-7801) ===================

private const val RCOL = 16
private const val RS = 50f
private const val RW = 800f
private const val RH = 500f

private class TrailObj(var x: Float, val len: Float)

private sealed class Lane {
    class Grass(val cacti: List<Int>, val coins: MutableList<Int>) : Lane()
    class Trail(val speed: Float, val kind: String, val objs: List<TrailObj>, val coins: MutableList<Int>) : Lane()
    class River(val speed: Float, val objs: List<TrailObj>) : Lane()
}

private class Hero {
    var x = 8 * RS + RS / 2
    var row = 0
    var hop = 0f
    var dead = false
}

private fun makeLane(i: Int): Lane {
    if (i < 3) return Lane.Grass(emptyList(), mutableListOf())
    val r = Random.nextDouble()
    if (r < 0.35) {
        val cacti = (0 until RCOL).filter { it != 8 && Random.nextDouble() < 0.15 }
        val coins = (0 until RCOL).filter { it !in cacti && Random.nextDouble() < 0.08 }.toMutableList()
        return Lane.Grass(cacti, coins)
    }
    if (r < 0.72) {
        val dir = if (Random.nextDouble() < 0.5) 1 else -1
        val speed = (60 + Random.nextDouble() * 90 + min(120.0, i * 1.5)).toFloat() * dir
        val kind = listOf("coach", "tumble", "bull").pick()
        val len = if (kind == "coach") 2.4f else 1f
        val objs = ArrayList<TrailObj>()
        var x = Random.nextDouble() * 200
        while (x < RW + 200) { objs.add(TrailObj(x.toFloat(), len)); x += (len + 2 + Random.nextDouble() * 5) * RS }
        val coins = (0 until RCOL).filter { Random.nextDouble() < 0.06 }.toMutableList()
        return Lane.Trail(speed, kind, objs, coins)
    }
    val dir = if (Random.nextDouble() < 0.5) 1 else -1
    val speed = (40 + Random.nextDouble() * 60).toFloat() * dir
    val objs = ArrayList<TrailObj>()
    var x = Random.nextDouble() * 100
    while (x < RW + 300) {
        val len = (2 + Random.nextInt(3)).toFloat()
        objs.add(TrailObj(x.toFloat(), len))
        x += (len + 1.5 + Random.nextDouble() * 2.5) * RS
    }
    return Lane.River(speed, objs)
}

/** The ephemeral Crossy-Road-style ride itself; recreated fresh every visit, like the original. */
private class RideSim(val g: GameViewModel, val ride: RideState) {
    val lanes = ArrayList<Lane>()
    var hero = Hero()
    var cam = 0f
    var over by mutableStateOf(false)
    var coins by mutableIntStateOf(0)
    var banner by mutableStateOf("")
    var bannerT = 0f
    var bannerOn by mutableStateOf(false)
    var showAgain by mutableStateOf(false)
    /** mirrors hero.row as real Compose state, so the "Rows:" HUD text (read during composition,
     * not just inside the Canvas draw phase) recomposes when it changes */
    var displayRow by mutableIntStateOf(0)

    fun laneAt(row: Int): Lane {
        while (lanes.size <= row + 30) lanes.add(makeLane(lanes.size))
        return lanes[row]
    }

    fun start() {
        lanes.clear()
        repeat(40) { lanes.add(makeLane(lanes.size)) }
        hero = Hero(); cam = 0f; over = false; coins = 0; showAgain = false; displayRow = 0
        rMsg("Tap up to gallop forward! Left and right to dodge. Watch out for stagecoaches and the river!", 3f)
    }

    fun rMsg(t: String, secs: Float = 2f) { banner = t; bannerOn = true; bannerT = secs }

    fun move(dr: Int, dc: Int) {
        if (over || hero.hop > 0) return
        val nr = hero.row + dr
        val nx = hero.x + dc * RS
        if (nr < 0 || nx < RS / 2 || nx > RW - RS / 2) return
        val lane = laneAt(nr)
        val col = floor(nx / RS).toInt()
        if (lane is Lane.Grass && lane.cacti.contains(col)) return
        hero.row = nr; hero.x = nx; hero.hop = 0.14f
        if (lane !is Lane.River) hero.x = floor(hero.x / RS) * RS + RS / 2
        val coinList = when (lane) { is Lane.Grass -> lane.coins; is Lane.Trail -> lane.coins; else -> null }
        if (coinList != null) {
            val ci = coinList.indexOf(floor(hero.x / RS).toInt())
            if (ci >= 0) { coinList.removeAt(ci); coins++ }
        }
        if (hero.row > ride.rideBest) ride.rideBest = hero.row
        displayRow = hero.row
    }

    fun crash(why: String) {
        if (over) return
        over = true; hero.dead = true
        val pay = coins * 25L + hero.row * 5L
        g.money += pay
        rMsg("$why You rode ${hero.row} rows and found $coins lemon coins: +${fmt(pay)}! Tap \"Ride again\"", 4f)
        showAgain = true
    }

    fun update(dt: Float) {
        if (bannerT > 0) { bannerT -= dt; if (bannerT <= 0) bannerOn = false }
        val lo = max(0, hero.row - 6)
        for (i in lo until hero.row + 12) {
            val lane = laneAt(i)
            val (speed, objs) = when (lane) {
                is Lane.Trail -> lane.speed to lane.objs
                is Lane.River -> lane.speed to lane.objs
                else -> continue
            }
            for (o in objs) {
                o.x += speed * dt
                if (speed > 0 && o.x > RW + 100) o.x -= RW + 260 + o.len * RS
                if (speed < 0 && o.x + o.len * RS < -100) o.x += RW + 260 + o.len * RS
            }
        }
        if (hero.hop > 0) hero.hop -= dt
        if (!over) {
            when (val lane = laneAt(hero.row)) {
                is Lane.Trail -> {
                    for (o in lane.objs) if (hero.x > o.x - 14 && hero.x < o.x + o.len * RS + 14) {
                        crash(when (lane.kind) { "coach" -> "CRASH! A stagecoach!"; "bull" -> "A bull bumped you!"; else -> "Tumbleweed attack!" })
                        break
                    }
                }
                is Lane.River -> if (hero.hop <= 0) {
                    val log = lane.objs.find { hero.x > it.x && hero.x < it.x + it.len * RS }
                    if (log == null) crash("SPLASH! Horses can't swim!")
                    else {
                        hero.x += lane.speed * dt
                        if (hero.x < 10 || hero.x > RW - 10) crash("The log floated away with you!")
                    }
                }
                else -> {}
            }
        }
        val want = max(0, hero.row - 3) * RS
        cam += (want - cam) * min(1f, dt * 6)
    }
}

/** Cowboy Realm: the Horse Ride and the horse companions who ride the trail for you. */
class RideState(val g: GameViewModel) : Feature {
    override val key = "ride"

    /** best row ever reached */
    var rideBest by mutableIntStateOf(0)
    var ridersHired by mutableIntStateOf(0)
    val riderT = mutableStateListOf(0, 0, 0, 0)
    var riderLog by mutableStateOf("")

    override fun save(j: JSONObject) {
        j.put("rideBest", rideBest)
        j.put("ridersHired", ridersHired)
        j.put("riderT", JSONArray(riderT.toList()))
    }

    override fun load(j: JSONObject) {
        rideBest = j.optInt("rideBest", 0)
        ridersHired = j.optInt("ridersHired", 0)
        j.optJSONArray("riderT")?.let { a -> for (i in 0 until min(4, a.length())) riderT[i] = a.optInt(i, 0) }
    }

    override fun reset() {
        rideBest = 0; ridersHired = 0
        for (i in 0 until 4) riderT[i] = 0
        riderLog = ""
    }

    // Rebirth keeps tools, war, space, pirates, digging "and everything else" - the ride is kept too,
    // which is just the shared Feature.onRebirth() default (no-op) - no override needed here.

    // the original's maxEverything() (kevin.html 8502-8526, the secret MAX OUT code) never touches
    // ridersHired/rideBest, so there's nothing for this feature's maxOut() to do.

    fun riderCost(): Long = Math.round(200 * Math.pow(2.3, ridersHired.toDouble()))

    /** once a second, wherever the player is: the hired companions ride the trail for you */
    override fun tick() {
        for (i in 0 until ridersHired) {
            riderT[i] = riderT[i] + 1
            if (riderT[i] < 15) continue
            riderT[i] = 0
            val rows = 5 + Random.nextInt(25)
            val coins = Random.nextInt(5)
            val pay = coins * 25L + rows * 5L
            g.money += pay
            riderLog = "${RIDERS[i].first} rode $rows rows and found $coins lemon coins: +${fmt(pay)}!"
        }
    }

    fun hireRider() {
        if (ridersHired >= RIDERS.size || g.money < riderCost()) return
        g.money -= riderCost(); ridersHired++
        riderLog = "${RIDERS[ridersHired - 1].first} saddles up! They'll ride the trail for you every 15 seconds."
    }

    companion object {
        val RIDERS = listOf("Dusty" to Color(0xFF8B5A2B), "Belle" to Color(0xFFE8E0D0), "Hank" to Color(0xFF3A2414), "Rosie" to Color(0xFFC98B4E))
    }
}

@Composable
fun RideScreen(g: GameViewModel) {
    val ride = g.ride
    val sim = remember { RideSim(g, ride).apply { start() } }
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
    Column(
        Modifier.fillMaxSize().background(Color(0xFF3D4425)).verticalScroll(rememberScrollState()).padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ChunkyButton("Back to lemonade", color = CARD) {
                if (!sim.over && sim.displayRow > 0) {
                    val pay = sim.coins * 25L + sim.displayRow * 5L
                    g.money += pay
                }
                g.screen = Screen.STAND
            }
            Spacer(Modifier.weight(1f))
            Text("Rows: ${sim.displayRow}", color = Color(0xFFF4EFD0), fontFamily = Lilita, fontSize = 15.sp)
            Spacer(Modifier.width(10.dp))
            Text("Coins: ${sim.coins}", color = Color(0xFFF4EFD0), fontFamily = Lilita, fontSize = 15.sp)
            Spacer(Modifier.width(10.dp))
            Text("Best: ${ride.rideBest}", color = Color(0xFFF4EFD0), fontFamily = Lilita, fontSize = 15.sp)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTitle("Horse Ride", 28, Color(0xFFE9B630))
        Spacer(Modifier.height(6.dp))
        val shape = RoundedCornerShape(20.dp)
        Box(Modifier.fillMaxWidth().background(LINE, shape).padding(bottom = 6.dp).aspectRatio(RW / RH).clip(shape).border(4.dp, LINE, shape)) {
            Canvas(Modifier.fillMaxSize().pointerInput(Unit) {
                awaitEachGesture {
                    val sc = size.width / RW
                    val down = awaitFirstDown()
                    down.consume()
                    val px = down.position.x / sc / RW
                    if (px < 0.3f) sim.move(0, -1) else if (px > 0.7f) sim.move(0, 1) else sim.move(1, 0)
                }
            }) {
                frame.let { }
                scale(size.width / RW, size.width / RW, Offset.Zero) { drawRide(sim) }
            }
            if (sim.bannerOn) {
                Box(Modifier.align(Alignment.TopCenter).padding(top = 10.dp).background(LINE, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp).fillMaxWidth(0.82f)) {
                    Text(sim.banner, color = Color(0xFFE9B630), fontWeight = FontWeight.Black, fontSize = 13.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth())
                }
            }
            // the 4-way direction pad, like the original's bottom-of-canvas buttons
            Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                RidePadButton("▲") { sim.move(1, 0) }
                Row {
                    RidePadButton("◄") { sim.move(0, -1) }
                    Spacer(Modifier.width(6.dp))
                    RidePadButton("▼") { sim.move(-1, 0) }
                    Spacer(Modifier.width(6.dp))
                    RidePadButton("►") { sim.move(0, 1) }
                }
            }
        }
        if (sim.showAgain) {
            Spacer(Modifier.height(10.dp))
            ChunkyButton("Ride again") { sim.start() }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Cowboy Kevin's horse ride! Use the 4 arrow buttons on the screen (or tap the left side, right side, or middle). " +
                "Gallop up the trail, dodge stagecoaches, bulls and tumbleweeds, hop on logs to cross the river, and grab lemon coins. " +
                "Every coin is \$25, every row is \$5.",
            color = Color(0xFFF4EFD0), fontSize = 12.sp, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(14.dp))
        Text("Horse companions", color = Color(0xFFF4EFD0), fontFamily = Lilita, fontSize = 20.sp)
        Text(
            "Hire cowboy and cowgirl lemons who ride the trail for you every 15 seconds and bring back the money, even while you're doing other things!",
            color = Color(0xFFF4EFD0), fontSize = 12.sp, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        FlowRowRiders(ride)
        if (ride.riderLog.isNotEmpty()) Text(ride.riderLog, color = Color(0xFFF4EFD0), fontSize = 12.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        if (ride.ridersHired >= RideState.RIDERS.size) {
            ChunkyButton("All 4 horse companions hired!", enabled = false) {}
        } else {
            ChunkyButton("Hire ${RideState.RIDERS[ride.ridersHired].first} (${fmt(ride.riderCost())})", enabled = g.money >= ride.riderCost()) { ride.hireRider() }
        }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun RidePadButton(glyph: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier.size(52.dp).clip(shape).background(Color(0xFFF4EFD0)).border(3.dp, LINE, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, fontSize = 20.sp, color = LINE)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowRiders(ride: RideState) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (i in 0 until ride.ridersHired) {
            val (nm, c) = RideState.RIDERS[i]
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Canvas(Modifier.size(70.dp)) { scale(size.width / 60f, size.height / 60f, Offset.Zero) { riderIcon(c) } }
                Text(nm, color = Color(0xFFFFF6E0), fontWeight = FontWeight.Black, fontSize = 13.sp)
                Text("${max(0, 15 - ride.riderT[i])}s", color = Color(0xFFFFF6E0), fontSize = 11.sp)
            }
        }
    }
}

private fun DrawScope.riderIcon(c: Color) {
    translate(30f, 36f) {
        oval(0f, 6f, 20f, 11f, c, LINE, 2f)
        rotate(30f, Offset(18f, -4f)) { oval(18f, -4f, 7f, 10f, c, LINE, 2f) } // the original SVG icon uses 30deg here (vs 0.5rad on the canvas horse)
        for (lx in listOf(-12f, -4f, 6f, 14f)) drawLine(LINE, Offset(lx, 14f), Offset(lx, 22f), 3f, StrokeCap.Butt)
        oval(-2f, -12f, 9f, 11f, Color(0xFFFFD21F), LINE, 2f)
        oval(-2f, -21f, 14f, 4f, Color(0xFF8B5A2B), LINE, 2f)
        drawRect(Color(0xFF8B5A2B), Offset(-8f, -29f), Size(12f, 8f))
    }
}

// ----------------------------- drawing -----------------------------

private fun DrawScope.drawRide(sim: RideSim) {
    val t = System.currentTimeMillis() % 1_000_000L
    drawRect(Color(0xFFE9C38A), Offset.Zero, Size(RW, RH))
    val first = floor(sim.cam / RS).toInt() - 1
    for (i in max(0, first) until first + (RH / RS).toInt() + 3) {
        val lane = sim.laneAt(i)
        val y = RH - (i + 1) * RS + sim.cam
        when (lane) {
            is Lane.Grass -> {
                drawRect(if (i % 2 == 1) Color(0xFFC9A86A) else Color(0xFFD4B477), Offset(0f, y), Size(RW, RS))
                for (c in lane.cacti) {
                    val cx = c * RS + RS / 2
                    drawRect(Color(0xFF4FA34A), Offset(cx - 6, y + 8), Size(12f, 36f))
                    drawRect(Color(0xFF2A5A1E), Offset(cx - 6, y + 8), Size(12f, 36f), style = Stroke(2f))
                    drawRect(Color(0xFF4FA34A), Offset(cx - 16, y + 18), Size(8f, 12f))
                    drawRect(Color(0xFF4FA34A), Offset(cx + 8, y + 14), Size(8f, 12f))
                }
                drawCoins(lane.coins, y)
            }
            is Lane.Trail -> {
                drawRect(Color(0xFFA8784A), Offset(0f, y), Size(RW, RS))
                var x = 0f
                while (x < RW) { drawLine(Color(0x4D503214), Offset(x, y + RS / 2), Offset(x + 12f, y + RS / 2), 2f); x += 24f }
                for (o in lane.objs) when (lane.kind) {
                    "coach" -> {
                        drawRect(Color(0xFF8B2E1E), Offset(o.x, y + 8), Size(o.len * RS, 30f))
                        drawRect(Color(0xFF3A1A0A), Offset(o.x, y + 8), Size(o.len * RS, 30f), style = Stroke(2f))
                        drawRect(Color(0xFFF2D49B), Offset(o.x + 10, y + 13), Size(18f, 12f))
                        drawRect(Color(0xFFF2D49B), Offset(o.x + o.len * RS - 28, y + 13), Size(18f, 12f))
                        for (wx in listOf(o.x + 14, o.x + o.len * RS - 14)) drawCircle(Color(0xFF1B1B1B), 7f, Offset(wx, y + 40))
                    }
                    "bull" -> {
                        oval(o.x + RS / 2, y + RS / 2, 22f, 14f, Color(0xFF5A3A2A), null)
                        val hd = if (lane.speed > 0) 1 else -1
                        val cx = o.x + RS / 2
                        drawLine(Color(0xFFF2EDE0), Offset(cx + hd * 18, y + 14), Offset(cx + hd * 28, y + 6), 3f, StrokeCap.Round)
                        drawLine(Color(0xFFF2EDE0), Offset(cx + hd * 18, y + 34), Offset(cx + hd * 28, y + 42), 3f, StrokeCap.Round)
                    }
                    else -> {
                        for (k in 0 until 4) {
                            val start = (t / 200f + k) * 180f / PI.toFloat()
                            val r = 8f + k * 4f
                            drawArc(Color(0xFF8B6A3E), start, 229f, false, Offset(o.x + RS / 2 - r, y + RS / 2 - r),
                                Size(r * 2, r * 2), style = Stroke(2f))
                        }
                    }
                }
                drawCoins(lane.coins, y)
            }
            is Lane.River -> {
                drawRect(Color(0xFF3D8BC9), Offset(0f, y), Size(RW, RS))
                var x = (t / 30f) % 40f
                while (x < RW) {
                    val p = Path().apply { moveTo(x, y + 20); quadraticBezierTo(x + 8, y + 16, x + 16, y + 20) }
                    drawPath(p, Color.White.copy(alpha = 0.25f), style = Stroke(2f))
                    x += 40f
                }
                for (o in lane.objs) {
                    drawRect(Color(0xFF7A5230), Offset(o.x, y + 10), Size(o.len * RS, 30f))
                    drawRect(Color(0xFF4A2E18), Offset(o.x, y + 10), Size(o.len * RS, 30f), style = Stroke(2f))
                    for (k in 1 until o.len.toInt()) drawLine(Color.Black.copy(alpha = 0.2f), Offset(o.x + k * RS, y + 12), Offset(o.x + k * RS, y + 38), 2f)
                }
            }
        }
    }
    drawHorseRider(sim)
}

private fun DrawScope.drawCoins(coins: List<Int>, y: Float) {
    for (c in coins) oval(c * RS + RS / 2, y + RS / 2, 9f, 7f, Color(0xFFFFD21F), Color(0xFFB8860B), 2f)
}

private fun DrawScope.drawHorseRider(sim: RideSim) {
    val hero = sim.hero
    val hopY = if (hero.hop > 0) sin(hero.hop / 0.14f * PI.toFloat()) * 12f else 0f
    val hy = RH - (hero.row + 1) * RS + sim.cam + RS / 2 - hopY
    translate(hero.x, hy) {
        rotate(if (hero.dead) 34.4f else 0f, Offset.Zero) {
            oval(0f, 6f, 20f, 11f, Color(0xFF8B5A2B), LINE, 2f)
            rotate(28.6f, Offset(18f, -4f)) { oval(18f, -4f, 7f, 10f, Color(0xFF8B5A2B), LINE, 2f) }
            for (lx in listOf(-12f, -4f, 6f, 14f)) drawLine(LINE, Offset(lx, 14f), Offset(lx, 22f), 3f)
            val tail = Path().apply { moveTo(-20f, 2f); quadraticBezierTo(-30f, 8f, -26f, 18f); lineTo(-20f, 8f); close() }
            drawPath(tail, Color(0xFF3A2414))
            oval(-2f, -12f, 10f, 12f, Color(0xFFFFD21F), LINE, 2f)
            oval(-2f, -22f, 16f, 4f, Color(0xFF8B5A2B), LINE, 2f)
            drawRect(Color(0xFF8B5A2B), Offset(-9f, -30f), Size(14f, 8f))
            drawCircle(LINE, 1.8f, Offset(2f, -13f))
        }
    }
}
