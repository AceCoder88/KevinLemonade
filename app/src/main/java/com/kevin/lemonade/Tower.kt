package com.kevin.lemonade

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.min
import kotlin.math.max

// =================== WIZARD REALM: the Wizard Tower (kevin.html 7801-7882, 7913-7965) ===================

private class Spell(val id: String, val name: String, val cost: Int, val desc: String, val cast: (TowerState) -> String)
private class Potion(val id: String, val name: String, val price: Long, val color: Color, val desc: String, val drink: (TowerState) -> String)

private val SPELLS = listOf(
    Spell("rainbow", "Summon a Rainbow", 60, "A rainbow for 2 minutes: double money!") { t ->
        t.g.rainbowUntil = t.g.now() + 120_000
        "RAINBOWUS APPEARUS! A rainbow over the stand!"
    },
    Spell("storm", "Lemon Storm", 40, "30 lemons fall from the sky") { t ->
        t.g.addLemons(30)
        "LEMONUS RAINUS! Lemons everywhere!"
    },
    Spell("cheer", "Cheer Charm", 20, "{N} becomes totally happy for a minute") { t ->
        t.g.sad = 0.0
        t.g.happyUntil = max(t.g.happyUntil, t.g.now() + 60_000)
        t.g.showFace(Face.HAPPY)
        "HAPPIUS MAXIMUS! I feel AMAZING!"
    },
    Spell("midas", "Midas Touch", 80, "Your next 5 squeezes are golden lemons") { t ->
        t.g.goldenNext += 5
        "GOLDUS TOUCHUS! Everything I touch turns to gold!"
    },
    Spell("crowd", "Summon Customers", 50, "A crowd shows up and buys 12 pitchers") { t ->
        val p = max(24L, t.g.price() * 12)
        t.g.money += p
        "CUSTOMERUS ARRIVUS! +${fmt(p)}!"
    },
    Spell("bonds", "Bond Blizzard", 70, "150 war bonds fall like snow") { t ->
        t.g.war.bonds += 150
        "BONDUS BLIZZARDUS! War bonds everywhere!"
    },
    // the original adds 15 limes with no cap here (the pirate hold's cap only limits what ships bring back)
    Spell("limes", "Summon Limes", 45, "15 limes appear (squeezing limes makes {N} happy)") { t ->
        t.g.pirate.limesLeft += 15
        "LIMUS APPEARUS! Fresh limes to squeeze!"
    },
    Spell("freeze", "Freeze Sadness", 55, "{N} can't get sad for 3 minutes") { t ->
        t.g.happyUntil = max(t.g.happyUntil, t.g.now() + 180_000)
        t.g.sad = 0.0
        "SADNESS FREEZUS! Nothing can bring me down!"
    },
)

private val POTIONS = listOf(
    Potion("speed", "Speed Potion", 150, Color(0xFF2E9BD6), "Squeezing goes TWICE as fast for 2 minutes") { t ->
        t.g.speedMult = 2.0; t.g.speedUntil = t.g.now() + 120_000
        "ZOOM! I can see SOUNDS!"
    },
    Potion("giant", "Giant Potion", 80, Color(0xFFE8483B), "{N} grows HUGE for 30 seconds") { t ->
        t.kevinScaleTo = 1.3f; t.kevinScaleUntil = t.g.now() + 30_000
        "WHOA! I'm a GIANT LEMON! Fee fi fo FRUIT!"
    },
    Potion("tiny", "Shrinking Potion", 60, Color(0xFFB79CFF), "{N} shrinks to tiny for 30 seconds") { t ->
        t.kevinScaleTo = 0.6f; t.kevinScaleUntil = t.g.now() + 30_000
        "I'm so tiny! Don't squeeze me by accident!"
    },
    Potion("love", "Love Potion", 120, Color(0xFFFF4D8D), "Heart eyes and triple money for 1 minute") { t ->
        t.heartEyesUntil = t.g.now() + 60_000
        t.g.partyUntil = max(t.g.partyUntil, t.g.now() + 60_000)
        "Everything is so... LOVELY. Triple money!"
    },
    Potion("gold", "Gold Potion", 300, Color(0xFFFFD21F), "Your next 8 squeezes are golden lemons") { t ->
        t.g.goldenNext += 8
        "I feel... SHINY. Golden lemons incoming!"
    },
    Potion("rainbowp", "Rainbow Potion", 400, Color(0xFF7B5CFF), "A rainbow for 3 minutes: double money, and {N} turns rainbow colors!") { t ->
        t.g.rainbowUntil = t.g.now() + 180_000
        t.rainbowKevinUntil = t.g.now() + 180_000
        "I burped a RAINBOW! And now I AM a rainbow! Double money!"
    },
    Potion("invis", "Invisible Potion", 90, Color(0xFFC0C8D0), "{N} turns invisible for 20 seconds (just for fun)") { t ->
        t.invisibleUntil = t.g.now() + 20_000
        "Where did I go?! I can't see my own FEET!"
    },
    Potion("bubble", "Bubble Potion", 70, Color(0xFF9ED8F5), "{N} floats up in a big bubble for 15 seconds") { t ->
        t.bubbleUntil = t.g.now() + 15_000
        "Wheee! I'm floating in a BUBBLE!"
    },
)

private val APPR_EVERY = mapOf("spell" to 30, "potion" to 45)

private class Apprentice(val name: String, val kind: String, val hat: Color, val cost: Long) {
    var hired by mutableStateOf(false)
    var job by mutableStateOf("")
    var t by mutableIntStateOf(0)
}

/** Wizard Tower: spells, potions brewed in the cauldron, mana, and the 4 apprentices who cast/brew for you. */
class TowerState(val g: GameViewModel) : Feature {
    override val key = "tower"

    var mana by mutableDoubleStateOf(50.0)
    var brewing by mutableStateOf(false)
    var cauldronColor by mutableStateOf(Color(0xFF7BC043))
    var wizBubble by mutableStateOf("")
    var sparkle by mutableIntStateOf(0)
    var apprLog by mutableStateOf("")

    private val apprentices = listOf(
        Apprentice("Pip", "spell", Color(0xFF2E9BD6), 300),
        Apprentice("Merlina", "spell", Color(0xFFFF4D8D), 700),
        Apprentice("Brewster", "potion", Color(0xFF4FA34A), 300),
        Apprentice("Hazel", "potion", Color(0xFFE9B630), 700),
    )

    // ---- visual effects potions put on Kevin; Ui.kt / StageArt.kt should read these to render them
    // on the stand screen (see Tower.kt's integration notes - nothing there does yet) ----
    var kevinScaleTo by mutableFloatStateOf(1f)
    var kevinScaleUntil by mutableLongStateOf(0L)
    var heartEyesUntil by mutableLongStateOf(0L)
    var invisibleUntil by mutableLongStateOf(0L)
    var bubbleUntil by mutableLongStateOf(0L)
    var rainbowKevinUntil by mutableLongStateOf(0L)
    fun kevinScale(): Float = if (g.now() < kevinScaleUntil) kevinScaleTo else 1f

    fun potionPrice(id: String): Long = potionPrice(POTIONS.first { it.id == id })
    private fun potionPrice(p: Potion): Long = Math.round(p.price * (if (g.closet.wearingSet("witch")) 0.5 else 1.0))

    fun castSpell(id: String) {
        val s = SPELLS.find { it.id == id } ?: return
        if (mana < s.cost || brewing) return
        mana -= s.cost
        val line = s.cast(this)
        wizBubble = "${g.name}: $line"
        sparkle++
    }

    fun startBrew(id: String) {
        val p = POTIONS.find { it.id == id } ?: return
        val cost = potionPrice(p)
        if (brewing || g.money < cost) return
        g.money -= cost
        brewing = true
        cauldronColor = p.color
        wizBubble = "${g.name}: Bubble bubble, lemon trouble... brewing a ${p.name}!"
        g.viewModelScope.launch {
            delay(2600)
            brewing = false
            val line = p.drink(this@TowerState)
            wizBubble = "${g.name}: *gulp* $line"
        }
    }

    fun apprenticeCount() = apprentices.size
    fun apprenticeName(i: Int) = apprentices[i].name
    fun apprenticeKind(i: Int) = apprentices[i].kind
    fun apprenticeHat(i: Int) = apprentices[i].hat
    fun apprenticeCost(i: Int) = apprentices[i].cost
    fun apprenticeHired(i: Int) = apprentices[i].hired
    fun apprenticeJob(i: Int) = apprentices[i].job
    fun apprenticeNextIn(i: Int): Int = (APPR_EVERY[apprentices[i].kind] ?: 0) - apprentices[i].t

    fun hireApprentice(i: Int) {
        val a = apprentices[i]
        if (a.hired || g.money < a.cost) return
        g.money -= a.cost; a.hired = true
        wizBubble = "${a.name}: Thank you, master ${g.name}! What should I ${if (a.kind == "spell") "cast" else "brew"}?"
    }

    fun setApprenticeJob(i: Int, job: String) {
        apprentices[i].job = job
        apprentices[i].t = 0
    }

    override fun tick() {
        mana = min(MANA_MAX, mana + 1.0)
        for (a in apprentices) {
            if (!a.hired || a.job.isEmpty()) continue
            val every = APPR_EVERY[a.kind] ?: continue
            a.t++
            if (a.t < every) continue
            a.t = 0
            if (a.kind == "spell") {
                val s = SPELLS.find { it.id == a.job } ?: continue
                if (mana < s.cost) { apprLog = "${a.name}: Not enough mana for ${s.name}! I'll try again soon."; continue }
                mana -= s.cost
                s.cast(this)
                apprLog = "${a.name} cast ${s.name}!"
                if (!g.inRealm()) g.popAt("${a.name} cast ${s.name}!", 0.5f, 0.3f)
            } else {
                val p = POTIONS.find { it.id == a.job } ?: continue
                val price = potionPrice(p)
                if (g.money < price) { apprLog = "${a.name}: We can't afford ${p.name} right now."; continue }
                g.money -= price
                p.drink(this)
                apprLog = "${a.name} brewed a ${p.name} and ${g.name} drank it!"
                if (!g.inRealm()) g.popAt("${a.name} brewed a ${p.name}!", 0.5f, 0.3f)
            }
        }
    }

    override fun save(j: JSONObject) {
        j.put("mana", mana)
        val arr = JSONArray()
        for (a in apprentices) arr.put(JSONObject().apply { put("hired", a.hired); put("job", a.job); put("t", a.t) })
        j.put("appr", arr)
    }

    override fun load(j: JSONObject) {
        mana = j.optDouble("mana", 50.0)
        j.optJSONArray("appr")?.let { arr ->
            for (i in 0 until min(arr.length(), apprentices.size)) {
                val o = arr.getJSONObject(i)
                apprentices[i].hired = o.optBoolean("hired", false)
                apprentices[i].job = o.optString("job", "")
                apprentices[i].t = o.optInt("t", 0)
            }
        }
    }

    override fun reset() {
        mana = 50.0; brewing = false; wizBubble = ""; apprLog = ""
        for (a in apprentices) { a.hired = false; a.job = ""; a.t = 0 }
        kevinScaleUntil = 0; heartEyesUntil = 0; invisibleUntil = 0; bubbleUntil = 0; rainbowKevinUntil = 0
    }

    // Rebirth keeps tools, war, space, pirates, digging "and everything else" - the tower is kept too,
    // which is just the shared Feature.onRebirth() default (no-op) - no override needed here.

    // the original's maxEverything() (kevin.html 8502-8526, the secret MAX OUT code) never touches
    // mana or the apprentices, so there's nothing for this feature's maxOut() to do.

    companion object { const val MANA_MAX = 100.0 }
}

@Composable
fun TowerScreen(g: GameViewModel) {
    val tower = g.tower
    Column(
        Modifier.fillMaxSize().background(Color(0xFF3D4425)).verticalScroll(rememberScrollState()).padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ChunkyButton("Back to lemonade", color = CARD) { g.screen = Screen.STAND }
            Spacer(Modifier.weight(1f))
            Text("Money: ${fmt(g.money)}", color = Color(0xFFF4EFD0), fontFamily = Lilita, fontSize = 15.sp)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTitle("Wizard Tower", 28, Color(0xFF7B5CFF))
        Spacer(Modifier.height(10.dp))

        // cauldron room
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.weight(1f).aspectRatio(260f / 180f)) { scale(size.width / 260f, size.width / 260f, Offset.Zero) { drawCauldron(tower) } }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Box(
                    Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(16.dp)).border(3.dp, Color(0xFF2A1408), RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        tower.wizBubble.ifEmpty { "${g.name}: Welcome to my tower! Spells use mana (it fills back up). Potions brew in the cauldron." },
                        color = Color(0xFF3A2E12), fontSize = 12.sp,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Mana", color = Color(0xFFF4EFD0), fontSize = 13.sp)
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier.weight(1f).height(16.dp).clip(CircleShape).background(Color(0xFF15180A)).border(2.dp, Color(0xFF15180A), CircleShape),
                    ) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth((tower.mana / TowerState.MANA_MAX).toFloat().coerceIn(0f, 1f))
                            .background(Color(0xFF7B5CFF)))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("${tower.mana.toInt()} / ${TowerState.MANA_MAX.toInt()}", color = Color(0xFFF4EFD0), fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Spells", color = Color(0xFFF4EFD0), fontFamily = Lilita, fontSize = 22.sp)
        Spacer(Modifier.height(6.dp))
        for (s in SPELLS) {
            SpellCard(
                name = s.name, cost = "${s.cost} mana", desc = g.named(s.desc),
                enabled = tower.mana >= s.cost && !tower.brewing,
            ) { tower.castSpell(s.id) }
        }

        Spacer(Modifier.height(16.dp))
        Text("Potions", color = Color(0xFFF4EFD0), fontFamily = Lilita, fontSize = 22.sp)
        Spacer(Modifier.height(6.dp))
        for (p in POTIONS) {
            val price = tower.potionPrice(p.id)
            SpellCard(
                name = p.name, cost = fmt(price), desc = g.named(p.desc), bottle = p.color,
                enabled = g.money >= price && !tower.brewing,
            ) { tower.startBrew(p.id) }
        }

        Spacer(Modifier.height(16.dp))
        Text("Apprentices", color = Color(0xFFF4EFD0), fontFamily = Lilita, fontSize = 22.sp)
        Text(
            "Two apprentices cast spells (using the tower's mana) and two brew potions (using your money). " +
                "Pick a job for each one and they keep doing it over and over, even when you leave the tower.",
            color = Color(0xFFF4EFD0), fontSize = 12.sp, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        for (i in 0 until tower.apprenticeCount()) ApprenticeCard(g, tower, i)
        if (tower.apprLog.isNotEmpty()) Text(tower.apprLog, color = Color(0xFFF4EFD0), fontSize = 12.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun SpellCard(name: String, cost: String, desc: String, enabled: Boolean, bottle: Color? = null, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).alpha(if (enabled) 1f else 0.55f).clip(shape)
            .background(Color(0xFFF4EFD0)).border(3.dp, Color(0xFF15180A), shape)
            .clickable(enabled = enabled, onClick = onClick).padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (bottle != null) {
                Box(Modifier.size(12.dp, 16.dp).background(bottle, RoundedCornerShape(3.dp, 3.dp, 6.dp, 6.dp)).border(2.dp, Color(0xFF2A1745), RoundedCornerShape(3.dp, 3.dp, 6.dp, 6.dp)))
                Spacer(Modifier.width(6.dp))
            }
            Text(name, color = Color(0xFF2A2E18), fontWeight = FontWeight.Black, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text(cost, color = Color(0xFF2E6BD6), fontWeight = FontWeight.Black, fontSize = 15.sp)
        }
        Text(desc, color = Color(0xFF2A2E18), fontSize = 12.sp, lineHeight = 15.sp)
    }
}

@Composable
private fun ApprenticeCard(g: GameViewModel, tower: TowerState, i: Int) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(shape).background(Color(0xFFEDE6FF)).border(3.dp, Color(0xFF140A2A), shape).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val hat = tower.apprenticeHat(i)
        Canvas(Modifier.size(32.dp, 22.dp)) {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(size.width / 2, 0f); lineTo(0f, size.height); lineTo(size.width, size.height); close()
            }
            drawPath(path, hat)
        }
        Text(tower.apprenticeName(i), color = Color(0xFF2A1745), fontWeight = FontWeight.Black, fontSize = 15.sp)
        if (!tower.apprenticeHired(i)) {
            Text(if (tower.apprenticeKind(i) == "spell") "Spell apprentice" else "Potion apprentice", color = Color(0xFF2A1745), fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            val cost = tower.apprenticeCost(i)
            ChunkyButton("Hire (${fmt(cost)})", enabled = g.money >= cost) { tower.hireApprentice(i) }
        } else {
            val kind = tower.apprenticeKind(i)
            Text("${if (kind == "spell") "Casts" else "Brews"} every ${if (kind == "spell") 30 else 45}s", color = Color(0xFF2A1745), fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            var open by remember { mutableStateOf(false) }
            val jobName = when (kind) {
                "spell" -> SPELLS.find { it.id == tower.apprenticeJob(i) }?.name
                else -> POTIONS.find { it.id == tower.apprenticeJob(i) }?.name
            } ?: "Resting (nothing)"
            Box {
                Text(
                    "Job: $jobName", color = Color(0xFF2A1745), fontWeight = FontWeight.Black, fontSize = 13.sp,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).border(2.dp, Color(0xFF140A2A), RoundedCornerShape(8.dp))
                        .clickable { open = true }.padding(horizontal = 10.dp, vertical = 6.dp),
                )
                DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                    DropdownMenuItem(text = { Text("Resting (nothing)") }, onClick = { tower.setApprenticeJob(i, ""); open = false })
                    if (kind == "spell") {
                        for (s in SPELLS) DropdownMenuItem(
                            text = { Text("${s.name} (${s.cost} mana)") },
                            onClick = { tower.setApprenticeJob(i, s.id); open = false },
                        )
                    } else {
                        for (p in POTIONS) DropdownMenuItem(
                            text = { Text("${p.name} (${fmt(tower.potionPrice(p.id))})") },
                            onClick = { tower.setApprenticeJob(i, p.id); open = false },
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                if (tower.apprenticeJob(i).isNotEmpty()) "Next one in ${tower.apprenticeNextIn(i)}s" else "Pick a job for me!",
                color = Color(0xFF6A4BE8), fontWeight = FontWeight.Black, fontSize = 12.sp,
            )
        }
    }
}

// ----------------------------- drawing -----------------------------

private fun DrawScope.drawCauldron(tower: TowerState) {
    val shape = RoundedCornerShape(16.dp)
    drawRoundRect(Color(0xFF2A1745), Offset.Zero, Size(260f, 180f), androidx.compose.ui.geometry.CornerRadius(16f, 16f))
    drawRect(Color(0xFF6B4A1E), Offset(30f, 20f), Size(60f, 8f))
    drawRect(Color(0xFF6B4A1E), Offset(170f, 30f), Size(60f, 8f))
    drawRect(Color(0xFF2E9BD6), Offset(36f, 4f), Size(12f, 16f))
    drawRect(Color(0xFFE8483B), Offset(56f, 6f), Size(12f, 14f))
    drawRect(Color(0xFF7BC043), Offset(74f, 2f), Size(10f, 18f))
    drawRect(Color(0xFFFF4D8D), Offset(180f, 12f), Size(12f, 18f))
    drawRect(Color(0xFFFFD21F), Offset(200f, 16f), Size(14f, 14f))
    drawLine(Color(0xFF3A3A4A), Offset(70f, 168f), Offset(84f, 150f), 8f)
    drawLine(Color(0xFF3A3A4A), Offset(190f, 168f), Offset(176f, 150f), 8f)
    oval(130f, 110f, 70f, 48f, Color(0xFF1B1B2A), Color(0xFF4A4A5A), 5f)
    oval(130f, 78f, 62f, 14f, tower.cauldronColor, null)
    if (tower.brewing) {
        drawCircle(Color.White.copy(alpha = 0.6f), 6f, Offset(110f, 72f))
        drawCircle(Color.White.copy(alpha = 0.6f), 4f, Offset(140f, 70f))
        drawCircle(Color.White.copy(alpha = 0.6f), 5f, Offset(158f, 74f))
    } else {
        drawCircle(Color.White.copy(alpha = 0.6f), 6f, Offset(110f, 76f))
        drawCircle(Color.White.copy(alpha = 0.6f), 4f, Offset(140f, 74f))
        drawCircle(Color.White.copy(alpha = 0.6f), 5f, Offset(158f, 78f))
    }
}
