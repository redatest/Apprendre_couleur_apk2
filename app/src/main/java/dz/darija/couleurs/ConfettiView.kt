package dz.darija.couleurs

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Calque transparent qui affiche des confettis à l'endroit touché. */
class ConfettiView(context: Context) : View(context) {

    private class Particle(
        var x: Float, var y: Float, val vx: Float, var vy: Float,
        var life: Float, val color: Int, val size: Float
    )

    private val particles = mutableListOf<Particle>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val extra = intArrayOf(0xFFFFD54F.toInt(), 0xFFFF8A65.toInt(), 0xFF4FC3F7.toInt(), 0xFF81C784.toInt())

    init {
        isClickable = false
        isFocusable = false
    }

    fun burst(cx: Float, cy: Float, color: Int) {
        val d = resources.displayMetrics.density
        repeat(34) { i ->
            val a = Random.nextFloat() * 2f * PI.toFloat()
            val s = (4f + Random.nextFloat() * 11f) * d
            val c = if (i % 3 == 0) extra[Random.nextInt(extra.size)] else color
            particles.add(
                Particle(
                    cx, cy, cos(a) * s, sin(a) * s - 5f * d, 1f, c,
                    (4f + Random.nextFloat() * 6f) * d
                )
            )
        }
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        val d = resources.displayMetrics.density
        val it = particles.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.x += p.vx
            p.y += p.vy
            p.vy += 0.55f * d
            p.life -= 0.018f
            if (p.life <= 0f) {
                it.remove()
                continue
            }
            paint.color = p.color
            paint.alpha = (255 * p.life).toInt().coerceIn(0, 255)
            canvas.drawCircle(p.x, p.y, p.size, paint)
        }
        if (particles.isNotEmpty()) postInvalidateOnAnimation()
    }
}
