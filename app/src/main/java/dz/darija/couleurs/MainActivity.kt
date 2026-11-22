package dz.darija.couleurs

import android.Manifest
import android.animation.ObjectAnimator
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.util.Locale

/**
 * ar    : mot affiché
 * say   : mot lu par la synthèse vocale (avec voyelles pour guider la prononciation)
 * latin : transcription pour le parent (sert aussi de nom au fichier d'enregistrement)
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

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var setupLaunched = false
    private lateinit var confetti: ConfettiView
    private lateinit var content: FrameLayout
    private lateinit var modeBtn: TextView
    private lateinit var banner: TextView
    private var gameMode = false
    private var locked = false
    private var target: ColorItem = ITEMS[0]

    private var player: MediaPlayer? = null
    private var recorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var tone: ToneGenerator? = null

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

        banner = TextView(this)
        banner.text = "🔇 Voix arabe introuvable. Touchez ici pour l'installer.\n" +
            "Ou appuyez longuement sur une carte pour enregistrer votre propre voix."
        banner.textSize = 14f
        banner.setTextColor(Color.WHITE)
        banner.setPadding(dp(14), dp(10), dp(14), dp(10))
        banner.background = GradientDrawable().apply {
            setColor(0xFFEF6C00.toInt())
            cornerRadius = dp(16).toFloat()
        }
        banner.visibility = View.GONE
        banner.setOnClickListener { openTtsSetup() }
        val blp = LinearLayout.LayoutParams(MATCH, WRAP)
        blp.setMargins(0, dp(8), 0, 0)
        column.addView(banner, blp)

        content = FrameLayout(this)
        column.addView(content, LinearLayout.LayoutParams(MATCH, 0, 1f))
        root.addView(column, FrameLayout.LayoutParams(MATCH, MATCH))

        confetti = ConfettiView(this)
        root.addView(confetti, FrameLayout.LayoutParams(MATCH, MATCH))

        setContentView(root)
        showLearn()

        tts = TextToSpeech(this, this)
    }

    // ---------- Synthèse vocale ----------

    override fun onInit(status: Int) {
        ttsReady = false
        val engine = tts
        if (status == TextToSpeech.SUCCESS && engine != null) {
            val candidates = listOf(
                Locale("ar", "DZ"), Locale.forLanguageTag("ar"),
                Locale("ar", "SA"), Locale("ar", "EG")
            )
            for (loc in candidates) {
                if (engine.isLanguageAvailable(loc) >= TextToSpeech.LANG_AVAILABLE) {
                    engine.setLanguage(loc)
                    engine.setSpeechRate(0.65f)
                    engine.setPitch(1.15f)
                    ttsReady = true
                    break
                }
            }
        }
        banner.visibility = if (ttsReady) View.GONE else View.VISIBLE
    }

    private fun say(text: String, queue: Boolean = false) {
        if (!ttsReady) return
        tts?.speak(
            text,
            if (queue) TextToSpeech.QUEUE_ADD else TextToSpeech.QUEUE_FLUSH,
            null,
            "u"
        )
    }

    private fun openTtsSetup() {
        val intents = listOf(
            Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA),
            Intent("com.android.settings.TTS_SETTINGS"),
            Intent(android.provider.Settings.ACTION_SETTINGS)
        )
        for (i in intents) {
            try {
                startActivity(i)
                setupLaunched = true
                return
            } catch (e: Exception) {
                // essayer le suivant
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (setupLaunched) {
            setupLaunched = false
            tts?.shutdown()
            ttsReady = false
            tts = TextToSpeech(this, this)
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        player?.release()
        stopRecording()
        tone?.release()
        super.onDestroy()
    }

    // ---------- Voix enregistrée / bips ----------

    private fun voiceFile(item: ColorItem) = File(filesDir, "voix_${item.latin}.m4a")

    private fun playRecording(item: ColorItem): Boolean {
        val f = voiceFile(item)
        if (!f.exists() || f.length() == 0L) return false
        return try {
            player?.release()
            val mp = MediaPlayer()
            mp.setAudioStreamType(AudioManager.STREAM_MUSIC)
            mp.setDataSource(f.absolutePath)
            mp.setOnCompletionListener { m ->
                m.release()
                if (player === m) player = null
            }
            mp.prepare()
            mp.start()
            player = mp
            true
        } catch (e: Exception) {
            player = null
            false
        }
    }

    private fun beep(ok: Boolean) {
        try {
            if (tone == null) tone = ToneGenerator(AudioManager.STREAM_MUSIC, 90)
            tone?.startTone(if (ok) ToneGenerator.TONE_PROP_ACK else ToneGenerator.TONE_PROP_NACK, 250)
        } catch (e: Exception) {
            // ignoré
        }
    }

    /** Dit le nom de la couleur : voix enregistrée > synthèse vocale > bip. */
    private fun sayColor(item: ColorItem, twice: Boolean = false) {
        if (playRecording(item)) return
        if (ttsReady) {
            say(item.say)
            if (twice) say(item.say, true)
        } else {
            beep(true)
        }
    }

    private fun sayPrompt() {
        if (playRecording(target)) return
        if (ttsReady) say("وِينْ ${target.say}؟") else beep(true)
    }

    private fun startRecording(item: ColorItem): Boolean {
        return try {
            val f = voiceFile(item)
            val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(this) else MediaRecorder()
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioSamplingRate(44100)
            r.setAudioEncodingBitRate(96000)
            r.setOutputFile(f.absolutePath)
            r.prepare()
            r.start()
            recorder = r
            recordingFile = f
            true
        } catch (e: Exception) {
            recorder = null
            Toast.makeText(this, "Impossible de démarrer l'enregistrement", Toast.LENGTH_LONG).show()
            false
        }
    }

    private fun stopRecording() {
        val r = recorder ?: return
        try {
            r.stop()
        } catch (e: Exception) {
            recordingFile?.delete() // enregistrement trop court
        }
        r.release()
        recorder = null
    }

    private fun showRecordDialog(item: ColorItem) {
        if (Build.VERSION.SDK_INT >= 23 &&
            checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1)
            Toast.makeText(this, "Autorisez le micro, puis refaites un appui long", Toast.LENGTH_LONG).show()
            return
        }
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(20), dp(16), dp(20), dp(8))
        val info = TextView(this)
        info.text = "Votre voix pour « ${item.ar} » (${item.latin})"
        info.textSize = 18f
        info.setPadding(0, 0, 0, dp(10))
        box.addView(info)
        val recBtn = Button(this)
        recBtn.text = "🎙️ Enregistrer"
        val playBtn = Button(this)
        playBtn.text = "▶ Écouter"
        val delBtn = Button(this)
        delBtn.text = "🗑 Supprimer"
        box.addView(recBtn)
        box.addView(playBtn)
        box.addView(delBtn)

        val dialog = AlertDialog.Builder(this)
            .setView(box)
            .setPositiveButton("Fermer", null)
            .create()

        recBtn.setOnClickListener {
            if (recorder == null) {
                if (startRecording(item)) recBtn.text = "⏹ Arrêter"
            } else {
                stopRecording()
                recBtn.text = "🎙️ Enregistrer"
                Toast.makeText(this, "Enregistré ✅", Toast.LENGTH_SHORT).show()
            }
        }
        playBtn.setOnClickListener {
            if (!playRecording(item)) Toast.makeText(this, "Aucun enregistrement", Toast.LENGTH_SHORT).show()
        }
        delBtn.setOnClickListener {
            voiceFile(item).delete()
            Toast.makeText(this, "Supprimé", Toast.LENGTH_SHORT).show()
        }
        dialog.setOnDismissListener { stopRecording() }
        dialog.show()
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
                val card = makeCard(item, true, dp(120)) { c, shape ->
                    bounce(shape)
                    burst(c, item.color)
                    sayColor(item, true)
                }
                card.setOnLongClickListener {
                    showRecordDialog(item)
                    true
                }
                row.addView(card, lp)
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
        prompt.setOnClickListener { sayPrompt() }
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
        content.postDelayed({ if (gameMode) sayPrompt() }, 500)
    }

    private fun onChoice(item: ColorItem, card: View, shape: ShapeView) {
        if (locked) return
        if (item == target) {
            locked = true
            bounce(shape)
            burst(card, item.color)
            beep(true)
            say(if ((0..1).random() == 0) "بْرَافُو" else "مْلِيح")
            content.postDelayed({
                locked = false
                if (gameMode) startRound()
            }, 1700)
        } else {
            ObjectAnimator.ofFloat(card, View.TRANSLATION_X, 0f, -dp(14).toFloat(), dp(14).toFloat(),
                -dp(10).toFloat(), dp(10).toFloat(), 0f).setDuration(400).start()
            if (ttsReady) {
                say("لَا، هَادَا ${item.say}")
            } else if (!playRecording(item)) {
                beep(false)
            }
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
