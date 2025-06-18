package com.kiwi.finanzas

import androidx.compose.ui.graphics.Color
import kotlin.math.roundToInt

data class HSVColor(val hue: Float, val saturation: Float, val value: Float)
fun Color.toHSV(): HSVColor {
    val maxColorComponent = maxOf(red, green, blue)
    val minColorComponent = minOf(red, green, blue)
    val delta = maxColorComponent - minColorComponent

    val hue = when {
        delta == 0f -> 0f
        maxColorComponent == red -> (60 * (((green - blue) / delta) + 6)) % 360
        maxColorComponent == green -> (60 * (((blue - red) / delta) + 2)) % 360
        else -> (60 * (((red - green) / delta) + 4)) % 360
    }

    val saturation = if (maxColorComponent != 0f) delta / maxColorComponent else 0f

    val value = maxColorComponent

    return HSVColor(hue, saturation, value)
}
fun hsvToColor(hue: Float, saturation: Float, value: Float): Color {
    val c = value * saturation
    val x = c * (1 - kotlin.math.abs((hue / 60f) % 2 - 1))
    val m = value - c

    val (r, g, b) = when {
        hue < 60 -> Triple(c, x, 0f)
        hue < 120 -> Triple(x, c, 0f)
        hue < 180 -> Triple(0f, c, x)
        hue < 240 -> Triple(0f, x, c)
        hue < 300 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    return Color(
        red = ((r + m) * 255).roundToInt(),
        green = ((g + m) * 255).roundToInt(),
        blue = ((b + m) * 255).roundToInt()
    )
}
