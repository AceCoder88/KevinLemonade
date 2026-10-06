package com.kevin.lemonade

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// Everything here is drawn in the original SVG's 800 x 500 coordinates.
val LINE = Color(0xFF3A2E12)
val RED = Color(0xFFE8483B)
val GRASS = Color(0xFF7CC56A)
val SKY = Color(0xFFA8DDF7)

private val pathCache = HashMap<String, Path>()
/** turns an SVG path string from the original game into a Compose Path */
fun svg(d: String): Path = pathCache.getOrPut(d) { PathParser().parsePathString(d).toPath() }

fun DrawScope.stroke(d: String, color: Color = LINE, w: Float = 4f, cap: StrokeCap = StrokeCap.Round) =
    drawPath(svg(d), color, style = Stroke(w, cap = cap, join = StrokeJoin.Round))

fun DrawScope.shape(d: String, fill: Brush, line: Color? = LINE, w: Float = 4f) {
    drawPath(svg(d), fill)
    if (line != null) drawPath(svg(d), line, style = Stroke(w, join = StrokeJoin.Round, cap = StrokeCap.Round))
}
fun DrawScope.shape(d: String, fill: Color, line: Color? = LINE, w: Float = 4f) = shape(d, SolidColor(fill), line, w)

fun DrawScope.oval(cx: Float, cy: Float, rx: Float, ry: Float, fill: Color, line: Color? = LINE, w: Float = 4f, alpha: Float = 1f) {
    drawOval(fill, Offset(cx - rx, cy - ry), Size(rx * 2, ry * 2), alpha = alpha)
    if (line != null) drawOval(line, Offset(cx - rx, cy - ry), Size(rx * 2, ry * 2), style = Stroke(w))
}
fun DrawScope.box(x: Float, y: Float, w: Float, h: Float, fill: Brush, line: Color? = LINE, sw: Float = 4f, r: Float = 0f) {
    val cr = androidx.compose.ui.geometry.CornerRadius(r, r)
    drawRoundRect(fill, Offset(x, y), Size(w, h), cr)
    if (line != null) drawRoundRect(line, Offset(x, y), Size(w, h), cr, style = Stroke(sw))
}

/** the squeezer's metal turns gold, then diamond, then ruby (the "Squeezer upgrade") */
private val METAL_LOOKS = listOf(
    listOf(0xFFF4F7F9, 0xFFC9D3DA, 0xFFE8EEF2, 0xFF9AA7B1) to listOf(0xFFA9B5BE, 0xFF7F8E99, 0xFF5E6B75),
    listOf(0xFFFFF4C2, 0xFFE9B630, 0xFFFFE07A, 0xFFB8860B) to listOf(0xFFE3B341, 0xFFB8860B, 0xFF8A6508),
    listOf(0xFFF2FFFF, 0xFF9FE8FF, 0xFFE6FCFF, 0xFF5FC4E8) to listOf(0xFFA8E6F5, 0xFF5FB8D6, 0xFF3A8FB0),
)
private fun metal(level: Int, x0: Float, x1: Float): Brush {
    val c = METAL_LOOKS[min(level, 2)].first.map { Color(it) }
    return Brush.horizontalGradient(0f to c[0], 0.45f to c[1], 0.55f to c[2], 1f to c[3], startX = x0, endX = x1)
}
private fun metalDark(level: Int, x0: Float, x1: Float): Brush {
    val c = METAL_LOOKS[min(level, 2)].second.map { Color(it) }
    return Brush.horizontalGradient(0f to c[0], 0.5f to c[1], 1f to c[2], startX = x0, endX = x1)
}
private val WOOD = Brush.verticalGradient(listOf(Color(0xFFE0A86E), Color(0xFFB07338)), startY = 400f, endY = 460f)

/** day/night cycle: 4 minutes per loop, 0 = full day, 1 = dead of night (the original's nightNow) */
const val DAY_LEN = 240000L
fun nightFrac(timeMs: Long): Float {
    val ph = (timeMs % DAY_LEN).toFloat() / DAY_LEN
    return max(0f, sin(ph * 2 * PI.toFloat() - PI.toFloat() / 2))
}
/** cloud positions on the 800x500 stage: cx, cy, scale */
val CLOUD_SPOTS = listOf(Triple(140f, 70f, 1f), Triple(520f, 52f, 0.8f), Triple(330f, 100f, 0.65f))

fun DrawScope.drawStage(g: GameViewModel, timeMs: Long) {
    val press = g.lv("press")
    val inRealm = g.inRealm()
    val night = if (g.og || inRealm) 0f else nightFrac(timeMs)

    // sky
    val skyColor = if (inRealm) Color(0xFF2A1745) else SKY
    drawRect(skyColor, Offset.Zero, Size(800f, 500f))
    if (!inRealm) {
        drawRect(Brush.verticalGradient(0f to Color.White.copy(alpha = 0f), 0.75f to Color.White.copy(alpha = 0.35f),
            1f to Color.White.copy(alpha = 0.55f), startY = 0f, endY = 450f), Offset.Zero, Size(800f, 450f))
        if (night > 0.02f) drawNightSky(night)
        // drifting clouds (tap one to make it rain a lemon)
        val drift = if (g.og) 0f else (timeMs / 1000f)
        for ((i, c) in CLOUD_SPOTS.withIndex()) {
            val x = (c.first + drift * (6f + 3 * i)) % 960f - 80f
            val s = c.third
            val cc = if (g.cute) Color(0xFFFFE6F4) else Color.White
            oval(x, c.second, 46f * s, 18f * s, cc, null)
            oval(x + 30f * s, c.second - 12f * s, 30f * s, 20f * s, cc, null)
            oval(x - 22f * s, c.second - 8f * s, 22f * s, 14f * s, cc, null)
        }
        drawPath(svg("M0 452 Q120 380 260 440 T560 430 T800 420 L800 452 Z"), GRASS.copy(alpha = 0.55f))
        drawSun(g, night)
        if (night > 0.5f && !g.og) drawFireflies(timeMs, (night - 0.5f) * 2f)
    }

    // grass, tufts, flowers (hidden in both realms, like the original)
    drawRect(if (inRealm) Color(0xFF3E2A5C) else GRASS, Offset(0f, 450f), Size(800f, 50f))
    drawRect(Color.Black.copy(alpha = 0.08f), Offset(0f, 472f), Size(800f, 28f))
    if (!inRealm) {
        val tuft = Color.Black.copy(alpha = 0.18f)
        for (d in listOf("M30 452 l-4 -10 M34 452 l0 -13 M38 452 l4 -10", "M250 455 l-4 -9 M254 455 l0 -12 M258 455 l4 -9",
            "M720 454 l-4 -10 M724 454 l0 -13 M728 454 l4 -10", "M470 470 l-3 -8 M473 470 l0 -10 M476 470 l3 -8", "M95 478 l-3 -8 M98 478 l0 -10 M101 478 l3 -8"))
            stroke(d, tuft, 2.5f)
        for ((x, y, c) in listOf(Triple(60f, 470f, Color.White), Triple(290f, 482f, Color(0xFFFF9AC1)), Triple(760f, 476f, Color.White), Triple(610f, 488f, Color(0xFFB79CFF)))) {
            drawCircle(c, 4f, Offset(x, y)); drawCircle(Color(0xFFFFD21F), 1.8f, Offset(x, y))
        }
    }

    // table
    oval(500f, 460f, 215f, 9f, Color.Black, null, alpha = 0.16f)
    box(320f, 418f, 14f, 40f, WOOD); box(666f, 418f, 14f, 40f, WOOD)
    box(300f, 400f, 400f, 18f, WOOD, r = 4f)
    stroke("M308 405 H692", Color.White.copy(alpha = 0.35f), 3f)

    if (g.lv("puppy") > 0 && !inRealm) drawPuppy(timeMs)
    if (g.lv("shovel") > 0 && !inRealm) drawDigHole()
    if (g.dating.momoWith && !inRealm) drawMomo(g)

    if (g.realm.active) drawDeathRealm(g, timeMs)
    if (g.realm.nightmare) drawNightmareRealm(g, timeMs)

    // the press
    box(354f, 130f, 16f, 242f, metalDark(press, 354f, 370f))
    box(350f, 370f, 140f, 30f, metalDark(press, 350f, 490f), r = 6f)
    box(405f, 290f, 30f, 82f, metal(press, 405f, 435f))
    box(354f, 126f, 90f, 16f, metalDark(press, 354f, 444f), r = 4f)
    if (g.lv("motor") == 0) {
        rotate(g.crankAngle, Offset(340f, 230f)) {
            drawCircle(metalDark(press, 318f, 362f), 22f, Offset(340f, 230f))
            drawCircle(LINE, 22f, Offset(340f, 230f), style = Stroke(4f))
            stroke("M340 208 V252 M318 230 H362", w = 3f)
            stroke("M340 230 L314 206", w = 6f)
            drawCircle(RED, 7f, Offset(312f, 204f)); drawCircle(LINE, 7f, Offset(312f, 204f), style = Stroke(4f))
        }
    } else {
        box(318f, 210f, 44f, 40f, SolidColor(Color(0xFF2E6BD6)), sw = 3f, r = 6f)
        shape("M344 216 L332 232 H342 L336 246 L352 226 H342 L348 216 Z", Color(0xFFFFD21F), w = 2f)
        drawCircle(Color(0xFF8FCB5A), 3f, Offset(324f, 244f))
    }
    // plunger
    translate(0f, g.plungerY) {
        box(413f, 140f, 14f, 34f, metal(press, 413f, 427f))
        box(385f, 170f, 70f, 14f, metal(press, 385f, 455f), r = 5f)
        drawCircle(RED, 12f, Offset(420f, 116f)); drawCircle(LINE, 12f, Offset(420f, 116f), style = Stroke(4f))
        drawCircle(Color.White.copy(alpha = 0.6f), 4f, Offset(416f, 112f))
        box(415f, 118f, 10f, 24f, metal(press, 415f, 425f))
    }
    // the falling lemon (one of Kevin's relatives)
    if (g.dropVisible) {
        translate(0f, g.dropY) {
            scale(g.dropSx, g.dropSy, Offset(420f, 238f)) {
                g.dropGlow?.let { drawCircle(it.copy(alpha = 0.6f), 38f, Offset(420f, 218f)) }
                oval(420f, 218f, 26f, 20f, g.dropColor)
                oval(392f, 218f, 5f, 4f, g.dropColor, w = 3f)
                oval(448f, 218f, 5f, 4f, g.dropColor, w = 3f)
                drawOval(fruitShade(420f, 218f, 24f, 18f), Offset(396f, 200f), Size(48f, 36f))
            }
        }
    }
    // bowl + spout, drawn over the lemon so it sits inside
    shape("M456 262 L524 279 L521 291 L450 282 Z", metal(press, 450f, 524f))
    shape("M368 232 L472 232 L456 292 L384 292 Z", metal(press, 368f, 472f))
    if (g.splash > 0f) {
        val t = g.splash
        for (k in 0 until 7) {
            val a = -Math.PI * (0.1 + 0.8 * k / 6.0)
            val dist = 10f + 50f * t
            drawCircle(g.splashColor.copy(alpha = 1f - t), 5f * (1 - t * 0.5f),
                Offset(420f + (kotlin.math.cos(a) * dist).toFloat(), 236f + (kotlin.math.sin(a) * dist).toFloat() + 40f * t * t))
        }
    }
    if (g.streamOn) drawRoundRect(GameViewModel.JUICE, Offset(514f, 289f), Size(8f, 108f), androidx.compose.ui.geometry.CornerRadius(4f, 4f))

    // pitcher
    val pitcher = svg("M505 300 L615 300 L608 400 L512 400 Z")
    clipPath(pitcher) { drawRect(GameViewModel.JUICE, Offset(500f, 400f - g.juiceLevel), Size(120f, g.juiceLevel)) }
    drawPath(pitcher, Brush.horizontalGradient(0f to Color(0x8CFFFFFF), 0.35f to Color(0x26FFFFFF), 1f to Color(0x59FFFFFF), startX = 505f, endX = 615f))
    drawPath(pitcher, LINE, style = Stroke(4f, join = StrokeJoin.Round))
    stroke("M598 316 L594 360", Color.White.copy(alpha = 0.45f), 3f)
    stroke("M613 318 C 662 318 662 382 609 382", w = 7f)
    stroke("M500 298 L620 298", w = 5f)
    stroke("M522 318 L526 380", Color.White.copy(alpha = 0.7f), 5f)

    if (!g.realm.nightmare && !g.realm.kevinGone) drawKevinWithPotions(g, timeMs, if (g.realm.active) 0.75f else 1f)

    // the "don't hold me" ring
    if (g.holdProgress > 0f) {
        drawArc(RED, -90f, 360f * g.holdProgress, false, Offset(42f, 172f), Size(256f, 256f), style = Stroke(7f, cap = StrokeCap.Round))
    }
    // holding dark Kevin, in the Nightmare Realm
    if (g.realm.darkProgress > 0f) {
        drawArc(Color(0xFFB79CFF), -90f, 360f * g.realm.darkProgress, false, Offset(42f, 172f), Size(256f, 256f), style = Stroke(7f, cap = StrokeCap.Round))
    }
}

private fun fruitShade(cx: Float, cy: Float, rx: Float, ry: Float) = Brush.radialGradient(
    0f to Color(0x8CFFFFFF), 0.35f to Color(0x00FFFFFF), 0.8f to Color(0x1F6B4A00), 1f to Color(0x4D6B4A00),
    center = Offset(cx - rx + 0.7f * rx, cy - ry + 0.6f * ry), radius = 1.5f * maxOf(rx, ry)
)

/** draws [block] at reduced opacity (ghosty, in the Death Realm) */
private fun DrawScope.withAlpha(alpha: Float, block: DrawScope.() -> Unit) {
    if (alpha >= 1f) { block(); return }
    drawIntoCanvas { canvas ->
        val paint = androidx.compose.ui.graphics.Paint().apply { this.alpha = alpha }
        canvas.saveLayer(androidx.compose.ui.geometry.Rect(Offset.Zero, size), paint)
        block()
        canvas.restore()
    }
}

private val RAINBOW = listOf(0xFFFF5A5A, 0xFFFF9A3C, 0xFFFFE14A, 0xFF5CD65C, 0xFF4AA8FF, 0xFFA06BFF, 0xFFFF5A5A).map { Color(it) }
private fun rainbowAt(t: Float): Color {
    val seg = (t - floor(t)) * (RAINBOW.size - 1)
    val i = seg.toInt().coerceAtMost(RAINBOW.size - 2)
    return lerp(RAINBOW[i], RAINBOW[i + 1], seg - i)
}

/** Kevin plus the Wizard Tower's potions: giant/shrinking, invisible, bubble, rainbow, heart eyes */
private fun DrawScope.drawKevinWithPotions(g: GameViewModel, timeMs: Long, alpha: Float) {
    val now = g.now()
    val t = g.tower
    val k = t.kevinScale()
    val bubble = now < t.bubbleUntil
    val lift = if (bubble) -40f + 8f * sin(timeMs / 400f) else 0f
    val a = alpha * (if (now < t.invisibleUntil) 0.12f else 1f)
    translate(0f, lift) {
        scale(k, k, Offset(170f, 458f)) {
            drawKevin(g, a, if (now < t.rainbowKevinUntil) timeMs / 2400f else null, now < t.heartEyesUntil)
            if (bubble) {
                drawCircle(Color(0xFFBFE9FF).copy(alpha = 0.25f), 150f, Offset(170f, 320f))
                drawCircle(Color.White.copy(alpha = 0.8f), 150f, Offset(170f, 320f), style = Stroke(4f))
                stroke("M90 230 C 100 205 120 190 140 185", Color.White.copy(alpha = 0.9f), 6f)
            }
        }
    }
}

private fun DrawScope.drawKevin(g: GameViewModel, alpha: Float = 1f, rainbow: Float? = null, heartEyes: Boolean = false) = withAlpha(alpha) {
    val s = g.sadFrac().toFloat()
    val peel = rainbow?.let { rainbowAt(it) } ?: lerp(Color(0xFFFFD21F), Color(0xFFB9B5A0), s)
    val leaf = rainbow?.let { rainbowAt(1f - it) } ?: lerp(Color(0xFF3E9B4F), Color(0xFF7C7F6E), s)

    oval(170f, 458f, 78f, 10f, Color.Black, null, alpha = 0.16f)
    stroke("M148 398 L142 448", w = 5f); stroke("M192 398 L198 448", w = 5f)
    oval(136f, 452f, 18f, 9f, RED); oval(204f, 452f, 18f, 9f, RED)
    drawOutfit(g.closet, "feet")

    val droop = if (g.sleeping) 6f else if (g.sleepy) 3f else 0f
    translate(g.kevinDx, g.kevinDy) {
        rotate(g.kevinRot + droop, Offset(170f, 400f)) {
            stroke("M78 300 L44 340", w = 6f)
            rotate(g.armRot, Offset(262f, 300f)) { stroke("M262 300 L300 262", w = 6f) }
            oval(170f, 190f, 13f, 10f, peel)
            oval(170f, 400f, 13f, 10f, peel)
            shape("M174 186 C 190 158 226 158 236 172 C 216 188 192 190 174 186 Z", leaf)
            drawOutfit(g.closet, "back")
            oval(170f, 295f, 95f, 106f, peel, w = 5f)
            drawOval(fruitShade(170f, 295f, 92f, 103f), Offset(78f, 192f), Size(184f, 206f))
            for ((x, y, r) in listOf(Triple(110f, 330f, 2.5f), Triple(125f, 360f, 2f), Triple(215f, 350f, 2.5f), Triple(235f, 300f, 2f), Triple(200f, 375f, 2f), Triple(140f, 385f, 2f)))
                drawCircle(Color(0xFF6B4A00).copy(alpha = 0.12f), r, Offset(x, y))
            stroke("M108 230 C 118 214 132 206 146 202", Color.White.copy(alpha = 0.6f), 7f)
            oval(118f, 318f, 15f, 9f, Color(0xFFFF9A8B), null, alpha = 0.75f)
            oval(222f, 318f, 15f, 9f, Color(0xFFFF9A8B), null, alpha = 0.75f)

            // eyes follow whatever's happening (closed if he's dozed off)
            for (c in listOf(Offset(142f, 276f), Offset(198f, 276f))) {
                if (g.sleeping) {
                    stroke("M${c.x - 10} ${c.y} Q${c.x} ${c.y + 6} ${c.x + 10} ${c.y}", LINE, 4f)
                    continue
                }
                drawCircle(Color.White, 19f, c); drawCircle(LINE, 19f, c, style = Stroke(4f))
                val dx = g.look.x - c.x; val dy = g.look.y - c.y
                val d = hypot(dx, dy).let { if (it == 0f) 1f else it }
                val m = min(9f, d)
                drawCircle(LINE, 8f - 3f * s, Offset(c.x + dx / d * m, c.y + dy / d * m))
                if (g.lid > 0.01f) {
                    val ry = 21f * g.lid
                    oval(c.x, 255f + ry, 21f, ry, peel)
                }
            }
            if (s > 0.5f) oval(134f, 302f, 5f, 8f, Color(0xFF7FD3FF), w = 2f)

            val worried = g.face == Face.WORRY || g.face == Face.GASP || (g.face == Face.HAPPY && s >= 0.67f)
            if (worried) { stroke("M126 248 L156 238", w = 5f); stroke("M184 238 L214 248", w = 5f) }
            else { stroke("M126 246 Q142 238 156 246", w = 5f); stroke("M184 246 Q198 238 214 246", w = 5f) }

            when (g.face) {
                Face.HAPPY -> {
                    val endY = 330 + 14 * s; val ctrlY = 362 - 70 * s
                    val p = Path().apply { moveTo(142f, endY); quadraticBezierTo(170f, ctrlY, 198f, endY) }
                    drawPath(p, LINE, style = Stroke(5f, cap = StrokeCap.Round))
                }
                Face.CHEER -> shape("M138 326 Q170 376 202 326 Z", Color(0xFF8C2F2F), w = 5f)
                Face.WORRY -> stroke("M146 344 Q170 326 194 344", w = 5f)
                Face.GASP -> oval(170f, 338f, 12f, 16f, Color(0xFF8C2F2F))
            }
            if (heartEyes) for (c in listOf(Offset(142f, 276f), Offset(198f, 276f)))
                shape("M${c.x} ${c.y + 14} C ${c.x - 26} ${c.y - 4} ${c.x - 12} ${c.y - 22} ${c.x} ${c.y - 8} " +
                    "C ${c.x + 12} ${c.y - 22} ${c.x + 26} ${c.y - 4} ${c.x} ${c.y + 14} Z", Color(0xFFFF4D8D), w = 3f)
            drawOutfit(g.closet, "top")
        }
    }
    if (g.sleeping) drawZzz(Offset(235f, 190f))
}

private fun DrawScope.drawZzz(p: Offset) {
    for ((i, dp) in listOf(0f to 0f, 10f to -14f, 20f to -26f).withIndex()) {
        val size = 10f + i * 3f
        stroke("M${p.x + dp.first - size} ${p.y + dp.second} h${size * 2} l-${size * 2} ${size} h${size * 2}", Color(0xFF7B5CFF), 3f)
    }
}

private fun DrawScope.drawPuppy(timeMs: Long) {
    translate(-160f, 0f) {
        oval(600f, 458f, 26f, 5f, Color.Black, null, alpha = 0.15f)
        rotate(((timeMs / 120) % 2 * 16 - 8).toFloat(), Offset(624f, 436f)) {
            stroke("M624 436 q14 -8 12 -24", Color(0xFFA8733E), 6f)
        }
        oval(606f, 440f, 24f, 14f, Color(0xFFC98B4E), w = 3f)
        stroke("M592 450 v8 M600 452 v7 M614 452 v7 M620 450 v8", w = 4f)
        drawCircle(Color(0xFFC98B4E), 15f, Offset(584f, 428f)); drawCircle(LINE, 15f, Offset(584f, 428f), style = Stroke(3f))
        rotate(20f, Offset(574f, 424f)) { oval(574f, 424f, 6f, 12f, Color(0xFF8B5A2B), w = 2.5f) }
        rotate(-20f, Offset(594f, 422f)) { oval(594f, 422f, 6f, 12f, Color(0xFF8B5A2B), w = 2.5f) }
        drawCircle(LINE, 2.5f, Offset(579f, 428f)); drawCircle(LINE, 2.5f, Offset(589f, 428f))
        oval(584f, 435f, 4f, 3f, LINE, null)
        stroke("M584 438 q0 6 5 6", RED, 3f)
        drawRoundRect(RED, Offset(574f, 440f), Size(22f, 5f), androidx.compose.ui.geometry.CornerRadius(2f, 2f))
    }
}

/** is this scene point on Kevin? (also where shadow Kevin sits, in the Nightmare Realm) */
fun onKevin(p: Offset): Boolean {
    val ex = (p.x - 170f) / 105f; val ey = (p.y - 300f) / 120f
    return ex * ex + ey * ey <= 1f || (p.x in 110f..230f && p.y in 390f..465f)
}
/** the sun, top-right: tap to go to Space (if unlocked) */
fun onSun(p: Offset): Boolean { val dx = p.x - 720f; val dy = p.y - 70f; return dx * dx + dy * dy <= 56f * 56f }
/** the dug hole next to the stand: tap to go Digging */
fun onDigHole(p: Offset): Boolean = p.x in 505f..620f && p.y in 405f..500f
/** Lemy, Kevin's girlfriend, standing by his feet */
fun onMomo(p: Offset): Boolean { val dx = p.x - 80f; val dy = p.y - 445f; return dx * dx + dy * dy <= 34f * 34f }
/** the puppy, to the right of the table */
fun onPuppy(p: Offset): Boolean = p.x in 405f..470f && p.y in 405f..455f
/** one of the drifting clouds, at the position it's drifted to by [timeMs] */
fun onCloud(p: Offset, timeMs: Long, index: Int): Boolean {
    val c = CLOUD_SPOTS[index]
    val drift = timeMs / 1000f
    val x = (c.first + drift * (6f + 3 * index)) % 960f - 80f
    val dx = p.x - x; val dy = p.y - c.second
    val r = 46f * c.third + 14f
    return dx * dx + dy * dy <= r * r
}
/** a family ghost in the Death Realm, at its slot index */
fun onGhost(p: Offset, index: Int): Boolean {
    if (index !in Lines.famSlots.indices) return false
    val (x, y) = Lines.famSlots[index]
    val dx = (p.x - x) / 30f; val dy = (p.y - (y - 5f)) / 35f
    return dx * dx + dy * dy <= 1f
}
/** the uncounted crowd of relatives, scattered across the sky in the Death Realm */
fun onCrowd(p: Offset): Boolean = p.x in 290f..780f && p.y in 25f..250f

private val STAR_SPOTS = listOf(
    60f to 40f, 150f to 90f, 250f to 30f, 330f to 70f, 470f to 40f,
    560f to 110f, 640f to 60f, 760f to 140f, 200f to 150f, 420f to 130f,
)
/** the night sky: stars + moon, fading in as the sun goes down */
private fun DrawScope.drawNightSky(night: Float) {
    val a = (night * 0.55f).coerceIn(0f, 1f)
    drawRect(Color(0xFF0B1240).copy(alpha = a), Offset.Zero, Size(800f, 450f))
    for ((x, y) in STAR_SPOTS) drawCircle(Color.White.copy(alpha = a), 2f, Offset(x, y))
    drawCircle(Color(0xFFF6F0C8).copy(alpha = a), 26f, Offset(120f, 80f))
    drawCircle(Color(0xFF0B1240).copy(alpha = a), 22f, Offset(132f, 72f))
}
/** fireflies over the grass at night */
private fun DrawScope.drawFireflies(timeMs: Long, alpha: Float) {
    val spots = listOf(90f to 400f, 260f to 420f, 610f to 380f, 740f to 430f, 480f to 440f)
    for ((i, s) in spots.withIndex()) {
        val flick = 0.5f + 0.5f * sin(timeMs / 420f + i * 1.7f)
        drawCircle(Color(0xFFF6F07A).copy(alpha = alpha * flick), 3f, Offset(s.first, s.second))
    }
}
/** the sun (or, at night, the Milky Way): tap to go to Space once you own the telescope */
private fun DrawScope.drawSun(g: GameViewModel, night: Float) {
    val unlocked = g.lv("telescope") > 0 || g.bypassLocks
    if (unlocked) drawCircle(Brush.radialGradient(0.55f to Color(0xB3FFF6B0), 1f to Color(0x00FFF6B0), center = Offset(720f, 70f), radius = 74f), 74f, Offset(720f, 70f))
    drawCircle(Color(0xFFFFE66B).copy(alpha = 1f - night * 0.95f), 42f, Offset(720f, 70f))
    drawCircle(Color.White.copy(alpha = 0.35f * (1f - night)), 13f, Offset(707f, 57f))
    if (night > 0.5f) {
        val a = night
        drawOval(Color(0xFFB9A8FF).copy(alpha = 0.28f * a), Offset(580f, 52f), Size(240f, 56f))
        drawCircle(Color(0xFFFFF6E0).copy(alpha = 0.9f * a), 14f, Offset(700f, 80f))
    }
}
/** the hole next to the stand, once you've bought a shovel: tap to go Digging */
private fun DrawScope.drawDigHole() {
    oval(560f, 486f, 44f, 11f, Color(0xFF5E3C20))
    oval(560f, 488f, 34f, 7f, Color(0xFF1A1008), null)
    stroke("M612 488 L630 430", Color(0xFF6B4A1E), 5f)
    shape("M622 430 L638 430 L636 418 Q630 410 624 418 Z", Color(0xFF9AA7B1), w = 2f)
}
/** Lemy, by Kevin's feet, once he's dating her */
private fun DrawScope.drawMomo(g: GameViewModel) {
    translate(80f, 445f) {
        oval(0f, 10f, 22f, 6f, Color.Black, null, alpha = 0.14f)
        oval(0f, 0f, 24f, 27f, Color(0xFFFFD21F), w = 3f)
        drawCircle(Color.White, 6f, Offset(-7f, -3f)); drawCircle(LINE, 6f, Offset(-7f, -3f), style = Stroke(2f))
        drawCircle(Color.White, 6f, Offset(7f, -3f)); drawCircle(LINE, 6f, Offset(7f, -3f), style = Stroke(2f))
        drawCircle(LINE, 2.5f, Offset(-7f, -3f)); drawCircle(LINE, 2.5f, Offset(7f, -3f))
        stroke("M-6 8 Q0 13 6 8", LINE, 3f)
        // pink bow
        shape("M-6 -22 L0 -16 L6 -22 L4 -14 L0 -17 L-4 -14 Z", Color(0xFFFF7FB0), w = 2f)
    }
}

/** ghosts in the Death Realm: tap one to squeeze it (don't!) */
private fun DrawScope.drawGhost(x: Float, y: Float, done: Boolean) {
    if (done) return
    translate(x, y) {
        oval(0f, -34f, 16f, 5f, Color(0xFFFFE66B), null)
        shape("M-24 8 C-24 -32 24 -32 24 8 L24 22 Q16 14 12 22 Q4 14 0 22 Q-4 14 -12 22 Q-16 14 -24 22 Z", Color(0xFFFFF9D6).copy(alpha = 0.9f), w = 3f)
        drawCircle(LINE, 3.5f, Offset(-8f, -2f)); drawCircle(LINE, 3.5f, Offset(8f, -2f))
        stroke("M-6 8 Q0 13 6 8", LINE, 3f)
    }
}
private fun DrawScope.drawCrowdGhost(x: Float, y: Float, s: Float) {
    scale(s, s, Offset(x, y)) {
        translate(x, y) {
            shape("M-24 8 C-24 -32 24 -32 24 8 L24 22 Q16 14 12 22 Q4 14 0 22 Q-4 14 -12 22 Q-16 14 -24 22 Z", Color(0xFFFFF9D6).copy(alpha = 0.75f), w = 3f)
            drawCircle(LINE, 3.5f, Offset(-8f, -2f)); drawCircle(LINE, 3.5f, Offset(8f, -2f))
        }
    }
}
/** the Death Realm: Kevin's ghost family, floating above the stand */
private fun DrawScope.drawDeathRealm(g: GameViewModel, timeMs: Long) {
    val r = g.realm
    for (i in 0 until r.famCount.toInt().coerceAtMost(Lines.famSlots.size)) {
        val (x, y) = Lines.famSlots[i]
        val bob = sin(timeMs / 500f + i) * 4f
        drawGhost(x, y + bob, r.famDone.getOrElse(i) { false })
    }
    val n = min(r.famExtra, 28L).toInt()
    for (i in 0 until n) {
        val x = 300f + ((i * 137) % 470)
        val y = 40f + ((i * 89) % 200)
        val s = 0.38f + ((i * 7) % 5) * 0.03f
        drawCrowdGhost(x, y, s)
    }
    r.deadWearing?.let { drawDeadOutfit(it) }
}

/** the shadow Kevin you have to hold down for ~30 seconds to escape the Nightmare Realm */
private fun DrawScope.drawNightmareRealm(g: GameViewModel, timeMs: Long) {
    val jitter = if (g.realm.darkProgress > 0f) (kotlin.random.Random.nextFloat() - 0.5f) * (2f + g.realm.darkProgress * 14f) else 0f
    translate(jitter, jitter / 2f) {
        shape("M98 268 C98 188 242 188 242 268 L242 322 Q218 294 206 322 Q184 294 170 322 Q156 294 134 322 Q122 294 98 322 Z",
            Color(0xFF0E0003).copy(alpha = 0.92f), line = null)
        drawCircle(Color(0xFFFF1A2E), 8f, Offset(154f, 288f))
        drawCircle(Color(0xFFFF1A2E), 8f, Offset(186f, 288f))
        stroke("M144 314 L154 322 L164 314 L174 322 L184 314 L194 322", Color(0xFFFF1A2E), 3f)
    }
    // the angry moon
    drawCircle(Color(0xFFFFE9E9), 46f, Offset(720f, 70f))
    drawCircle(Color(0xFFC0001A), 20f, Offset(712f, 72f))
}
/** the dead closet: a Death Realm-only outfit drawn over Kevin */
private fun DrawScope.drawDeadOutfit(id: String) {
    when (id) {
        "sheet" -> {
            shape("M74 300 Q74 186 170 186 Q266 186 266 300 L266 420 Q246 404 226 420 Q206 404 186 420 Q166 404 146 420 Q126 404 106 420 Q90 406 74 420 Z",
                Color.White.copy(alpha = 0.85f))
            oval(142f, 276f, 14f, 18f, Color(0xFF1A1A1A), null); oval(198f, 276f, 14f, 18f, Color(0xFF1A1A1A), null)
        }
        "skeleton" -> {
            stroke("M170 350 V410", Color.White, 6f); stroke("M130 360 Q170 350 210 360", Color.White, 6f)
            drawCircle(Color(0xFF1A1A1A), 22f, Offset(142f, 276f)); drawCircle(Color(0xFF1A1A1A), 22f, Offset(198f, 276f))
        }
        "vampire" -> {
            shape("M86 340 L60 230 L120 300 Z", Color(0xFF1A1A1A)); shape("M254 340 L280 230 L220 300 Z", Color(0xFF1A1A1A))
            shape("M100 340 Q170 380 240 340 L250 360 Q170 404 90 360 Z", Color(0xFF8A0012))
        }
    }
}
