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
