package dz.darija.couleurs

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.View
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Forme colorée avec un petit visage souriant qui "flotte" doucement. */
class ShapeView(context: Context, private val shapeColor: Int, private val type: Int) : View(context) {

    private val density = resources.displayMetrics.density
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = shapeColor
    }
    private val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f * density
        strokeJoin = Paint.Join.ROUND
        color = 0x55000000
    }
    private val face = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private var bob: ObjectAnimator? = null

    private fun isDark(c: Int): Boolean =
        (0.299 * Color.red(c) + 0.587 * Color.green(c) + 0.114 * Color.blue(c)) < 110

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val pad = edge.strokeWidth
        path.reset()
        var faceY = h * 0.5f
        when (type % 5) {
            0 -> path.addCircle(w / 2, h / 2, minOf(w, h) / 2 - pad, Path.Direction.CW)
            1 -> path.addRoundRect(RectF(pad, pad, w - pad, h - pad), w * 0.18f, w * 0.18f, Path.Direction.CW)
            2 -> {
                path.moveTo(w / 2, pad)
                path.lineTo(w - pad, h - pad)
                path.lineTo(pad, h - pad)
                path.close()
                faceY = h * 0.66f
            }
            3 -> {
                val cx = w / 2
                val cy = h / 2 + h * 0.03f
                val ro = minOf(w, h) / 2 - pad
                val ri = ro * 0.55f
                for (i in 0 until 10) {
                    val r = if (i % 2 == 0) ro else ri
                    val a = -PI / 2 + i * PI / 5
                    val x = cx + (r * cos(a)).toFloat()
                    val y = cy + (r * sin(a)).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                faceY = h * 0.52f
            }
            else -> {
                path.moveTo(w / 2, h * 0.92f)
                path.cubicTo(w * -0.12f, h * 0.45f, w * 0.15f, h * 0.02f, w / 2, h * 0.3f)
                path.cubicTo(w * 0.85f, h * 0.02f, w * 1.12f, h * 0.45f, w / 2, h * 0.92f)
                path.close()
                faceY = h * 0.45f
            }
        }
        canvas.drawPath(path, fill)
        canvas.drawPath(path, edge)

        // Visage
        val faceColor = if (isDark(shapeColor)) Color.WHITE else 0xFF333333.toInt()
        val ex = w * 0.16f
        face.color = faceColor
        face.style = Paint.Style.FILL
        canvas.drawCircle(w / 2 - ex, faceY - h * 0.04f, w * 0.045f, face)
        canvas.drawCircle(w / 2 + ex, faceY - h * 0.04f, w * 0.045f, face)
        face.style = Paint.Style.STROKE
        face.strokeWidth = w * 0.035f
        face.strokeCap = Paint.Cap.ROUND
        canvas.drawArc(RectF(w / 2 - ex, faceY, w / 2 + ex, faceY + h * 0.16f), 20f, 140f, false, face)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        bob = ObjectAnimator.ofFloat(this, View.TRANSLATION_Y, 0f, -7f * density, 0f).apply {
            duration = 1100L + Random.nextLong(700)
            repeatCount = ValueAnimator.INFINITE
            start()
        }
    }

    override fun onDetachedFromWindow() {
        bob?.cancel()
        super.onDetachedFromWindow()
    }
}
