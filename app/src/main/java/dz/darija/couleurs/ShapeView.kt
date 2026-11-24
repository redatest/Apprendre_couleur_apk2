package dz.darija.couleurs

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.os.SystemClock
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// Éclairage (espace caméra) : lumière en haut à gauche, devant.
private const val LX = -0.398f
private const val LY = 0.696f
private const val LZ = 0.597f
private const val HX = -0.223f
private const val HY = 0.390f
private const val HZ = 0.894f
private const val CAM = 3.2f // distance de la caméra

/** Maillage 3D : sommets (x,y,z) et faces (listes d'indices). Les normales sont orientées vers l'extérieur. */
private class Mesh(val v: FloatArray, val faces: List<IntArray>) {
    val normals = FloatArray(faces.size * 3)

    init {
        for (i in faces.indices) {
            val f = faces[i]
            val a = f[0]
            val b = f[1]
            val c = f[2]
            val ux = v[b * 3] - v[a * 3]
            val uy = v[b * 3 + 1] - v[a * 3 + 1]
            val uz = v[b * 3 + 2] - v[a * 3 + 2]
            val wx = v[c * 3] - v[a * 3]
            val wy = v[c * 3 + 1] - v[a * 3 + 1]
            val wz = v[c * 3 + 2] - v[a * 3 + 2]
            var nx = uy * wz - uz * wy
            var ny = uz * wx - ux * wz
            var nz = ux * wy - uy * wx
            val len = sqrt(nx * nx + ny * ny + nz * nz)
            if (len > 1e-6f) {
                nx /= len
                ny /= len
                nz /= len
            }
            var px = 0f
            var py = 0f
            var pz = 0f
            for (k in f) {
                px += v[k * 3]
                py += v[k * 3 + 1]
                pz += v[k * 3 + 2]
            }
            px /= f.size
            py /= f.size
            pz /= f.size
            if (nx * px + ny * py + nz * pz < 0f) {
                nx = -nx
                ny = -ny
                nz = -nz
            }
            normals[i * 3] = nx
            normals[i * 3 + 1] = ny
            normals[i * 3 + 2] = nz
        }
    }
}

private object Shapes {
    val cube: Mesh by lazy {
        val s = 0.75f
        Mesh(
            floatArrayOf(
                -s, -s, -s, s, -s, -s, s, s, -s, -s, s, -s,
                -s, -s, s, s, -s, s, s, s, s, -s, s, s
            ),
            listOf(
                intArrayOf(4, 5, 6, 7), intArrayOf(1, 0, 3, 2), intArrayOf(5, 1, 2, 6),
                intArrayOf(0, 4, 7, 3), intArrayOf(7, 6, 2, 3), intArrayOf(4, 0, 1, 5)
            )
        )
    }

    val pyramid: Mesh by lazy {
        val s = 0.75f
        val h = 0.85f
        Mesh(
            floatArrayOf(-s, -h, -s, s, -h, -s, s, -h, s, -s, -h, s, 0f, 1f, 0f),
            listOf(
                intArrayOf(0, 1, 2, 3), intArrayOf(3, 2, 4), intArrayOf(2, 1, 4),
                intArrayOf(1, 0, 4), intArrayOf(0, 3, 4)
            )
        )
    }

    val star: Mesh by lazy {
        val o = FloatArray(20)
        for (i in 0 until 10) {
            val r = if (i % 2 == 0) 1.0f else 0.5f
            val a = (-PI / 2.0 + i * PI / 5.0).toFloat()
            o[i * 2] = r * cos(a)
            o[i * 2 + 1] = -r * sin(a)
        }
        extrude(o, 0.33f)
    }

    val heart: Mesh by lazy {
        val n = 40
        val o = FloatArray(n * 2)
        for (i in 0 until n) {
            val t = 2.0 * PI * i / n
            val x = 16.0 * sin(t).pow(3)
            val y = 13.0 * cos(t) - 5.0 * cos(2.0 * t) - 2.0 * cos(3.0 * t) - cos(4.0 * t)
            o[i * 2] = (x / 17.0).toFloat()
            o[i * 2 + 1] = (y / 17.0 + 0.15).toFloat()
        }
        extrude(o, 0.36f)
    }

    /** Extrude un contour 2D (x,y alternés) en prisme d'épaisseur 2*half. */
    private fun extrude(o: FloatArray, half: Float): Mesh {
        val n = o.size / 2
        val v = FloatArray(n * 2 * 3)
        for (i in 0 until n) {
            v[i * 3] = o[i * 2]
            v[i * 3 + 1] = o[i * 2 + 1]
            v[i * 3 + 2] = half
            v[(n + i) * 3] = o[i * 2]
            v[(n + i) * 3 + 1] = o[i * 2 + 1]
            v[(n + i) * 3 + 2] = -half
        }
        val faces = ArrayList<IntArray>()
        faces.add(IntArray(n) { it })
        faces.add(IntArray(n) { n + it })
        for (i in 0 until n) {
            val j = (i + 1) % n
            faces.add(intArrayOf(i, j, n + j, n + i))
        }
        return Mesh(v, faces)
    }
}

/** mesh == null -> sphère. (ax,ay,az) = point du visage, (fnx,fny,fnz) = sa normale, faceU = taille du visage. */
private class Spec(
    val mesh: Mesh?, val ax: Float, val ay: Float, val az: Float,
    val fnx: Float, val fny: Float, val fnz: Float, val faceU: Float
)

private fun specFor(type: Int): Spec = when (type % 5) {
    0 -> Spec(null, 0f, 0f, 0.95f, 0f, 0f, 1f, 0.50f)
    1 -> Spec(Shapes.cube, 0f, 0f, 0.75f, 0f, 0f, 1f, 0.42f)
    2 -> Spec(Shapes.pyramid, 0f, -0.30f, 0.53f, 0f, 0.376f, 0.927f, 0.34f)
    3 -> Spec(Shapes.star, 0f, -0.03f, 0.33f, 0f, 0f, 1f, 0.30f)
    else -> Spec(Shapes.heart, 0f, 0.02f, 0.36f, 0f, 0f, 1f, 0.36f)
}

/**
 * Solide 3D (rendu logiciel) : éclairé, qui oscille en continu, ombre au sol, visage qui suit la rotation.
 * spin() fait tourner la forme sur elle-même.
 */
class ShapeView(context: Context, private val shapeColor: Int, private val type: Int) : View(context) {

    private val density = resources.displayMetrics.density
    private val spec = specFor(type)
    private val phase = Random.nextFloat() * 6.28f
    private var spin = 0f
    private var spinAnim: ValueAnimator? = null
    private var bob: ObjectAnimator? = null

    private val br = Color.red(shapeColor)
    private val bg = Color.green(shapeColor)
    private val bb = Color.blue(shapeColor)
    private val dark = (0.299f * br + 0.587f * bg + 0.114f * bb) < 70f

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeWidth = 1.5f * density
    }
    private val facePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val spherePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val hiPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(150, 255, 255, 255) }
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.5f * density
    }
    private val path = Path()
    private val oval = RectF()
    private var shadowR = 0f

    // tampons de calcul (évite les allocations à chaque image)
    private val rv = FloatArray(128 * 3)
    private val sxs = FloatArray(128)
    private val sys = FloatArray(128)
    private val fnx = FloatArray(64)
    private val fny = FloatArray(64)
    private val fnz = FloatArray(64)
    private val fz = FloatArray(64)
    private val fidx = IntArray(64)
    private val ord = IntArray(64)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val m = min(w, h).toFloat()
        shadowR = m * 0.36f
        shadowPaint.shader = RadialGradient(
            w / 2f, h * 0.87f, shadowR, 0x66000000, 0x00000000, Shader.TileMode.CLAMP
        )
        val r = m * 0.40f
        val cx = w / 2f
        val cy = h * 0.44f
        spherePaint.shader = RadialGradient(
            cx - r * 0.32f, cy - r * 0.38f, r * 1.55f,
            intArrayOf(mixColor(shapeColor, Color.WHITE, 0.65f), shapeColor, mixColor(shapeColor, Color.BLACK, 0.55f)),
            floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP
        )
        rimPaint.color = mixColor(shapeColor, Color.BLACK, 0.45f)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val deg = PI.toFloat() / 180f
        val t = (SystemClock.uptimeMillis() % 200000L) / 1000f
        val ry = (20f + 22f * sin(t * 1.5f + phase)) * deg + spin
        val rx = (14f + 7f * sin(t * 1.1f + phase * 1.7f)) * deg
        val cry = cos(ry)
        val sry = sin(ry)
        val crx = cos(rx)
        val srx = sin(rx)
        val m = min(w, h)
        val cx = w * 0.5f
        val cy = h * 0.44f
        val s = m * 0.36f

        // ombre au sol (reste fixe pendant que la forme flotte)
        val rise = (-translationY / (7f * density)).coerceIn(0f, 1f)
        val sc = 1f - 0.15f * rise
        canvas.save()
        canvas.translate(0f, -translationY)
        canvas.scale(sc, 0.22f * sc, cx, h * 0.87f)
        shadowPaint.alpha = (255f * (1f - 0.4f * rise)).toInt()
        canvas.drawCircle(cx, h * 0.87f, shadowR, shadowPaint)
        canvas.restore()

        val mesh = spec.mesh
        if (mesh == null) {
            drawSphere(canvas, cx, cy, m * 0.40f)
        } else {
            drawMesh(canvas, mesh, cry, sry, crx, srx, cx, cy, s)
        }
        drawFace(canvas, cry, sry, crx, srx, cx, cy, s)
        postInvalidateOnAnimation()
    }

    private fun drawSphere(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        canvas.drawCircle(cx, cy, r, spherePaint)
        canvas.drawCircle(cx, cy, r, rimPaint)
        val px = cx - r * 0.36f
        val py = cy - r * 0.42f
        canvas.save()
        canvas.rotate(-35f, px, py)
        oval.set(px - r * 0.22f, py - r * 0.12f, px + r * 0.22f, py + r * 0.12f)
        canvas.drawOval(oval, hiPaint)
        canvas.restore()
    }

    private fun drawMesh(
        canvas: Canvas, mesh: Mesh, cry: Float, sry: Float, crx: Float, srx: Float,
        cx: Float, cy: Float, s: Float
    ) {
        val nv = mesh.v.size / 3
        for (i in 0 until nv) {
            val x = mesh.v[i * 3]
            val y = mesh.v[i * 3 + 1]
            val z = mesh.v[i * 3 + 2]
            val x1 = x * cry + z * sry
            val z1 = -x * sry + z * cry
            val y2 = y * crx - z1 * srx
            val z2 = y * srx + z1 * crx
            rv[i * 3] = x1
            rv[i * 3 + 1] = y2
            rv[i * 3 + 2] = z2
            val f = CAM / (CAM - z2)
            sxs[i] = cx + x1 * f * s
            sys[i] = cy - y2 * f * s
        }

        // faces visibles (dos des faces éliminé)
        var nvis = 0
        for (fi in mesh.faces.indices) {
            val nx0 = mesh.normals[fi * 3]
            val ny0 = mesh.normals[fi * 3 + 1]
            val nz0 = mesh.normals[fi * 3 + 2]
            val nx1 = nx0 * cry + nz0 * sry
            val nz1 = -nx0 * sry + nz0 * cry
            val ny2 = ny0 * crx - nz1 * srx
            val nz2 = ny0 * srx + nz1 * crx
            val f = mesh.faces[fi]
            var mx = 0f
            var my = 0f
            var mz = 0f
            for (k in f) {
                mx += rv[k * 3]
                my += rv[k * 3 + 1]
                mz += rv[k * 3 + 2]
            }
            mx /= f.size
            my /= f.size
            mz /= f.size
            if (nx1 * (0f - mx) + ny2 * (0f - my) + nz2 * (CAM - mz) <= 0f) continue
            fnx[nvis] = nx1
            fny[nvis] = ny2
            fnz[nvis] = nz2
            fz[nvis] = mz
            fidx[nvis] = fi
            nvis++
        }

        // tri : du plus loin au plus proche
        for (a in 0 until nvis) ord[a] = a
        for (a in 1 until nvis) {
            val key = ord[a]
            var b = a - 1
            while (b >= 0 && fz[ord[b]] > fz[key]) {
                ord[b + 1] = ord[b]
                b--
            }
            ord[b + 1] = key
        }

        for (j in 0 until nvis) {
            val q = ord[j]
            val dif = max(0f, fnx[q] * LX + fny[q] * LY + fnz[q] * LZ)
            val spc = max(0f, fnx[q] * HX + fny[q] * HY + fnz[q] * HZ).pow(30f) * 0.5f
            val k = 0.5f + 0.55f * dif
            val lift = if (dark) 34f * dif else 0f
            val r = (br * k + lift + 255f * spc).toInt().coerceIn(0, 255)
            val g = (bg * k + lift + 255f * spc).toInt().coerceIn(0, 255)
            val b = (bb * k + lift + 255f * spc).toInt().coerceIn(0, 255)
            val col = Color.rgb(r, g, b)
            val f = mesh.faces[fidx[q]]
            path.reset()
            path.moveTo(sxs[f[0]], sys[f[0]])
            for (n in 1 until f.size) path.lineTo(sxs[f[n]], sys[f[n]])
            path.close()
            fill.color = col
            canvas.drawPath(path, fill)
            line.color = col
            canvas.drawPath(path, line)
        }
    }

    /** Visage collé à la surface : suit la rotation, s'aplatit de profil, disparaît quand il est derrière. */
    private fun drawFace(
        canvas: Canvas, cry: Float, sry: Float, crx: Float, srx: Float,
        cx: Float, cy: Float, s: Float
    ) {
        val x1 = spec.ax * cry + spec.az * sry
        val z1 = -spec.ax * sry + spec.az * cry
        val y2 = spec.ay * crx - z1 * srx
        val z2 = spec.ay * srx + z1 * crx
        val nx1 = spec.fnx * cry + spec.fnz * sry
        val nz1 = -spec.fnx * sry + spec.fnz * cry
        val ny2 = spec.fny * crx - nz1 * srx
        val nz2 = spec.fny * srx + nz1 * crx
        val vis = nx1 * (0f - x1) + ny2 * (0f - y2) + nz2 * (CAM - z2)
        if (vis <= 0f || nz2 < 0.12f) return

        val f = CAM / (CAM - z2)
        val px = cx + x1 * f * s
        val py = cy - y2 * f * s
        val u = spec.faceU * f * s
        val alpha = ((nz2 - 0.12f) / 0.25f).coerceIn(0f, 1f)
        val ink = if (dark) Color.WHITE else 0xFF3E2723.toInt()

        canvas.save()
        canvas.translate(px, py)
        canvas.scale(max(0.2f, nz2), 1f)

        facePaint.style = Paint.Style.FILL
        facePaint.color = Color.argb(90, 255, 82, 82) // joues
        facePaint.alpha = (90f * alpha).toInt()
        canvas.drawCircle(-0.66f * u, 0.14f * u, 0.14f * u, facePaint)
        canvas.drawCircle(0.66f * u, 0.14f * u, 0.14f * u, facePaint)

        facePaint.color = ink
        facePaint.alpha = (255f * alpha).toInt()
        canvas.drawCircle(-0.36f * u, -0.12f * u, 0.10f * u, facePaint)
        canvas.drawCircle(0.36f * u, -0.12f * u, 0.10f * u, facePaint)

        facePaint.style = Paint.Style.STROKE
        facePaint.strokeWidth = max(2f, 0.07f * u)
        facePaint.strokeCap = Paint.Cap.ROUND
        oval.set(-0.30f * u, -0.02f * u, 0.30f * u, 0.40f * u)
        canvas.drawArc(oval, 20f, 140f, false, facePaint)
        canvas.restore()
    }

    /** Fait tourner la forme sur elle-même (un tour). */
    fun spin() {
        spinAnim?.cancel()
        val a = ValueAnimator.ofFloat(0f, 2f * PI.toFloat())
        a.duration = 900
        a.interpolator = DecelerateInterpolator()
        a.addUpdateListener {
            spin = it.animatedValue as Float
            invalidate()
        }
        a.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                spin = 0f
            }
        })
        spinAnim = a
        a.start()
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
        spinAnim?.cancel()
        super.onDetachedFromWindow()
    }
}
