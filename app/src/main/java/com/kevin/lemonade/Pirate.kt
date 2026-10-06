package com.kevin.lemonade

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sign
import kotlin.math.sin
import kotlin.random.Random

/**
 * Pirate adventure: sail around, sink lime ships, bring the limes home for Kevin to squeeze.
 * Ported from original/kevin.html lines ~6665-6970 (pirateShop/pl/ship/allies/voyages).
 */
data class PirateUpgrade(val id: String, val name: String, val desc: String, val base: Double, val grow: Double, val max: Int) {
    fun cost(level: Int): Long = (base * grow.pow(level)).roundToLong()
}

/** Like every shop but mineShop, the original gives every upgrade here +2 extra levels. */
object PirateShop {
    val all = listOf(
        PirateUpgrade("cannon", "Bigger cannons", "Cannonballs hit harder", 40.0, 1.7, 10),
        PirateUpgrade("reload", "Fast reload", "Your cannons fire faster", 30.0, 1.7, 10),
        PirateUpgrade("sails", "Swift sails", "Your ship sails faster", 30.0, 1.6, 8),
        PirateUpgrade("hull", "Iron hull", "+20 ship health", 35.0, 1.7, 12),
        PirateUpgrade("double", "More cannons", "+2 cannonballs every shot", 120.0, 2.4, 6),
        PirateUpgrade("hold", "Bigger cargo hold", "Carry 20 more limes", 25.0, 1.5, 12),
        PirateUpgrade("maps", "Treasure maps", "+50% gold from every lime ship", 50.0, 1.9, 8),
        PirateUpgrade("carpenter", "Ship's carpenter", "Your ship slowly repairs itself", 45.0, 1.9, 7),
        PirateUpgrade("allies", "Hire a pirate crew", "Another pirate ship joins you. Its captain hunts limes all by himself", 80.0, 2.2, 6),
    )
}

class PirateFoe(var x: Float, var y: Float, var a: Float, var hp: Double, val max: Double, val speed: Float, var cd: Float, val big: Boolean) {
    var wx = 0f; var wy = 0f; var wt = 0f; var hit = 0f; var dead = false
}
class PirateAlly(val i: Int, var x: Float, var y: Float, var hp: Double) {
    var a = 0f; var cd = Random.nextFloat(); var dead = false; var back = 0f
}
class PirateBall(var x: Float, var y: Float, val vx: Float, val vy: Float, val dmg: Double, val mine: Boolean, var life: Float)
class PirateBoom(val x: Float, val y: Float, var t: Float = 0f)
class PirateFloat(val x: Float, var y: Float, val text: String, var life: Float)

private const val PW = 800f
private const val PH = 500f
private val ALLY_NAMES = listOf("Captain Barnacle", "Salty Sue", "One-Eyed Pete", "Captain Zest", "Barnacle Bob", "Captain Sourbeard")
private val ALLY_SAIL = listOf(Color(0xFF9ED8F5), Color(0xFFFFB3D1), Color(0xFFFFE36B), Color(0xFFC9B6FF), Color(0xFFFFC48A), Color(0xFFB8F0E6))
private val ISLANDS = listOf(Triple(620f, 90f, 34f), Triple(210f, 420f, 26f), Triple(700f, 400f, 22f))

class PirateState(val g: GameViewModel) : Feature {
    override val key = "pirate"

    val lvl = mutableStateMapOf<String, Int>()
    fun lv(id: String) = lvl[id] ?: 0

    /** Limes brought home from sunk lime ships, ready to squeeze. INTEGRATION: the stand's squeeze
     * flow (Game.kt, owned by another agent) should read/decrement g.pirate.limesLeft the same way
     * it does lemonsLeft - the original squeezes limes first, before lemons, and shows "Squeeze a
     * lime (N left)" on the main button while any remain. g.pirate.limeCap() is how many it can hold. */
    var limesLeft by mutableIntStateOf(0)
    fun limeCap() = 20 + 20 * lv("hold")
    fun shipMax() = 40 + 20 * lv("hull")
    private fun allyMax() = 30.0 + 12 * lv("hull")

    /** the original's kevinAboard() = wearingSet('pirate') && !kevinGone. There's no kevinGone
     * flag ported yet (Kevin being away doing something else); add `&& !g.kevinGone` here once
     * one exists. */
    fun kevinAboard() = g.closet.wearingSet("pirate")

    var shipX by mutableFloatStateOf(140f)
    var shipY by mutableFloatStateOf(250f)
    var shipA = 0f
    var shipHp by mutableFloatStateOf(40f)
    private var shipCd = 0f
    var target: Offset? = null

    val foes = ArrayList<PirateFoe>()
    val balls = ArrayList<PirateBall>()
    val booms = ArrayList<PirateBoom>()
    val floats = ArrayList<PirateFloat>()
    val allies = ArrayList<PirateAlly>()

    var voyage by mutableIntStateOf(1)
    private var pBetween = 1.5f
    var banner by mutableStateOf("")
    private var bannerT = 0f
    var bannerOn by mutableStateOf(false)
    var kevinLine by mutableStateOf("")
    private var kevinQuip = 0f
    var kevinLineOn by mutableStateOf(false)

    fun pMsg(t: String, secs: Float = 1.8f) { banner = g.named(t); bannerT = secs; bannerOn = true }
    private fun kevinSays(t: String) { kevinLine = "${g.name}: $t"; kevinQuip = 2.5f; kevinLineOn = true }

    /** called once when entering the Pirate screen */
    fun enter() {
        shipHp = min(max(shipHp, 1f), shipMax().toFloat())
        if (kevinAboard()) kevinSays("Arr! Captain Kevin, ready to sail! Let's get those limes!")
        pMsg("Tap the sea to sail. Your cannons fire by themselves!", 2.6f)
    }

    fun leave() {
        if (limesLeft > 0) g.say("LIMES! ${limesLeft} of them! Finally, something I WANT to squeeze!")
    }

    private fun syncAllies() {
        while (allies.size < lv("allies")) {
            val i = allies.size
            allies.add(PirateAlly(i, 90f + i * 30, 120f + i * 90, allyMax()))
        }
    }

    private fun startVoyage() {
        foes.clear()
        syncAllies()
        for (al in allies) { al.dead = false; al.hp = allyMax() }
        val n = min(10, 2 + voyage)
        repeat(n) { i ->
            val big = voyage >= 3 && i == 0
            val hp = (if (big) 20.0 else 6.0) + voyage * 2
            foes.add(PirateFoe(
                x = 480f + Random.nextFloat() * 280f, y = 60f + Random.nextFloat() * 380f, a = PI.toFloat(),
                hp = hp, max = hp, speed = 40f + voyage * 3f + Random.nextFloat() * 20f, cd = 1f + Random.nextFloat() * 2f, big = big,
            ))
        }
        pMsg("Voyage $voyage: $n lime ships ahoy!")
    }

    private fun sink(f: PirateFoe) {
        f.dead = true
        booms.add(PirateBoom(f.x, f.y))
        val limes = (2 + voyage / 2) * (if (f.big) 3 else 1) * (if (kevinAboard()) 2 else 1)
        val room = limeCap() - limesLeft
        val got = max(0, min(room, limes))
        limesLeft += got
        val gold = Math.round((20 + 8 * voyage) * (if (f.big) 3.0 else 1.0) * (1 + 0.5 * lv("maps")) * (if (kevinAboard()) 1.25 else 1.0))
        g.money += gold
        floats.add(PirateFloat(f.x, f.y - 20f, "+${fmt(gold)}" + (if (got > 0) "  +$got limes" else "  (hold full!)"), 1.4f))
        if (kevinAboard() && Random.nextDouble() < 0.5) kevinSays(listOf("Arr! Take THAT, limes!", "Sink 'em all, matey!", "More limes for the squeezer!", "Yo ho ho!").pick())
    }

    private fun fireAt(fromX: Float, fromY: Float, tx: Float, ty: Float, speed: Float, dmg: Double, mine: Boolean, spread: Float = 0f) {
        val a = atan2(ty - fromY, tx - fromX) + spread
        balls.add(PirateBall(fromX, fromY, cos(a) * speed, sin(a) * speed, dmg, mine, 1.6f))
    }

    fun update(dt: Float) {
        if (bannerT > 0f) { bannerT -= dt; if (bannerT <= 0f) bannerOn = false }
        if (kevinQuip > 0f) { kevinQuip -= dt; if (kevinQuip <= 0f) kevinLineOn = false }
        if (pBetween > 0f) { pBetween -= dt; if (pBetween <= 0f) startVoyage() }

        target?.let { tgt ->
            val dx = tgt.x - shipX; val dy = tgt.y - shipY; val d = hypot(dx, dy)
            if (d > 14f) {
                val want = atan2(dy, dx)
                var da = ((want - shipA + PI.toFloat() * 3) % (PI.toFloat() * 2)) - PI.toFloat()
                shipA += max(-3f * dt, min(3f * dt, da))
                val sp = (90f + 22f * lv("sails")) * min(1f, d / 60f)
                shipX += cos(shipA) * sp * dt; shipY += sin(shipA) * sp * dt
            }
        }
        shipX = shipX.coerceIn(30f, PW - 30f); shipY = shipY.coerceIn(30f, PH - 30f)
        if (lv("carpenter") > 0) shipHp = min(shipMax().toFloat(), shipHp + 0.6f * lv("carpenter") * dt)

        shipCd -= dt
        var near: PirateFoe? = null; var nd = 1e9f
        for (f in foes) { val d = hypot(f.x - shipX, f.y - shipY); if (d < nd) { nd = d; near = f } }
        if (near != null && nd < 300f && shipCd <= 0f) {
            val n = 1 + 2 * lv("double") + (if (kevinAboard()) 1 else 0)
            for (i in 0 until n) fireAt(shipX, shipY, near.x, near.y, 380f, 2.0 + lv("cannon"), true, (i - (n - 1) / 2f) * 0.08f)
            shipCd = max(0.35f, 1.5f * 0.88f.pow(lv("reload")))
        }

        for (f in foes) {
            if (f.hit > 0f) f.hit -= dt
            f.wt -= dt
            if (f.wt <= 0f) {
                f.wt = 2f + Random.nextFloat() * 2f
                f.wx = 300f + Random.nextFloat() * 470f; f.wy = 50f + Random.nextFloat() * 400f
                if (Random.nextDouble() < 0.4) { f.wx = shipX + (Random.nextFloat() - 0.5f) * 200f; f.wy = shipY + (Random.nextFloat() - 0.5f) * 200f }
            }
            val want = atan2(f.wy - f.y, f.wx - f.x)
            val da = ((want - f.a + PI.toFloat() * 3) % (PI.toFloat() * 2)) - PI.toFloat()
            f.a += max(-1.5f * dt, min(1.5f * dt, da))
            f.x += cos(f.a) * f.speed * dt; f.y += sin(f.a) * f.speed * dt
            f.x = f.x.coerceIn(30f, PW - 30f); f.y = f.y.coerceIn(30f, PH - 30f)
            f.cd -= dt
            var tx = shipX; var ty = shipY; var td = hypot(f.x - shipX, f.y - shipY)
            for (al in allies) { if (al.dead) continue; val d = hypot(f.x - al.x, f.y - al.y); if (d < td) { td = d; tx = al.x; ty = al.y } }
            if (f.cd <= 0f && td < 250f) { fireAt(f.x, f.y, tx, ty, 260f, 3.0 + voyage, false); f.cd = 2.2f + Random.nextFloat() }
        }

        for (al in allies) {
            if (al.dead) { if ((al.back - dt).also { al.back = it } <= 0f) { al.dead = false; al.hp = allyMax(); al.x = 60f; al.y = 80f + al.i * 100f }; continue }
            var near2: PirateFoe? = null; var nd2 = 1e9f
            for (f in foes) { val d = hypot(f.x - al.x, f.y - al.y); if (d < nd2) { nd2 = d; near2 = f } }
            var gx: Float; var gy: Float
            if (near2 != null) {
                val back = atan2(al.y - near2.y, al.x - near2.x) + 0.6f * (if (al.i % 2 != 0) 1 else -1)
                gx = near2.x + cos(back) * 150f; gy = near2.y + sin(back) * 150f
            } else { gx = shipX - 60f - al.i * 25f; gy = shipY + (al.i - 1.5f) * 50f }
            val want = atan2(gy - al.y, gx - al.x); val dist = hypot(gx - al.x, gy - al.y)
            val da = ((want - al.a + PI.toFloat() * 3) % (PI.toFloat() * 2)) - PI.toFloat()
            al.a += max(-2.2f * dt, min(2.2f * dt, da))
            if (dist > 20f) { val sp = (80f + 15f * lv("sails")) * min(1f, dist / 80f); al.x += cos(al.a) * sp * dt; al.y += sin(al.a) * sp * dt }
            al.x = al.x.coerceIn(30f, PW - 30f); al.y = al.y.coerceIn(30f, PH - 30f)
            al.cd -= dt
            if (near2 != null && nd2 < 280f && al.cd <= 0f) {
                val n = 1 + lv("double")
                for (k in 0 until n) fireAt(al.x, al.y, near2.x, near2.y, 360f, 1.5 + 0.75 * lv("cannon"), true, (k - (n - 1) / 2f) * 0.08f)
                al.cd = max(0.5f, 1.9f * 0.9f.pow(lv("reload")))
            }
            if (lv("carpenter") > 0) al.hp = min(allyMax(), al.hp + 0.4 * lv("carpenter") * dt)
        }

        var sunk = false
        var shipSank = false
        for (b in balls.toList()) {
            if (shipSank) break
            b.x += b.vx * dt; b.y += b.vy * dt; b.life -= dt
            if (b.mine) {
                val hitFoe = foes.firstOrNull { !it.dead && hypot(b.x - it.x, b.y - it.y) < (if (it.big) 42f else 32f) }
                if (hitFoe != null) { b.life = 0f; hitFoe.hp -= b.dmg; hitFoe.hit = 0.12f; if (hitFoe.hp <= 0) { sink(hitFoe); sunk = true } }
            } else {
                val hitAl = allies.firstOrNull { !it.dead && hypot(b.x - it.x, b.y - it.y) < 28f }
                if (hitAl != null) {
                    b.life = 0f
                    if (Random.nextDouble() < g.dodge()) floats.add(PirateFloat(hitAl.x, hitAl.y - 40f, "LUCKY!", 0.6f))
                    else hitAl.hp -= b.dmg
                    if (hitAl.hp <= 0) {
                        hitAl.dead = true; hitAl.back = 8f; booms.add(PirateBoom(hitAl.x, hitAl.y))
                        floats.add(PirateFloat(hitAl.x, hitAl.y - 30f, "${ALLY_NAMES[hitAl.i]} sank! Back soon...", 1.6f))
                    }
                } else if (hypot(b.x - shipX, b.y - shipY) < 30f) {
                    b.life = 0f
                    if (Random.nextDouble() < g.dodge()) { floats.add(PirateFloat(shipX, shipY - 40f, "LUCKY! No damage", 0.7f)) }
                    else {
                        shipHp -= b.dmg.toFloat()
                        if (shipHp <= 0f) {
                            pMsg("Your ship sank! Patched it up, try again!", 2.4f)
                            shipHp = shipMax().toFloat(); shipX = 140f; shipY = 250f; target = null; balls.clear(); pBetween = 2.4f; foes.clear()
                            allies.forEachIndexed { i, al -> al.x = 90f + i * 30f; al.y = 120f + i * 90f }
                            if (kevinAboard()) kevinSays("Abandon ship! ...Okay, we're back.")
                            shipSank = true
                        }
                    }
                }
            }
        }
        if (!shipSank) balls.removeAll { it.life <= 0f }
        if (sunk) foes.removeAll { it.dead }
        if (pBetween <= 0f && foes.isEmpty()) {
            val bonus = Math.round((50 + 20 * voyage) * (1 + 0.5 * lv("maps")))
            g.money += bonus
            pMsg("All the limes are defeated! +${fmt(bonus)}", 2.4f)
            if (kevinAboard()) kevinSays("We beat ALL the limes! Arr!")
            voyage++; pBetween = 2.6f
        }
        for (b in booms) b.t += dt
        booms.removeAll { it.t >= 0.8f }
        for (f in floats) { f.y -= 30f * dt; f.life -= dt }
        floats.removeAll { it.life <= 0f }
    }

    fun buy(u: PirateUpgrade) {
        val level = lv(u.id)
        val c = u.cost(level)
        if (g.war.bonds < c || level >= u.max) return
        g.war.bonds -= c
        lvl[u.id] = level + 1
        if (u.id == "hull") shipHp = shipMax().toFloat()
        if (u.id == "allies") { syncAllies(); pMsg("${ALLY_NAMES[lv("allies") - 1]} joins your fleet!", 1.8f) }
    }

    override fun save(j: JSONObject) {
        j.put("lvl", JSONObject(lvl.toMap()))
        j.put("limesLeft", limesLeft); j.put("voyage", voyage); j.put("shipHp", shipHp.toDouble())
    }
    override fun load(j: JSONObject) {
        j.optJSONObject("lvl")?.let { o -> o.keys().forEach { lvl[it] = o.getInt(it) } }
        limesLeft = j.optInt("limesLeft", 0)
        voyage = j.optInt("voyage", 1)
        shipHp = j.optDouble("shipHp", shipMax().toDouble()).toFloat()
    }
    override fun reset() {
        lvl.clear(); limesLeft = 0; voyage = 1; pBetween = 1.5f
        shipX = 140f; shipY = 250f; shipA = 0f; shipHp = 40f; target = null
        foes.clear(); balls.clear(); booms.clear(); floats.clear(); allies.clear()
        bannerOn = false; kevinLineOn = false
    }
    /** the secret MAX OUT code: matches the original's maxEverything() for the pirateShop (8502-8526) */
    override fun maxOut() {
        for (u in PirateShop.all) lvl[u.id] = u.max
        shipHp = shipMax().toFloat()
        syncAllies()
    }
}

@Composable
fun PirateScreen(g: GameViewModel) {
    val pirate = g.pirate
    LaunchedEffect(Unit) { pirate.enter() }
    DisposableEffect(Unit) { onDispose { pirate.leave() } }
    var frame by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) withFrameNanos { now ->
            val dt = if (last == 0L) 0f else min(0.05f, (now - last) / 1e9f)
            last = now
            pirate.update(dt)
            frame = now
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ChunkyButton("Back to lemonade", color = CARD) { g.screen = Screen.STAND }
            Spacer(Modifier.weight(1f))
            MoneyText(g, 24)
        }
        Spacer(Modifier.height(4.dp))
        OutlinedTitle("Be a Pirate", 30, Color(0xFF9ED8F5))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Bonds: ${g.war.bonds}", fontWeight = FontWeight.Black, fontSize = 14.sp)
            Text("Limes: ${pirate.limesLeft} / ${pirate.limeCap()}", fontWeight = FontWeight.Black, fontSize = 14.sp)
            Text("Voyage ${pirate.voyage}", fontWeight = FontWeight.Black, fontSize = 14.sp)
        }
        Spacer(Modifier.height(6.dp))
        val shape = RoundedCornerShape(24.dp)
        Box(Modifier.fillMaxWidth().background(LINE, shape).padding(bottom = 6.dp).aspectRatio(PW / PH).clip(shape).border(4.dp, LINE, shape)) {
            Canvas(Modifier.fillMaxSize().pointerInput(Unit) {
                awaitEachGesture {
                    val sc = size.width / PW
                    val down = awaitFirstDown()
                    down.consume()
                    pirate.target = down.position / sc
                    while (true) {
                        val ev = awaitPointerEvent()
                        val c = ev.changes.firstOrNull() ?: break
                        c.consume()
                        pirate.target = c.position / sc
                        if (!c.pressed) break
                    }
                }
            }) {
                frame.let { }
                scale(size.width / PW, size.width / PW, Offset.Zero) { drawPirate(pirate) }
            }
            if (pirate.bannerOn) {
                Box(Modifier.align(Alignment.TopCenter).padding(top = 10.dp).background(LINE, RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)) {
                    Text(pirate.banner, color = PEEL, fontWeight = FontWeight.Black, fontSize = 14.sp, textAlign = TextAlign.Center)
                }
            }
            if (pirate.kevinLineOn) {
                Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp).background(Color.White, RoundedCornerShape(14.dp))
                    .border(3.dp, LINE, RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(pirate.kevinLine, fontSize = 13.sp, textAlign = TextAlign.Center)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Ship", fontWeight = FontWeight.Black, fontSize = 13.sp)
            Spacer(Modifier.width(6.dp))
            Box(Modifier.width(180.dp).height(14.dp).clip(CircleShape).background(Color(0xFFB9B5A0)).border(3.dp, LINE, CircleShape)) {
                val frac = (max(0f, pirate.shipHp) / pirate.shipMax()).coerceIn(0f, 1f)
                Box(Modifier.fillMaxHeight().fillMaxWidth(frac).background(Color(0xFF7CC56A)))
            }
            Spacer(Modifier.width(6.dp))
            Text("${max(0, pirate.shipHp.roundToIntDown())}/${pirate.shipMax()}", fontSize = 13.sp)
        }
        Text(
            if (pirate.kevinAboard()) "${g.name} is aboard in his pirate outfit! Double limes, +25% gold, and an extra cannon."
            else "Dress ${g.name} in the full Pirate outfit and he'll sail with you (double limes!).",
            fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp),
        )
        Text("Tap (or drag on) the sea to steer. Your cannons shoot the closest lime ship all by themselves.",
            fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(10.dp))
        Text("Pirate shop (costs war bonds)", fontFamily = Lilita, fontSize = 20.sp)
        for (u in PirateShop.all) {
            val level = pirate.lv(u.id)
            UpgradeCard(
                name = u.name, desc = u.desc,
                level = level, max = u.max, cost = u.cost(level), canBuy = g.war.bonds >= u.cost(level),
                levelText = if (u.id == "allies") "Pirate ships: $level of ${u.max}" else null,
            ) { pirate.buy(u) }
        }
        Spacer(Modifier.height(30.dp))
    }
}

private fun Float.roundToIntDown(): Int = kotlin.math.ceil(this).toInt()

private fun DrawScope.drawPirateShip(x: Float, y: Float, a: Float, lime: Boolean, hit: Boolean, big: Boolean, t: Float, sailColor: Color?) {
    translate(x, y) {
        rotate(a * 180f / PI.toFloat(), Offset.Zero) {
            val s = (if (big) 1.35f else 1f) * 1.45f
            scale(s, s, Offset.Zero) {
                drawOval(Color.White.copy(alpha = 0.35f), Offset(-36f, -16f), Size(20f, 32f))
                val hullColor = if (hit) Color.White else if (lime) Color(0xFF3E6B2E) else Color(0xFF8B5A2B)
                val hull = Path().apply {
                    moveTo(28f, 0f); quadraticBezierTo(18f, -15f, -22f, -13f); lineTo(-24f, 13f); quadraticBezierTo(18f, 15f, 28f, 0f); close()
                }
                drawPath(hull, hullColor); drawPath(hull, LINE, style = Stroke(3f))
                drawRect(if (lime) Color(0xFF5E8F3E) else Color(0xFFA8733E), Offset(-18f, -8f), Size(34f, 16f))
            }
        }
    }
    val ss = (if (big) 1.35f else 1f) * 1.45f
    translate(x, y - 6f * ss) {
        scale(ss, ss, Offset.Zero) {
            drawLine(LINE, Offset(0f, 10f), Offset(0f, -30f), 2.5f)
            val flap = sin(t * 3f + x) * 2f
            val sailFill = if (lime) Color(0xFFD8F5B0) else (sailColor ?: Color(0xFFF4EFD0))
            val sail = Path().apply {
                moveTo(-14f, -26f); quadraticBezierTo(0f, -30f + flap, 14f, -26f); lineTo(14f, -2f)
                quadraticBezierTo(0f, 2f + flap, -14f, -2f); close()
            }
            drawPath(sail, sailFill); drawPath(sail, LINE, style = Stroke(2.5f))
            if (lime) {
                drawCircle(Color(0xFF7BC043), 7f, Offset(0f, -14f)); drawCircle(LINE, 7f, Offset(0f, -14f), style = Stroke(1.5f))
                for (k in 0 until 6) {
                    val q = k * PI.toFloat() / 3
                    drawLine(Color(0xFFD8F5B0), Offset(0f, -14f), Offset(cos(q) * 6f, -14f + sin(q) * 6f), 1.5f)
                }
            } else {
                drawCircle(Color(0xFF1B1B2A), 5f, Offset(0f, -16f))
                drawLine(Color(0xFF1B1B2A), Offset(-6f, -8f), Offset(6f, -4f), 2f)
                drawLine(Color(0xFF1B1B2A), Offset(6f, -8f), Offset(-6f, -4f), 2f)
                val flag = Path().apply { moveTo(0f, -30f); lineTo(12f, -34f); lineTo(0f, -38f); close() }
                drawPath(flag, Color(0xFF1B1B2A))
            }
        }
    }
}

private fun DrawScope.pLabel(text: String, x: Float, y: Float, size: Float, fill: Color, outline: Color, alpha: Float = 1f) {
    val c = drawContext.canvas.nativeCanvas
    val paint = android.graphics.Paint().apply { isAntiAlias = true; textAlign = android.graphics.Paint.Align.CENTER; isFakeBoldText = true; textSize = size }
    val a255 = (alpha * 255).toInt().coerceIn(0, 255)
    paint.style = android.graphics.Paint.Style.STROKE; paint.strokeWidth = 4f
    paint.color = android.graphics.Color.argb(a255, (outline.red * 255).toInt(), (outline.green * 255).toInt(), (outline.blue * 255).toInt())
    c.drawText(text, x, y, paint)
    paint.style = android.graphics.Paint.Style.FILL
    paint.color = android.graphics.Color.argb(a255, (fill.red * 255).toInt(), (fill.green * 255).toInt(), (fill.blue * 255).toInt())
    c.drawText(text, x, y, paint)
}

private fun DrawScope.drawPirate(pirate: PirateState) {
    val t = (System.currentTimeMillis() % 1_000_000L).toFloat()
    drawRect(Brush.verticalGradient(listOf(Color(0xFF2E9BD6), Color(0xFF1E6FA8))), Offset.Zero, Size(PW, PH))
    var yy = 20f
    while (yy < PH) {
        var xx = (yy / 40 % 2) * 40
        while (xx < PW) {
            val o = sin(t / 700f + xx * 0.05f + yy) * 6f
            drawPath(Path().apply { moveTo(xx + o, yy); quadraticBezierTo(xx + 10f + o, yy - 6f, xx + 20f + o, yy) },
                Color.White.copy(alpha = 0.18f), style = Stroke(2f))
            xx += 80f
        }
        yy += 40f
    }
    for ((ix, iy, r) in ISLANDS) {
        drawOval(Color(0xFFF2D49B), Offset(ix - r * 1.4f, iy - r), Size(r * 2.8f, r * 2))
        drawOval(Color(0xFF7CC56A), Offset(ix - r, iy - 2f - r * 0.6f), Size(r * 2, r * 1.2f))
        drawLine(Color(0xFF6B4A1E), Offset(ix, iy), Offset(ix + 2f, iy - r * 1.4f), 3f)
        for (k in -2..2) drawOval(Color(0xFF3E9B4F), Offset(ix + 2f + k * 7f - 10f, iy - r * 1.4f - 4f), Size(20f, 8f))
    }
    pirate.target?.let { tgt ->
        drawCircle(Color.White.copy(alpha = 0.6f), 10f + sin(t / 150f) * 2f, tgt, style = Stroke(2f))
    }
    for (f in pirate.foes) {
        drawPirateShip(f.x, f.y, f.a, true, f.hit > 0f, f.big, t / 1000f, null)
        drawRect(Color(0xFF330000), Offset(f.x - 22f, f.y - 68f), Size(44f, 5f))
        drawRect(Color(0xFF7BC043), Offset(f.x - 22f, f.y - 68f), Size(44f * (max(0.0, f.hp) / f.max).toFloat(), 5f))
    }
    for (al in pirate.allies) {
        if (al.dead) continue
        drawPirateShip(al.x, al.y, al.a, false, false, false, t / 1000f, ALLY_SAIL[al.i % ALLY_SAIL.size])
        drawRect(Color(0xFF330000), Offset(al.x - 20f, al.y - 66f), Size(40f, 4f))
        drawRect(Color(0xFF8FCB5A), Offset(al.x - 20f, al.y - 66f), Size(40f * (max(0.0, al.hp) / 30.0).toFloat().coerceAtMost(40f), 4f))
        pLabel(ALLY_NAMES[al.i % ALLY_NAMES.size], al.x, al.y - 72f, 12f, Color.White, Color(0xFF0E2A3E))
    }
    drawPirateShip(pirate.shipX, pirate.shipY, pirate.shipA, false, false, false, t / 1000f, null)
    if (pirate.kevinAboard()) {
        val kx = pirate.shipX + 18f; val ky = pirate.shipY - 6f
        drawOval(Color(0xFFFFD21F), Offset(kx - 8f, ky - 10f), Size(16f, 20f))
        drawOval(LINE, Offset(kx - 8f, ky - 10f), Size(16f, 20f), style = Stroke(2f))
        val hat = Path().apply { moveTo(kx - 11f, ky - 7f); quadraticBezierTo(kx, ky - 20f, kx + 11f, ky - 7f); close() }
        drawPath(hat, Color(0xFF1B1B2A))
        drawCircle(Color.White, 2f, Offset(kx + 3f, ky - 1f))
        drawCircle(Color(0xFF1B1B2A), 2.4f, Offset(kx - 3f, ky - 1f))
    }
    for (b in pirate.balls) drawCircle(if (b.mine) Color(0xFF1B1B2A) else Color(0xFF4A6B2A), 4f, Offset(b.x, b.y))
    for (b in pirate.booms) {
        val a = 1f - b.t / 0.8f
        drawCircle(Color(0xFFFFC850).copy(alpha = a.coerceIn(0f, 1f)), 10f + b.t * 50f, Offset(b.x, b.y))
        for (k in 0 until 6) {
            val q = k + b.t * 6f
            drawCircle(Color(0xFF7BC043).copy(alpha = a.coerceIn(0f, 1f)), 5f, Offset(b.x + cos(q) * b.t * 60f, b.y + sin(q) * b.t * 60f))
        }
    }
    for (f in pirate.floats) pLabel(f.text, f.x, f.y, 16f, PEEL, Color(0xFF0E2A3E), max(0f, f.life))
}
