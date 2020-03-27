package br.com.livox.speechrecognition.debugging

import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import br.com.livox.speechrecognition.R
import kotlinx.android.synthetic.main.activity_config_tts.*
import java.util.*

class TTSActivity : AppCompatActivity() {
    private val app: App by lazy { application as App }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config_tts)

        initViews()
    }

    private fun initViews() {
        initEngines()
        btn_speak.setOnClickListener {
            speak(text_to_speak.text.toString())
        }
    }

    private fun speak(text: String) {
        val bundle = Bundle()
        bundle.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "stringId")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            app.tts?.speak(text, TextToSpeech.QUEUE_FLUSH, bundle, "stringId")
        } else {
            val ks = bundle.keySet()
            val array = ks.map { it to bundle[it].toString() }.toTypedArray()
            val map = hashMapOf(*array)
            @Suppress("DEPRECATION")
            app.tts?.speak(text, TextToSpeech.QUEUE_FLUSH, map)
        }
    }

    private fun initEngines() {
        val engines = app.tts?.engines!!
        val adapter = ArrayAdapter(
            this,
            R.layout.support_simple_spinner_dropdown_item,
            engines.map { it.label }
        )

        spinner_engines.adapter = adapter
        spinner_engines.setSelection(engines.indexOfFirst { app.selectedEngine?.name == it.name })
        spinner_engines.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, i: Long) {
                app.selectedEngine = engines[pos]
                reloadTTS()
            }
        }
    }

    private fun initLanguages() {
        val languages = Locale
            .getAvailableLocales()
            .filter {
                app.tts?.isLanguageAvailable(it) == TextToSpeech.LANG_COUNTRY_AVAILABLE
            }
        Log.d(TAG, languages.toString())
        val adapter = ArrayAdapter(
            this,
            R.layout.support_simple_spinner_dropdown_item,
            languages
        )
        spinner_languages.adapter = adapter
        spinner_languages.setSelection(languages.indexOf(app.selectedLanguage))
        spinner_languages.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}

            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, i: Long) {
                app.selectedLanguage = languages[pos]
                app.tts?.language = app.selectedLanguage
                initVoices()
            }
        }
    }

    private fun initVoices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val voicesList = app.tts?.voices?.toList() ?: emptyList()
            val voices = voicesList.filter {
                it.locale.isO3Language == app.selectedLanguage.isO3Language &&
                        it.locale.isO3Country == app.selectedLanguage.isO3Country
            }
            Log.d(TAG, voices.toString())
            Log.d(TAG, "selectedLanguage: $app.selectedLanguage")
            val adapter = ArrayAdapter(
                this,
                R.layout.support_simple_spinner_dropdown_item,
                voices.map { it.name }
            )
            spinner_voices.adapter = adapter
            spinner_voices.setSelection(voices.indexOfFirst { app.selectedVoice?.name == it.name })
            spinner_voices.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: AdapterView<*>?) {}

                override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, i: Long) {
                    app.selectedVoice = voices[pos]
                    app.tts?.voice = app.selectedVoice
                }
            }
        }
    }

    private fun reloadTTS() {
        Log.d(
            TAG,
            "{selectedEngine: {label: ${app.selectedEngine?.label}, name: ${app.selectedEngine?.name}}}}"
        )
        app.tts = buildTTS(app.selectedEngine?.name)
    }

    private fun buildTTS(engine: String? = ""): TextToSpeech {
        return TextToSpeech(applicationContext, { status ->
            if (status == TextToSpeech.SUCCESS) {
                app.tts?.language = app.selectedLanguage
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    if(app.selectedVoice != null)
                        app.tts?.voice = app.selectedVoice
                }
                initLanguages()
            }
        }, engine)
    }

    companion object {
        const val TAG = "TTSListener"
    }
}