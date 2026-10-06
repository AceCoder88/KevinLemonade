package com.kevin.lemonade

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

val BG = Color(0xFFFFF4B8)
val CARD = Color(0xFFFFFDF2)
val PEEL = Color(0xFFFFD21F)

val Lilita = FontFamily(Font(R.font.lilita_one))
@OptIn(ExperimentalTextApi::class)
val Nunito = FontFamily(
    Font(R.font.nunito, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.nunito, FontWeight.Black, variationSettings = FontVariation.Settings(FontVariation.weight(900))),
)

@Composable
fun KevinApp(g: GameViewModel) {
    ProvideTextStyle(TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, color = LINE, fontSize = 16.sp)) {
        Box(Modifier.fillMaxSize().background(BG).systemBarsPadding().imePadding()) {
            when (g.screen) {
                Screen.STAND -> StandScreen(g)
                Screen.SHOP -> { BackHandler { g.screen = Screen.STAND }; ShopScreen(g) }
                Screen.CATCH -> { BackHandler { g.screen = Screen.STAND }; CatchScreen(g) }
            }
        }
    }
}

/** chunky cartoon button like the original's */
@Composable
fun ChunkyButton(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = PEEL,
    textColor: Color = LINE,
    big: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(999.dp)
    Box(
        modifier
            .alpha(if (enabled) 1f else 0.5f)
            .background(LINE, shape)
            .padding(bottom = 5.dp)
            .clip(shape)
            .background(color)
            .border(3.dp, LINE, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = if (big) 14.dp else 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = textColor, fontFamily = if (big) Lilita else Nunito, fontWeight = FontWeight.Black,
            fontSize = if (big) 24.sp else 16.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun OutlinedTitle(text: String, size: Int = 34, color: Color = PEEL) {
    Box {
        Text(text, fontFamily = Lilita, fontSize = size.sp, lineHeight = (size * 1.05).sp, textAlign = TextAlign.Center,
            color = LINE, modifier = Modifier.offset(y = 4.dp).fillMaxWidth())
        Text(text, fontFamily = Lilita, fontSize = size.sp, lineHeight = (size * 1.05).sp, textAlign = TextAlign.Center,
            style = TextStyle(drawStyle = Stroke(6f)), color = LINE, modifier = Modifier.fillMaxWidth())
        Text(text, fontFamily = Lilita, fontSize = size.sp, lineHeight = (size * 1.05).sp, textAlign = TextAlign.Center,
            color = color, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun MoneyText(g: GameViewModel, size: Int = 30) {
    val bump = remember { Animatable(1f) }
    LaunchedEffect(g.moneyBump) { if (g.moneyBump > 0) { bump.snapTo(1.18f); bump.animateTo(1f, tween(250)) } }
    Text(fmt(g.money), fontFamily = Lilita, fontSize = size.sp, color = Color(0xFF3E9B4F), modifier = Modifier.scale(bump.value))
}

// ======================= the lemonade stand =======================
@Composable
fun StandScreen(g: GameViewModel) {
    var confirmRestart by remember { mutableStateOf(false) }
    var showInfo by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            MoneyText(g)
            Spacer(Modifier.weight(1f))
            ChunkyButton("Restart", color = CARD) { confirmRestart = true }
        }
        OutlinedTitle("${g.name} watches the lemonade get made")
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Your lemon friend's name:", fontSize = 15.sp)
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = g.name,
                onValueChange = { g.name = it.take(16) },
                singleLine = true,
                textStyle = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = LINE),
                cursorBrush = SolidColor(LINE),
                modifier = Modifier.width(130.dp).border(3.dp, LINE, CircleShape).background(CARD, CircleShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        SadMeter(g)
        Spacer(Modifier.height(10.dp))
        Stage(g)
        Spacer(Modifier.height(14.dp))
        ChunkyButton(
            text = if (g.cranking) "Crank! (${g.crankN}/${g.crankNeed})" else "Squeeze a lemon",
            big = true,
            enabled = !g.busy || g.cranking,
            modifier = Modifier.fillMaxWidth(0.85f),
        ) { g.mainButton() }
        Spacer(Modifier.height(6.dp))
        val out = g.lemonsLeft <= 0
        Text(if (out) "Out of lemons! Go catch more below." else "Lemons: ${g.lemonsLeft} / ${g.lemonCap()}",
            color = if (out) RED else LINE, fontWeight = FontWeight.Black, fontSize = 17.sp)
        Text("Pitchers sold: ${g.glasses}", fontSize = 15.sp)
        Glasses(g.glasses)
        Spacer(Modifier.height(6.dp))
        Text(g.priceText(), fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(14.dp))

        // places to go
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(CARD).border(3.dp, LINE, RoundedCornerShape(22.dp)).padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Places to go", fontFamily = Lilita, fontSize = 22.sp)
            Spacer(Modifier.height(8.dp))
            ChunkyButton("Open the Shop", color = Color(0xFFFF9AC1), modifier = Modifier.fillMaxWidth()) { g.openShop() }
            val shake = remember { Animatable(0f) }
            LaunchedEffect(g.catchNudge) {
                if (g.catchNudge > 0) for (x in listOf(-10f, 10f, -8f, 8f, -4f, 0f)) shake.animateTo(x, tween(50))
            }
            ChunkyButton("Catch the Lemons", color = Color(0xFF8FCB5A),
                modifier = Modifier.fillMaxWidth().graphicsLayer { translationX = shake.value * density }) { g.screen = Screen.CATCH }
        }
        Spacer(Modifier.height(10.dp))
        Text(if (showInfo) "Hide info" else "More info", fontWeight = FontWeight.Black,
            modifier = Modifier.clickable { showInfo = !showInfo }.padding(8.dp))
        if (showInfo) {
            Text(
                "Squeeze lemons to fill a pitcher, then it sells for money. Each squeeze uses one lemon.\n\n" +
                    "Until you buy the electric motor, you crank the squeezer by hand. Tap fast for a super squeeze!\n\n" +
                    "When your friend gets sad, you make less money. Tap him a lot for free therapy.\n\n" +
                    "Out of lemons? Go catch more under the lemon tree.\n\n" +
                    "Whatever you do, don't hold down on your friend.",
                fontSize = 14.sp, modifier = Modifier.padding(horizontal = 6.dp),
            )
        }
        Spacer(Modifier.height(30.dp))
    }
    if (confirmRestart) {
        AlertDialog(
            onDismissRequest = { confirmRestart = false },
            title = { Text("Start over?", fontFamily = Lilita, fontSize = 24.sp) },
            text = { Text("All your money, lemons and upgrades will be gone.") },
            confirmButton = { TextButton({ confirmRestart = false; g.restart() }) { Text("Restart", color = RED) } },
            dismissButton = { TextButton({ confirmRestart = false }) { Text("Keep playing") } },
            containerColor = CARD,
        )
    }
}

@Composable
fun SadMeter(g: GameViewModel) {
    val pct = g.sadFrac().toFloat()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("\uD83D\uDE00", fontSize = 20.sp)
        Box(Modifier.padding(horizontal = 8.dp).width(180.dp).height(16.dp).clip(CircleShape).background(Color(0xFFB9B5A0))
            .border(3.dp, LINE, CircleShape)) {
            val fill = when (g.stage()) { 0 -> Color(0xFF7CC56A); 1 -> Color(0xFFFFB13B); else -> RED }
            Box(Modifier.fillMaxHeight().fillMaxWidth(1f - pct).background(fill))
        }
        Text("\uD83D\uDE2D", fontSize = 20.sp)
    }
    Text(g.sadText(), fontSize = 15.sp)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Glasses(n: Int) {
    FlowRow(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.Center) {
        repeat(minOf(n, 30)) {
            Canvas(Modifier.size(20.dp, 28.dp).padding(1.dp)) {
                scale(size.width / 30f, size.height / 42f, Offset.Zero) {
                    shape("M4 6 L26 6 L23 38 L7 38 Z", Color(0xFFFFE94A), w = 3f)
                    drawPath(svg("M4 6 L26 6 L25.3 13 L4.7 13 Z"), Color.White.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
fun Stage(g: GameViewModel) {
    var time by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) { while (true) withFrameMillis { time = it } }
    val shape = RoundedCornerShape(28.dp)
    BoxWithConstraints(
        Modifier.fillMaxWidth()
            .background(LINE, shape).padding(bottom = 6.dp) // chunky shadow under the stage
            .aspectRatio(800f / 500f)
            .clip(shape).border(4.dp, LINE, shape)
    ) {
        Canvas(
            Modifier.fillMaxSize().pointerInput(Unit) {
                awaitEachGesture {
                    val sc = size.width / 800f
                    val down = awaitFirstDown()
                    val p = down.position / sc
                    val kevin = onKevin(p)
                    if (kevin) g.kevinDown() else { g.lookAtTouch(p); g.crankTap() }
                    while (true) {
                        val ev = awaitPointerEvent()
                        val c = ev.changes.firstOrNull() ?: break
                        if (!kevin) g.lookAtTouch(c.position / sc)
                        if (!c.pressed) break
                    }
                    if (kevin) g.kevinUp()
                }
            }
        ) {
            scale(size.width / 800f, size.width / 800f, Offset.Zero) { drawStage(g, time) }
        }

        // speech bubble
        val bump = remember { Animatable(1f) }
        LaunchedEffect(g.bubbleBump) { bump.snapTo(1.06f); bump.animateTo(1f, tween(150)) }
        Box(
            Modifier.padding(start = maxWidth * 0.03f, top = maxHeight * 0.04f)
                .widthIn(min = 100.dp, max = maxWidth * 0.46f)
                .scale(bump.value)
                .background(Color.White, RoundedCornerShape(16.dp))
                .border(3.dp, LINE, RoundedCornerShape(16.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(g.bubble, fontSize = 12.sp, lineHeight = 15.sp, color = LINE)
        }

        if (g.cranking) {
            Box(Modifier.align(Alignment.TopCenter).padding(top = maxHeight * 0.06f)
                .background(LINE, CircleShape).border(3.dp, PEEL, CircleShape).padding(horizontal = 14.dp, vertical = 5.dp)) {
                Text("Crank it! ${g.crankN} / ${g.crankNeed}", color = PEEL, fontWeight = FontWeight.Black, fontSize = 14.sp)
            }
        }
        if (g.partyOn()) {
            Text("PARTY! x3", fontFamily = Lilita, color = RED, fontSize = 18.sp,
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 12.dp))
        }

        for (p in g.pops) key(p.id) { FloatingText(p, maxWidth, maxHeight, 15) }
        for (h in g.hearts) key(h.id) { FloatingText(h, maxWidth, maxHeight, 22) }
    }
}

@Composable
fun FloatingText(p: Pop, w: androidx.compose.ui.unit.Dp, h: androidx.compose.ui.unit.Dp, size: Int) {
    val t = remember { Animatable(0f) }
    LaunchedEffect(p.id) { t.animateTo(1f, tween(p.ms.toInt(), easing = LinearEasing)) }
    Box(Modifier.offset(x = w * p.x - 60.dp, y = h * p.y - 40.dp * t.value).width(120.dp).alpha(1f - t.value * t.value),
        contentAlignment = Alignment.Center) {
        Text(p.text, color = LINE, fontWeight = FontWeight.Black, fontSize = size.sp, textAlign = TextAlign.Center,
            style = TextStyle(drawStyle = Stroke(5f)))
        Text(p.text, color = p.color, fontWeight = FontWeight.Black, fontSize = size.sp, textAlign = TextAlign.Center)
    }
}

// ======================= Mr. Zest's shop =======================
@Composable
fun ShopScreen(g: GameViewModel) {
    val open = remember { mutableStateMapOf(Cat.MONEY to true) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ChunkyButton("Back to lemonade", color = CARD) { g.screen = Screen.STAND }
            Spacer(Modifier.weight(1f))
            MoneyText(g, 26)
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color(0xFFFFE9A8))
                .border(3.dp, LINE, RoundedCornerShape(22.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Canvas(Modifier.size(84.dp).clickable { g.pokeZest() }) {
                scale(size.width / 120f, size.height / 120f, Offset.Zero) { drawZest() }
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("Mr. Zest, the shopkeeper", fontWeight = FontWeight.Black, fontSize = 15.sp)
                Spacer(Modifier.height(4.dp))
                Box(Modifier.background(Color.White, RoundedCornerShape(14.dp)).border(3.dp, LINE, RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text(g.zestSays, fontSize = 13.sp, lineHeight = 16.sp)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTitle("Upgrade shop", 30)
        Text("Tap a category to open it.", fontSize = 14.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        for (cat in Cat.entries) {
            val items = Upgrades.all.filter { it.cat == cat && g.upgradeVisible(it) }
            if (items.isEmpty()) continue
            val isOpen = open[cat] == true
            Column(Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(18.dp)).background(CARD)
                .border(3.dp, LINE, RoundedCornerShape(18.dp))) {
                Row(Modifier.fillMaxWidth().clickable { open[cat] = !isOpen }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(cat.title, fontFamily = Lilita, fontSize = 20.sp)
                        Text(cat.note, fontSize = 13.sp)
                    }
                    Text(if (isOpen) "\u25B2" else "\u25BC", fontSize = 16.sp)
                }
                if (isOpen) {
                    Column(Modifier.padding(start = 10.dp, end = 10.dp, bottom = 10.dp)) {
                        for (u in items) UpgradeCard(
                            name = g.named(u.name),
                            desc = g.upgradeDesc(u),
                            level = g.lv(u.id), max = u.max, cost = u.cost(g.lv(u.id)),
                            canBuy = g.money >= u.cost(g.lv(u.id)),
                            levelText = null,
                        ) { g.buy(u) }
                    }
                }
            }
        }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
fun UpgradeCard(name: String, desc: String, level: Int, max: Int, cost: Long, canBuy: Boolean, levelText: String?, locked: Boolean = false, onBuy: () -> Unit) {
    val maxed = level >= max
    val enabled = !maxed && canBuy && !locked
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).alpha(if (enabled) 1f else 0.55f).clip(shape)
            .background(if (maxed) Color(0xFFE6F4D9) else Color.White).border(3.dp, LINE, shape)
            .clickable(enabled = enabled, onClick = onBuy).padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(name, fontWeight = FontWeight.Black, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text(if (maxed) "Maxed" else fmt(cost), fontWeight = FontWeight.Black, fontSize = 15.sp,
                color = if (maxed) Color(0xFF3E9B4F) else Color(0xFF2E6BD6))
        }
        Text(desc, fontSize = 13.sp, lineHeight = 16.sp)
        Text(levelText ?: "Level $level of $max", fontSize = 12.sp, color = LINE.copy(alpha = 0.7f))
    }
}

/** Mr. Zest: a lemon with a big mustache, drawn in a 120 x 120 box */
fun androidx.compose.ui.graphics.drawscope.DrawScope.drawZest() {
    oval(60f, 66f, 44f, 48f, PEEL)
    oval(60f, 17f, 7f, 5f, PEEL, w = 3f)
    shape("M62 16 C 70 2 88 2 94 10 C 84 18 72 20 62 16 Z", Color(0xFF3E9B4F), w = 3f)
    oval(40f, 74f, 8f, 5f, Color(0xFFFF9A8B), null, alpha = 0.75f)
    oval(80f, 74f, 8f, 5f, Color(0xFFFF9A8B), null, alpha = 0.75f)
    for (x in listOf(47f, 73f)) { drawCircle(Color.White, 9f, Offset(x, 56f)); drawCircle(LINE, 9f, Offset(x, 56f), style = Stroke(3f)); drawCircle(LINE, 4f, Offset(x + 1f, 57f)) }
    shape("M60 74 C 52 66 34 66 28 80 C 40 76 50 80 60 78 C 70 80 80 76 92 80 C 86 66 68 66 60 74 Z", Color(0xFF6B3A1E), w = 3f)
    stroke("M50 90 Q60 98 70 90", w = 3.5f)
    // bow tie
    shape("M60 112 L48 104 L48 120 Z M60 112 L72 104 L72 120 Z", RED, w = 3f)
}
