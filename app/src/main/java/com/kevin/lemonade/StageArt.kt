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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser
import kotlin.math.hypot
import kotlin.math.min

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

fun DrawScope.drawStage(g: GameViewModel, timeMs: Long) {
    val press = g.lv("press")

    // sky
    drawRect(SKY, Offset.Zero, Size(800f, 500f))
    drawRect(Brush.verticalGradient(0f to Color.White.copy(alpha = 0f), 0.75f to Color.White.copy(alpha = 0.35f),
        1f to Color.White.copy(alpha = 0.55f), startY = 0f, endY = 450f), Offset.Zero, Size(800f, 450f))
    // drifting clouds
    val drift = (timeMs / 1000f)
    for ((i, c) in listOf(Triple(140f, 70f, 1f), Triple(520f, 52f, 0.8f), Triple(330f, 100f, 0.65f)).withIndex()) {
        val x = (c.first + drift * (6f + 3 * i)) % 960f - 80f
        val s = c.third
        oval(x, c.second, 46f * s, 18f * s, Color.White, null)
        oval(x + 30f * s, c.second - 12f * s, 30f * s, 20f * s, Color.White, null)
        oval(x - 22f * s, c.second - 8f * s, 22f * s, 14f * s, Color.White, null)
    }
    drawPath(svg("M0 452 Q120 380 260 440 T560 430 T800 420 L800 452 Z"), GRASS.copy(alpha = 0.55f))
    // sun
    drawCircle(Brush.radialGradient(0.55f to Color(0xB3FFF6B0), 1f to Color(0x00FFF6B0), center = Offset(720f, 70f), radius = 74f), 74f, Offset(720f, 70f))
    drawCircle(Color(0xFFFFE66B), 42f, Offset(720f, 70f))
    drawCircle(Color.White.copy(alpha = 0.35f), 13f, Offset(707f, 57f))

    // grass, tufts, flowers
    drawRect(GRASS, Offset(0f, 450f), Size(800f, 50f))
    drawRect(Color.Black.copy(alpha = 0.08f), Offset(0f, 472f), Size(800f, 28f))
    val tuft = Color.Black.copy(alpha = 0.18f)
    for (d in listOf("M30 452 l-4 -10 M34 452 l0 -13 M38 452 l4 -10", "M250 455 l-4 -9 M254 455 l0 -12 M258 455 l4 -9",
        "M720 454 l-4 -10 M724 454 l0 -13 M728 454 l4 -10", "M470 470 l-3 -8 M473 470 l0 -10 M476 470 l3 -8", "M95 478 l-3 -8 M98 478 l0 -10 M101 478 l3 -8"))
        stroke(d, tuft, 2.5f)
    for ((x, y, c) in listOf(Triple(60f, 470f, Color.White), Triple(290f, 482f, Color(0xFFFF9AC1)), Triple(760f, 476f, Color.White), Triple(610f, 488f, Color(0xFFB79CFF)))) {
        drawCircle(c, 4f, Offset(x, y)); drawCircle(Color(0xFFFFD21F), 1.8f, Offset(x, y))
    }

    // table
    oval(500f, 460f, 215f, 9f, Color.Black, null, alpha = 0.16f)
    box(320f, 418f, 14f, 40f, WOOD); box(666f, 418f, 14f, 40f, WOOD)
    box(300f, 400f, 400f, 18f, WOOD, r = 4f)
    stroke("M308 405 H692", Color.White.copy(alpha = 0.35f), 3f)

    if (g.lv("puppy") > 0) drawPuppy(timeMs)

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

    drawKevin(g)

    // the "don't hold me" ring
    if (g.holdProgress > 0f) {
        drawArc(RED, -90f, 360f * g.holdProgress, false, Offset(42f, 172f), Size(256f, 256f), style = Stroke(7f, cap = StrokeCap.Round))
    }
}

private fun fruitShade(cx: Float, cy: Float, rx: Float, ry: Float) = Brush.radialGradient(
    0f to Color(0x8CFFFFFF), 0.35f to Color(0x00FFFFFF), 0.8f to Color(0x1F6B4A00), 1f to Color(0x4D6B4A00),
    center = Offset(cx - rx + 0.7f * rx, cy - ry + 0.6f * ry), radius = 1.5f * maxOf(rx, ry)
)

private fun DrawScope.drawKevin(g: GameViewModel) {
    val s = g.sadFrac().toFloat()
    val peel = lerp(Color(0xFFFFD21F), Color(0xFFB9B5A0), s)
    val leaf = lerp(Color(0xFF3E9B4F), Color(0xFF7C7F6E), s)

    oval(170f, 458f, 78f, 10f, Color.Black, null, alpha = 0.16f)
    stroke("M148 398 L142 448", w = 5f); stroke("M192 398 L198 448", w = 5f)
    oval(136f, 452f, 18f, 9f, RED); oval(204f, 452f, 18f, 9f, RED)

    translate(g.kevinDx, g.kevinDy) {
        rotate(g.kevinRot, Offset(170f, 400f)) {
            stroke("M78 300 L44 340", w = 6f)
            rotate(g.armRot, Offset(262f, 300f)) { stroke("M262 300 L300 262", w = 6f) }
            oval(170f, 190f, 13f, 10f, peel)
            oval(170f, 400f, 13f, 10f, peel)
            shape("M174 186 C 190 158 226 158 236 172 C 216 188 192 190 174 186 Z", leaf)
            oval(170f, 295f, 95f, 106f, peel, w = 5f)
            drawOval(fruitShade(170f, 295f, 92f, 103f), Offset(78f, 192f), Size(184f, 206f))
            for ((x, y, r) in listOf(Triple(110f, 330f, 2.5f), Triple(125f, 360f, 2f), Triple(215f, 350f, 2.5f), Triple(235f, 300f, 2f), Triple(200f, 375f, 2f), Triple(140f, 385f, 2f)))
                drawCircle(Color(0xFF6B4A00).copy(alpha = 0.12f), r, Offset(x, y))
            stroke("M108 230 C 118 214 132 206 146 202", Color.White.copy(alpha = 0.6f), 7f)
            oval(118f, 318f, 15f, 9f, Color(0xFFFF9A8B), null, alpha = 0.75f)
            oval(222f, 318f, 15f, 9f, Color(0xFFFF9A8B), null, alpha = 0.75f)

            // eyes follow whatever's happening
            for (c in listOf(Offset(142f, 276f), Offset(198f, 276f))) {
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
        }
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

/** is this scene point on Kevin? */
fun onKevin(p: Offset): Boolean {
    val ex = (p.x - 170f) / 105f; val ey = (p.y - 300f) / 120f
    return ex * ex + ey * ey <= 1f || (p.x in 110f..230f && p.y in 390f..465f)
}
