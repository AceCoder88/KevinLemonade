package com.kevin.lemonade

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sign
import kotlin.math.sin
import kotlin.random.Random

// Canvas field, ported from the original's #warCanvas (800x500 coordinates).
const val WAR_W = 800f
const val WAR_H = 500f
const val WAR_GROUND = 448f
const val WAR_WALL_X = 112f
const val WAR_GRAV = 260f
val WAR_BOW = Offset(92f, 236f)
val WAR_PIRATE_SHIP = Offset(470f, 330f)
val WAR_TUNNEL_Y = WAR_GROUND + 30f

/** every enemy kind the original spawns, same stats (keyed by wave number w). */
private class EStat(
    val hp: Double, val speed: Float, val r: Float, val pay: Int, val bite: Float,
    val fly: Boolean = false, val zig: Boolean = false, val hop: Boolean = false,
    val split: Boolean = false, val dodge: Boolean = false, val under: Boolean = false,
)

private fun eStat(kind: String, w: Int): EStat = when (kind) {
    "bug" -> EStat(1.0, 95f + w * 5, 9f, 1, 0.5f)
    "worm" -> EStat(1.0, 34f + w * 3, 14f, 0, 1f, split = true)
    "mini" -> EStat(1.0, 46f + w * 3, 9f, 1, 0.5f, dodge = true)
    "rat" -> EStat(4.0 + Math.floor(w / 5.0), 70f + w * 4, 18f, 3, 2f)
    "snail" -> EStat(6.0 + Math.floor(w / 3.0), 16f + w, 20f, 4, 1.5f)
    "king" -> EStat(30.0 + w * 4, 30f + w, 34f, 25 + w * 3, 5f)
    "bird" -> EStat(24.0 + w * 4, 60f, 30f, 25 + w * 3, 4f, fly = true)
    "bee" -> EStat(1.0, 105f + w * 4, 10f, 1, 0.5f, fly = true, zig = true)
    "crow" -> EStat(3.0 + Math.floor(w / 5.0), 80f + w * 3, 16f, 3, 1.5f, fly = true)
    "beetle" -> EStat(8.0 + Math.floor(w / 3.0), 30f + w * 2, 17f, 5, 1.5f)
    "frog" -> EStat(4.0 + Math.floor(w / 4.0), 150f + w * 4, 16f, 4, 1.5f, hop = true)
    "mole" -> EStat(3.0 + Math.floor(w / 5.0), 60f + w * 3, 15f, 4, 2f, under = true)
    "queen" -> EStat(40.0 + w * 5, 38f, 32f, 30 + w * 3, 4f, fly = true)
    "gsnail" -> EStat(80.0 + w * 6, 10f + w * 0.3f, 40f, 35 + w * 3, 6f)
    "wormking" -> EStat(35.0 + w * 5, 26f, 26f, 30 + w * 3, 4f)
    "megafrog" -> EStat(50.0 + w * 5, 210f, 36f, 35 + w * 3, 5f, hop = true)
    "moleking" -> EStat(120.0 + w * 6, 38f, 36f, 60 + w * 3, 7f, under = true)
    else -> EStat(1.0, 95f + w * 5, 9f, 1, 0.5f)
}

val WAR_BOSS_KINDS = setOf("bird", "king", "queen", "gsnail", "wormking", "megafrog", "moleking")
/** (kind, display name), same order and names as the original's BOSSES/ALL_BOSSES. */
val WAR_ALL_BOSSES = listOf(
    "bird" to "GIANT BIRD", "king" to "KING RAT", "queen" to "QUEEN BEE", "gsnail" to "GIANT SNAIL",
    "wormking" to "WORM KING", "megafrog" to "MEGA FROG", "moleking" to "MOLE KING",
)
val WAR_BOSSES = WAR_ALL_BOSSES.take(6)

class Enemy(val kind: String, x0: Float, val speed: Float, val r: Float, val pay: Int, val bite: Float,
            val fly: Boolean, val zig: Boolean, val hop: Boolean, val split: Boolean, val dodge: Boolean,
            under0: Boolean, val boss: Boolean, hp0: Double) {
    var x = x0
    var hp = hp0
    var max = hp0
    var under = under0
    var y = 0f
    var baseY = 0f
    var t = Random.nextFloat() * 6f
    var hit = 0f
    var atk = 0f
    var lt = 0f
    var lucky = false
    var cycle = 0f
    var burrowT = 0f
    var hopT = Random.nextFloat()
    var spawnT = 2f
    var dodgeT = 0f
    var slow = 0f
    var burn = 0f
    var burnDps = 0.0
    var mAtk = 0f
    var killed = false
    var revived = false
    var intangible = false
    var phaseT = Random.nextFloat() * 3f
    var glitch = 0f
    var spitCd = 2f
    var spitLeft = 0
    var spitGap = 0f
}

fun makeWarEnemy(kind: String, wave: Int, x: Float? = null): Enemy {
    val s = eStat(kind, wave)
    val e = Enemy(kind, x ?: (WAR_W + 40f), s.speed, s.r, s.pay, s.bite, s.fly, s.zig, s.hop, s.split, s.dodge,
        s.under, WAR_BOSS_KINDS.contains(kind), s.hp)
    e.baseY = if (s.fly) (if (kind == "bee") 230f + Random.nextFloat() * 80f else 150f + Random.nextFloat() * 60f)
              else WAR_GROUND - e.r * 0.6f
    e.y = e.baseY
    return e
}

class WArrow(var x: Float, var y: Float, var vx: Float, var vy: Float, val tower: Boolean) {
    var a = 0f
    var dead = false
    val hitSet = HashSet<Enemy>()
}

class WSoldier(val i: Int, val recruit: Boolean, var hp: Double, var max: Double, var x: Float, var y: Float,
               var dmg: Double, val sarge: Boolean = false, val reach: Float = 20f) {
    var swing = 0f
    var cd = Random.nextFloat() * 0.3f
    var hurt = 0f
}

class Miner(var x: Float) {
    var cd = 0f; var swing = 0f; var face = 1f; var phase = 0f
    var hp = 0.0; var max = 0.0; var hurt = 0f; var dead = false; var back = 0f
}

class Rock(val x0: Float, val y0: Float, val tx: Float) { var t = 0f; val dur = 1.1f; var done = false; var boom = 0f }
class Glob(val x0: Float, val y0: Float, val tx: Float) { var t = 0f; val dur = 0.85f; var done = false }
class DeadTroop(val i: Int, var t: Float)
class WFloater(val x: Float, var y: Float, val text: String, var life: Float)
class WConfetti(var x: Float, var y: Float, val vy: Float, val c: Color, var r: Float)

/** The battle: enemies, arrows, soldiers, towers, miners... stepped once per frame. Same numbers as the original. */
class WarSim(val war: WarState) {
    val g get() = war.g
    fun wl(id: String) = war.wl(id)

    val enemies = ArrayList<Enemy>()
    val arrows = ArrayList<WArrow>()
    var soldiers = ArrayList<WSoldier>()
    val miners = ArrayList<Miner>()
    val rocks = ArrayList<Rock>()
    val globs = ArrayList<Glob>()
    val floaters = ArrayList<WFloater>()
    val deadTroops = ArrayList<DeadTroop>()
    val spawnQ = ArrayList<Enemy>()
    val warConfetti = ArrayList<WConfetti>()

    var toSpawn = 0
    var spawnTimer = 0.5f
    var between = 1f
    var catT = 2f
    var aimX = WAR_W / 2; var aimY = 300f
    var firing = false
    var cooldown = 0f

    var banner by mutableStateOf("Defend the wall!")
    var bannerOn by mutableStateOf(true)
    private var bannerTimer = 1.8f

    private val towerCd = floatArrayOf(0f, .3f, .6f, .9f, 1.2f, 1.5f)
    private val archerCd = floatArrayOf(0f, .2f, .4f, .6f, .8f, 1f)
    val towerX = floatArrayOf(175f, 250f, 325f, 400f, 475f, 550f)
    val archerPos = arrayOf(84f to 330f, 84f to 390f, 34f to 345f, 34f to 405f, 84f to 450f, 34f to 462f)

    fun banner(text: String, secs: Float = 1.8f) { banner = text; bannerOn = true; bannerTimer = secs }

    fun bossInfo(): Pair<String, String> =
        if (war.wave % 40 == 0) "moleking" to "MOLE KING" else WAR_BOSSES[((war.wave / 5) - 1).mod(WAR_BOSSES.size)]
    fun bossName() = bossInfo().second

    fun startWave() {
        toSpawn = 8 + war.wave * 4; spawnTimer = 0.5f
        banner(if (war.wave % 5 == 0) "Wave ${war.wave}: ${bossName()}!" else "Wave ${war.wave}")
        war.maxWave = max(war.maxWave, war.wave)
        deploySoldiers(); deadTroops.clear()
        val mMax = 6.0 + 2 * wl("pick")
        for (m in miners) { m.dead = false; m.hp = mMax; m.max = mMax }
    }

    fun makeSoldierAt(i: Int): WSoldier {
        val recruit = i == 0
        val hp = (if (recruit) 6.0 else 8.0) + 2 * wl("armor")
        val dmg = (if (recruit) 0.6 else 1.2) * (1 + 0.25 * wl("sword"))
        return WSoldier(i, recruit, hp, hp, WAR_WALL_X + 14f + (i % 3) * 8f, WAR_GROUND - 16f - (i % 3) * 7f, dmg)
    }

    fun makeSarge(): WSoldier {
        val hp = (160.0 + 20 * wl("armor")) * (if (g.closet.wearingSet("soldier")) 2.0 else 1.0)
        val dmg = 6.0 * (1 + 0.25 * wl("sword")) * (if (g.closet.wearing("cape")) 1.25 else 1.0) *
            (if (g.closet.wearingSet("soldier")) 2.0 else 1.0) * (if (g.closet.wearingSet("hero")) 2.0 else 1.0)
        return WSoldier(6, false, hp, hp, WAR_WALL_X + 40f, WAR_GROUND - 46f, dmg, sarge = true, reach = 48f)
    }

    fun deploySoldiers() {
        soldiers = ArrayList()
        for (i in 0 until wl("troops")) soldiers.add(makeSoldierAt(i))
        if (g.closet.wearing("army")) soldiers.add(makeSarge())
    }

    fun hitEnemy(e: Enemy, dmg: Double, babies: MutableList<Enemy>) {
        if (e.split) {
            e.hp = 0.0
            val m1 = makeWarEnemy("mini", war.wave, e.x - 6f)
            val m2 = makeWarEnemy("mini", war.wave, e.x + 34f)
            m2.dodgeT = 1.2f
            babies.add(m1); babies.add(m2)
            floaters.add(WFloater(e.x, e.y - 30f, "SPLIT!", 0.8f))
            return
        }
        e.hp -= dmg; e.hit = 0.12f
        if (e.hp <= 0) kill(e)
    }

    fun kill(e: Enemy) {
        if (e.killed) return
        e.killed = true
        if (war.mode == WarMode.DEATH && !e.revived && !e.boss && e.kind != "mini" && Random.nextDouble() < 0.3) {
            val gh = makeWarEnemy(e.kind, war.wave, e.x + 20f)
            gh.revived = true; gh.hp = max(1.0, ceil(e.max / 2)); gh.max = gh.hp
            spawnQ.add(gh)
            floaters.add(WFloater(e.x, e.y - e.r - 34f, "BOO! It came back!", 1f))
        }
        val mult = if (war.mode == WarMode.DEATH) 2.0 else if (war.mode == WarMode.BROKEN) 3.0 else 1.0
        val got = max(1L, (e.pay * war.bondMult() * mult).roundToLong())
        war.bonds += got; war.totalBondsEarned += got
        floaters.add(WFloater(e.x, e.y - e.r - 10f, "+$got", 1f))
        if (e.boss) { war.bossesKilled++; banner("You defeated the ${WAR_ALL_BOSSES.first { it.first == e.kind }.second}!", 2.2f) }
        if (e.kind == "wormking") {
            for (k in 0 until 6) spawnQ.add(makeWarEnemy("worm", war.wave, e.x + k * 26f))
            floaters.add(WFloater(e.x, e.y - 50f, "WORMS!", 1f))
        }
    }

    fun wallFell() {
        banner("The wall fell! Try again!", 2.2f)
        enemies.clear(); arrows.clear(); soldiers = ArrayList(); spawnQ.clear(); rocks.clear(); globs.clear(); deadTroops.clear()
        war.wallHP = war.wallMax()
        between = 2.2f; toSpawn = 0
    }

    fun spawn() {
        val w = war.wave
        if (w % 5 == 0 && toSpawn == 1) { enemies.add(makeWarEnemy(bossInfo().first, w)); return }
        val pool = mutableListOf("bug" to 3f, "worm" to 3f, "rat" to min(4f, 0.6f + w * 0.5f))
        if (w >= 2) pool.add("bee" to 2f)
        if (w >= 3) pool.add("snail" to min(3f, (w - 2) * 0.6f))
        if (w >= 4) { pool.add("crow" to 1.5f); pool.add("beetle" to 1.5f) }
        if (w >= 5) pool.add("frog" to 2f)
        if (w >= 6) pool.add("mole" to 1.5f)
        var r = Random.nextFloat() * pool.sumOf { it.second.toDouble() }.toFloat()
        for ((k, wt) in pool) { r -= wt; if (r <= 0f) { enemies.add(makeWarEnemy(k, w)); return } }
        enemies.add(makeWarEnemy("bug", w))
    }

    private fun fireFrom(x0: Float, y0: Float, tx: Float, ty: Float, speed: Float, tower: Boolean) {
        val a = atan2(ty - y0, tx - x0)
        val dist = max(60f, hypot(tx - x0, ty - y0))
        val t = dist / speed
        val ar = WArrow(x0, y0, cos(a) * speed, sin(a) * speed - 0.5f * WAR_GRAV * t, tower)
        ar.a = a
        arrows.add(ar)
    }

    private fun towerGap() = (1.5 * 0.84.pow(wl("tspeed")) / (if (g.now() < g.superTowersUntil) 3.0 else 1.0)).toFloat()
    private fun towersAct(dt: Float) {
        for (i in 0 until wl("towers")) {
            towerCd[i] -= dt
            if (towerCd[i] > 0f) continue
            val tx = towerX[i]; val ty = WAR_GROUND - 112f
            var best: Enemy? = null
            for (e in enemies) if (e.hp > 0 && !e.under && e.x < WAR_W - 10f && (best == null || e.x < best!!.x)) best = e
            val b = best ?: continue
            val t = hypot(b.x - tx, b.y - ty) / 750f
            val lead = if (b.hop) b.speed * 0.35f * t else if (b.fly || (b.x - b.r > WAR_WALL_X)) b.speed * t else 0f
            val aimXb = b.x - lead + (if (b.kind == "worm") b.r * 1.5f else 0f)
            for (k in 0..wl("tmulti")) fireFrom(tx, ty, aimXb + (k - wl("tmulti") / 2f) * 14f, b.y - (k % 2) * 6f, 750f, true)
            towerCd[i] = towerGap()
        }
    }

    private fun wallArchersAct(dt: Float) {
        for (i in 0 until g.wallArchers) {
            archerCd[i] -= dt
            if (archerCd[i] > 0f) continue
            val (ax, ay0) = archerPos[i]; val ay = ay0 - 6f
            var best: Enemy? = null
            for (e in enemies) if (e.hp > 0 && !e.under && e.x < WAR_W - 10f && (best == null || e.x < best!!.x)) best = e
            val b = best ?: continue
            val t = hypot(b.x - ax, b.y - ay) / 800f
            val lead = if (b.hop) b.speed * 0.35f * t
                       else if (b.fly || (b.x - b.r > WAR_WALL_X)) b.speed * t * (if (b.slow > 0f) 0.4f else 1f) else 0f
            val aimXb = b.x - lead + (if (b.kind == "worm") b.r * 1.5f else 0f)
            for (k in -1..1) fireFrom(ax, ay, aimXb + k * 12f, b.y, 800f, false)
            archerCd[i] = max(0.35f, (1.1 * 0.9.pow(wl("rate"))).toFloat())
        }
    }

    private fun minersAct(dt: Float) {
        val mMax = 6.0 + 2 * wl("pick")
        while (miners.size < wl("miners")) miners.add(Miner(WAR_WALL_X + 20f + miners.size * 28f).also { it.hp = mMax; it.max = mMax })
        for ((i, m) in miners.withIndex()) {
            if (m.dead) {
                m.back -= dt
                if (m.back <= 0f) { m.dead = false; m.hp = mMax; m.max = mMax; m.x = WAR_WALL_X + 20f; floaters.add(WFloater(WAR_WALL_X + 30f, WAR_GROUND + 4f, "BACK!", 0.7f)) }
                continue
            }
            if (m.hurt > 0f) m.hurt -= dt
            m.cd -= dt; if (m.swing > 0f) m.swing -= dt
            var target: Enemy? = null
            for (e in enemies) if (e.under && e.hp > 0 && e.x < WAR_W && (target == null || abs(e.x - m.x) < abs(target!!.x - m.x))) target = e
            val goal = target?.x ?: (WAR_WALL_X + 30f + i * 28f)
            val d = goal - m.x
            if (abs(d) > 18f || target == null) {
                val step = sign(d) * min(abs(d), (150f + 25f * wl("pick")) * dt)
                m.x += step; m.phase += abs(step) * 0.15f
                if (abs(step) > 0.3f) m.face = sign(step)
            }
            if (target != null && abs(d) <= 22f && m.cd <= 0f) {
                target.hp -= 1.5 * (1 + 0.35 * wl("pick")); target.hit = 0.12f
                floaters.add(WFloater(target.x, WAR_GROUND - 10f, "CLANG!", 0.5f))
                if (target.hp <= 0) kill(target)
                m.swing = 0.25f; m.cd = max(0.3f, 0.75f - 0.06f * wl("pick"))
            }
            if (target != null && abs(d) <= 22f && target.hp > 0) {
                target.mAtk -= dt
                if (target.mAtk <= 0f) {
                    if (Random.nextDouble() < g.dodge()) floaters.add(WFloater(m.x, WAR_TUNNEL_Y - 24f, "LUCKY!", 0.5f))
                    else { m.hp -= if (target.boss) 2.0 else 1.0; m.hurt = 0.15f }
                    target.mAtk = 1f
                    if (m.hp <= 0) { m.dead = true; m.back = 10f; floaters.add(WFloater(m.x, WAR_GROUND, "OUCH!", 0.8f)) }
                }
            }
        }
    }

    fun shoot() {
        var n = 1 + wl("multi") * 2
        if (war.mode == WarMode.BROKEN && Random.nextDouble() < 0.3) n += 2 + Random.nextInt(3)
        val base = atan2(aimY - WAR_BOW.y, aimX - WAR_BOW.x)
        for (i in 0 until n) {
            val a = base + (i - (n - 1) / 2f) * min(0.07f, 1.4f / max(1, n - 1))
            val dist = max(60f, hypot(aimX - WAR_BOW.x, aimY - WAR_BOW.y))
            val t = dist / 900f
            val ar = WArrow(WAR_BOW.x, WAR_BOW.y, cos(a) * 900f, sin(a) * 900f - 0.5f * WAR_GRAV * t, false)
            ar.a = a
            arrows.add(ar)
        }
    }

    fun spawnConfetti() {
        warConfetti.clear()
        repeat(60) {
            warConfetti.add(WConfetti(Random.nextFloat() * WAR_W, -10f - Random.nextFloat() * 80f, 120f + Random.nextFloat() * 120f,
                listOf(RED, PEEL, Color(0xFF4FA34A), Color(0xFF2E9BD6), Color(0xFFFF7FB0)).pick(), Random.nextFloat() * 6f))
        }
    }

    fun tapPirateShip(p: Offset): Boolean {
        if ((g.lv("map") > 0 || g.bypassLocks) && abs(p.x - WAR_PIRATE_SHIP.x) < 30f &&
            p.y > WAR_PIRATE_SHIP.y - 40f && p.y < WAR_PIRATE_SHIP.y + 14f) {
            g.screen = Screen.PIRATE
            return true
        }
        return false
    }

    fun press(p: Offset) {
        if (tapPirateShip(p)) return
        if (war.paused) return
        aimX = p.x; aimY = p.y; firing = true
        if (cooldown <= 0f) { shoot(); cooldown = war.fireGap() }
    }
    fun move(p: Offset) { aimX = p.x; aimY = p.y }
    fun release() { firing = false }

    fun spawnBoss(kind: String) {
        enemies.add(makeWarEnemy(kind, war.wave))
        banner("${WAR_ALL_BOSSES.first { it.first == kind }.second} appears!", 1.6f)
    }

    fun update(dt: Float) {
        if (bannerTimer > 0f) { bannerTimer -= dt; if (bannerTimer <= 0f) bannerOn = false }
        cooldown -= dt
        if (firing && cooldown <= 0f) { shoot(); cooldown = war.fireGap() }
        towersAct(dt)
        wallArchersAct(dt)
        if (wl("regen") > 0) war.wallHP = min(war.wallMax(), war.wallHP + 0.4f * wl("regen") * dt)
        minersAct(dt)
        for (gl in globs) {
            gl.t += dt
            if (gl.t >= gl.dur && !gl.done) {
                gl.done = true
                spawnQ.add(makeWarEnemy("mini", war.wave, gl.tx))
                floaters.add(WFloater(gl.tx, WAR_GROUND - 30f, "SPLAT!", 0.6f))
            }
        }
        globs.removeAll { it.done }
        if (wl("catapult") > 0) {
            catT -= dt
            if (catT <= 0f) {
                var target: Enemy? = null; var bestN = 0
                for (e in enemies) {
                    if (e.fly || e.under || e.hp <= 0 || e.x > WAR_W - 20f) continue
                    val n = enemies.count { !it.fly && !it.under && abs(it.x - e.x) < 70f }
                    if (n > bestN) { bestN = n; target = e }
                }
                if (target != null) {
                    val tx = max(WAR_WALL_X + 40f, target.x - target.speed * 1.1f * (if (target.slow > 0f) 0.4f else 1f))
                    rocks.add(Rock(30f, 250f, tx))
                    catT = max(1.6f, 6f - 0.8f * wl("catapult"))
                } else catT = 0.5f
            }
        }
        for (r in rocks) {
            r.t += dt
            if (r.t >= r.dur && !r.done) {
                r.done = true
                val dmg = 2.0 + 1.6 * wl("catapult")
                for (e in enemies) if (!e.fly && !e.under && e.hp > 0 && abs(e.x - r.tx) < 70f) {
                    if (e.split) hitEnemy(e, 0.0, spawnQ) else { e.hp -= dmg; e.hit = 0.15f; if (e.hp <= 0) kill(e) }
                }
                floaters.add(WFloater(r.tx, WAR_GROUND - 40f, "BOOM!", 0.8f))
                r.boom = 0.35f
            }
            if (r.done) r.boom -= dt
        }
        rocks.removeAll { it.done && it.boom <= 0f }
        for (d in deadTroops) d.t -= dt
        for (d in deadTroops.filter { it.t <= 0f }) { soldiers.add(makeSoldierAt(d.i)); floaters.add(WFloater(WAR_WALL_X + 30f, WAR_GROUND - 40f, "BACK!", 0.8f)) }
        deadTroops.removeAll { it.t <= 0f }

        if (between > 0f) { between -= dt; if (between <= 0f) startWave() }
        else if (toSpawn > 0) {
            spawnTimer -= dt
            if (spawnTimer <= 0f) { spawn(); toSpawn--; spawnTimer = max(0.35f, 1.6f - war.wave * 0.08f) * (0.6f + Random.nextFloat() * 0.8f) }
        } else if (toSpawn == 0 && enemies.isEmpty()) {
            val bonus = max(0L, (5 * war.wave * war.bondMult()).roundToLong())
            war.bonds += bonus; war.totalBondsEarned += bonus
            banner("Wave ${war.wave} cleared! +$bonus bonds", 2f)
            if (war.wave % 5 == 0) spawnConfetti()
            war.wave++; war.maxWave = max(war.maxWave, war.wave)
            between = 2.3f
        }

        val babies = ArrayList<Enemy>()
        for (e in enemies) {
            e.t += dt; if (e.hit > 0f) e.hit -= dt
            e.atk -= dt
            if (war.mode == WarMode.DEATH) {
                e.phaseT += dt
                e.intangible = !e.boss && (e.phaseT % 3.2f) > 2.3f
            } else e.intangible = false
            if (war.mode == WarMode.BROKEN && !e.boss && !e.under) {
                if (Random.nextDouble() < 0.18 * dt) {
                    e.x = (e.x + (if (Random.nextDouble() < 0.6) -1 else 1) * (40f + Random.nextFloat() * 60f)).coerceIn(WAR_WALL_X + 40f, WAR_W - 20f)
                    e.glitch = 0.2f
                    floaters.add(WFloater(e.x, e.y - e.r - 14f, "GLITCH", 0.5f))
                }
                if (Random.nextDouble() < 0.025 * dt && enemies.size < 80) {
                    val cl = makeWarEnemy(e.kind, war.wave, e.x + 30f)
                    cl.hp = max(1.0, e.hp / 2); cl.max = cl.hp
                    spawnQ.add(cl)
                    floaters.add(WFloater(e.x, e.y - e.r - 26f, "CLONE!", 0.7f))
                }
                if (e.glitch > 0f) e.glitch -= dt
            }
            val stopAt = if (e.fly) WAR_WALL_X + 40f else WAR_WALL_X
            if (e.burrowT > 0f) e.burrowT -= dt
            if (e.dodge) {
                e.dodgeT += dt
                if (!e.under && e.dodgeT > 2.2f) { e.under = true; e.burrowT = 1.3f; e.dodgeT = 0f }
                else if (e.under && e.burrowT <= 0f) { e.under = false; e.dodgeT = 0f }
            }
            if (e.under && !e.dodge && e.burrowT <= 0f && e.x - e.r < WAR_WALL_X + 150f) { e.under = false; floaters.add(WFloater(e.x, e.y - 30f, "POP!", 0.7f)) }
            if (e.kind == "wormking" && e.x < WAR_W - 20f) {
                e.spitCd -= dt
                if (e.spitCd <= 0f && e.spitLeft == 0) { e.spitLeft = 6; e.spitGap = 0f; floaters.add(WFloater(e.x, e.y - 60f, "PTOO!", 0.8f)) }
                if (e.spitLeft > 0) {
                    e.spitGap -= dt
                    if (e.spitGap <= 0f) {
                        globs.add(Glob(e.x - 24f, e.y - 14f, max(WAR_WALL_X + 40f, e.x - 90f - Random.nextFloat() * 260f)))
                        e.spitLeft--; e.spitGap = 0.28f
                        if (e.spitLeft == 0) e.spitCd = 6f
                    }
                }
            }
            if (e.kind == "moleking" && !e.under) {
                e.cycle += dt
                if (e.cycle > 6f) {
                    e.cycle = 0f; e.under = true; e.burrowT = 1.8f
                    for (k in 0 until 2) spawnQ.add(makeWarEnemy("mole", war.wave, WAR_W + 40f + k * 60f))
                    floaters.add(WFloater(e.x, e.y - 50f, "DIG!", 0.8f))
                }
            }
            val blocker = if (e.fly || e.under) null else soldiers.firstOrNull { it.hp > 0 && it.x < e.x && e.x - e.r - it.x < (if (it.sarge) it.reach else 16f) }
            var move = e.speed
            if (e.under && miners.any { !it.dead && it.x < e.x && e.x - it.x < 22f }) move = 0f
            if (e.hop) {
                e.hopT += dt
                val ph = e.hopT % 1.4f; val hopping = ph < 0.5f
                move = if (hopping) e.speed else 0f
                e.y = e.baseY - (if (hopping && blocker == null && e.x - e.r > stopAt) sin(ph / 0.5f * PI.toFloat()) * e.r * 2.2f else 0f)
            }
            if (e.kind == "queen" && e.x < WAR_W) {
                e.spawnT -= dt
                if (e.spawnT <= 0f) { val b = makeWarEnemy("bee", war.wave, e.x); b.baseY = e.y; spawnQ.add(b); e.spawnT = 2.6f }
            }
            if (e.slow > 0f) { e.slow -= dt; move *= 0.4f }
            if (e.burn > 0f) {
                e.burn -= dt; e.hp -= e.burnDps * dt
                if (e.hp <= 0) { kill(e); continue }
            }
            if (blocker != null) {
                if (e.atk <= 0f) {
                    if (Random.nextDouble() < g.dodge()) floaters.add(WFloater(blocker.x, blocker.y - 30f, "LUCKY MISS!", 0.6f))
                    else { blocker.hp -= (if (e.boss) 2.0 else 1.0); blocker.hurt = 0.15f }
                    e.atk = 1f
                }
            } else if (e.x - e.r > stopAt) e.x -= move * dt
            else {
                e.lt -= dt
                if (e.lt <= 0f) { e.lt = 1f; e.lucky = Random.nextDouble() < g.dodge(); if (e.lucky) floaters.add(WFloater(WAR_WALL_X + 30f, e.y - e.r - 16f, "LUCKY! No damage", 0.7f)) }
                if (!e.lucky) war.wallHP -= e.bite * dt
                if (wl("spikes") > 0 && !e.fly) {
                    e.hp -= 0.6 * wl("spikes") * dt; e.hit = max(e.hit, 0.05f)
                    if (e.hp <= 0) { kill(e); continue }
                }
            }
            if (e.fly) {
                val amp = if (e.zig) 30f else 45f; val sp = if (e.zig) 6f else 2.2f
                e.y = if (e.x - e.r > stopAt) e.baseY + sin(e.t * sp) * amp else 255f + sin(e.t * 9f) * 18f
            }
        }
        if (war.wallHP <= 0f) { wallFell(); return }

        for (a in arrows) {
            a.vy += WAR_GRAV * dt; a.x += a.vx * dt; a.y += a.vy * dt; a.a = atan2(a.vy, a.vx)
            for (e in enemies) {
                val worm = e.kind == "worm" || e.kind == "wormking"
                val hx = if (worm) e.x + e.r * 1.7f else e.x
                val hr = if (worm) e.r * 2.4f else e.r + 10f
                if (e.hp > 0 && !e.under && !e.intangible && !a.hitSet.contains(e) && abs(a.x - hx) < hr && abs(a.y - e.y) < e.r + 10f) {
                    a.hitSet.add(e)
                    if (a.hitSet.size > wl("pierce")) a.dead = true
                    var dmg = if (a.tower) war.arrowDmg() * 0.8 else war.arrowDmg()
                    val critChance = 0.08 * wl("crit") + (if (g.closet.wearingSet("ninja")) 0.2 else 0.0) + 0.05 * g.lv("critluck")
                    if ((wl("crit") > 0 || g.closet.wearingSet("ninja") || g.lv("critluck") > 0) && Random.nextDouble() < critChance) {
                        dmg *= 3; floaters.add(WFloater(e.x, e.y - e.r - 26f, "CRIT!", 0.7f))
                    }
                    if (wl("fire") > 0) { e.burn = 3f; e.burnDps = 0.35 * wl("fire") }
                    if (wl("ice") > 0 && Random.nextDouble() < 0.18 * wl("ice")) e.slow = 2f
                    hitEnemy(e, dmg, babies)
                    if (a.dead) break
                }
            }
            if (a.x > WAR_W + 30f || a.y > WAR_H + 30f || a.y < -200f) a.dead = true
        }
        for (s in soldiers) {
            if (s.hp <= 0) continue
            s.cd -= dt; if (s.swing > 0f) s.swing -= dt; if (s.hurt > 0f) s.hurt -= dt
            var target: Enemy? = null; var best = 1e9f
            for (e in enemies) if (!e.fly && !e.under && e.hp > 0 && e.x > s.x - 10f && e.x - s.x < best) { best = e.x - s.x; target = e }
            val reach = if (s.sarge) s.reach else 20f
            if (target != null && target.x - target.r - s.x < reach) {
                if (s.cd <= 0f) { hitEnemy(target, s.dmg, babies); s.swing = 0.25f; s.cd = if (s.recruit) 0.8f else 0.6f }
            } else {
                val goal = target?.x?.coerceAtMost(WAR_W - 60f) ?: (WAR_WALL_X + 70f + s.i * 34f)
                if (abs(goal - s.x) > 3f) s.x += sign(goal - s.x) * 110f * dt
            }
        }
        if (wl("barracks") > 0) for (s in soldiers) if (s.hp <= 0) deadTroops.add(DeadTroop(s.i, 12f - 2f * wl("barracks")))
        soldiers = ArrayList(soldiers.filter { it.hp > 0 })
        arrows.removeAll { it.dead }
        while (arrows.size > 1500) arrows.removeAt(0)
        enemies.removeAll { it.hp <= 0 }
        enemies.addAll(babies)
        enemies.addAll(spawnQ)
        spawnQ.clear()
        for (f in floaters) { f.y -= 40f * dt; f.life -= dt }
        floaters.removeAll { it.life <= 0f }
        for (c in warConfetti) { c.y += c.vy * dt; c.r += 0.1f }
        warConfetti.removeAll { it.y > WAR_H + 10f }
    }
}

// =================================== drawing ===================================
private val warPaint = android.graphics.Paint().apply { isAntiAlias = true; textAlign = android.graphics.Paint.Align.CENTER; isFakeBoldText = true }
private fun DrawScope.warLabel(text: String, x: Float, y: Float, size: Float, fill: Int, outline: Int = 0xFF15180A.toInt()) {
    val c = drawContext.canvas.nativeCanvas
    warPaint.textSize = size
    warPaint.style = android.graphics.Paint.Style.STROKE; warPaint.strokeWidth = 4f; warPaint.color = outline
    c.drawText(text, x, y, warPaint)
    warPaint.style = android.graphics.Paint.Style.FILL; warPaint.color = fill
    c.drawText(text, x, y, warPaint)
}
private fun DrawScope.warShadow(x: Float, y: Float, w: Float, a: Float) =
    drawOval(Color.Black.copy(alpha = a), Offset(x - w, y - w * 0.22f), Size(w * 2, w * 0.44f))
private fun DrawScope.crown(x: Float, y: Float, s: Float) {
    translate(x, y) {
        scale(s, s, Offset.Zero) {
            val p = Path().apply {
                moveTo(-14f, 6f); lineTo(-12f, -8f); lineTo(-6f, -1f); lineTo(0f, -11f); lineTo(6f, -1f); lineTo(12f, -8f); lineTo(14f, 6f); close()
            }
            drawPath(p, Color(0xFFFFD21F)); drawPath(p, LINE, style = Stroke(2f))
            drawCircle(Color(0xFFE8483B), 2.5f, Offset(0f, 1f))
        }
    }
}
private fun DrawScope.healthBar(x: Float, topY: Float, w: Float, hp: Double, maxHp: Double) {
    if (maxHp <= 1) return
    drawRect(Color(0xFF300000), Offset(x - w / 2, topY), Size(w, 6f))
    drawRect(Color(0xFFE8483B), Offset(x - w / 2, topY), Size((w * max(0.0, hp) / maxHp).toFloat(), 6f))
}

private fun DrawScope.drawEnemyBody(e: Enemy) {
    when (e.kind) {
        "worm", "wormking", "mini" -> {
            if (e.under) {
                drawOval(Color(0xFF7A5230), Offset(e.x - 6f, WAR_GROUND - 9f), Size(32f, 14f))
                return
            }
            val es = e.r / 14f
            for (i in 4 downTo 0) {
                if (e.kind == "mini" && i > 1) continue
                val x = e.x + i * e.r * 0.85f; val y = e.y + sin(e.t * 8 + i) * 4f
                drawCircle(if (e.hit > 0f) Color.White else if (i == 0) Color(0xFFE77F9B) else Color(0xFFF29BB2), e.r * es * (if (i == 0) 1f else 0.85f), Offset(x, y))
            }
            if (e.kind == "wormking") crown(e.x - 2f, e.y - e.r - 2f, 1f)
        }
        "rat", "king" -> {
            val s = e.r / 18f
            drawOval(if (e.hit > 0f) Color.White else if (e.kind == "king") Color(0xFF5E5468) else Color(0xFF9A9AA6), Offset(e.x - 18f * s, e.y - 14f * s), Size(36f * s, 28f * s))
            drawOval(Color(0xFFF2B8C6), Offset(e.x - 30f * s, e.y - 20f * s), Size(16f * s, 14f * s))
            drawCircle(Color(0xFF111111), 2f * s, Offset(e.x - 27f * s, e.y - 16f * s))
            if (e.kind == "king") crown(e.x - 22f * s, e.y - 20f * s, 0.7f * s)
        }
        "bug" -> drawOval(if (e.hit > 0f) Color.White else Color(0xFF3E8E4A), Offset(e.x - 11f, e.y - 8f), Size(22f, 16f))
        "snail", "gsnail" -> {
            val s = e.r / 20f
            drawOval(Color(0xFFC9B48A), Offset(e.x - 26f * s, e.y + 2f * s), Size(52f * s, 16f * s))
            drawCircle(if (e.hit > 0f) Color.White else Color(0xFFB5652E), 17f * s, Offset(e.x + 6f * s, e.y - 4f * s))
        }
        "bird" -> {
            drawOval(if (e.hit > 0f) Color.White else Color(0xFF5B7FC9), Offset(e.x - 28f, e.y - 20f), Size(56f, 40f))
            drawPath(Path().apply { moveTo(e.x - 46f, e.y - 10f); lineTo(e.x - 62f, e.y - 4f); lineTo(e.x - 46f, e.y) }, Color(0xFFF2A531))
        }
        "bee", "queen" -> {
            drawCircle(if (e.hit > 0f) Color.White else Color(0xFFFFC81F), 13f * (e.r / 10f), Offset(e.x + 2f, e.y))
            drawRect(Color(0xFF2A2010), Offset(e.x - 2f, e.y - 10f * (e.r / 10f)), Size(4f, 20f * (e.r / 10f)))
            if (e.kind == "queen") crown(e.x - 2f, e.y - 12f * (e.r / 10f), 0.55f)
        }
        "crow" -> {
            drawOval(if (e.hit > 0f) Color.White else Color(0xFF2B2B35), Offset(e.x - 14f, e.y - 10f), Size(36f, 20f))
            drawPath(Path().apply { moveTo(e.x + 20f, e.y - 2f); lineTo(e.x + 30f, e.y - 6f); lineTo(e.x + 28f, e.y + 4f) }, Color(0xFFF2A531))
        }
        "beetle" -> drawOval(if (e.hit > 0f) Color.White else Color(0xFF5B2E9E), Offset(e.x - 20f, e.y - 14f), Size(40f, 28f))
        "frog", "megafrog" -> {
            val col = if (e.hit > 0f) Color.White else if (e.kind == "megafrog") Color(0xFF3E7A2E) else Color(0xFF5DBB46)
            drawOval(col, Offset(e.x - 18f, e.y - 13f), Size(36f, 26f))
            for (ex in listOf(-12f, -2f)) drawCircle(col, 6f, Offset(e.x + ex, e.y - 12f))
            for (ex in listOf(-12f, -2f)) drawCircle(Color.White, 4f, Offset(e.x + ex, e.y - 12f))
            if (e.kind == "megafrog") crown(e.x - 7f, e.y - 24f, 0.6f)
        }
        "mole", "moleking" -> {
            if (e.under) { drawOval(Color(0xFF7A5230), Offset(e.x - 20f, WAR_GROUND - 8f), Size(40f, 16f)); return }
            val s = e.r / 15f
            drawOval(if (e.hit > 0f) Color.White else Color(0xFF6B4A3A), Offset(e.x - 18f * s, e.y - 14f * s), Size(36f * s, 28f * s))
            drawCircle(Color(0xFFFF9AB0), 4f * s, Offset(e.x - 17f * s, e.y - 1f * s))
            if (e.kind == "moleking") crown(e.x - 4f, e.y - e.r - 4f, 1.2f)
        }
    }
}

private fun DrawScope.drawSoldier(s: WSoldier, name: String) {
    if (s.sarge) { drawSarge(s, name); return }
    val x = s.x; val y = s.y
    drawOval(if (s.hurt > 0f) Color.White else Color(0xFFFFD21F), Offset(x - 13f, y - 16f), Size(26f, 32f))
    drawOval(if (s.recruit) Color(0xFFA0522D) else Color(0xFF8C96A0), Offset(x - 14f, y - 18f), Size(28f, 16f))
    rotate(if (s.swing > 0f) (0.6f - (s.swing / 0.25f) * 1.8f) * 57.3f else -0.9f * 57.3f, Offset(x + 10f, y + 2f)) {
        drawLine(if (s.recruit) Color(0xFFB8B8B8) else Color(0xFFE6EEF5), Offset(x + 10f, y - 5f), Offset(x + 10f, y - (if (s.recruit) 22f else 28f)), 4f, StrokeCap.Round)
    }
    healthBar(x, y - 36f, 28f, s.hp, s.max)
}

private fun DrawScope.drawSarge(s: WSoldier, name: String) {
    val x = s.x; val y = s.y
    drawOval(if (s.hurt > 0f) Color.White else Color(0xFFFFD21F), Offset(x - 36f, y - 42f), Size(72f, 84f))
    drawArc(Color(0xFF4B5A2A), 180f, 180f, true, Offset(x - 38f, y - 82f), Size(76f, 56f))
    rotate(if (s.swing > 0f) (0.9f - (s.swing / 0.25f) * 2.2f) * 57.3f else -0.7f * 57.3f, Offset(x + 34f, y + 4f)) {
        drawLine(Color(0xFFE6EEF5), Offset(x + 34f, y - 10f), Offset(x + 34f, y - 62f), 7f, StrokeCap.Round)
    }
    healthBar(x, y - 92f, 80f, s.hp, s.max)
    warLabel("Sgt. $name", x, y - 98f, 14f, 0xFFFFD21F.toInt())
}

private fun DrawScope.drawMiner(m: Miner) {
    if (m.dead) return
    val x = m.x; val y = WAR_TUNNEL_Y
    if (m.max > 1) { drawRect(Color(0xFF300000), Offset(x - 10f, y - 24f), Size(20f, 3f)); drawRect(Color(0xFF8FCB5A), Offset(x - 10f, y - 24f), Size((20f * max(0.0, m.hp) / m.max).toFloat(), 3f)) }
    drawOval(if (m.hurt > 0f) Color.White else Color(0xFFFFD21F), Offset(x - 8f, y - 10f), Size(16f, 20f))
    drawOval(Color(0xFFF2C200), Offset(x - 9f, y - 13f), Size(18f, 10f))
}

private fun DrawScope.drawTowerAt(x: Float) {
    val top = WAR_GROUND - 100f
    warShadow(x, WAR_GROUND + 3f, 26f, 0.2f)
    drawRect(Color(0xFF8B5A2B), Offset(x - 20f, top), Size(7f, 100f))
    drawRect(Color(0xFF8B5A2B), Offset(x + 13f, top), Size(7f, 100f))
    val ag = Brush.radialGradient(0f to Color(0xFFFFF6B8), 0.5f to Color(0xFFFFD21F), 1f to Color(0xFFE3A600), center = Offset(x, top - 12f), radius = 16f)
    drawOval(ag, Offset(x - 11f, top - 26f), Size(22f, 28f))
    drawArc(Color(0xFF4B5A2A), 180f, 180f, true, Offset(x - 12f, top - 28f), Size(24f, 12f))
    drawRect(Color(0xFFA8733E), Offset(x - 26f, top - 4f), Size(52f, 12f))
    drawLine(LINE, Offset(x - 22f, top - 10f), Offset(x - 22f, top - 42f), 2f)
    drawPath(Path().apply { moveTo(x - 22f, top - 42f); lineTo(x - 6f, top - 37f); lineTo(x - 22f, top - 31f) }, RED)
}

private fun DrawScope.drawWallArcherAt(x: Float, y: Float) {
    drawRect(Color(0xFF2A2620), Offset(x - 18f, y - 12f), Size(36f, 32f))
    drawArc(Color(0xFF2A2620), 180f, 180f, true, Offset(x - 18f, y - 30f), Size(36f, 36f))
    val ag = Brush.radialGradient(0f to Color(0xFFFFF6B8), 0.5f to Color(0xFFFFD21F), 1f to Color(0xFFE3A600), center = Offset(x - 4f, y - 4f), radius = 18f)
    drawOval(ag, Offset(x - 12f, y - 13f), Size(24f, 30f))
    drawArc(Color(0xFF4B5A2A), 180f, 180f, true, Offset(x - 13f, y - 18f), Size(26f, 13f))
}

private fun DrawScope.drawArrow(a: WArrow) {
    translate(a.x, a.y) {
        rotate(a.a * 57.29578f, Offset.Zero) {
            drawLine(Color(0xFF4A2E12), Offset(-22f, 0f), Offset(0f, 0f), 3f)
            drawRect(RED, Offset(-24f, -3f), Size(5f, 6f))
            drawPath(Path().apply { moveTo(6f, 0f); lineTo(-2f, -4f); lineTo(-2f, 4f) }, Color(0xFF555555))
        }
    }
}

private fun DrawScope.drawArcher(war: WarState, sim: WarSim) {
    val ag = Brush.radialGradient(0f to Color(0xFFFFF6B8), 0.5f to Color(0xFFFFD21F), 1f to Color(0xFFE3A600), center = Offset(54f, 238f), radius = 36f)
    drawOval(ag, Offset(36f, 218f), Size(52f, 64f))
    drawArc(Color(0xFF4B5A2A), 180f, 180f, true, Offset(34f, 212f), Size(56f, 28f))
    drawCircle(Color.White, 6f, Offset(72f, 243f)); drawCircle(LINE, 2.8f, Offset(74f, 243f))
    val a = atan2(sim.aimY - WAR_BOW.y, sim.aimX - WAR_BOW.x)
    translate(WAR_BOW.x, WAR_BOW.y) {
        rotate(a * 57.29578f, Offset.Zero) {
            drawArc(Color(0xFF7A4A1E), -69f, 138f, false, Offset(-26f, -26f), Size(52f, 52f), style = Stroke(5f))
        }
    }
}

fun DrawScope.drawWar(war: WarState, sim: WarSim, timeMs: Long) {
    // sky + sun
    drawRect(Brush.verticalGradient(listOf(Color(0xFFF6B66B), Color(0xFFF9DFA8))), Offset.Zero, Size(WAR_W, WAR_H))
    drawCircle(Brush.radialGradient(0f to Color(0xCCFFAA5A), 0.45f to Color(0xE6E66E3C), 1f to Color(0x00E66E3C), center = Offset(640f, 120f), radius = 110f), 110f, Offset(640f, 120f))
    val tNow = timeMs / 1000f
    for ((cx0, cy0, s) in listOf(Triple(100f, 80f, 1f), Triple(420f, 60f, .8f), Triple(700f, 170f, .6f))) {
        val x = ((cx0 + tNow * 6f) % (WAR_W + 160f)) - 80f
        oval(x, cy0, 46f * s, 14f * s, Color(0xBFFFF0DC), null)
    }
    // sea + pirate ship
    drawRect(Color(0xFF5FA8C9), Offset(0f, 318f), Size(WAR_W, 60f))
    val shipAlpha = if (war.g.lv("map") > 0 || war.g.bypassLocks) 1f else 0f
    if (shipAlpha > 0f) {
        val psx = WAR_PIRATE_SHIP.x; val psy = WAR_PIRATE_SHIP.y + sin(tNow * 2f) * 2f
        translate(psx, psy) {
            drawPath(Path().apply { moveTo(-22f, 0f); lineTo(22f, 0f); lineTo(16f, 9f); lineTo(-16f, 9f); close() }, Color(0xFF6B4A1E))
            drawLine(Color(0xFF2A1408), Offset(0f, 0f), Offset(0f, -28f), 2f)
            drawPath(Path().apply { moveTo(-12f, -24f); lineTo(12f, -24f); lineTo(12f, -6f); lineTo(-12f, -6f); close() }, Color(0xFFF4EFD0))
        }
    }
    // ground
    drawRect(Brush.verticalGradient(listOf(Color(0xFF8A9A4A), Color(0xFF66763A)), startY = WAR_GROUND, endY = WAR_H), Offset(0f, WAR_GROUND), Size(WAR_W, WAR_H - WAR_GROUND))
    if (war.mode == WarMode.DEATH) {
        for (gx in listOf(190f, 330f, 470f, 610f, 740f)) {
            drawPath(Path().apply { moveTo(gx - 12f, WAR_GROUND); lineTo(gx - 12f, WAR_GROUND - 26f); arcTo(androidx.compose.ui.geometry.Rect(Offset(gx - 12f, WAR_GROUND - 38f), Size(24f, 24f)), 180f, 180f, false); lineTo(gx + 12f, WAR_GROUND); close() }, Color(0xFF6E6A80))
        }
    } else if (war.mode == WarMode.BROKEN) {
        for (k in 0 until 5) {
            val bx = 180f + k * 130f; val by = 120f + sin(tNow + k) * 18f + (k % 2) * 60f
            translate(bx, by) { drawRect(Color(0xFF7A3A2A), Offset(-18f, -8f), Size(36f, 16f)) }
        }
    }
    // wall
    drawRect(Brush.horizontalGradient(listOf(Color(0xFF8A8478), Color(0xFFABA597), Color(0xFF8F897D))), Offset(0f, 280f), Size(WAR_WALL_X, WAR_H - 280f))
    drawRect(LINE, Offset(0f, 280f), Size(WAR_WALL_X, WAR_H - 280f), style = Stroke(3f))
    for (c in 0 until 3) drawRect(Color(0xFF9B958A), Offset(c * 40f, 262f), Size(26f, 20f))
    val dmg = 1 - war.wallHP / war.wallMax()
    if (dmg > 0.3f) drawPath(Path().apply { moveTo(90f, 300f); lineTo(70f, 340f); lineTo(85f, 370f); lineTo(60f, 410f) }, Color(0xFF3A362F), style = Stroke(3f))
    if (dmg > 0.65f) drawPath(Path().apply { moveTo(30f, 320f); lineTo(50f, 360f); lineTo(35f, 400f) }, Color(0xFF3A362F), style = Stroke(3f))
    for (i in 0 until war.wl("towers")) drawTowerAt(sim.towerX[i])
    if (war.wl("spikes") > 0) {
        var y = WAR_GROUND - 70f
        while (y < WAR_GROUND) { drawPath(Path().apply { moveTo(WAR_WALL_X, y); lineTo(WAR_WALL_X + 14f + war.wl("spikes") * 2f, y + 5f); lineTo(WAR_WALL_X, y + 10f) }, Color(0xFF7A5A3A)); y += 14f }
    }
    if (war.wl("catapult") > 0) {
        translate(22f, 262f) {
            drawRect(Color(0xFF8B5A2B), Offset(-14f, -4f), Size(30f, 8f))
            rotate(if (sim.rocks.any { it.t < 0.2f }) -63f else -34f, Offset.Zero) { drawRect(Color(0xFF8B5A2B), Offset(-2f, -34f), Size(6f, 34f)) }
        }
    }
    for (i in 0 until war.g.wallArchers) { val (ax, ay) = sim.archerPos[i]; drawWallArcherAt(ax, ay - 6f) }
    drawArcher(war, sim)
    for (gl in sim.globs) {
        val k = gl.t / gl.dur; val x = gl.x0 + (gl.tx - gl.x0) * k; val y = gl.y0 + (WAR_GROUND - 10f - gl.y0) * k - sin(k * PI.toFloat()) * 110f
        drawOval(Color(0xFF8BD45A), Offset(x - 9f, y - 7f), Size(18f, 14f))
    }
    for (r in sim.rocks) {
        if (r.done) { drawOval(Color(0xFF966E46).copy(alpha = max(0f, r.boom / 0.35f)), Offset(r.tx - 70f * (1 - r.boom / 0.35f) - 20f, WAR_GROUND - 24f), Size((70f * (1 - r.boom / 0.35f) + 20f) * 2, 36f)); continue }
        val k = r.t / r.dur; val x = r.x0 + (r.tx - r.x0) * k; val y = r.y0 + (WAR_GROUND - 14f - r.y0) * k - sin(k * PI.toFloat()) * 170f
        drawCircle(Brush.radialGradient(listOf(Color(0xFFC9C2B5), Color(0xFF6E675C))), 13f, Offset(x, y))
    }
    // underground
    if (war.wl("miners") > 0) {
        drawRect(Color(0xFF5E3C20), Offset(WAR_WALL_X, WAR_GROUND + 12f), Size(WAR_W - WAR_WALL_X, WAR_H - WAR_GROUND - 12f))
        for (e in sim.enemies) if (e.under) drawEnemyBody(e)
        for (m in sim.miners) drawMiner(m)
    }
    for (s in sim.soldiers) drawSoldier(s, war.g.name)
    for (e in sim.enemies) {
        if (e.under) continue
        val sw = if (e.kind == "worm" || e.kind == "wormking") e.r * 3.4f else e.r * 1.3f
        drawOval(Color.Black.copy(alpha = if (e.fly) 0.12f else 0.22f), Offset(e.x + (if (e.kind == "worm") e.r * 1.7f else 0f) - sw, WAR_GROUND - 1f), Size(sw * 2, 10f))
    }
    for (e in sim.enemies) {
        if (e.under) continue
        val alpha = if (war.mode == WarMode.DEATH) (if (e.intangible) 0.22f else if (e.revived) 0.55f else 0.85f) else 1f
        drawEnemyBody(e)
        if (e.max > 1) healthBar(e.x, e.y - e.r - 16f, if (e.r > 25f) 80f else 40f, e.hp, e.max)
        if (alpha < 1f) { /* approximate fade handled visually by health/overlay already */ }
        if (e.slow > 0f) drawCircle(Color(0x598CD2FF), e.r + 6f, Offset(e.x, e.y), style = Stroke(3f))
        if (e.burn > 0f) drawCircle(Color(0x80FF8A1F), e.r * 0.6f, Offset(e.x, e.y - e.r * 0.6f))
    }
    for (a in sim.arrows) drawArrow(a)
    for (f in sim.floaters) warLabel(f.text, f.x, f.y, 22f, 0xFFFFD21F.toInt())
    for (c in sim.warConfetti) { translate(c.x, c.y) { rotate(c.r * 57.3f, Offset.Zero) { drawRect(c.c, Offset(-4f, -2f), Size(8f, 5f)) } } }
    if (war.mode == WarMode.DEATH) {
        drawRect(Color(0x61461E8C), Offset.Zero, Size(WAR_W, WAR_H))
        warLabel("DEATH WAR: double bonds · ghosts fade out · some come back!", WAR_W - 12f, WAR_H - 14f, 18f, 0xFFE6D9FF.toInt())
    } else if (war.mode == WarMode.BROKEN) {
        drawRect(Color(0x59960014), Offset.Zero, Size(WAR_W, WAR_H))
        warLabel("BROKEN WAR: triple bonds · glitches · clones · wild arrows", WAR_W - 12f, WAR_H - 14f, 18f, 0xFFFFB3B3.toInt())
    }
    drawCircle(Color(0x8015180A), 10f, Offset(sim.aimX, sim.aimY), style = Stroke(2f))
    drawLine(Color(0x8015180A), Offset(sim.aimX - 15f, sim.aimY), Offset(sim.aimX + 15f, sim.aimY), 2f)
    drawLine(Color(0x8015180A), Offset(sim.aimX, sim.aimY - 15f), Offset(sim.aimX, sim.aimY + 15f), 2f)
}
