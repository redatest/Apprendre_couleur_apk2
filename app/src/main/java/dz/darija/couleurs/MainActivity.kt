package dz.darija.couleurs

import android.animation.ObjectAnimator
import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * ar    : mot affiché (arabe standard)
 * latin : transcription pour le parent ; en : nom anglais
 * sound : prononciation (fichiers du dossier colors/, convertis dans res/raw)
 */
data class ColorItem(val color: Int, val ar: String, val latin: String, val en: String, val sound: Int)

private val ITEMS = listOf(
    ColorItem(0xFFE53935.toInt(), "أحمر", "ahmar", "red", R.raw.c_ahmar),
    ColorItem(0xFF1E88E5.toInt(), "أزرق", "azraq", "blue", R.raw.c_azraq),
    ColorItem(0xFF43A047.toInt(), "أخضر", "akhdar", "green", R.raw.c_akhdar),
    ColorItem(0xFFFDD835.toInt(), "أصفر", "asfar", "yellow", R.raw.c_asfar),
    ColorItem(0xFFFB8C00.toInt(), "برتقالي", "burtuqali", "orange", R.raw.c_burtuqali),
    ColorItem(0xFF8E24AA.toInt(), "بنفسجي", "banafsaji", "purple", R.raw.c_banafsaji),
    ColorItem(0xFFF06292.toInt(), "وردي", "wardi", "pink", R.raw.c_wardi),
    ColorItem(0xFFFFFFFF.toInt(), "أبيض", "abyad", "white", R.raw.c_abyad),
    ColorItem(0xFF212121.toInt(), "أسود", "aswad", "black", R.raw.c_aswad),
    ColorItem(0xFF6D4C41.toInt(), "بني", "bunni", "brown", R.raw.c_bunni)
)

// « wow » (tes fichiers) revient plus souvent que les félicitations de la voix de synthèse
private val PRAISE = intArrayOf(
    R.raw.pr_wow, R.raw.pr_wow, R.raw.pr_raai, R.raw.pr_mumtaz, R.raw.pr_ahsanta
)

private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

class MainActivity : Activity() {

    private lateinit var rootLayout: FrameLayout
    private lateinit var confetti: ConfettiView
    private lateinit var content: FrameLayout
    private lateinit var modeBtn: TextView
    private var gameMode = false
    private var locked = false
    private var target: ColorItem = ITEMS[0]

    private var player: MediaPlayer? = null
    private var playToken = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        volumeControlStream = AudioManager.STREAM_MUSIC

        val root = FrameLayout(this)
        rootLayout = root
        root.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(0xFF81D4FA.toInt(), 0xFFE1F5FE.toInt(), 0xFFFFF8E1.toInt())
        )
        root.addView(BubblesView(this), FrameLayout.LayoutParams(MATCH, MATCH))

        val column = LinearLayout(this)
        column.orientation = LinearLayout.VERTICAL
        column.setPadding(dp(12), dp(28), dp(12), dp(12))

        val header = LinearLayout(this)
        header.gravity = Gravity.CENTER_VERTICAL
        val title = TextView(this)
        title.text = "🌈 الألوان"
        title.textSize = 32f
        title.setTypeface(null, Typeface.BOLD)
        title.setTextColor(0xFFFFFFFF.toInt())
        title.setShadowLayer(1f, 0f, dp(4).toFloat(), 0xFF5D4037.toInt()) // lettres « extrudées »
        header.addView(title, LinearLayout.LayoutParams(0, WRAP, 1f))

        modeBtn = TextView(this)
        modeBtn.text = "🎮 لعب"
        modeBtn.textSize = 22f
        modeBtn.setTextColor(Color.WHITE)
        modeBtn.setTypeface(null, Typeface.BOLD)
        modeBtn.gravity = Gravity.CENTER
        val depth = dp(7)
        modeBtn.setPadding(dp(22), dp(10), dp(22), dp(10) + depth)
        modeBtn.background = button3d(0xFF9575CD.toInt(), 0xFF7E57C2.toInt(), 0xFF4527A0.toInt(), dp(28), depth)
        modeBtn.elevation = dp(6).toFloat()
        modeBtn.cameraDistance = resources.displayMetrics.density * 1600f
        attachPressEffect(modeBtn, 10f)
        modeBtn.setOnClickListener { toggleMode() }
        header.addView(modeBtn, LinearLayout.LayoutParams(WRAP, WRAP))
        column.addView(header, LinearLayout.LayoutParams(MATCH, WRAP))

        content = FrameLayout(this)
        column.addView(content, LinearLayout.LayoutParams(MATCH, 0, 1f))
        root.addView(column, FrameLayout.LayoutParams(MATCH, MATCH))

        confetti = ConfettiView(this)
        root.addView(confetti, FrameLayout.LayoutParams(MATCH, MATCH))

        setContentView(root)
        showLearn()
    }

    // ---------- Sons intégrés (res/raw) ----------

    /** Joue les clips les uns après les autres ; un nouvel appel interrompt le précédent. */
    private fun play(vararg ids: Int, onDone: (() -> Unit)? = null) {
        playToken++
        player?.release()
        player = null
        playNext(ids.toList(), playToken, onDone)
    }

    private fun playNext(ids: List<Int>, token: Int, onDone: (() -> Unit)?) {
        if (token != playToken) return
        if (ids.isEmpty()) {
            onDone?.invoke() // fin de la dernière prononciation (si non interrompue)
            return
        }
        val mp = MediaPlayer.create(this, ids[0])
        if (mp == null) {
            playNext(ids.drop(1), token, onDone)
            return
        }
        player = mp
        mp.setOnCompletionListener { m ->
            m.release()
            if (player === m) player = null
            content.postDelayed({ playNext(ids.drop(1), token, onDone) }, 120)
        }
        mp.start()
    }

    /** Pouce levé qui apparaît au centre de la carte puis disparaît. */
    private fun showThumb(card: View) {
        val tv = TextView(this)
        tv.text = "👍"
        tv.textSize = 72f
        tv.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val a = IntArray(2)
        val b = IntArray(2)
        card.getLocationInWindow(a)
        rootLayout.getLocationInWindow(b)
        val lp = FrameLayout.LayoutParams(WRAP, WRAP)
        lp.leftMargin = a[0] - b[0] + card.width / 2 - tv.measuredWidth / 2
        lp.topMargin = a[1] - b[1] + card.height / 2 - tv.measuredHeight / 2
        tv.alpha = 0f
        tv.scaleX = 0.2f
        tv.scaleY = 0.2f
        rootLayout.addView(tv, lp)
        tv.animate().alpha(1f).scaleX(1.3f).scaleY(1.3f)
            .setInterpolator(OvershootInterpolator(2.5f)).setDuration(350)
            .withEndAction {
                tv.animate().alpha(0f).setStartDelay(600).setDuration(300)
                    .withEndAction { rootLayout.removeView(tv) }.start()
            }.start()
    }

    override fun onDestroy() {
        playToken++
        player?.release()
        player = null
        super.onDestroy()
    }

    // ---------- Modes ----------

    private fun toggleMode() {
        gameMode = !gameMode
        locked = false
        modeBtn.text = if (gameMode) "📚 تعلّم" else "🎮 لعب"
        if (gameMode) startRound() else showLearn()
    }

    private fun showLearn() {
        play() // coupe le son en cours
        content.removeAllViews()
        val scroll = ScrollView(this)
        scroll.isVerticalScrollBarEnabled = false
        val list = LinearLayout(this)
        list.orientation = LinearLayout.VERTICAL
        list.setPadding(0, dp(8), 0, dp(24))
        ITEMS.chunked(2).forEach { pair ->
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            pair.forEach { item ->
                val lp = LinearLayout.LayoutParams(0, WRAP, 1f)
                lp.setMargins(dp(6), dp(6), dp(6), dp(6))
                row.addView(makeCard(item, true, dp(120)) { card, shape ->
                    bounce(shape)
                    burst(card, item.color)
                    play(item.sound, onDone = { showThumb(card) })
                }, lp)
            }
            list.addView(row, LinearLayout.LayoutParams(MATCH, WRAP))
        }
        scroll.addView(list, ViewGroup.LayoutParams(MATCH, WRAP))
        content.addView(scroll, FrameLayout.LayoutParams(MATCH, MATCH))
    }

    private fun startRound() {
        content.removeAllViews()
        target = ITEMS.random()
        val choices = (ITEMS.filter { it != target }.shuffled().take(2) + target).shuffled()

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.gravity = Gravity.CENTER

        val prompt = TextView(this)
        prompt.text = "🔊  أين اللون؟"
        prompt.textSize = 34f
        prompt.gravity = Gravity.CENTER
        prompt.setTypeface(null, Typeface.BOLD)
        prompt.setTextColor(0xFFFFFFFF.toInt())
        prompt.setShadowLayer(1f, 0f, dp(4).toFloat(), 0xFF5D4037.toInt())
        prompt.setPadding(0, 0, 0, dp(6))
        prompt.setOnClickListener { play(R.raw.p_ayna, target.sound) }
        col.addView(prompt, LinearLayout.LayoutParams(MATCH, WRAP))

        val word = TextView(this)
        word.text = target.ar
        word.textSize = 48f
        word.gravity = Gravity.CENTER
        word.setTypeface(null, Typeface.BOLD)
        word.setTextColor(0xFFFFFFFF.toInt())
        word.setShadowLayer(1f, 0f, dp(5).toFloat(), 0xFF5D4037.toInt())
        col.addView(word, LinearLayout.LayoutParams(MATCH, WRAP))

        val latin = TextView(this)
        latin.text = "${target.latin} · ${target.en}"
        latin.textSize = 16f
        latin.gravity = Gravity.CENTER
        latin.setTextColor(0xFF5D4037.toInt())
        latin.setPadding(0, dp(4), 0, dp(24))
        col.addView(latin, LinearLayout.LayoutParams(MATCH, WRAP))

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        choices.forEach { item ->
            val lp = LinearLayout.LayoutParams(0, WRAP, 1f)
            lp.setMargins(dp(5), 0, dp(5), 0)
            row.addView(makeCard(item, false, dp(90)) { card, shape -> onChoice(item, card, shape) }, lp)
        }
        col.addView(row, LinearLayout.LayoutParams(MATCH, WRAP))

        content.addView(col, FrameLayout.LayoutParams(MATCH, MATCH))
        content.postDelayed({ if (gameMode) play(R.raw.p_ayna, target.sound) }, 500)
    }

    private fun onChoice(item: ColorItem, card: View, shape: ShapeView) {
        if (locked) return
        if (item == target) {
            locked = true
            bounce(shape)
            burst(card, item.color)
            play(PRAISE.random(), onDone = { showThumb(card) })
            content.postDelayed({
                locked = false
                if (gameMode) startRound()
            }, 2600)
        } else {
            ObjectAnimator.ofFloat(card, View.TRANSLATION_X, 0f, -dp(14).toFloat(), dp(14).toFloat(),
                -dp(10).toFloat(), dp(10).toFloat(), 0f).setDuration(400).start()
            play(R.raw.p_la_hadha, item.sound)
        }
    }

    // ---------- Éléments d'interface 3D ----------

    /** Bouton « en relief » : face dégradée + tranche plus sombre en dessous. */
    private fun button3d(top: Int, bottom: Int, edge: Int, radius: Int, depth: Int): LayerDrawable {
        val side = GradientDrawable()
        side.setColor(edge)
        side.cornerRadius = radius.toFloat()
        val face = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(top, bottom))
        face.cornerRadius = radius.toFloat()
        val ld = LayerDrawable(arrayOf(side, face))
        ld.setLayerInset(0, 0, depth, 0, 0)
        ld.setLayerInset(1, 0, 0, 0, depth)
        return ld
    }

    /** Effet 3D au toucher : la carte s'incline vers le doigt, s'enfonce, puis rebondit. */
    private fun attachPressEffect(v: View, tiltDeg: Float) {
        v.setOnTouchListener { view, e ->
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    val nx = (e.x / view.width.coerceAtLeast(1) - 0.5f) * 2f
                    val ny = (e.y / view.height.coerceAtLeast(1) - 0.5f) * 2f
                    view.animate().cancel()
                    view.rotationY = nx * tiltDeg
                    view.rotationX = -ny * tiltDeg
                    view.scaleX = 0.95f
                    view.scaleY = 0.95f
                    view.translationZ = -dp(3).toFloat()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    view.animate().rotationX(0f).rotationY(0f).scaleX(1f).scaleY(1f)
                        .translationZ(0f)
                        .setInterpolator(OvershootInterpolator(3f)).setDuration(380).start()
                }
            }
            false // laisse passer le clic
        }
    }

    private fun makeCard(
        item: ColorItem,
        label: Boolean,
        shapeSize: Int,
        onTap: (LinearLayout, ShapeView) -> Unit
    ): LinearLayout {
        val depth = dp(8)
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.gravity = Gravity.CENTER_HORIZONTAL
        card.setPadding(dp(8), dp(12), dp(8), dp(12) + depth)
        card.background = button3d(
            Color.WHITE,
            mixColor(Color.WHITE, item.color, 0.25f),
            mixColor(item.color, Color.BLACK, 0.3f),
            dp(28), depth
        )
        card.elevation = dp(8).toFloat()
        card.cameraDistance = resources.displayMetrics.density * 1600f
        attachPressEffect(card, 14f)

        val shape = ShapeView(this, item.color, ITEMS.indexOf(item))
        card.addView(shape, LinearLayout.LayoutParams(shapeSize, shapeSize))

        if (label) {
            val ar = TextView(this)
            ar.text = item.ar
            ar.textSize = 34f
            ar.gravity = Gravity.CENTER
            ar.setTypeface(null, Typeface.BOLD)
            ar.setTextColor(0xFF4E342E.toInt())
            card.addView(ar, LinearLayout.LayoutParams(MATCH, WRAP))

            val lat = TextView(this)
            lat.text = "${item.latin} · ${item.en}"
            lat.textSize = 16f
            lat.gravity = Gravity.CENTER
            lat.setTextColor(0xFF8D6E63.toInt())
            card.addView(lat, LinearLayout.LayoutParams(MATCH, WRAP))
        }
        card.setOnClickListener { onTap(card, shape) }
        return card
    }

    private fun bounce(shape: ShapeView) {
        shape.spin()
        shape.animate().cancel()
        shape.animate()
            .scaleX(1.3f).scaleY(1.3f).setDuration(200)
            .withEndAction {
                shape.animate().scaleX(1f).scaleY(1f)
                    .setInterpolator(OvershootInterpolator(3f)).setDuration(380).start()
            }.start()
    }

    private fun burst(card: View, color: Int) {
        val a = IntArray(2)
        val b = IntArray(2)
        card.getLocationInWindow(a)
        confetti.getLocationInWindow(b)
        confetti.burst(a[0] - b[0] + card.width / 2f, a[1] - b[1] + card.height / 2f, color)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
