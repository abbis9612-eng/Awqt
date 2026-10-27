package com.dcc.yawm.shared.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The icon's meaning colour; brighter on the dark paper (prototype `brightness(1.55)`). */
fun accentOf(name: String, dark: Boolean): Color {
    val c = Color(YawmIconData[name]?.accent ?: 0xFF555555)
    return if (dark) lerp(c, Color.White, 0.3f) else c
}

/** One of our 31 icons: ink outline, one part in [accent]. Never mirrored in RTL. */
@Composable
fun YIcon(name: String, size: Dp, ink: Color, accent: Color, modifier: Modifier = Modifier) {
    val data = YawmIconData[name] ?: YawmIconData.getValue("page")
    val paths = remember(name) { data.parts.map { PathParser().parsePathString(it.d).toPath() } }
    Canvas(modifier.size(size)) {
        val k = this.size.width / 24f
        scale(k, pivot = Offset.Zero) {
            data.parts.forEachIndexed { i, part ->
                if (part.filled) drawPath(paths[i], accent)
                else drawPath(paths[i], ink, style = Stroke(width = part.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}

enum class TileSize(val box: Dp, val radius: Dp, val icon: Dp) {
    XS(28.dp, 9.dp, 18.dp), SM(34.dp, 11.dp, 20.dp), MD(42.dp, 13.dp, 24.dp), BIG(54.dp, 17.dp, 30.dp),
}

/** An icon on its tile: prayers solid in their colour with a white icon, the rest a 13% tint. */
@Composable
fun Tile(name: String, solid: Boolean, size: TileSize = TileSize.MD, done: Boolean = false) {
    val pal = LocalPalette.current
    val accent = if (done) pal.grey else accentOf(name, pal.isDark && !solid)
    val bg = if (solid) accent else lerp(pal.paper, accent, 0.13f)
    Box(Modifier.size(size.box).background(bg, RoundedCornerShape(size.radius)), contentAlignment = Alignment.Center) {
        if (solid) YIcon(name, size.icon, Color.White, Color.White)
        else YIcon(name, size.icon, pal.ink, accent)
    }
}
