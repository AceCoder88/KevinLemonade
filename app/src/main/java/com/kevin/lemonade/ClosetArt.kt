package com.kevin.lemonade

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate

/**
 * Drawing for Kevin's Closet: each clothing piece drawn in Kevin's own body coordinates
 * (the original's CLOTH_SVG / CLOTH_BACK, viewBox space 0..340 x 0..500), so the same
 * [drawClothFront] / [drawClothBack] calls work both in the closet preview below and on
 * the main stand stage. See the bottom of this file for [drawOutfit], the public entry
 * point StageArt.kt's drawKevin() should call - it isn't wired up there yet; see the
 * integration notes in the final report.
 */

private fun DrawScope.rect(x: Float, y: Float, w: Float, h: Float, fill: Color, line: Color? = LINE, sw: Float = 4f, r: Float = 0f) =
    box(x, y, w, h, SolidColor(fill), line, sw, r)

private val kevinBody = Rect(170f - 95f, 295f - 106f, 170f + 95f, 295f + 106f)
/** clips rect-shaped body clothing (sweaters, suits...) to Kevin's round body, like the original's #kevinClip */
private fun DrawScope.clippedToKevin(block: DrawScope.() -> Unit) {
    clipPath(Path().apply { addOval(kevinBody) }) { block() }
}

/** front-facing art for one piece (hat/face/body/feet slots). Pieces with no visual (set-only bonuses) draw nothing. */
fun DrawScope.drawClothFront(id: String) {
    when (id) {
        "tophat" -> {
            rect(132f, 112f, 76f, 66f, Color(0xFF1B1B2A), LINE, 4f, r = 5f)
            rect(132f, 156f, 76f, 12f, Color(0xFFE8483B), null)
            oval(170f, 182f, 64f, 12f, Color(0xFF1B1B2A), LINE, 4f)
            stroke("M142 122 V150", Color.White.copy(alpha = 0.25f), 4f)
        }
        "army" -> {
            shape("M100 214 Q170 118 240 214 Z", Color(0xFF4B5A2A), LINE, 4f)
            rect(92f, 208f, 156f, 14f, Color(0xFF3E4A22), LINE, 4f, r = 7f)
            shape("M170 156 L175 168 L188 168 L178 176 L182 189 L170 181 L158 189 L162 176 L152 168 L165 168 Z", Color(0xFFFFD21F), LINE, 2f)
        }
        "space" -> {
            oval(170f, 290f, 132f, 132f, Color(0x38B4E6FF), Color(0xFF9ED8F5), 7f)
            stroke("M80 220 Q100 180 140 168", Color.White.copy(alpha = 0.7f), 8f)
            stroke("M170 158 V128", LINE, 4f)
            oval(170f, 124f, 8f, 8f, Color(0xFFE8483B), LINE, 3f)
        }
        "starglasses" -> {
            shape("M142 252 L148 268 L165 268 L151 278 L156 295 L142 285 L128 295 L133 278 L119 268 L136 268 Z", Color(0xFFFF7FB0).copy(alpha = 0.85f), LINE, 3f)
            shape("M198 252 L204 268 L221 268 L207 278 L212 295 L198 285 L184 295 L189 278 L175 268 L192 268 Z", Color(0xFFFF7FB0).copy(alpha = 0.85f), LINE, 3f)
            stroke("M160 274 H180", LINE, 4f)
        }
        "monocle" -> {
            oval(198f, 276f, 24f, 24f, Color(0x40FFFFFF), Color(0xFFE9B630), 5f)
            stroke("M220 288 Q240 320 228 352", Color(0xFFE9B630), 3f)
        }
        "sweater" -> {
            clippedToKevin {
                rect(60f, 356f, 220f, 60f, Color(0xFFE8483B), null)
                stroke("M60 372 H280", Color.White.copy(alpha = 0.9f), 6f)
                stroke("M60 392 H280", Color.White.copy(alpha = 0.9f), 6f)
                stroke("M60 356 H280", Color(0xFFB8326E), 8f)
            }
            stroke("M78 300 L44 340", Color(0xFFE8483B), 12f)
            stroke("M262 300 L300 262", Color(0xFFE8483B), 12f)
        }
        "tie" -> {
            shape("M160 350 L180 350 L176 362 L186 400 L170 412 L154 400 L164 362 Z", Color(0xFF2E6BD6), LINE, 3f)
            stroke("M166 372 L178 384 M162 390 L176 402", Color(0xFF9ED8F5), 3f)
        }
        "slippers" -> {
            oval(136f, 452f, 24f, 13f, Color.White, LINE, 4f)
            oval(124f, 436f, 5f, 13f, Color.White, LINE, 3f); oval(136f, 434f, 5f, 13f, Color.White, LINE, 3f)
            oval(124f, 437f, 2f, 8f, Color(0xFFFF9AB0), null); oval(136f, 435f, 2f, 8f, Color(0xFFFF9AB0), null)
            oval(122f, 450f, 2f, 2f, LINE, null); oval(114f, 454f, 3f, 3f, Color(0xFFFF9AB0), null)
            oval(204f, 452f, 24f, 13f, Color.White, LINE, 4f)
            oval(216f, 436f, 5f, 13f, Color.White, LINE, 3f); oval(204f, 434f, 5f, 13f, Color.White, LINE, 3f)
            oval(216f, 437f, 2f, 8f, Color(0xFFFF9AB0), null); oval(204f, 435f, 2f, 8f, Color(0xFFFF9AB0), null)
            oval(218f, 450f, 2f, 2f, LINE, null); oval(226f, 454f, 3f, 3f, Color(0xFFFF9AB0), null)
        }
        "sneakers" -> {
            oval(134f, 452f, 24f, 11f, Color(0xFF2E9BD6), LINE, 4f)
            stroke("M114 456 H154", Color.White, 4f); stroke("M128 446 l6 4 M136 444 l6 4", Color.White, 2f)
            oval(206f, 452f, 24f, 11f, Color(0xFF2E9BD6), LINE, 4f)
            stroke("M186 456 H226", Color.White, 4f); stroke("M198 446 l6 4 M206 444 l6 4", Color.White, 2f)
        }
        "crown" -> {
            shape("M120 196 L116 140 L142 168 L170 126 L198 168 L224 140 L220 196 Z", Color(0xFFFFD21F), LINE, 4f)
            rect(118f, 186f, 104f, 14f, Color(0xFFE9B630), LINE, 3f, r = 4f)
            oval(170f, 168f, 7f, 7f, Color(0xFFE8483B), LINE, 2f)
            oval(140f, 182f, 5f, 5f, Color(0xFF2E9BD6), null); oval(200f, 182f, 5f, 5f, Color(0xFF4FA34A), null)
        }
        "chef" -> {
            rect(128f, 168f, 84f, 28f, Color.White, LINE, 4f, r = 4f)
            oval(140f, 150f, 24f, 24f, Color.White, LINE, 4f); oval(170f, 136f, 28f, 28f, Color.White, LINE, 4f); oval(200f, 150f, 24f, 24f, Color.White, LINE, 4f)
            rect(131f, 152f, 78f, 20f, Color.White, null)
        }
        "pirate" -> {
            shape("M96 204 Q170 110 244 204 Q170 180 96 204 Z", Color(0xFF1B1B2A), LINE, 4f)
            stroke("M96 204 Q170 186 244 204", Color(0xFFE9B630), 5f)
            oval(170f, 168f, 11f, 11f, Color.White, null)
            stroke("M162 184 L178 192 M178 184 L162 192", Color.White, 3f)
            oval(166f, 166f, 2.5f, 2.5f, Color(0xFF1B1B2A), null); oval(174f, 166f, 2.5f, 2.5f, Color(0xFF1B1B2A), null)
        }
        "mustache" -> {
            shape("M130 312 Q150 296 170 310 Q190 296 210 312 Q216 322 206 324 Q190 316 170 318 Q150 316 134 324 Q124 322 130 312 Z", Color(0xFF6B4A1E), LINE, 3f)
            stroke("M130 312 q-10 -6 -6 -14 M210 312 q10 -6 6 -14", Color(0xFF6B4A1E), 4f)
        }
        "clownnose" -> {
            oval(170f, 304f, 16f, 16f, Color(0xFFE8483B), LINE, 3f)
            oval(164f, 298f, 5f, 5f, Color.White.copy(alpha = 0.6f), null)
        }
        "sunhat" -> {
            oval(170f, 198f, 96f, 16f, Color(0xFFF3D9A0), LINE, 4f)
            shape("M124 198 Q124 140 170 138 Q216 140 216 198 Z", Color(0xFFF3D9A0), LINE, 4f)
            rect(124f, 180f, 92f, 12f, Color(0xFFFF7FB0), null)
            oval(206f, 184f, 8f, 8f, Color(0xFFFFD21F), LINE, 2f)
        }
        "beanie" -> {
            shape("M104 216 Q170 112 236 216 Z", Color(0xFF2E6BD6), LINE, 4f)
            rect(98f, 204f, 144f, 20f, Color.White, LINE, 3f, r = 8f)
            stroke("M130 170 V204 M150 156 V204 M170 150 V204 M190 156 V204 M210 170 V204", Color(0xFF1E4FA8), 3f)
            oval(170f, 136f, 16f, 16f, Color.White, LINE, 3f)
        }
        "rockhair" -> {
            shape("M96 222 L104 160 L124 196 L134 128 L154 186 L170 112 L186 186 L206 128 L216 196 L236 160 L244 222 Z", Color(0xFF7B3FA0), LINE, 4f)
            stroke("M134 128 L146 170 M206 128 L194 170", Color(0xFFB79CFF), 3f)
        }
        "nightcap" -> {
            shape("M110 214 Q130 150 196 150 Q246 160 262 214 Q240 196 238 214 Z", Color(0xFF9ED8F5), LINE, 4f)
            stroke("M196 150 Q250 150 262 214", Color(0xFF2E6BD6), 4f)
            rect(104f, 206f, 140f, 16f, Color.White, LINE, 3f, r = 8f)
            oval(262f, 216f, 12f, 12f, Color.White, LINE, 3f)
        }
        "bowler" -> {
            oval(170f, 196f, 74f, 12f, Color(0xFF3A2A1A), LINE, 4f)
            shape("M122 194 Q122 132 170 130 Q218 132 218 194 Z", Color(0xFF3A2A1A), LINE, 4f)
            rect(122f, 178f, 96f, 10f, Color(0xFF6B4A1E), null)
        }
        "clownwig" -> {
            oval(96f, 232f, 26f, 26f, Color(0xFFE8483B), LINE, 3f); oval(86f, 270f, 24f, 24f, Color(0xFFFFD21F), LINE, 3f)
            oval(244f, 232f, 26f, 26f, Color(0xFF2E9BD6), LINE, 3f); oval(254f, 270f, 24f, 24f, Color(0xFF4FA34A), LINE, 3f)
            oval(140f, 196f, 22f, 22f, Color(0xFFB79CFF), LINE, 3f); oval(200f, 196f, 22f, 22f, Color(0xFFFF7FB0), LINE, 3f)
            oval(170f, 182f, 22f, 22f, Color(0xFFE8483B), LINE, 3f)
        }
        "sleepmask" -> {
            rect(108f, 256f, 124f, 40f, Color(0xFFFF9AC1), LINE, 3f, r = 18f)
            stroke("M126 276 Q142 286 158 276 M182 276 Q198 286 214 276", LINE, 3f)
            stroke("M96 268 L108 272 M232 272 L244 268", Color(0xFFFF9AC1), 5f)
        }
        "hawaiian" -> clippedToKevin {
            rect(60f, 350f, 220f, 74f, Color(0xFF2EC4B6), null)
            oval(100f, 372f, 9f, 9f, Color(0xFFFF7FB0), null); oval(150f, 398f, 9f, 9f, Color(0xFFFFD21F), null)
            oval(200f, 368f, 9f, 9f, Color(0xFFFF7FB0), null); oval(240f, 396f, 9f, 9f, Color(0xFFFFD21F), null)
            stroke("M170 350 V424", Color(0xFF1E8F86), 3f)
            shape("M150 350 L170 372 L190 350", Color.White, LINE, 2f)
        }
        "leather" -> clippedToKevin {
            rect(60f, 348f, 220f, 80f, Color(0xFF1B1B2A), null)
            stroke("M170 348 V428", Color(0xFFC0C8D0), 4f)
            stroke("M140 348 L160 380 M200 348 L180 380", Color(0xFF3A3A4A), 6f)
            oval(120f, 370f, 3f, 3f, Color(0xFFC0C8D0), null); oval(220f, 370f, 3f, 3f, Color(0xFFC0C8D0), null)
            oval(110f, 392f, 3f, 3f, Color(0xFFC0C8D0), null); oval(230f, 392f, 3f, 3f, Color(0xFFC0C8D0), null)
        }
        "pajamas" -> clippedToKevin {
            rect(60f, 350f, 220f, 74f, Color(0xFF9ED8F5), null)
            shape("M100 368 a8 8 0 1 0 8 10 a6 6 0 1 1 -8 -10", Color(0xFFFFF07A), null)
            shape("M200 392 a8 8 0 1 0 8 10 a6 6 0 1 1 -8 -10", Color(0xFFFFF07A), null)
            oval(150f, 372f, 3f, 3f, Color.White, null); oval(240f, 372f, 3f, 3f, Color.White, null); oval(130f, 404f, 3f, 3f, Color.White, null)
            oval(170f, 364f, 4f, 4f, Color.White, LINE, 1.5f); oval(170f, 384f, 4f, 4f, Color.White, LINE, 1.5f)
        }
        "apron" -> {
            clippedToKevin {
                shape("M110 350 H230 V430 H110 Z", Color.White, LINE, 3f)
                rect(146f, 380f, 48f, 26f, Color.White, Color(0xFF9AA7B1), 2f, r = 4f)
                oval(132f, 366f, 6f, 6f, Color(0xFFEDE6D8), null); oval(214f, 410f, 8f, 8f, Color(0xFFEDE6D8), null)
            }
            stroke("M110 352 Q90 330 104 310 M230 352 Q250 330 236 310", Color.White, 5f)
        }
        "clownsuit" -> clippedToKevin {
            rect(60f, 348f, 220f, 80f, Color.White, null)
            oval(96f, 368f, 9f, 9f, Color(0xFFE8483B), null); oval(136f, 402f, 9f, 9f, Color(0xFF2E9BD6), null)
            oval(204f, 402f, 9f, 9f, Color(0xFF4FA34A), null); oval(244f, 368f, 9f, 9f, Color(0xFFB79CFF), null)
            oval(170f, 368f, 8f, 8f, Color(0xFFFFD21F), LINE, 2f); oval(170f, 396f, 8f, 8f, Color(0xFFFFD21F), LINE, 2f)
            stroke("M60 348 Q90 362 120 348 Q150 362 170 348 Q190 362 220 348 Q250 362 280 348", Color(0xFFE8483B), 6f)
        }
        "camo" -> clippedToKevin {
            rect(60f, 348f, 220f, 80f, Color(0xFF6B7A3A), null)
            oval(96f, 372f, 20f, 10f, Color(0xFF4B5A2A), null); oval(160f, 398f, 24f, 12f, Color(0xFF8A7A4A), null)
            oval(230f, 370f, 22f, 11f, Color(0xFF3E4A22), null); oval(200f, 412f, 18f, 8f, Color(0xFF4B5A2A), null)
            rect(60f, 384f, 220f, 10f, Color(0xFF3B2A1A), null)
            rect(160f, 382f, 20f, 14f, Color(0xFFC0C8D0), LINE, 2f)
        }
        "flipflops" -> {
            oval(134f, 456f, 24f, 7f, Color(0xFFFF7FB0), LINE, 3f); oval(206f, 456f, 24f, 7f, Color(0xFFFF7FB0), LINE, 3f)
            stroke("M122 452 L134 444 L146 452 M194 452 L206 444 L218 452", Color(0xFF2EC4B6), 4f)
        }
        "snowboots" -> {
            shape("M118 424 H152 V454 Q152 464 140 464 H112 Q104 464 106 454 L118 448 Z", Color(0xFF8B5A2B), LINE, 3f)
            shape("M188 424 H222 V448 L234 454 Q236 464 228 464 H200 Q188 464 188 454 Z", Color(0xFF8B5A2B), LINE, 3f)
            rect(114f, 418f, 42f, 12f, Color.White, LINE, 2f, r = 6f); rect(184f, 418f, 42f, 12f, Color.White, LINE, 2f, r = 6f)
        }
        "clogs" -> {
            shape("M112 448 Q112 438 130 438 Q154 440 158 452 Q158 462 140 462 H118 Q110 462 112 448 Z", Color(0xFFC98B4E), LINE, 3f)
            shape("M182 452 Q186 440 210 438 Q228 438 228 448 Q230 462 222 462 H200 Q182 462 182 452 Z", Color(0xFFC98B4E), LINE, 3f)
        }
        "loafers" -> {
            oval(134f, 454f, 24f, 9f, Color(0xFF6B4A1E), LINE, 3f); oval(206f, 454f, 24f, 9f, Color(0xFF6B4A1E), LINE, 3f)
            rect(124f, 447f, 16f, 5f, Color(0xFFE9B630), null, r = 2f); rect(200f, 447f, 16f, 5f, Color(0xFFE9B630), null, r = 2f)
        }
        "goldslippers" -> {
            shape("M110 456 Q108 444 132 444 Q156 446 160 458 Q140 466 110 456 Z", Color(0xFFFFD21F), LINE, 3f)
            shape("M180 458 Q184 446 208 444 Q232 444 230 456 Q200 466 180 458 Z", Color(0xFFFFD21F), LINE, 3f)
            stroke("M110 456 q-8 -10 2 -16 M230 456 q8 -10 -2 -16", LINE, 3f)
        }
        "armyboots" -> {
            shape("M120 418 H150 V454 Q150 464 140 464 H112 Q104 464 106 454 L120 448 Z", Color(0xFF2A2018), LINE, 3f)
            shape("M190 418 H220 V448 L234 454 Q236 464 228 464 H200 Q190 464 190 454 Z", Color(0xFF2A2018), LINE, 3f)
            stroke("M126 428 H144 M126 436 H144 M196 428 H214 M196 436 H214", Color(0xFF9AA7B1), 2f)
        }
        "royalrobe" -> {
            clippedToKevin {
                rect(60f, 346f, 220f, 84f, Color(0xFF6A2E9A), null)
                rect(60f, 346f, 220f, 16f, Color.White, null)
                oval(84f, 354f, 2f, 2f, Color(0xFF1B1B2A), null); oval(144f, 354f, 2f, 2f, Color(0xFF1B1B2A), null)
                oval(204f, 354f, 2f, 2f, Color(0xFF1B1B2A), null); oval(114f, 358f, 2f, 2f, Color(0xFF1B1B2A), null)
                oval(174f, 358f, 2f, 2f, Color(0xFF1B1B2A), null); oval(234f, 358f, 2f, 2f, Color(0xFF1B1B2A), null)
                stroke("M170 362 V430", Color.White, 12f)
                stroke("M170 372 h4 M168 392 h4 M170 412 h4", Color(0xFF1B1B2A), 4f)
            }
            stroke("M128 346 Q170 380 212 346", Color(0xFFE9B630), 4f)
            oval(170f, 368f, 7f, 7f, Color(0xFFE8483B), Color(0xFFE9B630), 3f)
        }
        "witchhat" -> {
            shape("M110 206 Q150 150 162 110 Q170 70 206 50 Q190 90 196 130 Q204 170 230 206 Z", Color(0xFF1B1B2A), LINE, 4f)
            oval(170f, 206f, 98f, 15f, Color(0xFF1B1B2A), LINE, 4f)
            rect(128f, 186f, 84f, 12f, Color(0xFF7B5CFF), null)
            rect(162f, 184f, 16f, 16f, Color.Transparent, Color(0xFFE9B630), 3f, r = 2f)
        }
        "witchdress" -> {
            clippedToKevin {
                shape("M60 350 H280 V430 H60 Z", Color(0xFF4B2E9E), null)
                stroke("M60 350 Q170 372 280 350", Color(0xFF2A1745), 6f)
                shape("M84 430 L104 404 L124 430 L144 404 L164 430 L184 404 L204 430 L224 404 L244 430 L264 404 L280 430", Color(0xFF2A1745), null)
            }
            shape("M162 362 l8 -10 l8 10 l-8 10 Z", Color(0xFF7BC043), LINE, 2f)
        }
        "witchboots" -> {
            shape("M126 418 H148 V452 L162 452 Q170 460 160 464 H118 Q112 464 114 456 L126 448 Z", Color(0xFF1B1B2A), LINE, 3f)
            shape("M192 418 H214 V448 L226 456 Q228 464 222 464 H180 Q170 460 178 452 L192 452 Z", Color(0xFF1B1B2A), LINE, 3f)
            stroke("M126 424 H148 M126 432 H148 M126 440 H148 M192 424 H214 M192 432 H214 M192 440 H214", Color(0xFF7B5CFF), 3f)
        }
        "ninjahood" -> {
            shape("M84 250 Q84 178 170 176 Q256 178 256 250 L246 250 Q246 196 170 194 Q94 196 94 250 Z", Color(0xFF1B1B2A), LINE, 4f)
            shape("M246 214 Q280 210 292 236 Q270 228 250 232 Z", Color(0xFFE8483B), LINE, 3f)
        }
        "ninjamask" -> shape("M86 296 Q170 330 254 296 L250 360 Q170 392 90 360 Z", Color(0xFF1B1B2A), LINE, 3f)
        "ninjasuit" -> clippedToKevin {
            rect(60f, 348f, 220f, 80f, Color(0xFF1B1B2A), null)
            rect(60f, 376f, 220f, 10f, Color(0xFFE8483B), null)
        }
        "tabi" -> { oval(134f, 452f, 22f, 9f, Color(0xFF1B1B2A), LINE, 3f); oval(206f, 452f, 22f, 9f, Color(0xFF1B1B2A), LINE, 3f) }
        "cowboyhat" -> {
            oval(170f, 198f, 104f, 18f, Color(0xFF8B5A2B), LINE, 4f)
            shape("M118 198 Q114 132 150 140 Q170 126 190 140 Q226 132 222 198 Z", Color(0xFF8B5A2B), LINE, 4f)
            rect(120f, 180f, 100f, 10f, Color(0xFF3A2414), null)
            stroke("M66 196 Q60 176 74 170 M274 196 Q280 176 266 170", Color(0xFF8B5A2B), 10f)
        }
        "bandana" -> {
            shape("M96 318 Q170 340 244 318 L170 384 Z", Color(0xFFE8483B), LINE, 3f)
            oval(150f, 340f, 3f, 3f, Color.White, null); oval(186f, 346f, 3f, 3f, Color.White, null); oval(168f, 362f, 3f, 3f, Color.White, null)
        }
        "vest" -> {
            clippedToKevin {
                shape("M60 350 H150 L170 430 H60 Z M280 350 H190 L170 430 H280 Z", Color(0xFF8B5A2B), null)
                oval(148f, 384f, 4f, 4f, Color(0xFFE9B630), null); oval(192f, 384f, 4f, 4f, Color(0xFFE9B630), null)
            }
            shape("M206 372 l8 -8 l8 8 l-8 8 Z", Color(0xFFE9B630), LINE, 2f)
        }
        "cowboyboots" -> {
            shape("M120 416 H150 V454 Q150 464 138 464 H110 Q102 464 104 456 L120 448 Z", Color(0xFF6B4A1E), LINE, 3f)
            shape("M190 416 H220 V448 L236 456 Q238 464 230 464 H202 Q190 464 190 454 Z", Color(0xFF6B4A1E), LINE, 3f)
            oval(116f, 462f, 4f, 4f, Color(0xFFC0C8D0), null); oval(234f, 462f, 4f, 4f, Color(0xFFC0C8D0), null)
        }
        "wizardhat" -> {
            shape("M100 206 L170 60 L240 206 Z", Color(0xFF4B2E9E), LINE, 4f)
            oval(170f, 204f, 92f, 14f, Color(0xFF4B2E9E), LINE, 4f)
            shape("M150 140 l6 12 l13 1 l-10 8 l4 13 l-11 -7 l-11 7 l4 -13 l-10 -8 l13 -1 Z", Color(0xFFFFD21F), null)
            oval(196f, 110f, 5f, 5f, Color(0xFFFFD21F), null); oval(178f, 176f, 4f, 4f, Color(0xFFFFD21F), null)
        }
        "wizbeard" -> {
            shape("M110 320 Q170 350 230 320 Q240 380 200 410 Q186 440 170 446 Q154 440 140 410 Q100 380 110 320 Z", Color(0xFFF4F7F9), LINE, 3f)
            stroke("M146 330 Q170 344 194 330", Color(0xFF9AA7B1), 3f)
        }
        "robe" -> clippedToKevin {
            rect(60f, 350f, 220f, 80f, Color(0xFF4B2E9E), null)
            oval(110f, 380f, 4f, 4f, Color(0xFFFFD21F), null); oval(236f, 400f, 4f, 4f, Color(0xFFFFD21F), null); oval(170f, 414f, 4f, 4f, Color(0xFFFFD21F), null)
        }
        "heromask" -> {
            shape("M104 262 Q170 238 236 262 Q240 294 206 298 Q186 300 170 286 Q154 300 134 298 Q100 294 104 262 Z M142 262 a14 14 0 1 0 0.1 0 Z M198 262 a14 14 0 1 0 0.1 0 Z", Color(0xFFE8483B), LINE, 3f)
            stroke("M104 266 L86 256 M236 266 L254 256", Color(0xFFE8483B), 5f)
        }
        "herosuit" -> {
            clippedToKevin {
                rect(60f, 346f, 220f, 84f, Color(0xFF2E6BD6), null)
                rect(60f, 400f, 220f, 12f, Color(0xFFE8483B), null)
                rect(160f, 398f, 20f, 16f, Color(0xFFFFD21F), LINE, 2f, r = 3f)
            }
            shape("M170 350 L194 368 L170 392 L146 368 Z", Color(0xFFFFD21F), LINE, 3f)
        }
        "heroboots" -> {
            shape("M120 420 H150 V454 Q150 464 140 464 H112 Q104 464 106 454 L120 448 Z", Color(0xFFE8483B), LINE, 3f)
            shape("M190 420 H220 V448 L234 454 Q236 464 228 464 H200 Q190 464 190 454 Z", Color(0xFFE8483B), LINE, 3f)
            shape("M118 422 L152 422 L148 432 L122 432 Z M188 422 L222 422 L218 432 L192 432 Z", Color(0xFFFFD21F), LINE, 2f)
        }
        "labcoat" -> clippedToKevin {
            rect(60f, 352f, 220f, 70f, Color(0xFFF4F7F9), null)
            stroke("M170 352 L150 420 M170 352 L190 420", Color(0xFFB8C4CC), 4f)
            stroke("M60 352 H280", Color(0xFFB8C4CC), 5f)
            rect(206f, 370f, 22f, 18f, Color.White, Color(0xFF9AA7B1), 2f, r = 2f)
            stroke("M212 370 V360 M220 370 V362", Color(0xFF2E6BD6), 3f)
        }
        "skates" -> {
            rect(114f, 436f, 40f, 18f, Color(0xFFB79CFF), LINE, 3f, r = 6f)
            oval(122f, 460f, 6f, 6f, Color(0xFFFFD21F), LINE, 2f); oval(146f, 460f, 6f, 6f, Color(0xFFFFD21F), LINE, 2f)
            rect(186f, 436f, 40f, 18f, Color(0xFFB79CFF), LINE, 3f, r = 6f)
            oval(194f, 460f, 6f, 6f, Color(0xFFFFD21F), LINE, 2f); oval(218f, 460f, 6f, 6f, Color(0xFFFFD21F), LINE, 2f)
        }
        "rainboots" -> {
            shape("M126 420 H150 V452 Q150 462 140 462 H118 Q110 462 112 454 L126 448 Z", Color(0xFFFFD21F), LINE, 3f)
            shape("M190 420 H214 V448 L228 454 Q230 462 222 462 H200 Q190 462 190 452 Z", Color(0xFFFFD21F), LINE, 3f)
            stroke("M126 426 H150 M190 426 H214", Color(0xFFE8483B), 4f)
        }
        "shades" -> {
            rect(118f, 262f, 48f, 28f, Color(0xFF1B1B2A), LINE, 3f, r = 10f)
            rect(174f, 262f, 48f, 28f, Color(0xFF1B1B2A), LINE, 3f, r = 10f)
            stroke("M166 272 H174", LINE, 4f)
            stroke("M126 268 L140 268", Color.White.copy(alpha = 0.6f), 3f)
            stroke("M182 268 L196 268", Color.White.copy(alpha = 0.6f), 3f)
        }
        "eyepatch" -> {
            stroke("M100 236 L240 262", Color(0xFF1B1B2A), 5f)
            oval(142f, 278f, 24f, 22f, Color(0xFF1B1B2A), LINE, 3f)
        }
        "piratecoat" -> clippedToKevin {
            rect(60f, 350f, 220f, 74f, Color(0xFFB8262E), null)
            stroke("M170 350 L156 424 M170 350 L184 424", Color(0xFF7A1418), 5f)
            rect(60f, 380f, 220f, 12f, Color(0xFF1B1B2A), null)
            rect(160f, 378f, 20f, 16f, Color(0xFFE9B630), LINE, 2f, r = 2f)
            oval(146f, 364f, 4f, 4f, Color(0xFFE9B630), null); oval(194f, 364f, 4f, 4f, Color(0xFFE9B630), null)
            oval(146f, 406f, 4f, 4f, Color(0xFFE9B630), null); oval(194f, 406f, 4f, 4f, Color(0xFFE9B630), null)
        }
        "pirateboots" -> {
            shape("M124 414 H152 V452 Q152 464 140 464 H114 Q106 464 108 455 L124 448 Z", Color(0xFF1B1B2A), LINE, 3f)
            shape("M188 414 H216 V448 L232 455 Q234 464 226 464 H200 Q188 464 188 452 Z", Color(0xFF1B1B2A), LINE, 3f)
            rect(128f, 426f, 20f, 8f, Color(0xFFE9B630), null); rect(192f, 426f, 20f, 8f, Color(0xFFE9B630), null)
        }
        "spacesuit" -> {
            clippedToKevin {
                rect(60f, 348f, 220f, 80f, Color(0xFFF4F7F9), null)
                stroke("M60 348 H280", Color(0xFFB8C4CC), 6f)
                rect(146f, 362f, 48f, 30f, Color(0xFF2A2A3A), LINE, 3f, r = 5f)
                oval(158f, 372f, 4f, 4f, Color(0xFFE8483B), null); oval(170f, 372f, 4f, 4f, Color(0xFFFFD21F), null); oval(182f, 372f, 4f, 4f, Color(0xFF4FA34A), null)
                rect(154f, 381f, 32f, 5f, Color(0xFF9ED8F5), null, r = 2f)
                rect(92f, 364f, 22f, 14f, Color(0xFF2E6BD6), null, r = 3f)
            }
            stroke("M78 300 L44 340", Color(0xFFF4F7F9), 13f)
            stroke("M262 300 L300 262", Color(0xFFF4F7F9), 13f)
        }
        "moonboots" -> {
            rect(110f, 430f, 50f, 30f, Color(0xFFE8EEF2), LINE, 3f, r = 12f); rect(180f, 430f, 50f, 30f, Color(0xFFE8EEF2), LINE, 3f, r = 12f)
            stroke("M112 450 H158 M182 450 H228", Color(0xFF9AA7B1), 4f)
        }
        "tuxedo" -> {
            clippedToKevin {
                rect(60f, 348f, 220f, 80f, Color(0xFF1B1B2A), null)
                shape("M146 348 L170 400 L194 348 Z", Color.White, null)
                oval(170f, 374f, 3f, 3f, Color(0xFF1B1B2A), null); oval(170f, 388f, 3f, 3f, Color(0xFF1B1B2A), null)
                stroke("M140 348 L166 404 M200 348 L174 404", Color(0xFF3A3A4A), 4f)
            }
            shape("M152 344 L170 352 L188 344 L188 362 L170 354 L152 362 Z", Color(0xFFE8483B), LINE, 2.5f)
        }
        "fancyshoes" -> {
            oval(134f, 452f, 24f, 10f, Color(0xFF1B1B2A), LINE, 3f); oval(206f, 452f, 24f, 10f, Color(0xFF1B1B2A), LINE, 3f)
            stroke("M120 448 Q130 444 140 447", Color.White.copy(alpha = 0.8f), 3f)
            stroke("M192 448 Q202 444 212 447", Color.White.copy(alpha = 0.8f), 3f)
        }
    }
}

/** the "back" slot art, drawn before Kevin's body so it peeks out from behind him (broom, cape, royalrobe's cloak). */
fun DrawScope.drawClothBack(id: String) {
    when (id) {
        "broom" -> {
            stroke("M40 420 L290 220", Color(0xFF8B5A2B), 10f)
            shape("M20 400 Q10 450 40 470 Q70 470 76 430 Z", Color(0xFFE9B630), LINE, 3f)
            stroke("M30 410 L36 462 M46 414 L50 466 M60 420 L60 462", Color(0xFFB8860B), 2f)
        }
        "royalrobe" -> {
            shape("M106 222 Q36 330 56 446 L284 446 Q304 330 234 222 Z", Color(0xFF6A2E9A), LINE, 4f)
            stroke("M56 446 L284 446", Color.White, 10f)
            stroke("M80 446 h4 M130 446 h4 M180 446 h4 M230 446 h4", Color(0xFF1B1B2A), 4f)
        }
        "cape" -> {
            shape("M110 220 Q40 330 62 440 L278 440 Q300 330 230 220 Z", Color(0xFFE8483B), LINE, 4f)
            stroke("M80 420 L100 440 M260 420 L240 440", Color(0xFFB8326E), 4f)
        }
    }
}

/**
 * Public entry point for drawing Kevin's equipped outfit, in his own stage coordinates.
 * StageArt.kt's drawKevin() should call this at three points (see the final report's
 * integration notes for exact placement): drawOutfit(g.closet, "feet") right after the
 * bare feet, drawOutfit(g.closet, "back") right after the leaf (before the big body
 * oval), and drawOutfit(g.closet, "top") at the very end (on top of the face).
 */
fun DrawScope.drawOutfit(c: ClosetState, part: String) {
    when (part) {
        "feet" -> c.equipped["feet"]?.let { drawClothFront(it) }
        "back" -> c.equipped["back"]?.let { drawClothBack(it) }
        "top" -> for (slot in listOf("body", "face", "hat")) c.equipped[slot]?.let { drawClothFront(it) }
    }
}

/** the closet screen's preview of Kevin wearing his current outfit (the original's previewSVG) */
@Composable
fun ClosetPreview(c: ClosetState, modifier: Modifier) {
    Canvas(modifier) {
        scale(size.width / 300f, size.height / 380f, Offset.Zero) {
            translate(-20f, -100f) { drawClosetPreviewArt(c) }
        }
    }
}

private fun DrawScope.drawClosetPreviewArt(c: ClosetState) {
    oval(170f, 462f, 80f, 10f, Color.Black.copy(alpha = 0.15f), null)
    stroke("M148 398 L142 448 M192 398 L198 448", LINE, 5f)
    oval(136f, 452f, 18f, 9f, Color(0xFFE8483B), LINE, 4f)
    oval(204f, 452f, 18f, 9f, Color(0xFFE8483B), LINE, 4f)
    drawOutfit(c, "feet")
    drawOutfit(c, "back")
    stroke("M78 300 L44 340 M262 300 L300 262", LINE, 6f)
    shape("M174 186 C 190 158 226 158 236 172 C 216 188 192 190 174 186 Z", Color(0xFF3E9B4F), LINE, 4f)
    oval(170f, 400f, 13f, 10f, Color(0xFFFFD21F), LINE, 4f)
    oval(170f, 295f, 95f, 106f, Color(0xFFFFD21F), LINE, 5f)
    oval(118f, 318f, 15f, 9f, Color(0xFFFF9A8B).copy(alpha = 0.75f), null)
    oval(222f, 318f, 15f, 9f, Color(0xFFFF9A8B).copy(alpha = 0.75f), null)
    oval(142f, 276f, 19f, 19f, Color.White, LINE, 4f)
    oval(198f, 276f, 19f, 19f, Color.White, LINE, 4f)
    oval(146f, 278f, 8f, 8f, LINE, null)
    oval(202f, 278f, 8f, 8f, LINE, null)
    stroke("M126 246 Q142 238 156 246 M184 246 Q198 238 214 246", LINE, 5f)
    stroke("M142 330 Q170 360 198 330", LINE, 5f)
    drawOutfit(c, "top")
}
