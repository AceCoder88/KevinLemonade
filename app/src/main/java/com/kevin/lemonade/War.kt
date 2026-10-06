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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong
import kotlin.random.Random
import org.json.JSONObject

/** Normal war, or the Death Realm / Nightmare Realm variants (the original's warTheme). */
enum class WarMode { NORMAL, DEATH, BROKEN }

/** one row of General Sourpuss's War Shop (the original's warShop / wl). */
data class WarUpgrade(val id: String, val name: String, val desc: String, val base: Double, val grow: Double, val max: Int) {
    fun cost(level: Int): Long = (base * dpow(grow, level)).roundToLong()
}
private fun dpow(base: Double, n: Int): Double = Math.pow(base, n.toDouble())

/** same ids, names, prices and caps as the original's warShop array. */
val warShop = listOf(
    WarUpgrade("dmg", "Sharper arrows", "+25% arrow damage", 15.0, 2.0, 8),
    WarUpgrade("rate", "Quick draw", "Shoot faster", 8.0, 1.7, 8),
    WarUpgrade("multi", "Multi-shot", "+2 arrows per shot (up to 99 arrows!)", 40.0, 1.3, 49),
    WarUpgrade("hunter", "Bond hunter", "+50% war bonds per kill", 15.0, 2.0, 8),
    WarUpgrade("value", "Bond value", "Each bond trades for +\$1 more", 20.0, 1.9, 20),
    WarUpgrade("wall", "Stronger wall", "+10 wall health and repair it", 12.0, 1.7, 10),
    WarUpgrade("troops", "Frontline soldier", "", 10.0, 3.0, 6),
    WarUpgrade("armor", "Soldier armor", "Soldiers take +2 more hits", 15.0, 1.9, 10),
    WarUpgrade("sword", "Soldier swords", "Soldiers hit 25% harder", 20.0, 2.0, 8),
    WarUpgrade("towers", "Archer tower", "A wooden tower with its own archer that shoots by itself", 30.0, 2.5, 4),
    WarUpgrade("tspeed", "Tower training", "Tower archers shoot faster", 25.0, 1.9, 6),
    WarUpgrade("tmulti", "Tower multi-shot", "Tower archers shoot +1 arrow at a time", 45.0, 1.6, 20),
    WarUpgrade("fire", "Fire arrows", "Arrows set enemies on fire so they keep getting hurt", 40.0, 2.0, 5),
    WarUpgrade("ice", "Ice arrows", "Arrows can freeze enemies and slow them way down", 35.0, 2.0, 5),
    WarUpgrade("pierce", "Piercing arrows", "Arrows fly through 1 more enemy", 50.0, 2.5, 3),
    WarUpgrade("crit", "Critical hits", "Sometimes an arrow does triple damage", 30.0, 1.9, 6),
    WarUpgrade("catapult", "Catapult", "Throws a giant rock that smashes groups of enemies", 80.0, 2.2, 5),
    WarUpgrade("spikes", "Spiky wall", "Spikes hurt enemies that chew on your wall", 25.0, 1.8, 5),
    WarUpgrade("regen", "Wall builders", "Your wall slowly repairs itself", 30.0, 1.9, 5),
    WarUpgrade("barracks", "Barracks", "Fallen soldiers come back during the wave", 60.0, 2.0, 4),
)

/** the traveling shop's single-buy super deals (the original's wSpecials). */
private class SpecialTemplate(val tag: String, val name: String, val desc: String, val price: Long, val payMoney: Boolean = false,
                               val ok: ((WarState) -> Boolean)? = null, val effect: (WarState) -> Unit)
private const val WAR_MIN_MS = 60_000L

private fun specialTemplates(): List<SpecialTemplate> = listOf(
    SpecialTemplate("SUPER", "Extra archer", "Another archer joins you on the wall and shoots 3 arrows at a time. Forever!", 120,
        ok = { it.g.wallArchers < 6 }) { it.g.wallArchers++; it.sim.banner("An archer joins the wall!", 1.4f) },
    SpecialTemplate("SUPER", "Archer squad", "TWO more archers join you on the wall at once!", 220,
        ok = { it.g.wallArchers < 5 }) { it.g.wallArchers = min(6, it.g.wallArchers + 2); it.sim.banner("An archer squad joins!", 1.4f) },
    SpecialTemplate("SUPER", "Arrow storm", "Your arrows do 5 TIMES damage for 2 minutes", 150) { it.g.stormUntil = it.g.now() + 2 * WAR_MIN_MS },
    SpecialTemplate("SUPER", "Giant wall", "+200 wall health, forever!", 250) { it.g.wallBonus += 200; it.wallHP = it.wallMax() },
    SpecialTemplate("SUPER", "Golden arrows", "Every kill gives 3 TIMES the bonds for 2 minutes", 120) { it.g.goldBondsUntil = it.g.now() + 2 * WAR_MIN_MS },
    SpecialTemplate("SUPER", "Super towers", "Your archer towers shoot 3 TIMES faster for 2 minutes", 100,
        ok = { it.wl("towers") > 0 }) { it.g.superTowersUntil = it.g.now() + 2 * WAR_MIN_MS },
    SpecialTemplate("SUPER", "Freeze bomb", "Freezes EVERY enemy on the field", 60) {
        for (e in it.sim.enemies) e.slow = 8f; it.sim.banner("FREEZE!", 1.2f)
    },
    SpecialTemplate("SUPER", "Battle bomb", "Instantly defeats EVERY enemy on the field, bosses too, and you get their bonds", 180) {
        for (e in it.sim.enemies.toList()) if (e.hp > 0) { e.hp = 0.0; it.sim.kill(e) }
        it.sim.globs.clear(); it.sim.banner("KABOOM!", 1.4f)
    },
    SpecialTemplate("SUPER", "Bond printer", "Get 10 war bonds EVERY SECOND for 2 minutes", 1000, payMoney = true) {
        it.g.bondPrinterUntil = it.g.now() + 2 * WAR_MIN_MS
    },
)
private val MEGA_SKIP = setOf("tspeed", "tmulti", "barracks")

/** a generated traveling-shop offer: either a scripted special, or a mega-deal bundle of upgrade levels. */
class WarDeal(val tag: String, val name: String, val desc: String, val price: Long, val payMoney: Boolean,
              val up: WarUpgrade? = null, val n: Int = 0, val effect: ((WarState) -> Unit)? = null) {
    var sold by mutableStateOf(false)
}

private fun specialDeals(war: WarState): List<WarDeal> =
    specialTemplates().filter { it.ok?.invoke(war) ?: true }
        .map { WarDeal(it.tag, it.name, it.desc, it.price, it.payMoney, effect = it.effect) }

private fun megaDeals(war: WarState): List<WarDeal> {
    val out = ArrayList<WarDeal>()
    for (u in warShop) {
        val cur = war.wl(u.id)
        if (u.id in MEGA_SKIP && !((u.id == "tspeed" || u.id == "tmulti") && war.wl("towers") > 0) && !(u.id == "barracks" && war.wl("troops") > 0)) continue
        if (cur >= u.max) continue
        val n = min(5, u.max - cur)
        var total = 0L
        for (k in 0 until n) total += u.cost(cur + k)
        out.add(WarDeal("MEGA +$n", u.name, "$n levels at once!", max(1L, (total * 0.5).roundToLong()), false, up = u, n = n))
    }
    return out
}

/** General Sourpuss: the war's own shopkeeper (the original's generalLines). */
object GeneralLines {
    val hello = listOf(
        "ATTENTION, soldier! The bugs are coming. Buy what you need and hold that wall!",
        "At ease, soldier. ...Actually, NO. Get back to shooting!",
        "Back for more, private? Good. The rats won't defeat themselves!",
    )
    val buy = listOf(
        "OUTSTANDING purchase, soldier!", "Now THAT'S what I call firepower!", "Good choice. The worms are trembling already.",
        "Supplies approved! Move out!", "HOO-RAH! That'll show those beetles!", "I'd salute you, but I'm holding my mustache.",
    )
    val poke = listOf(
        "Don't poke a general, soldier! That's ten push-ups!", "These medals? Won them fighting the Giant Snail. Took three weeks.",
        "Remember, soldier: aim for the worms BEFORE they split!", "I fear nothing. Except the Mole King. And maybe spiders.",
    )
    val shopHello = listOf(
        "Welcome to MY shop, soldier! Everything here is battle-tested.",
        "You want firepower? I've got firepower. Pay up, private!",
        "Step into my tent, soldier. Best gear on the whole battlefield.",
    )
}

/** War: the wall, the bugs, soldiers, bosses, war bonds, the General's shop. */
class WarState(val g: GameViewModel) : Feature {
    override val key = "war"

    /** war bonds, the war's money; other shops take them too */
    var bonds by mutableLongStateOf(0L)

    /** warShop upgrade levels (also read/written by the digging hole for "miners" and "pick") */
    val wl = mutableStateMapOf<String, Int>()
    /** miners and golden pickaxes are bought in the digging hole's shop, so their levels live in DigState */
    fun wl(id: String) = if (id == "miners" || id == "pick") g.dig.lv(id) else wl[id] ?: 0

    var wave by mutableIntStateOf(1)
    /** highest wave ever reached; trophies should read this */
    var maxWave by mutableIntStateOf(1)
    var wallHP by mutableFloatStateOf(20f)
    var paused by mutableStateOf(false)

    /** Death War / Broken War. Set by whoever hosts the realm buttons, then navigate to Screen.WAR. */
    var mode by mutableStateOf(WarMode.NORMAL)

    // lifetime stats, for Trophies
    var totalBondsEarned by mutableLongStateOf(0L)
    var bossesKilled by mutableIntStateOf(0)

    // the General's traveling shop
    val wDeals = mutableStateListOf<WarDeal>()
    var wTravelLeft by mutableIntStateOf(90)
    var atGShop by mutableStateOf(false)
    private var wasPausedBeforeShop = false
    var generalBubble by mutableStateOf(GeneralLines.hello.first())

    val sim by lazy { WarSim(this) }

    fun wallMax(): Float = (20 + 10 * wl("wall") + g.wallBonus).toFloat()
    fun fireGap() = (0.55 * Math.pow(0.82, wl("rate").toDouble())).toFloat()
    fun arrowDmg(): Double = (1 + 0.25 * wl("dmg")) * (if (g.now() < g.stormUntil) 5.0 else 1.0) *
        (if (g.closet.wearing("cape")) 1.25 else 1.0) * (if (g.closet.wearingSet("hero")) 2.0 else 1.0) * g.space.saberMult()
    fun bondRate(): Long = 2L + wl("value")
    fun bondMult(): Double = (1 + 0.5 * wl("hunter")) * (if (g.now() < g.goldBondsUntil) 3.0 else 1.0)

    fun troopDesc() = if (wl("troops") == 0) "A recruit with a sword. Not super strong, but great for early waves."
                       else "A knight with a sword. Comes back every wave."
    fun lockedReason(u: WarUpgrade): String? = when {
        (u.id == "tspeed" || u.id == "tmulti") && wl("towers") == 0 -> "Build an archer tower first"
        u.id == "barracks" && wl("troops") == 0 -> "Hire a soldier first"
        else -> null
    }

    fun buy(u: WarUpgrade) {
        val c = u.cost(wl(u.id))
        if (bonds < c || wl(u.id) >= u.max || lockedReason(u) != null) return
        bonds -= c
        wl[u.id] = wl(u.id) + 1
        when (u.id) {
            "wall" -> wallHP = wallMax()
            "towers" -> sim.banner("Archer tower built!", 1.4f)
            "troops" -> { sim.soldiers.add(sim.makeSoldierAt(wl("troops") - 1)); sim.banner(if (wl("troops") == 1) "A recruit joins!" else "A knight joins!", 1.4f) }
        }
        if (u.id == "armor" || u.id == "sword") for (s in sim.soldiers) {
            val fresh = sim.makeSoldierAt(s.i)
            s.dmg = fresh.dmg
            if (u.id == "armor") { s.max = fresh.max; s.hp += 2 }
        }
    }

    /** returns true if the purchase went through (so the UI can show a shopkeeper line). */
    fun buyAndTell(u: WarUpgrade): Boolean { val before = bonds; buy(u); return bonds != before }

    fun repairWall() { if (bonds < 5 || wallHP >= wallMax()) return; bonds -= 5; wallHP = min(wallMax(), wallHP + 10f) }
    fun trade(nIn: Long) { val n = min(nIn, bonds); if (n < 1) return; bonds -= n; g.money += n * bondRate() }

    data class WaveResult(val ok: Boolean, val msg: String)
    fun chooseWave(raw: String): WaveResult {
        val trimmed = raw.trim()
        val n = trimmed.toIntOrNull()
        if (trimmed.isEmpty() || n == null) return WaveResult(false, "That is not a wave number. Try something like 7.")
        if (n < 1) return WaveResult(false, "Waves start at 1!")
        if (n > 999) return WaveResult(false, "Whoa, too big! Pick 999 or less.")
        wave = n
        sim.enemies.clear(); sim.arrows.clear(); sim.spawnQ.clear(); sim.rocks.clear(); sim.between = 0f
        wallHP = wallMax()
        sim.startWave()
        val boss = if (wave % 5 == 0) " Boss wave: the ${sim.bossName()}!" else ""
        val tough = if (wave > maxWave + 10) " That one is going to be TOUGH." else if (wave <= 3) " Nice and easy." else ""
        return WaveResult(true, "Good to go! Wave $wave.$boss$tough")
    }

    fun setWarPaused(p: Boolean) {
        paused = p; sim.firing = false
        if (p) sim.banner("Paused", 1000f) else sim.bannerOn = false
    }

    fun leaveWar() { mode = WarMode.NORMAL; atGShop = false; g.screen = Screen.STAND }

    /** called by the Death Realm / Nightmare Realm screens instead of setting g.screen directly. */
    fun enterNormalWar() { mode = WarMode.NORMAL; g.screen = Screen.WAR }
    fun enterDeathWar() { mode = WarMode.DEATH; g.screen = Screen.WAR }
    fun enterBrokenWar() { mode = WarMode.BROKEN; g.screen = Screen.WAR }

    fun openGShop() {
        atGShop = true; wasPausedBeforeShop = paused
        if (!paused) setWarPaused(true)
        generalBubble = GeneralLines.shopHello.pick()
        if (wDeals.isEmpty()) newWarDeals()
    }
    fun closeGShop() { atGShop = false; if (!wasPausedBeforeShop) setWarPaused(false) }
    fun pokeGeneral() { generalBubble = GeneralLines.poke.pick() }
    fun generalSay(t: String) { generalBubble = t }

    fun newWarDeals() {
        val sp = specialDeals(this).shuffled().toMutableList()
        val mg = megaDeals(this).shuffled().toMutableList()
        val pool = ArrayList<WarDeal>()
        while (pool.size < 4 && (sp.isNotEmpty() || mg.isNotEmpty())) {
            val takeSpecial = (Random.nextDouble() < 0.6 && sp.isNotEmpty()) || mg.isEmpty()
            if (takeSpecial && sp.isNotEmpty()) pool.add(sp.removeAt(sp.size - 1)) else if (mg.isNotEmpty()) pool.add(mg.removeAt(mg.size - 1))
        }
        wDeals.clear(); wDeals.addAll(pool)
        wTravelLeft = 90
    }
    fun buyWarDeal(d: WarDeal) {
        if (d.sold) return
        val afford = if (d.payMoney) g.money >= d.price else bonds >= d.price
        if (!afford) return
        if (d.payMoney) g.money -= d.price else bonds -= d.price
        d.sold = true
        val effect = d.effect
        val up = d.up
        if (effect != null) effect(this)
        else if (up != null) {
            var k = 0
            while (k < d.n && wl(up.id) < up.max) {
                wl[up.id] = wl(up.id) + 1
                if (up.id == "wall") wallHP = wallMax()
                if (up.id == "troops") sim.soldiers.add(sim.makeSoldierAt(wl("troops") - 1))
                k++
            }
        }
    }
    fun buyDealAndTell(d: WarDeal): Boolean {
        val beforeB = bonds; val beforeM = g.money
        buyWarDeal(d)
        return bonds != beforeB || g.money != beforeM
    }
    fun rerollWarDeals() { if (bonds < 10) return; bonds -= 10; newWarDeals() }

    // ---------------- Feature ----------------
    override fun save(j: JSONObject) {
        j.put("bonds", bonds)
        j.put("wl", JSONObject(wl.toMap()))
        j.put("wave", wave); j.put("maxWave", maxWave); j.put("wallHP", wallHP.toDouble())
        j.put("totalBondsEarned", totalBondsEarned); j.put("bossesKilled", bossesKilled)
    }
    override fun load(j: JSONObject) {
        bonds = j.optLong("bonds", 0)
        j.optJSONObject("wl")?.let { o -> o.keys().forEach { k -> wl[k] = o.getInt(k) } }
        wave = j.optInt("wave", 1); maxWave = j.optInt("maxWave", 1)
        wallHP = j.optDouble("wallHP", wallMax().toDouble()).toFloat()
        totalBondsEarned = j.optLong("totalBondsEarned", 0); bossesKilled = j.optInt("bossesKilled", 0)
    }
    override fun reset() {
        bonds = 0; wl.clear(); wave = 1; maxWave = 1; wallHP = 20f; mode = WarMode.NORMAL
        totalBondsEarned = 0; bossesKilled = 0; atGShop = false; paused = false
        wDeals.clear(); wTravelLeft = 90
        sim.hardReset()
    }
    /** Rebirth keeps everything about the war (same as the original: rebirth only resets money/Mr. Zest's shop). */
    override fun onRebirth() {}
    /** the MAX OUT secret code: max every war upgrade, fill every wall archer window, full wall health. */
    override fun maxOut() {
        for (u in warShop) wl[u.id] = u.max
        g.wallArchers = 6
        wallHP = wallMax()
        sim.deploySoldiers()
    }
    override fun tick() {
        if (g.now() < g.bondPrinterUntil) bonds += 10
        if (g.screen == Screen.WAR && !paused) {
            wTravelLeft--
            if (wTravelLeft <= 0) newWarDeals()
        }
    }
}

fun WarSim.hardReset() {
    enemies.clear(); arrows.clear(); soldiers = ArrayList(); miners.clear(); rocks.clear(); globs.clear()
    floaters.clear(); deadTroops.clear(); spawnQ.clear(); warConfetti.clear()
    toSpawn = 0; between = 1f; catT = 2f; firing = false
}

// =============================== UI ===============================
@Composable
fun WarScreen(g: GameViewModel) {
    val war = g.war
    if (war.atGShop) { GShopScreen(g); return }
    val sim = war.sim
    var frame by remember { mutableLongStateOf(0L) }
    LaunchedEffect(war) {
        var last = 0L
        while (true) withFrameNanos { now ->
            val dt = if (last == 0L) 0f else min(0.05f, (now - last) / 1e9f)
            last = now
            if (!war.paused) sim.update(dt)
            frame = now
        }
    }
    var showInfo by remember { mutableStateOf(false) }
    var waveText by remember { mutableStateOf("") }
    var waveMsg by remember { mutableStateOf<WarState.WaveResult?>(null) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ChunkyButton("Back to lemonade", color = CARD) { war.leaveWar() }
            ChunkyButton(if (war.paused) "Resume" else "Pause", color = if (war.paused) Color(0xFF8FCB5A) else PEEL) { war.setWarPaused(!war.paused) }
            ChunkyButton("War Shop", color = Color(0xFF4B5A2A), textColor = PEEL) { war.openGShop() }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Text("War bonds: ${war.bonds}", fontWeight = FontWeight.Black)
            Text("Wave ${war.wave}", fontWeight = FontWeight.Black)
            MoneyText(g, 18)
        }
        Spacer(Modifier.height(8.dp))
        Text("Spawn a boss:", fontSize = 13.sp)
        WarBossRow(sim)
        Spacer(Modifier.height(8.dp))
        val shape = RoundedCornerShape(20.dp)
        Box(Modifier.fillMaxWidth().background(LINE, shape).padding(bottom = 6.dp).aspectRatio(WAR_W / WAR_H).clip(shape).border(4.dp, LINE, shape)) {
            Canvas(Modifier.fillMaxSize().pointerInput(war) {
                awaitEachGesture {
                    val sc = size.width / WAR_W
                    val down = awaitFirstDown()
                    down.consume()
                    sim.press(down.position / sc)
                    while (true) {
                        val ev = awaitPointerEvent()
                        val c = ev.changes.firstOrNull() ?: break
                        c.consume()
                        sim.move(c.position / sc)
                        if (!c.pressed) break
                    }
                    sim.release()
                }
            }) {
                frame.let { }
                scale(size.width / WAR_W, size.width / WAR_W, Offset.Zero) { drawWar(war, sim, frame / 1_000_000L) }
            }
            if (sim.bannerOn) {
                Box(Modifier.align(Alignment.TopCenter).padding(top = 10.dp).background(LINE, CircleShape).padding(horizontal = 14.dp, vertical = 5.dp)) {
                    Text(sim.banner, color = PEEL, fontWeight = FontWeight.Black, fontSize = 15.sp)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        // wall bar
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Wall", fontWeight = FontWeight.Black, modifier = Modifier.padding(end = 6.dp))
            val pct = max(0f, war.wallHP / war.wallMax())
            Box(Modifier.weight(1f).height(14.dp).clip(CircleShape).background(LINE)) {
                val fill = if (pct > 0.5f) Color(0xFF8FCB5A) else if (pct > 0.25f) Color(0xFFF2C14E) else RED
                Box(Modifier.fillMaxHeight().fillMaxWidth(pct).background(fill, CircleShape))
            }
            Text(" ${max(0, war.wallHP.toInt())}/${war.wallMax().toInt()} ", fontWeight = FontWeight.Black, fontSize = 13.sp)
            ChunkyButton("Repair +10 (5 bonds)", color = CARD, enabled = war.bonds >= 5 && war.wallHP < war.wallMax()) { war.repairWall() }
        }
        Spacer(Modifier.height(8.dp))
        if (g.lv("map") > 0 || g.bypassLocks) {
            Text("...is that a pirate ship sailing by in the distance?", color = Color(0xFF2E6BD6),
                fontWeight = FontWeight.Black, fontSize = 13.sp,
                modifier = Modifier.clickable { g.screen = Screen.PIRATE }.padding(6.dp))
        }
        Text(if (showInfo) "Hide info" else "More info", fontWeight = FontWeight.Black,
            modifier = Modifier.clickable { showInfo = !showInfo }.padding(6.dp))
        if (showInfo) {
            Text(
                "Tap or click to shoot. Hold down to keep shooting.\n\n" +
                    "Press Pause any time to upgrade or catch your breath. Repair the wall for 5 bonds.\n\n" +
                    "Type any wave number in Choose a wave to jump forward to harder enemies or go back to an easier one. " +
                    "Every 5th wave is a boss, and wave 40 is the Mole King!",
                fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 6.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        // wave choosing
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Choose a wave:", fontSize = 14.sp, modifier = Modifier.padding(end = 6.dp))
            BasicTextField(
                value = waveText, onValueChange = { waveText = it.filter { c -> c.isDigit() }.take(3) }, singleLine = true,
                textStyle = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = LINE),
                cursorBrush = SolidColor(LINE),
                modifier = Modifier.width(60.dp).border(3.dp, LINE, CircleShape).background(CARD, CircleShape).padding(horizontal = 10.dp, vertical = 6.dp),
            )
            Spacer(Modifier.width(6.dp))
            ChunkyButton("Go", color = CARD) { waveMsg = war.chooseWave(waveText) }
        }
        waveMsg?.let { Text(it.msg, color = if (it.ok) Color(0xFF3E9B4F) else RED, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp)) }
        Spacer(Modifier.height(10.dp))
        // trade
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("1 war bond = ${fmt(war.bondRate())}", fontSize = 14.sp)
            ChunkyButton("Trade 10 bonds", color = CARD, enabled = war.bonds >= 10) { war.trade(10) }
            ChunkyButton("Trade all bonds", color = CARD, enabled = war.bonds >= 1) { war.trade(war.bonds) }
        }
        Spacer(Modifier.height(10.dp))
        ChunkyButton("Visit General Sourpuss's War Shop", color = Color(0xFF4B5A2A), textColor = PEEL, big = true) { war.openGShop() }
        Spacer(Modifier.height(30.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WarBossRow(sim: WarSim) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        for ((kind, label) in WAR_ALL_BOSSES) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFFFFD9D9),
                modifier = Modifier.clip(CircleShape).background(Color(0xFF5A1A1A)).border(2.dp, LINE, CircleShape)
                    .clickable { sim.spawnBoss(kind) }.padding(horizontal = 10.dp, vertical = 5.dp))
        }
    }
}

@Composable
fun GShopScreen(g: GameViewModel) {
    val war = g.war
    Column(Modifier.fillMaxSize().background(Color(0xFF3E4A22)).verticalScroll(rememberScrollState()).padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ChunkyButton("Back to the battle", color = CARD) { war.closeGShop() }
            Spacer(Modifier.weight(1f))
            Text("War bonds: ${war.bonds}", color = PEEL, fontWeight = FontWeight.Black, fontSize = 14.sp)
            Spacer(Modifier.width(10.dp))
            Text(fmt(g.money), color = PEEL, fontWeight = FontWeight.Black, fontSize = 14.sp)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTitle("General Sourpuss's War Shop", 24, PEEL)
        Text("The battle is paused while you shop.", color = Color(0xFFE8E4CF), fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0xFF4B5A2A))
            .border(3.dp, LINE, RoundedCornerShape(20.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.clickable { war.pokeGeneral() }) { GeneralArt() }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("General Sourpuss", color = PEEL, fontFamily = Lilita, fontSize = 17.sp)
                Box(Modifier.background(Color.White, RoundedCornerShape(14.dp)).border(2.dp, LINE, RoundedCornerShape(14.dp)).padding(10.dp)) {
                    Text(war.generalBubble, color = LINE, fontSize = 13.sp)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("War traveling shop", color = PEEL, fontFamily = Lilita, fontSize = 20.sp)
        Text("New deals in ${war.wTravelLeft / 60}:${(war.wTravelLeft % 60).toString().padStart(2, '0')}.", color = Color(0xFFE8E4CF), fontSize = 12.sp)
        for (d in war.wDeals) WarDealCard(d) { if (war.buyDealAndTell(d)) war.generalSay(GeneralLines.buy.pick()) }
        ChunkyButton("New deals now (10 bonds)", color = CARD, enabled = war.bonds >= 10, modifier = Modifier.padding(top = 8.dp)) { war.rerollWarDeals() }
        Spacer(Modifier.height(16.dp))
        Text("War upgrades", color = PEEL, fontFamily = Lilita, fontSize = 20.sp)
        for (u in warShop) {
            val level = war.wl(u.id)
            val locked = war.lockedReason(u) != null
            UpgradeCard(
                name = u.name,
                desc = if (locked) war.lockedReason(u)!! else if (u.id == "troops") war.troopDesc() else u.desc,
                level = level, max = u.max, cost = u.cost(level), canBuy = war.bonds >= u.cost(level), locked = locked,
                levelText = when (u.id) {
                    "troops" -> "Soldiers: $level of ${u.max}"
                    "towers" -> "Towers: $level of ${u.max}"
                    "multi" -> "Arrows: ${1 + 2 * level} of ${1 + 2 * u.max}"
                    else -> "Level $level of ${u.max}"
                },
            ) { if (war.buyAndTell(u)) war.generalSay(GeneralLines.buy.pick()) }
        }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun WarDealCard(d: WarDeal, onBuy: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(shape)
            .background(if (d.sold) Color(0xFFDFC9A0) else Color(0xFFF4EFD0))
            .border(3.dp, Color(0xFF6A3A0E), shape).clickable(enabled = !d.sold, onClick = onBuy).padding(10.dp),
    ) {
        Text(if (d.sold) "SOLD OUT" else d.tag, color = Color(0xFFB8560E), fontWeight = FontWeight.Black, fontSize = 11.sp)
        Row {
            Text(d.name, fontWeight = FontWeight.Black, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text(if (d.payMoney) fmt(d.price) else "${d.price} bonds", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF2E6BD6))
        }
        Text(d.desc, fontSize = 13.sp)
    }
}

/** General Sourpuss's portrait, same SVG art as the original's #generalBtn. */
@Composable
private fun GeneralArt() {
    Canvas(Modifier.size(90.dp, 82.dp)) {
        scale(size.width / 220f, size.width / 220f, Offset.Zero) {
            oval(110f, 192f, 90f, 7f, Color.Black, null, alpha = 0.2f)
            oval(110f, 108f, 58f, 66f, Color(0xFFFFD21F), LINE, 4f)
            shape("M60 124 Q110 150 160 124 L156 172 L64 172 Z", Color(0xFF4B5A2A), LINE, 3f)
            box(52f, 122f, 22f, 9f, SolidColor(Color(0xFFE9B630)), LINE, 2f, 3f)
            box(146f, 122f, 22f, 9f, SolidColor(Color(0xFFE9B630)), LINE, 2f, 3f)
            oval(92f, 146f, 6f, 6f, Color(0xFFE9B630), LINE, 2f); drawRect(RED, Offset(89f, 134f), Size(6f, 7f))
            oval(108f, 148f, 6f, 6f, Color(0xFFC0C8D0), LINE, 2f); drawRect(Color(0xFF2E6BD6), Offset(105f, 136f), Size(6f, 7f))
            stroke("M84 84 L106 88 M114 88 L136 84", LINE, 5f)
            oval(96f, 96f, 8f, 8f, Color.White, LINE, 3f); drawCircle(LINE, 3.5f, Offset(98f, 97f))
            oval(124f, 96f, 8f, 8f, Color.White, LINE, 3f); drawCircle(LINE, 3.5f, Offset(126f, 97f))
            shape("M76 114 Q94 100 110 112 Q126 100 144 114 Q130 126 110 116 Q90 126 76 114 Z", Color(0xFF8A8A8A), LINE, 2f)
            stroke("M100 122 H120", LINE, 3f)
            shape("M58 58 Q110 18 162 58 Q160 70 110 66 Q60 70 58 58 Z", Color(0xFF4B5A2A), LINE, 3f)
            shape("M66 64 Q110 84 154 64 L150 74 Q110 88 70 74 Z", Color(0xFF2A3214), LINE, 3f)
            drawRect(Color(0xFFE9B630), Offset(70f, 58f), Size(80f, 6f))
            shape("M110 34 L114 44 L125 44 L116 50 L120 61 L110 54 L100 61 L104 50 L95 44 L106 44 Z", Color(0xFFFFD21F), LINE, 2f)
        }
    }
}
