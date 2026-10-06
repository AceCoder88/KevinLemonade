package com.kevin.lemonade

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import kotlin.math.ceil
import kotlin.math.pow
import kotlin.math.roundToLong

const val TRAVEL_EVERY = 90

/** one offer in Wanda's traveling shop, or the General's war shop (the original's deals/wDeals entries). */
class TravelDeal(
    val tag: String,
    val name: String,
    val desc: String,
    val payBonds: Boolean,
    val price: Long,
    val upId: String? = null,
    val lemonId: String? = null,
    val levels: Int = 0,
    val apply: (() -> Unit)? = null,
) {
    var sold by mutableStateOf(false)
}

private val wandaHello = listOf(
    "Psst! Super deals from faraway lands. Today only! ...Well, for 90 seconds.", "Back again? Wanda always has something special.",
    "I've traveled across seven deserts and one very big puddle to bring you these deals!",
)
private val wandaBuyLines = listOf("A wise choice, traveler!", "Ooh, that one came all the way from the Lemon Mountains!", "Sold! My camel thanks you.", "Spend it well, friend!")
private val wandaPokeLines = listOf("My backpack? Full of secrets. And snacks.", "I've seen a lemon as big as a house. True story.", "Hire some helpers and they'll shop while you squeeze!")

/** Wanda's traveling shop (90-second rotating super deals) and the traveling helpers who shop for you. */
class TravelState(val g: GameViewModel) : Feature {
    override val key = "travel"

    var travelLvl by mutableIntStateOf(0)
    var travelLeft by mutableIntStateOf(TRAVEL_EVERY)
    val deals = mutableStateListOf<TravelDeal>()
    var shopHelpers by mutableIntStateOf(0)
    var shopperLog by mutableStateOf("")
    var wandaSays by mutableStateOf(wandaHello.pick())
    private var shopperT = 0
    private var shopperTurn = 0

    fun tSlots() = 3 + travelLvl
    fun biggerCost(): Long = (200.0 * 2.2.pow(travelLvl)).roundToLong()
    fun shopperCost(): Long = (300.0 * 2.5.pow(shopHelpers)).roundToLong()

    private fun startRainbow(ms: Long) { g.rainbowUntil = maxOf(g.now(), g.rainbowUntil) + ms }

    private fun specials(): List<TravelDeal> = buildList {
        add(TravelDeal("SUPER", "MEGA FRENZY", "Every pitcher sells for 10 TIMES as much for 90 seconds!", false, 900) { g.megaUntil = g.now() + 90_000 })
        add(TravelDeal("SUPER", "Golden storm", "Your next 25 squeezes are ALL golden lemons", false, 1500) { g.goldenNext += 25 })
        add(TravelDeal("SUPER", "Endless lemons", "Squeezing uses NO lemons for 2 minutes", false, 400) { g.freeLemonsUntil = g.now() + 120_000 })
        if (g.lv("squeezers") > 0) add(TravelDeal("SUPER", "Robot rush", "Your extra squeezers go 5 TIMES faster for 2 minutes", false, 700) { g.robotRushUntil = g.now() + 120_000 })
        add(TravelDeal("SUPER", "Forever happy {N}", "{N} can't get sad at all for 5 minutes", false, 350) { g.happyUntil = g.now() + 300_000; g.sad = 0.0 })
        add(TravelDeal("SUPER", "Money tree", "Get \$50 EVERY SECOND for 2 minutes (\$6,000 total!)", false, 1500) { g.moneyTreeUntil = g.now() + 120_000 })
        add(TravelDeal("SUPER", "Rainbow in a bottle", "A rainbow over the stand for 5 whole minutes: DOUBLE money!", false, 450) { startRainbow(300_000) })
        if (g.llv("cap") < 9) add(TravelDeal("SUPER", "Lemon mountain", "Fill your lemons all the way up, AND +2 levels of lemon storage", false, 600) {
            g.lemonLvl["cap"] = minOf(9, g.llv("cap") + 2); g.lemonsLeft = g.lemonCap()
        })
    }

    /** "MEGA" deals: 5 levels of a shop/catch-the-lemons upgrade at once, half price */
    private fun megaDeals(): List<TravelDeal> = buildList {
        for (u in Upgrades.all) {
            if (u.id == "therapy" || u.cat == Cat.SPOOKY) continue
            val cur = g.lv(u.id); if (cur >= u.max) continue
            val n = minOf(5, u.max - cur)
            var total = 0L; for (k in 0 until n) total += u.cost(cur + k)
            add(TravelDeal("MEGA +$n", u.name, "$n levels at once! ${u.desc}", false, maxOf(1L, (total * 0.5).roundToLong()), upId = u.id, levels = n))
        }
        for (u in LemonUpgrades.all) {
            if (u.id == "hspeed") continue
            val cur = g.llv(u.id); if (cur >= u.max) continue
            val n = minOf(5, u.max - cur)
            var total = 0L; for (k in 0 until n) total += u.cost(cur + k)
            add(TravelDeal("MEGA +$n", u.name, "$n levels at once! ${u.desc}", false, maxOf(1L, (total * 0.5).roundToLong()), lemonId = u.id, levels = n))
        }
    }

    fun newDeals() {
        val sp = specials().shuffled().toMutableList()
        val mg = megaDeals().shuffled().toMutableList()
        val pool = ArrayList<TravelDeal>()
        while (pool.size < tSlots() && (sp.isNotEmpty() || mg.isNotEmpty())) {
            val takeSpecial = (Math.random() < 0.55 && sp.isNotEmpty()) || mg.isEmpty()
            pool.add(if (takeSpecial && sp.isNotEmpty()) sp.removeAt(sp.size - 1) else mg.removeAt(mg.size - 1))
        }
        deals.clear(); deals.addAll(pool)
        travelLeft = TRAVEL_EVERY
    }

    fun buy(i: Int) {
        val d = deals.getOrNull(i) ?: return
        if (d.sold) return
        if (d.payBonds) { if (g.war.bonds < d.price) return; g.war.bonds -= d.price }
        else { if (g.money < d.price) return; g.money -= d.price }
        d.sold = true
        val apply = d.apply
        when {
            apply != null -> apply.invoke()
            d.upId != null -> repeat(d.levels) { if (g.lv(d.upId) < Upgrades.all.find { it.id == d.upId }!!.max) g.lvl[d.upId] = g.lv(d.upId) + 1 }
            d.lemonId != null -> repeat(d.levels) { if (g.llv(d.lemonId) < LemonUpgrades.all.find { it.id == d.lemonId }!!.max) g.lemonLvl[d.lemonId] = g.llv(d.lemonId) + 1 }
        }
        wandaSays = wandaBuyLines.pick()
        g.save()
    }

    fun reroll() {
        if (g.money < 25) return
        g.money -= 25
        newDeals()
    }

    fun bigger() {
        if (travelLvl >= 7 || g.money < biggerCost()) return
        g.money -= biggerCost()
        travelLvl++
        newDeals()
    }

    fun hireHelper() {
        if (shopHelpers >= 6 || g.money < shopperCost()) return
        g.money -= shopperCost()
        shopHelpers++
        wandaSays = "${SHOPPER_NAMES[shopHelpers - 1]} is hired! Watch out, they LOVE a good deal."
    }

    override fun tick() {
        if (g.now() < g.moneyTreeUntil) g.money += 50
        travelLeft--
        if (travelLeft <= 0) newDeals()
        if (shopHelpers > 0) {
            shopperT++
            if (shopperT >= ceil(12.0 / shopHelpers).toInt()) {
                shopperT = 0
                if (shopperTurn >= shopHelpers) shopperTurn = 0
                shopperBuy()
            }
        }
    }

    /** a helper buys the cheapest deal you can easily afford (they keep half your money/bonds saved).
     * Only shops Wanda's own deals for now - see the final report for wiring in the General's war shop too. */
    private fun shopperBuy() {
        if (shopHelpers == 0) return
        val who = SHOPPER_NAMES[shopperTurn]
        shopperTurn = (shopperTurn + 1) % shopHelpers
        var best = -1
        for ((i, d) in deals.withIndex()) {
            if (d.sold) continue
            val afford = if (d.payBonds) g.war.bonds >= d.price * 2 else g.money >= d.price * 2
            if (afford && (best == -1 || d.price < deals[best].price)) best = i
        }
        if (best == -1) { shopperLog = "$who is looking around... nothing they can afford right now."; return }
        val name = deals[best].name
        buy(best)
        shopperLog = "$who bought $name!"
        wandaSays = "$who just bought $name! What a shopper!"
    }

    override fun save(j: JSONObject) { j.put("travelLvl", travelLvl); j.put("shopHelpers", shopHelpers) }
    override fun load(j: JSONObject) { travelLvl = j.optInt("travelLvl", 0); shopHelpers = j.optInt("shopHelpers", 0); newDeals() }
    override fun reset() { travelLvl = 0; shopHelpers = 0; deals.clear(); newDeals() }
    /** traveling-shop progress (travelLvl, helpers) is kept through rebirth in the original. */
    override fun onRebirth() {}
    override fun maxOut() { travelLvl = 7; shopHelpers = 6; newDeals() }

    companion object {
        val SHOPPER_NAMES = listOf("Tilly", "Benny", "Rosa", "Max", "Zara", "Ollie")
    }
}

// ======================= Traveling shop screen =======================
@Composable
fun TravelScreen(g: GameViewModel) {
    val t = g.travel
    if (t.deals.isEmpty() && t.travelLeft == TRAVEL_EVERY) t.newDeals()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ChunkyButton("Back to lemonade", color = CARD) { g.screen = Screen.STAND }
            Spacer(Modifier.weight(1f))
            MoneyText(g, 20)
            Spacer(Modifier.width(10.dp))
            Text("Bonds: ${g.war.bonds}", fontWeight = FontWeight.Black, fontSize = 15.sp)
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFFCDEFF2)).border(3.dp, LINE, RoundedCornerShape(18.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFFFD21F)).border(3.dp, LINE, CircleShape).clickable { t.wandaSays = wandaPokeLines.pick() })
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Wanda the Wanderer", fontWeight = FontWeight.Black, fontSize = 15.sp)
                Spacer(Modifier.height(4.dp))
                Box(Modifier.background(Color.White, RoundedCornerShape(14.dp)).border(3.dp, LINE, RoundedCornerShape(14.dp)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(t.wandaSays, fontSize = 13.sp, lineHeight = 16.sp)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTitle("Traveling shop", 26)
        val m = t.travelLeft / 60; val s = t.travelLeft % 60
        Text("${t.tSlots()} super deals at a time. New deals in $m:${s.toString().padStart(2, '0')}.",
            fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        if (t.deals.isEmpty()) Text("No deals right now. Check back soon!", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        for ((i, d) in t.deals.withIndex()) {
            val afford = if (d.payBonds) g.war.bonds >= d.price else g.money >= d.price
            TravelDealCard(g, d, afford) { t.buy(i) }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChunkyButton("New deals now (\$25)", enabled = g.money >= 25) { t.reroll() }
        }
        Spacer(Modifier.height(6.dp))
        val biggerText = if (t.travelLvl >= 7) "Shop is as big as it gets (10 deals)" else "Bigger shop: ${t.tSlots() + 1} deals (${fmt(t.biggerCost())})"
        ChunkyButton(biggerText, enabled = t.travelLvl < 7 && g.money >= t.biggerCost()) { t.bigger() }

        Spacer(Modifier.height(18.dp))
        OutlinedTitle("Traveling helpers", 22)
        Text("Traveling helpers buy traveling shop deals for you. They always keep some money and bonds saved for you.",
            fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        if (t.shopHelpers > 0) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (i in 0 until t.shopHelpers) Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFFFD21F)).border(2.dp, LINE, CircleShape))
        }
        if (t.shopperLog.isNotEmpty()) Text(t.shopperLog, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        val hireText = if (t.shopHelpers >= 6) "All 6 traveling helpers hired!" else "Hire ${TravelState.SHOPPER_NAMES[t.shopHelpers]} (${fmt(t.shopperCost())})"
        ChunkyButton(hireText, enabled = t.shopHelpers < 6 && g.money >= t.shopperCost()) { t.hireHelper() }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun TravelDealCard(g: GameViewModel, d: TravelDeal, afford: Boolean, onBuy: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).alpha(if (d.sold || !afford) 0.55f else 1f).clip(shape)
            .background(if (d.sold) Color(0xFFE6F4D9) else Color.White).border(3.dp, LINE, shape)
            .clickable(enabled = !d.sold && afford, onClick = onBuy).padding(10.dp)
    ) {
        Text(if (d.sold) "SOLD OUT" else d.tag, fontWeight = FontWeight.Black, fontSize = 11.sp, color = RED)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(g.named(d.name), fontWeight = FontWeight.Black, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text(if (d.payBonds) "${d.price} bonds" else fmt(d.price), fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF2E6BD6))
        }
        Text(g.named(d.desc), fontSize = 13.sp, lineHeight = 16.sp)
    }
}
