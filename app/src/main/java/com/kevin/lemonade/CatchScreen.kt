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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

private const val CW = 800f
private const val CGROUND = 468f
private const val BASKET_Y = 432f

private class Lemon(var x: Float, var y: Float, var vx: Float, var vy: Float, var spin: Float, var done: Boolean = false)
private class Helper(var x: Float, val name: String, val hat: Color) {
    var phase = 0f; var hop = 0f; var lookX = 400f; var lookY = 200f; var face = 1f
}
private class Floater(val x: Float, var y: Float, var life: Float, val two: Boolean)
private class Splat(val x: Float, var life: Float)

/** Catch the Lemons, ported from the original's canvas game. Same numbers. */
private class CatchSim(val g: GameViewModel) {
    var basketX = 400f
    var basketPile = 0
    val falling = ArrayList<Lemon>()
    val splats = ArrayList<Splat>()
    val helpers = ArrayList<Helper>()
    val floaters = ArrayList<Floater>()
    var toDrop = 0; var dropTimer = 0f; var treeShake = 0f; var tapShake = 0f; var roundOn = false
    var banner by mutableStateOf("Tap the tree to shake it!")
    var bannerT = 3f
    var bannerOn by mutableStateOf(true)
    var autoShakeIn = -1f
    val hanging = List(22) {
        val a = Random.nextFloat() * 2 * PI.toFloat(); val r = sqrt(Random.nextFloat())
        Offset(400 + cos(a) * 170 * r, 150 + sin(a) * 85 * r)
    }

    fun basketW() = 80f + 22 * g.llv("basket")
    fun helperSpeed() = 130f + 45 * g.llv("hspeed")
    fun msg(t: String, secs: Float = 1.6f) { banner = t; bannerT = secs; bannerOn = true }

    fun syncHelpers() {
        val crew = listOf("Lou" to 0xFFE8483B, "Lily" to 0xFF7B5CFF, "Leo" to 0xFF2E9BD6, "Lola" to 0xFFFF7FB0)
        while (helpers.size < g.llv("helper")) {
            val (n, c) = crew[helpers.size]
            helpers.add(Helper(150f + helpers.size * 160, n, Color(c)))
        }
    }

    fun dropLemon() {
        falling.add(Lemon(230 + Random.nextFloat() * 340, 170 + Random.nextFloat() * 60,
            (Random.nextFloat() - 0.5f) * 130, 20 + Random.nextFloat() * 40, Random.nextFloat() * 6))
    }

    fun startRound() {
        if (g.lemonsLeft >= g.lemonCap()) { msg("Your lemon storage is full!"); return }
        roundOn = true; toDrop = 12 + 6 * g.llv("shake") + 4 * g.llv("helper"); dropTimer = 0f; treeShake = 0.6f
        msg("Catch them!", 1.2f)
    }

    fun caught(by: Float): Boolean {
        if (g.lemonsLeft >= g.lemonCap()) return false
        val two = g.llv("double") > 0 && Random.nextDouble() < 0.12 * g.llv("double")
        g.addLemons(if (two) 2 else 1)
        if (abs(by - basketX) < 1) basketPile = min(6, basketPile + 1)
        floaters.add(Floater(by, BASKET_Y - 20, 0.8f, two))
        if (g.lemonsLeft >= g.lemonCap()) { toDrop = 0; msg("Storage full! Go squeeze some lemons.", 2.2f) }
        return true
    }

    fun tap(p: Offset) {
        val onTree = hypot(p.x - 400, (p.y - 165) * 1.6f) < 230 || (abs(p.x - 402) < 30 && p.y > 200 && p.y < CGROUND)
        if (onTree) {
            if (!roundOn) startRound()
            else if (tapShake <= 0 && g.lemonsLeft < g.lemonCap()) { tapShake = 0.35f; dropLemon(); dropLemon() }
        }
        move(p.x)
    }
    fun move(x: Float) { basketX = x.coerceIn(40f, CW - 40) }

    private fun landX(l: Lemon): Float {
        val gr = 420f
        val t = (-l.vy + sqrt(l.vy * l.vy + 2 * gr * max(0f, BASKET_Y - l.y))) / gr
        return (l.x + l.vx * t).coerceIn(20f, CW - 20)
    }

    fun update(dt: Float) {
        if (bannerT > 0) { bannerT -= dt; if (bannerT <= 0) bannerOn = false }
        if (treeShake > 0) treeShake -= dt
        if (tapShake > 0) tapShake -= dt
        if (autoShakeIn > 0) { autoShakeIn -= dt; if (autoShakeIn <= 0 && !roundOn) startRound() }
        if (roundOn) {
            if (toDrop > 0) { dropTimer -= dt; if (dropTimer <= 0) { dropLemon(); toDrop--; dropTimer = 0.25f + Random.nextFloat() * 0.45f } }
            if (toDrop == 0 && falling.isEmpty()) {
                roundOn = false; basketPile = 0
                if (g.llv("autoshake") > 0 && g.lemonsLeft < g.lemonCap()) autoShakeIn = 1.5f
                msg(if (g.lemonsLeft >= g.lemonCap()) "Storage full!" else "Tap the tree to shake it again!", 2.5f)
            }
        }
        val bw = basketW()
        for (l in falling) {
            if (g.llv("magnet") > 0 && l.y > 250) l.x += sign(basketX - l.x) * min(abs(basketX - l.x), 40f * g.llv("magnet") * dt)
            l.vy += 420 * dt; l.y += l.vy * dt; l.x += l.vx * dt; l.spin += dt * 4
            if (l.x < 20 || l.x > CW - 20) l.vx *= -1
            if (!l.done && l.y >= BASKET_Y - 8 && l.y <= BASKET_Y + 14) {
                if (abs(l.x - basketX) < bw / 2) { l.done = true; caught(basketX); continue }
                for (hp in helpers) if (abs(l.x - hp.x) < 30) { l.done = true; caught(hp.x); hp.hop = 0.3f; break }
            }
            if (!l.done && l.y > CGROUND) { l.done = true; splats.add(Splat(l.x, 1.2f)) }
        }
        falling.removeAll { it.done }
        // each helper guards their own lane under the tree
        val n = helpers.size; val l0 = 170f; val l1 = 630f
        helpers.forEachIndexed { i, hp ->
            val lo = l0 + (l1 - l0) * i / n; val hi = l0 + (l1 - l0) * (i + 1) / n
            var best: Lemon? = null
            for (l in falling) {
                val lx = landX(l)
                if (lx < lo - 20 || lx > hi + 20) continue
                if (best == null || l.y > best.y) best = l
            }
            val goal = best?.let { landX(it) } ?: ((lo + hi) / 2)
            val d = goal - hp.x
            val step = sign(d) * min(abs(d), helperSpeed() * dt * (if (best != null) 1f else 0.5f))
            hp.x += step; hp.phase += abs(step) * 0.12f
            if (abs(step) > 0.2f) hp.face = sign(step)
            if (hp.hop > 0) hp.hop -= dt
            hp.lookX = best?.x ?: (hp.x + hp.face * 60); hp.lookY = best?.y ?: 300f
        }
        splats.forEach { it.life -= dt }; splats.removeAll { it.life <= 0 }
        floaters.forEach { it.y -= 50 * dt; it.life -= dt }; floaters.removeAll { it.life <= 0 }
    }
}

@Composable
fun CatchScreen(g: GameViewModel) {
    val sim = remember { CatchSim(g).apply { if (g.lemonsLeft >= g.lemonCap()) msg("Your lemon storage is full!", 3f) } }
    var frame by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) withFrameNanos { now ->
            val dt = if (last == 0L) 0f else min(0.05f, (now - last) / 1e9f)
            last = now
            sim.syncHelpers()
            sim.update(dt)
            frame = now
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            ChunkyButton("Back to lemonade", color = CARD) { g.screen = Screen.STAND }
            Spacer(Modifier.weight(1f))
            MoneyText(g, 26)
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTitle("Catch the Lemons", 30, Color(0xFF8FCB5A))
        Text("Lemons: ${g.lemonsLeft} / ${g.lemonCap()}", fontWeight = FontWeight.Black, fontSize = 17.sp)
        Spacer(Modifier.height(8.dp))
        val shape = RoundedCornerShape(24.dp)
        Box(Modifier.fillMaxWidth().background(LINE, shape).padding(bottom = 6.dp).aspectRatio(800f / 500f).clip(shape).border(4.dp, LINE, shape)) {
            Canvas(Modifier.fillMaxSize().pointerInput(Unit) {
                awaitEachGesture {
                    val sc = size.width / 800f
                    val down = awaitFirstDown()
                    down.consume()
                    sim.tap(down.position / sc)
                    while (true) {
                        val ev = awaitPointerEvent()
                        val c = ev.changes.firstOrNull() ?: break
                        c.consume()
                        sim.move(c.position.x / sc)
                        if (!c.pressed) break
                    }
                }
            }) {
                frame.let { }
                scale(size.width / 800f, size.width / 800f, Offset.Zero) { drawCatch(sim) }
            }
            if (sim.bannerOn) {
                Box(Modifier.align(Alignment.TopCenter).padding(top = 10.dp).background(LINE, CircleShape)
                    .padding(horizontal = 14.dp, vertical = 5.dp)) {
                    Text(sim.banner, color = PEEL, fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
            }
        }
        Text("Tap the tree to shake it. Drag to move your basket.", fontSize = 13.sp, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp))
        Spacer(Modifier.height(10.dp))
        Text("Lemon tree shop", fontFamily = Lilita, fontSize = 22.sp)
        for (u in LemonUpgrades.all) {
            val level = g.llv(u.id)
            val locked = u.id == "hspeed" && g.llv("helper") == 0
            UpgradeCard(
                name = u.name,
                desc = if (locked) "Hire a helper first" else u.desc,
                level = level, max = u.max, cost = u.cost(level), canBuy = g.money >= u.cost(level),
                levelText = when (u.id) {
                    "cap" -> "Holds ${g.lemonCap()} lemons (max ${20 + 20 * u.max})"
                    "helper" -> "Helpers: $level of ${u.max}"
                    else -> null
                },
                locked = locked,
            ) { g.buyLemon(u); if (u.id == "helper") sim.msg("A helper joins!", 1.2f) }
        }
        Spacer(Modifier.height(30.dp))
    }
}

private fun DrawScope.lemonShape(x: Float, y: Float, r: Float, rot: Float) {
    translate(x, y) {
        rotate(rot * 180f / PI.toFloat(), Offset.Zero) {
            val br = Brush.radialGradient(0f to Color(0xFFFFF6B8), 0.45f to Color(0xFFFFD21F), 1f to Color(0xFFE3A600),
                center = Offset(-r * 0.4f, -r * 0.4f), radius = r * 1.3f)
            drawOval(br, Offset(-r * 1.25f, -r), Size(r * 2.5f, r * 2))
            drawOval(Color(0xFF6B4A00), Offset(-r * 1.25f, -r), Size(r * 2.5f, r * 2), style = Stroke(1.8f))
            for ((cx, rx, ry) in listOf(Triple(r * 1.3f, 3f, 2.5f), Triple(-r * 1.3f, 2.5f, 2f))) {
                drawOval(Color(0xFFF2C200), Offset(cx - rx, -ry), Size(rx * 2, ry * 2))
                drawOval(Color(0xFF6B4A00), Offset(cx - rx, -ry), Size(rx * 2, ry * 2), style = Stroke(1.8f))
            }
        }
    }
}

private fun DrawScope.cShadow(x: Float, y: Float, w: Float, a: Float) =
    drawOval(Color.Black.copy(alpha = a), Offset(x - w, y - w * 0.22f), Size(w * 2, w * 0.44f))

private fun DrawScope.basket(x: Float, w: Float, pile: Int, handle: Boolean) {
    val top = BASKET_Y - 8; val bot = BASKET_Y + 22
    if (handle) drawArc(Color(0xFF7A4A1E), 180f, 180f, false, Offset(x - w / 2 + 6, top - 26), Size(w - 12, 52f), style = Stroke(5f))
    for (i in 0 until pile) lemonShape(x - w / 2 + 16 + (i * (w - 32)) / max(1, pile - 1), top - 3 - (i % 2) * 4, 7f, 0.4f * (i % 3 - 1))
    val body = Path().apply {
        moveTo(x - w / 2, top); lineTo(x + w / 2, top); lineTo(x + w / 2 - 10, bot)
        quadraticBezierTo(x, bot + 6, x - w / 2 + 10, bot); close()
    }
    drawPath(body, Brush.verticalGradient(listOf(Color(0xFFD9A066), Color(0xFF9C6533)), startY = top, endY = bot))
    var yy = top + 7
    while (yy < bot) { drawLine(Color(0x735A3A18), Offset(x - w / 2 + 6, yy), Offset(x + w / 2 - 6, yy), 2f); yy += 7 }
    drawPath(body, Color(0xFF5A3A18), style = Stroke(3f))
    drawRoundRect(Color(0xFFC08A4E), Offset(x - w / 2 - 3, top - 4), Size(w + 6, 8f), androidx.compose.ui.geometry.CornerRadius(4f, 4f))
    drawRoundRect(Color(0xFF5A3A18), Offset(x - w / 2 - 3, top - 4), Size(w + 6, 8f), androidx.compose.ui.geometry.CornerRadius(4f, 4f), style = Stroke(3f))
}

private val labelPaint = android.graphics.Paint().apply {
    isAntiAlias = true; textAlign = android.graphics.Paint.Align.CENTER; isFakeBoldText = true
}
private fun DrawScope.label(text: String, x: Float, y: Float, size: Float, fill: Int, outline: Int) {
    val c = drawContext.canvas.nativeCanvas
    labelPaint.textSize = size
    labelPaint.style = android.graphics.Paint.Style.STROKE; labelPaint.strokeWidth = 4f; labelPaint.color = outline
    c.drawText(text, x, y, labelPaint)
    labelPaint.style = android.graphics.Paint.Style.FILL; labelPaint.color = fill
    c.drawText(text, x, y, labelPaint)
}

private fun DrawScope.drawHelper(hp: Helper) {
    val x = hp.x
    val hopY = if (hp.hop > 0) -sin((hp.hop / 0.3f) * PI.toFloat()) * 10 else 0f
    val bodyY = BASKET_Y - 50 + hopY; val swing = sin(hp.phase) * 8
    cShadow(x, CGROUND + 1, 32f, 0.2f)
    for ((lx, sw) in listOf(-9f to swing, 9f to -swing)) {
        drawLine(LINE, Offset(x + lx, bodyY + 24), Offset(x + lx + sw, CGROUND - 5), 4.5f, StrokeCap.Round)
        oval(x + lx + sw + hp.face * 3, CGROUND - 3, 8f, 4.5f, Color.White, w = 2.5f)
    }
    val br = Brush.radialGradient(0f to Color(0xFFFFF6B8), 0.5f to Color(0xFFFFD21F), 1f to Color(0xFFE3A600), center = Offset(x - 9, bodyY - 12), radius = 32f)
    drawOval(br, Offset(x - 23, bodyY - 28), Size(46f, 56f)); drawOval(LINE, Offset(x - 23, bodyY - 28), Size(46f, 56f), style = Stroke(3f))
    oval(x - 14, bodyY + 3, 5f, 3.2f, Color(0x8CFF8C82), null); oval(x + 14, bodyY + 3, 5f, 3.2f, Color(0x8CFF8C82), null)
    val dx = hp.lookX - x; val dy = hp.lookY - bodyY; val d = hypot(dx, dy).let { if (it == 0f) 1f else it }
    for (ex in listOf(-8f, 8f)) {
        drawCircle(Color.White, 6.5f, Offset(x + ex, bodyY - 7)); drawCircle(LINE, 6.5f, Offset(x + ex, bodyY - 7), style = Stroke(2f))
        drawCircle(LINE, 3f, Offset(x + ex + dx / d * 2.8f, bodyY - 7 + dy / d * 2.8f))
    }
    if (hp.hop > 0) oval(x, bodyY + 8, 4.5f, 5f, Color(0xFF8C2F2F), w = 2.5f)
    else drawArc(LINE, 14f, 152f, false, Offset(x - 7, bodyY - 4), Size(14f, 14f), style = Stroke(2.5f))
    // straw sun hat with a colored band
    val hy = bodyY - 24
    oval(x, hy, 30f, 7f, Color(0xFFF3D9A0), w = 2.5f)
    drawArc(Color(0xFFF3D9A0), 180f, 180f, true, Offset(x - 15, hy - 15), Size(30f, 26f))
    drawArc(LINE, 180f, 180f, false, Offset(x - 15, hy - 15), Size(30f, 26f), style = Stroke(2.5f))
    drawRect(hp.hat, Offset(x - 15, hy - 7), Size(30f, 5f))
    label(hp.name, x, hy - 20, 15f, 0xFF3A2E12.toInt(), 0xF2FFFFFF.toInt())
    translate(0f, hopY) { basket(x, 60f, 0, false) }
    drawPath(Path().apply { moveTo(x - 21, bodyY + 6); quadraticBezierTo(x - 32, bodyY + 22, x - 27, BASKET_Y - 10 + hopY)
        moveTo(x + 21, bodyY + 6); quadraticBezierTo(x + 32, bodyY + 22, x + 27, BASKET_Y - 10 + hopY) },
        LINE, style = Stroke(4f, cap = StrokeCap.Round))
}

private fun DrawScope.drawCatch(sim: CatchSim) {
    val now = System.currentTimeMillis() % 1_000_000L
    drawRect(Brush.verticalGradient(0f to Color(0xFF7CC7F2), 0.7f to Color(0xFFCFEFFF), 1f to Color(0xFFF2FBFF), startY = 0f, endY = 500f), Offset.Zero, Size(800f, 500f))
    drawCircle(Brush.radialGradient(0f to Color(0xFFFFF096), 0.45f to Color(0xE6FFE15A), 1f to Color(0x00FFE15A), center = Offset(80f, 70f), radius = 80f), 80f, Offset(80f, 70f))
    for ((cx0, cy, s, v) in listOf(listOf(90f, 70f, 1f, 8f), listOf(560f, 50f, 0.8f, 6f), listOf(700f, 120f, 0.6f, 10f))) {
        val x = ((cx0 + now / 1000f * v) % (CW + 160)) - 80
        oval(x, cy, 50 * s, 18 * s, Color(0xF2FFFFFF), null); oval(x + 26 * s, cy - 12 * s, 30 * s, 20 * s, Color(0xF2FFFFFF), null)
        oval(x - 24 * s, cy - 6 * s, 22 * s, 14 * s, Color(0xF2FFFFFF), null)
    }
    drawPath(svg("M0 400 Q160 330 330 395 Q560 320 800 390 L800 468 L0 468 Z"), Color(0xFF9FD68B))
    drawPath(svg("M0 430 Q250 380 520 430 Q680 400 800 425 L800 468 L0 468 Z"), Color(0xFF86C774))
    drawRect(Brush.verticalGradient(listOf(Color(0xFF6DBA58), Color(0xFF4E9440)), startY = CGROUND, endY = 500f), Offset(0f, CGROUND), Size(800f, 32f))

    val sh = if (sim.treeShake > 0 || sim.tapShake > 0) sin(now / 25f) * 6 else sin(now / 900f) * 1.2f
    cShadow(402f, CGROUND + 2, 120f, 0.15f)
    val trunk = Path().apply {
        moveTo(360f, CGROUND); quadraticBezierTo(380f, CGROUND - 20, 385 + sh * .3f, 200f); lineTo(417 + sh * .3f, 200f)
        quadraticBezierTo(425f, CGROUND - 20, 446f, CGROUND); close()
    }
    drawPath(trunk, Brush.horizontalGradient(0f to Color(0xFF6E4220), 0.5f to Color(0xFF9A6334), 1f to Color(0xFF5E3818), startX = 370f, endX = 435f))
    drawPath(trunk, LINE, style = Stroke(4f))
    translate(sh, 0f) {
        val blobs = listOf(Triple(300f, 165f, 80f), Triple(400f, 110f, 95f), Triple(500f, 165f, 80f), Triple(350f, 215f, 70f), Triple(450f, 215f, 70f), Triple(400f, 180f, 90f))
        for ((x, y, r) in blobs) { drawCircle(Color(0xFF2F7A3C), r, Offset(x, y)); drawCircle(Color(0xFF1F4F28), r, Offset(x, y), style = Stroke(4f)) }
        for ((x, y, r) in blobs) drawCircle(Brush.radialGradient(0f to Color(0xFF6FCB6A), 0.7f to Color(0xFF3E9B4F), 1f to Color(0xFF3A9049),
            center = Offset(x - r * 0.35f, y - r * 0.4f), radius = r), r - 3, Offset(x, y))
        val show = max(3, round(sim.hanging.size * max(0.15f, 1 - sim.g.lemonsLeft.toFloat() / sim.g.lemonCap())).toInt())
        for (i in 0 until min(show, sim.hanging.size)) {
            val h = sim.hanging[i]
            drawLine(Color(0xFF2F5A1E), Offset(h.x, h.y - 14), Offset(h.x, h.y - 8), 1.5f)
            lemonShape(h.x, h.y, 9f, 0.25f + sin(now / 700f + i) * 0.08f)
        }
    }
    for (s in sim.splats) {
        val a = min(1f, s.life)
        drawOval(Color(0xFFFFE94A).copy(alpha = a), Offset(s.x - 20, CGROUND - 2), Size(40f, 10f))
        for (k in 0 until 5) drawCircle(Color(0xFFFFE94A).copy(alpha = a), 2.5f, Offset(s.x + cos(k * 1.3f) * 22, CGROUND - 2 - abs(sin(k * 2.1f)) * 6))
    }
    for (l in sim.falling) cShadow(l.x, CGROUND + 2, 6 + (l.y / CGROUND) * 10, 0.08f + (l.y / CGROUND) * 0.15f)
    for (l in sim.falling) lemonShape(l.x, l.y, 11f, l.spin)
    for (hp in sim.helpers) drawHelper(hp)
    cShadow(sim.basketX, CGROUND + 2, sim.basketW() / 2 + 6, 0.2f)
    basket(sim.basketX, sim.basketW(), sim.basketPile, true)
    for (f in sim.floaters) label(if (f.two) "+2!" else "+1", f.x, f.y, 24f, 0xFFFFE66B.toInt(), 0xFF1D3A12.toInt())
}
