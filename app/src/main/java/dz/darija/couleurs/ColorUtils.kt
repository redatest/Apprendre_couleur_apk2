package dz.darija.couleurs

import android.graphics.Color

/** Mélange deux couleurs : t = 0 -> a, t = 1 -> b. */
fun mixColor(a: Int, b: Int, t: Float): Int {
    val r = (Color.red(a) + (Color.red(b) - Color.red(a)) * t).toInt().coerceIn(0, 255)
    val g = (Color.green(a) + (Color.green(b) - Color.green(a)) * t).toInt().coerceIn(0, 255)
    val bl = (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t).toInt().coerceIn(0, 255)
    return Color.rgb(r, g, bl)
}
