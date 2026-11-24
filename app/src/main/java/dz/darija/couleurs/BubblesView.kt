package dz.darija.couleurs

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.View
import kotlin.math.sin
import kotlin.random.Random

/** Fond animé : bulles brillantes en relief qui montent doucement. */
class BubblesView(context: Context) : View(context) {

    private class Bubble(var x: Float, var y: Float, val r: Float, val speed: Float, val phase: Float, val shader: Int)

    private val density = resources.displayMetrics.density
    private val bubbles = mutableListOf<Bubble>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shaders = ArrayList<RadialGradient>()
    private val base = intArrayOf(
        0xFFFF8A80.toInt(), 0xFF80D8FF.toInt(), 0xFFB9F6CA.toInt(),
        0xFFFFE57F.toInt(), 0xFFEA80FC.toInt(), 0xFFFFAB40.toInt()
    )
    private var time = 0f

    init {
        isClickable = false
        isFocusable = false
        for (c in base) {
            // cercle unité : reflet en haut à gauche, bord plus sombre
            shaders.add(
                RadialGradient(
                    -0.3f, -0.35f, 1.4f,
                    intArrayOf(
                        Color.argb(190, 255, 255, 255),
                        Color.argb(120, Color.red(c), Color.green(c), Color.blue(c)),
                        Color.argb(90, Color.red(c) / 2, Color.green(c) / 2, Color.blue(c) / 2)
                    ),
                    floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
                )
            )
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        bubbles.clear()
        repeat(9) {
            bubbles.add(newBubble(w, h, true))
        }
    }

    private fun newBubble(w: Int, h: Int, anywhere: Boolean): Bubble {
        val r = (14f + Random.nextFloat() * 30f) * density
        return Bubble(
            Random.nextFloat() * w,
            if (anywhere) Random.nextFloat() * h else h + r,
            r,
            (0.3f + Random.nextFloat() * 0.7f) * density,
            Random.nextFloat() * 6.28f,
            Random.nextInt(shaders.size)
        )
    }

    override fun onDraw(canvas: Canvas) {
        time += 0.02f
        for (i in bubbles.indices) {
            val b = bubbles[i]
            b.y -= b.speed
            b.x += sin(time + b.phase) * 0.5f * density
            if (b.y < -b.r) bubbles[i] = newBubble(width, height, false)
            canvas.save()
            canvas.translate(b.x, b.y)
            canvas.scale(b.r, b.r)
            paint.shader = shaders[b.shader]
            canvas.drawCircle(0f, 0f, 1f, paint)
            canvas.restore()
        }
        postInvalidateOnAnimation()
    }
}
