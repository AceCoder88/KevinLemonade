package com.kevin.lemonade

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import org.json.JSONArray
import org.json.JSONObject

/** One piece of clothing (the original's CLOTHES). `slot` is one of hat/face/body/feet/back. */
data class ClothItem(val id: String, val slot: String, val name: String, val effect: String)

/** A full outfit of pieces with a combined bonus (the original's SETS). */
data class ClothSet(val key: String, val name: String, val pieces: List<String>, val bonus: String)

const val CLOTH_PRICE = 50L

val CLOTHES: List<ClothItem> = listOf(
    ClothItem("tophat", "hat", "Fancy top hat", "+25% money for every pitcher"),
    ClothItem("army", "hat", "Army helmet", "{N} gets a job in the army: he fights in every war wave as a sergeant"),
    ClothItem("space", "hat", "Space helmet", "{N} explores the solar system in a rocket: space juice sells for 50% more"),
    ClothItem("starglasses", "face", "Star glasses", "Golden lemons show up more often"),
    ClothItem("monocle", "face", "Fancy monocle", "Customers leave tips more often"),
    ClothItem("sweater", "body", "Cozy sweater", "More comfort: {N} gets sad 30% slower"),
    ClothItem("tie", "body", "Business tie", "+\$3 for every pitcher"),
    ClothItem("slippers", "feet", "Bunny slippers", "So comfy, {N} slowly cheers himself up"),
    ClothItem("sneakers", "feet", "Speedy sneakers", "Squeezing goes 20% faster"),
    ClothItem("crown", "hat", "Royal crown", "Royal tax: you get money every 10 seconds"),
    ClothItem("chef", "hat", "Chef hat", "Pitchers need 1 less lemon"),
    ClothItem("pirate", "hat", "Pirate hat", "{N} digs up buried treasure every 30 seconds"),
    ClothItem("mustache", "face", "Fancy mustache", "Mr. Zest likes it: 10% off his regular upgrades"),
    ClothItem("clownnose", "face", "Clown nose", "Tapping {N} cheers him up twice as much"),
    ClothItem("sunhat", "hat", "Sun hat", ""),
    ClothItem("beanie", "hat", "Beanie", ""),
    ClothItem("rockhair", "hat", "Rockstar hair", ""),
    ClothItem("nightcap", "hat", "Night cap", ""),
    ClothItem("bowler", "hat", "Bowler hat", ""),
    ClothItem("clownwig", "hat", "Clown wig", ""),
    ClothItem("sleepmask", "face", "Sleep mask", ""),
    ClothItem("hawaiian", "body", "Hawaiian shirt", ""),
    ClothItem("leather", "body", "Leather jacket", ""),
    ClothItem("pajamas", "body", "Pajamas", ""),
    ClothItem("apron", "body", "Baker apron", ""),
    ClothItem("clownsuit", "body", "Clown suit", ""),
    ClothItem("camo", "body", "Camo uniform", ""),
    ClothItem("flipflops", "feet", "Flip-flops", ""),
    ClothItem("snowboots", "feet", "Snow boots", ""),
    ClothItem("clogs", "feet", "Wooden clogs", ""),
    ClothItem("loafers", "feet", "Loafers", ""),
    ClothItem("goldslippers", "feet", "Gold slippers", ""),
    ClothItem("armyboots", "feet", "Army boots", ""),
    ClothItem("royalrobe", "body", "Royal robe", ""),
    ClothItem("witchhat", "hat", "Witch hat", ""),
    ClothItem("witchdress", "body", "Witch dress", ""),
    ClothItem("witchboots", "feet", "Witch boots", ""),
    ClothItem("broom", "back", "Broomstick", ""),
    ClothItem("ninjahood", "hat", "Ninja hood", ""),
    ClothItem("ninjamask", "face", "Ninja mask", ""),
    ClothItem("ninjasuit", "body", "Ninja suit", ""),
    ClothItem("tabi", "feet", "Ninja shoes", ""),
    ClothItem("cowboyhat", "hat", "Cowboy hat", ""),
    ClothItem("bandana", "face", "Bandana", ""),
    ClothItem("vest", "body", "Cowboy vest", ""),
    ClothItem("cowboyboots", "feet", "Cowboy boots", ""),
    ClothItem("wizardhat", "hat", "Wizard hat", ""),
    ClothItem("wizbeard", "face", "Wizard beard", ""),
    ClothItem("robe", "body", "Wizard robe", ""),
    ClothItem("heromask", "face", "Hero mask", ""),
    ClothItem("herosuit", "body", "Hero suit", ""),
    ClothItem("heroboots", "feet", "Hero boots", ""),
    ClothItem("cape", "back", "Hero cape", "Your arrows and Sgt. {N} hit 25% harder in war"),
    ClothItem("labcoat", "body", "Lab coat", "Extra squeezers work 30% faster"),
    ClothItem("skates", "feet", "Roller skates", "The auto-squeezer goes twice as fast"),
    ClothItem("rainboots", "feet", "Rain boots", "In Catch the Lemons, lemons fall slower"),
    ClothItem("shades", "face", "Cool shades", "{N} looks cool and gets sad half as fast"),
    ClothItem("eyepatch", "face", "Eye patch", "Pirate treasure is twice as big"),
    ClothItem("piratecoat", "body", "Pirate coat", "+10% money for every pitcher"),
    ClothItem("pirateboots", "feet", "Pirate boots", "+\$2 for every pitcher"),
    ClothItem("spacesuit", "body", "Space suit", "Space juice sells for 25% more"),
    ClothItem("moonboots", "feet", "Moon boots", "Helpers in Catch the Lemons run 30% faster"),
    ClothItem("tuxedo", "body", "Tuxedo", "+20% money for every pitcher"),
    ClothItem("fancyshoes", "feet", "Shiny shoes", "Tips are twice as big"),
)
private val CLOTH_BY_ID = CLOTHES.associateBy { it.id }

val SETS: List<ClothSet> = listOf(
    ClothSet("summer", "Summer", listOf("sunhat", "shades", "hawaiian", "flipflops"), "Hot summer day! Lemonade sells for 75% more"),
    ClothSet("winter", "Winter", listOf("beanie", "sweater", "snowboots"), "Toasty warm: {N} gets sad 3 times slower"),
    ClothSet("rockstar", "Rockstar", listOf("rockhair", "starglasses", "leather", "sneakers"), "Rock concert! Fans throw money at you every 5 seconds"),
    ClothSet("sleep", "Sleepy time", listOf("nightcap", "sleepmask", "pajamas", "slippers"), "Sweet dreams: {N} stays happy no matter what"),
    ClothSet("baker", "Baker", listOf("chef", "apron", "clogs"), "Fresh-baked: pitchers need 2 fewer lemons"),
    ClothSet("mustache", "Mustache Guy", listOf("bowler", "mustache", "tie", "loafers"), "Mr. Zest LOVES the mustache: 30% off his regular upgrades"),
    ClothSet("royalty", "Royalty", listOf("crown", "royalrobe", "goldslippers"), "Royal tax every 3 seconds instead of 10"),
    ClothSet("witch", "Witch", listOf("witchhat", "witchdress", "witchboots", "broom"), "Potions in the Wizard Tower cost half, and the tower opens for you"),
    ClothSet("ninja", "Ninja", listOf("ninjahood", "ninjamask", "ninjasuit", "tabi"), "Sneaky strikes: arrows in war crit way more often"),
    ClothSet("cowboy", "Cowboy", listOf("cowboyhat", "bandana", "vest", "cowboyboots"), "Yee-haw! The auto-squeezer goes 30% faster"),
    ClothSet("wizard", "Wizard", listOf("wizardhat", "wizbeard", "robe"), "Magic: golden lemons show up twice as often"),
    ClothSet("hero", "Superhero", listOf("heromask", "herosuit", "cape", "heroboots"), "Super strength: everything you do in war hits TWICE as hard"),
    ClothSet("clown", "Clown", listOf("clownwig", "clownnose", "clownsuit", "skates"), "Clown show: every tap on {N} earns you money"),
    ClothSet("soldier", "Soldier", listOf("army", "camo", "armyboots"), "Sgt. {N} fights in war with DOUBLE health and power"),
    ClothSet("astronaut", "Astronaut", listOf("space", "spacesuit", "moonboots"), "Space juice sells for DOUBLE, and {N} brings home moon rocks to sell"),
    ClothSet("pirate", "Pirate", listOf("pirate", "eyepatch", "piratecoat", "pirateboots"), "Treasure every 10 seconds instead of 30, and 3 times bigger"),
    ClothSet("fancy", "Fancy Man", listOf("tophat", "monocle", "tuxedo", "fancyshoes"), "+50% money on every pitcher. Very distinguished."),
)
private val SET_BY_KEY = SETS.associateBy { it.key }

/** the original's closetLines: Kevin's one-liner when a single piece gets worn (only pieces with a real effect have one) */
private val closetLines = mapOf(
    "tophat" to "A top hat?! I'm a FANCY lemon now. Please call me Sir Kevin.",
    "army" to "Reporting for duty! Sergeant Kevin, ready to fight bugs!",
    "space" to "I'm an ASTRONAUT! To the stars! ...Is there oxygen in here?",
    "starglasses" to "Star glasses! I can see golden lemons from a mile away!",
    "monocle" to "Indubitably. That's a fancy word. I have a monocle now.",
    "sweater" to "So cozy. I could watch the squeezer all day. ...Almost.",
    "tie" to "A business tie! Let's talk business. Lemonade business.",
    "slippers" to "Bunny slippers! My feet have never been this happy.",
    "sneakers" to "Speedy sneakers! Zoom zoom!",
    "crown" to "A CROWN?! Bow before King Kevin! ...Please?",
    "chef" to "A chef hat! Today's special: my cousins. ...Wait.",
    "pirate" to "Arr! Pirate Kevin, here to find treasure!",
    "mustache" to "A mustache! Now I look just like Mr. Zest!",
    "clownnose" to "Honk honk! It's hard to be sad with a clown nose.",
    "cape" to "A hero cape! Sgt. Kevin is ready to SAVE the day!",
    "labcoat" to "A lab coat! I'm a lemon SCIENTIST now.",
    "skates" to "Roller skates! Whee! ...How do I stop?!",
    "rainboots" to "Rain boots! Splish splash, here come the lemons!",
    "shades" to "Whoa. I look SO cool. Nothing can bother me now.",
    "eyepatch" to "Arr! I can only see half the squeezer now. Good.",
    "piratecoat" to "A captain's coat! Call me Captain Kevin!",
    "pirateboots" to "Pirate boots! Now I can walk the plank. Wait, no thanks.",
    "spacesuit" to "A real space suit! Houston, we have a lemon.",
    "moonboots" to "Moon boots! Boing! Boing!",
    "tuxedo" to "A tuxedo! I'm dressed for the fanciest lemonade party ever.",
    "fancyshoes" to "Shiny shoes! I can see my face in them!",
)

/** the original's setLines: Kevin's line when a full outfit gets put on */
private val setLines = mapOf(
    "astronaut" to "FULL ASTRONAUT! One small step for a lemon, one giant leap for lemonade!",
    "pirate" to "FULL PIRATE! Yo ho ho and a bottle of... lemonade!",
    "fancy" to "FULL FANCY MAN! Good day to you. Lemonade? Splendid.",
    "summer" to "Summer time! Sun's out, lemonade's out!",
    "winter" to "So warm and toasty! Bring on the snow!",
    "rockstar" to "ROCKSTAR KEVIN! Thank you, thank you! I'll be here all week!",
    "sleep" to "Sleepy time... zzz... I'm so happy... zzz...",
    "baker" to "Fresh lemon cookies, coming right up!",
    "mustache" to "Behold: the mustache of a true gentleman.",
    "royalty" to "All hail King Kevin! ...That's me!",
    "clown" to "Honk honk! Tap me for a show!",
    "soldier" to "Sergeant Kevin, reporting for FULL duty, sir!",
    "hero" to "Up, up, and AWAY! Super Kevin is here to save the day!",
    "witch" to "Hee hee hee! Double, double, lemon trouble!",
    "ninja" to "Hiii-YA! Silent but deadly. ...Wait, that sounds bad.",
    "cowboy" to "Yee-haw! Howdy, partner! This town ain't big enough for two squeezers.",
    "wizard" to "You shall not SQUEEZE! ...Okay, you can squeeze a little.",
)

/** Kevin's Closet: outfits with special powers. */
class ClosetState(val g: GameViewModel) : Feature {
    override val key = "closet"

    val owned = mutableStateListOf<String>()
    val equipped = mutableStateMapOf("hat" to null as String?, "face" to null as String?, "body" to null as String?, "feet" to null as String?, "back" to null as String?)


    private var crownT = 0
    private var pirateT = 0
    private var rockT = 0
    private var rockstarT = 0

    /** is {N} wearing this piece anywhere? (the original's wearing(id)) */
    fun wearing(id: String): Boolean = equipped.values.contains(id)
    /** is {N} wearing every piece of this set? (the original's wearingSet(id)) */
    fun wearingSet(key: String): Boolean = SET_BY_KEY[key]?.pieces?.all { wearing(it) } == true
    /** Mr. Zest's mustache discount: 30% off wearing the whole Mustache Guy set, 10% off just the mustache */
    fun mustacheDiscount(): Double = if (wearingSet("mustache")) 0.7 else if (wearing("mustache")) 0.9 else 1.0

    /** buy (if not owned) and toggle wearing one piece */
    fun buyOrWear(c: ClothItem) {
        if (!owned.contains(c.id)) {
            if (g.money < CLOTH_PRICE) return
            g.money -= CLOTH_PRICE
            owned.add(c.id)
        }
        if (equipped[c.slot] == c.id) equipped[c.slot] = null
        else {
            equipped[c.slot] = c.id
            closetLines[c.id]?.let { g.say(it) }
        }
        g.save()
    }

    /** buy (any missing pieces) and wear a whole outfit at once, replacing whatever's equipped */
    fun buyOrWearSet(key: String) {
        val st = SET_BY_KEY[key] ?: return
        if (wearingSet(key)) {
            for (id in st.pieces) equipped[CLOTH_BY_ID[id]!!.slot] = null
            g.save()
            return
        }
        val missing = st.pieces.filter { !owned.contains(it) }
        val cost = missing.size * CLOTH_PRICE
        if (g.money < cost) return
        g.money -= cost
        for (slot in equipped.keys.toList()) equipped[slot] = null
        for (id in st.pieces) { owned.add(id); equipped[CLOTH_BY_ID[id]!!.slot] = id }
        setLines[key]?.let { g.say(it) }
        g.save()
    }


    override fun tick() {
        if (wearing("slippers") && g.sad > 0) g.sad = maxOf(0.0, g.sad - 0.12)
        if (wearingSet("sleep") && g.sad > 0) g.sad = 0.0
        if (wearingSet("astronaut")) { rockT++; if (rockT >= 20) { rockT = 0; val rocks = maxOf(40L, g.price() * 4); g.money += rocks; g.popAt("Moon rocks! +${fmt(rocks)}", 0.25f, 0.15f) } }
        if (wearingSet("rockstar")) { rockstarT++; if (rockstarT >= 5) { rockstarT = 0; g.money += maxOf(5L, g.price()) } }
        if (wearing("crown")) {
            crownT++
            val need = if (wearingSet("royalty")) 3 else 10
            if (crownT >= need) { crownT = 0; g.money += maxOf(10L, g.price()) }
        }
        if (wearing("pirate")) {
            pirateT++
            val need = if (wearingSet("pirate")) 10 else 30
            if (pirateT >= need) {
                pirateT = 0
                val loot = (maxOf(50L, g.price() * 6) * (if (wearing("eyepatch")) 2 else 1) * (if (wearingSet("pirate")) 3 else 1))
                g.money += loot
                g.popAt("Treasure! +${fmt(loot)}", 0.25f, 0.15f)
            }
        }
    }

    override fun save(j: JSONObject) {
        j.put("owned", JSONArray(owned.toList()))
        val eq = JSONObject(); for ((slot, id) in equipped) if (id != null) eq.put(slot, id)
        j.put("equipped", eq)
    }

    override fun load(j: JSONObject) {
        owned.clear(); owned.addAll(j.optJSONArray("owned")?.let { a -> (0 until a.length()).map { a.getString(it) } } ?: emptyList())
        for (slot in equipped.keys.toList()) equipped[slot] = null
        j.optJSONObject("equipped")?.let { eq -> eq.keys().forEach { slot -> equipped[slot] = eq.getString(slot) } }
    }

    override fun reset() {
        owned.clear()
        for (slot in equipped.keys.toList()) equipped[slot] = null
    }

    /** Rebirth doesn't touch the closet in the original - outfits are kept. */
    override fun onRebirth() {}

    override fun maxOut() {
        owned.addAll(CLOTHES.map { it.id })
    }
}

// ======================= Kevin's Closet screen =======================
@Composable
fun ClosetScreen(g: GameViewModel) {
    val c = g.closet
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ChunkyButton("Back to lemonade", color = CARD) { g.screen = Screen.STAND }
            Spacer(Modifier.weight(1f))
            MoneyText(g, 24)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTitle("${g.name}'s Closet", 28)
        Text("Each outfit costs \$50 a piece. ${g.name} wears one at a time per slot, and every outfit has its own special power.",
            fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))

        // preview
        ClosetPreview(c, Modifier.align(Alignment.CenterHorizontally).size(160.dp, 200.dp))
        val wornSets = SETS.filter { c.wearingSet(it.key) }
        Text(if (wornSets.isNotEmpty()) "Wearing the ${wornSets[0].name} outfit!" else "${g.name} isn't wearing an outfit yet.",
            fontWeight = FontWeight.Black, fontSize = 14.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(14.dp))

        // boy or girl
        ClosetSection("Boy or girl?") {
            ClosetTile("Boy: Kevin", "The classic.", if (g.gender == "boy") "Picked!" else "Tap to pick", g.gender == "boy") {
                g.gender = "boy"
            }
            ClosetTile("Girl: Kevia", "With a pink bow. She can have a boyfriend!", if (g.gender == "girl") "Picked!" else "Tap to pick", g.gender == "girl") {
                g.gender = "girl"
            }
        }

        // outfits (sets)
        ClosetSection("Outfits") {
            for (st in SETS) {
                val worn = c.wearingSet(st.key)
                val missing = st.pieces.count { !c.owned.contains(it) }
                val status = if (worn) "Wearing it! Tap to take it off" else if (missing > 0) "Buy for ${fmt(missing * CLOTH_PRICE)}" else "Tap to wear"
                val names = st.pieces.joinToString(" + ") { CLOTH_BY_ID[it]!!.name }
                ClosetTile(st.name, "${g.named(st.bonus)}. ($names)", status, worn, enabled = worn || g.money >= missing * CLOTH_PRICE) {
                    c.buyOrWearSet(st.key)
                }
            }
        }

        // hat / face / body / feet grids (the original never lists "back" pieces individually - only via their set)
        for ((slot, title) in listOf("hat" to "Hats", "face" to "Face", "body" to "Body", "feet" to "Feet")) {
            ClosetSection(title) {
                for (item in CLOTHES.filter { it.slot == slot }) {
                    val have = c.owned.contains(item.id)
                    val worn = c.equipped[slot] == item.id
                    val status = if (worn) "Wearing it! Tap to take off" else if (have) "Tap to wear" else "Buy for ${fmt(CLOTH_PRICE)}"
                    ClosetTile(item.name, g.named(item.effect), status, worn, enabled = worn || have || g.money >= CLOTH_PRICE) {
                        c.buyOrWear(item)
                    }
                }
            }
        }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun ClosetSection(title: String, content: @Composable () -> Unit) {
    Spacer(Modifier.height(6.dp))
    Text(title, fontFamily = Lilita, fontSize = 18.sp)
    Spacer(Modifier.height(4.dp))
    content()
}

@Composable
private fun ClosetTile(name: String, effect: String, status: String, on: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier.fillMaxWidth().padding(vertical = 3.dp).alpha(if (enabled) 1f else 0.55f).clip(shape)
            .background(if (on) Color(0xFFFFE1EF) else Color.White).border(3.dp, LINE, shape)
            .clickable(enabled = enabled, onClick = onClick).padding(10.dp)
    ) {
        Text(name, fontWeight = FontWeight.Black, fontSize = 15.sp)
        if (effect.isNotBlank()) Text(effect, fontSize = 12.sp, lineHeight = 15.sp)
        Text(status, fontSize = 12.sp, color = LINE.copy(alpha = 0.7f))
    }
}

