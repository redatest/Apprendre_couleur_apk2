package dz.darija.couleurs

import android.animation.ObjectAnimator
import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

/**
 * ar    : mot affiché
 * say   : mot lu par la synthèse vocale (avec voyelles pour guider la prononciation)
 * latin : transcription pour le parent
 * Modifiez librement cette liste pour ajuster le vocabulaire (darija varie selon les régions).
 */
data class ColorItem(val color: Int, val ar: String, val say: String, val latin: String)

private val ITEMS = listOf(
    ColorItem(0xFFE53935.toInt(), "حمر", "حْمَر", "hmar"),
    ColorItem(0xFF1E88E5.toInt(), "زرق", "زْرَق", "zerq"),
    ColorItem(0xFF43A047.toInt(), "خضر", "خْضَر", "khdhar"),
    ColorItem(0xFFFDD835.toInt(), "صفر", "صْفَر", "sfar"),
    ColorItem(0xFFFB8C00.toInt(), "برتقالي", "بُرْتُقَالِي", "bortoqali"),
    ColorItem(0xFF8E24AA.toInt(), "بنفسجي", "بَنَفْسَجِي", "banafsaji"),
    ColorItem(0xFFF06292.toInt(), "وردي", "وَرْدِي", "wardi"),
    ColorItem(0xFFFFFFFF.toInt(), "بيض", "بْيَض", "bayad"),
    ColorItem(0xFF212121.toInt(), "كحل", "كَحْل", "kahl"),
    ColorItem(0xFF6D4C41.toInt(), "قهوي", "قَهْوِي", "qahwi")
)

private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

class MainActivity : Activity(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private var ttsReady = false
    private lateinit var confetti: ConfettiView
    private lateinit var content: FrameLayout
    private lateinit var modeBtn: TextView
    private var gameMode = false
    private var locked = false
    private var target: ColorItem = ITEMS[0]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)

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

    // ---------- Synthèse vocale ----------

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val r = tts.setLanguage(Locale.forLanguageTag("ar"))
            ttsReady = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
            tts.setSpeechRate(0.65f)
            tts.setPitch(1.15f)
        }
        if (!ttsReady) {
            Toast.makeText(
                this,
                "Installez une voix arabe : Paramètres > Synthèse vocale (Google)",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun say(text: String, queue: Boolean = false) {
        if (!ttsReady) return
        tts.speak(
            text,
            if (queue) TextToSpeech.QUEUE_ADD else TextToSpeech.QUEUE_FLUSH,
            null,
            "u"
        )
    }

    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
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
                    say(item.say)
                    say(item.say, true)
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
        prompt.text = "🔊  وِين ${target.ar}؟"
        prompt.textSize = 40f
        prompt.gravity = Gravity.CENTER
        prompt.setTypeface(null, Typeface.BOLD)
        prompt.setTextColor(0xFF4E342E.toInt())
        prompt.setPadding(0, 0, 0, dp(10))
        prompt.setOnClickListener { say("وِينْ ${target.say}؟") }
        col.addView(prompt, LinearLayout.LayoutParams(MATCH, WRAP))

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
        content.postDelayed({ if (gameMode) say("وِينْ ${target.say}؟") }, 500)
    }

    private fun onChoice(item: ColorItem, card: View, shape: ShapeView) {
        if (locked) return
        if (item == target) {
            locked = true
            bounce(shape)
            burst(card, item.color)
            say(if ((0..1).random() == 0) "بْرَافُو" else "مْلِيح")
            content.postDelayed({
                locked = false
                if (gameMode) startRound()
            }, 1700)
        } else {
            ObjectAnimator.ofFloat(card, View.TRANSLATION_X, 0f, -dp(14).toFloat(), dp(14).toFloat(),
                -dp(10).toFloat(), dp(10).toFloat(), 0f).setDuration(400).start()
            say("لَا، هَادَا ${item.say}")
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
