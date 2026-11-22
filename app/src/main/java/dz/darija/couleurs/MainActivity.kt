package dz.darija.couleurs

import android.animation.ObjectAnimator
import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * ar    : mot affiché (arabe standard)
 * latin : transcription pour le parent
 * sound : clip audio du nom de la couleur (res/raw)
 * ask   : clip audio « أين الـ… ؟ » utilisé dans le jeu
 */
data class ColorItem(val color: Int, val ar: String, val latin: String, val sound: Int, val ask: Int)

private val ITEMS = listOf(
    ColorItem(0xFFE53935.toInt(), "أحمر", "ahmar", R.raw.c_ahmar, R.raw.q_ahmar),
    ColorItem(0xFF1E88E5.toInt(), "أزرق", "azraq", R.raw.c_azraq, R.raw.q_azraq),
    ColorItem(0xFF43A047.toInt(), "أخضر", "akhdar", R.raw.c_akhdar, R.raw.q_akhdar),
    ColorItem(0xFFFDD835.toInt(), "أصفر", "asfar", R.raw.c_asfar, R.raw.q_asfar),
    ColorItem(0xFFFB8C00.toInt(), "برتقالي", "burtuqali", R.raw.c_burtuqali, R.raw.q_burtuqali),
    ColorItem(0xFF8E24AA.toInt(), "بنفسجي", "banafsaji", R.raw.c_banafsaji, R.raw.q_banafsaji),
    ColorItem(0xFFF06292.toInt(), "وردي", "wardi", R.raw.c_wardi, R.raw.q_wardi),
    ColorItem(0xFFFFFFFF.toInt(), "أبيض", "abyad", R.raw.c_abyad, R.raw.q_abyad),
    ColorItem(0xFF212121.toInt(), "أسود", "aswad", R.raw.c_aswad, R.raw.q_aswad),
    ColorItem(0xFF6D4C41.toInt(), "بني", "bunni", R.raw.c_bunni, R.raw.q_bunni)
)

private val PRAISE = intArrayOf(R.raw.pr_raai, R.raw.pr_mumtaz, R.raw.pr_ahsanta)

private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

class MainActivity : Activity() {

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
        root.setBackgroundColor(0xFFFFF8E1.toInt())

        val column = LinearLayout(this)
        column.orientation = LinearLayout.VERTICAL
        column.setPadding(dp(12), dp(28), dp(12), dp(12))

        val header = LinearLayout(this)
        header.gravity = Gravity.CENTER_VERTICAL
        val title = TextView(this)
        title.text = "🌈 الألوان"
        title.textSize = 30f
        title.setTypeface(null, Typeface.BOLD)
        title.setTextColor(0xFF4E342E.toInt())
        header.addView(title, LinearLayout.LayoutParams(0, WRAP, 1f))

        modeBtn = TextView(this)
        modeBtn.text = "🎮 لعب"
        modeBtn.textSize = 22f
        modeBtn.setTextColor(Color.WHITE)
        modeBtn.setTypeface(null, Typeface.BOLD)
        modeBtn.setPadding(dp(20), dp(10), dp(20), dp(10))
        modeBtn.background = GradientDrawable().apply {
            setColor(0xFF7E57C2.toInt())
            cornerRadius = dp(28).toFloat()
        }
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
    private fun play(vararg ids: Int) {
        playToken++
        player?.release()
        player = null
        playNext(ids.toList(), playToken)
    }

    private fun playNext(ids: List<Int>, token: Int) {
        if (ids.isEmpty() || token != playToken) return
        val mp = MediaPlayer.create(this, ids[0]) ?: return
        player = mp
        mp.setOnCompletionListener { m ->
            m.release()
            if (player === m) player = null
            content.postDelayed({ playNext(ids.drop(1), token) }, 150)
        }
        mp.start()
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
                    play(item.sound, item.sound)
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
        prompt.text = "🔊  أين ال${target.ar}؟"
        prompt.textSize = 32f
        prompt.gravity = Gravity.CENTER
        prompt.setTypeface(null, Typeface.BOLD)
        prompt.setTextColor(0xFF4E342E.toInt())
        prompt.setPadding(0, 0, 0, dp(6))
        prompt.setOnClickListener { play(target.ask) }
        col.addView(prompt, LinearLayout.LayoutParams(MATCH, WRAP))

        val word = TextView(this)
        word.text = target.ar
        word.textSize = 44f
        word.gravity = Gravity.CENTER
        word.setTypeface(null, Typeface.BOLD)
        word.setTextColor(0xFF4E342E.toInt())
        col.addView(word, LinearLayout.LayoutParams(MATCH, WRAP))

        val latin = TextView(this)
        latin.text = target.latin
        latin.textSize = 16f
        latin.gravity = Gravity.CENTER
        latin.setTextColor(0xFF8D6E63.toInt())
        latin.setPadding(0, 0, 0, dp(24))
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
        content.postDelayed({ if (gameMode) play(target.ask) }, 500)
    }

    private fun onChoice(item: ColorItem, card: View, shape: ShapeView) {
        if (locked) return
        if (item == target) {
            locked = true
            bounce(shape)
            burst(card, item.color)
            play(PRAISE.random())
            content.postDelayed({
                locked = false
                if (gameMode) startRound()
            }, 1900)
        } else {
            ObjectAnimator.ofFloat(card, View.TRANSLATION_X, 0f, -dp(14).toFloat(), dp(14).toFloat(),
                -dp(10).toFloat(), dp(10).toFloat(), 0f).setDuration(400).start()
            play(R.raw.p_la_hadha, item.sound)
        }
    }

    // ---------- Éléments d'interface ----------

    private fun makeCard(
        item: ColorItem,
        label: Boolean,
        shapeSize: Int,
        onTap: (LinearLayout, ShapeView) -> Unit
    ): LinearLayout {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.gravity = Gravity.CENTER_HORIZONTAL
        card.setPadding(dp(8), dp(14), dp(8), dp(14))
        card.background = GradientDrawable().apply {
            setColor(Color.WHITE)
            cornerRadius = dp(28).toFloat()
            setStroke(dp(4), darker(item.color))
        }
        card.elevation = dp(4).toFloat()

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
            lat.text = item.latin
            lat.textSize = 16f
            lat.gravity = Gravity.CENTER
            lat.setTextColor(0xFF8D6E63.toInt())
            card.addView(lat, LinearLayout.LayoutParams(MATCH, WRAP))
        }
        card.setOnClickListener { onTap(card, shape) }
        return card
    }

    private fun bounce(shape: View) {
        shape.animate().cancel()
        shape.animate()
            .scaleX(1.35f).scaleY(1.35f).rotationBy(360f).setDuration(220)
            .withEndAction {
                shape.animate().scaleX(1f).scaleY(1f)
                    .setInterpolator(OvershootInterpolator(3f)).setDuration(350).start()
            }.start()
    }

    private fun burst(card: View, color: Int) {
        val a = IntArray(2)
        val b = IntArray(2)
        card.getLocationInWindow(a)
        confetti.getLocationInWindow(b)
        confetti.burst(a[0] - b[0] + card.width / 2f, a[1] - b[1] + card.height / 2f, color)
    }

    private fun darker(c: Int): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(c, hsv)
        hsv[2] *= 0.75f
        return Color.HSVToColor(hsv)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()
}
