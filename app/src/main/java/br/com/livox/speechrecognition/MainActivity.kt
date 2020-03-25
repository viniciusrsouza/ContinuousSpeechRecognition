package br.com.livox.speechrecognition

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity(), RecognitionListener {

    private var selectedLanguage: String = "default"
    private lateinit var speechRecognizer: ContinuousSpeechRecognizer

    var textToSpeech: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        speechRecognizer = buildRecognizer()
        textToSpeech = buildTTS()

        initViews()

        Handler().postDelayed({
            //speechRecognizer.startListening()
        }, 3000)
        Log.d(TAG, "onCreate")
        Log.d(TAG, "${Locale.getDefault()}")

        reload.setOnClickListener {
            speechRecognizer.destroy()
            speechRecognizer = buildRecognizer()
            speechRecognizer.startListening()
            textToSpeech?.stop()
            textToSpeech = buildTTS()
        }

        button.setOnClickListener {
            speak()
        }
    }

    private fun initViews() {
        initLanguageSpinner()
    }

    private fun buildTTS(): TextToSpeech {
        return TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val language =
                    if (selectedLanguage == "default") Locale.getDefault()
                        .toString() else selectedLanguage
                val split = language.split("_")
                val lang = split[0]
                val country = split[1]
                textToSpeech?.language = Locale(lang, country)
            }
        }
    }

    private fun initLanguageSpinner() {
        val list = listOf("default", "pt_BR", "en_US", "de_DE", "es_ES", "fr_FR")
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            list
        )
        spinner_language.adapter = adapter
        spinner_language.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                this@MainActivity.selectedLanguage = list[position]
            }
        }
    }

    private fun buildRecognizer(): ContinuousSpeechRecognizer {
        val language =
            if (selectedLanguage == "default") Locale.getDefault().toString() else selectedLanguage

        val maxResultsText = edit_amount_of_results.text.toString()
        val maxResults = if (maxResultsText.isNotEmpty()) maxResultsText.toInt() else 0

        val silenceTimeoutText = edit_silence_timeout.text.toString()
        val silenceTimeout = if (silenceTimeoutText.isNotEmpty()) silenceTimeoutText.toInt() else 0

        val minimumLengthText = edit_minimum_length.text.toString()
        val minimumLength = if (minimumLengthText.isNotEmpty()) minimumLengthText.toInt() else 0

        Log.d(TAG, "Reloading: { Language: $language, maxResults: $maxResults, silenceTimeout: $silenceTimeout, minimumLength: $minimumLength}")

        return ContinuousSpeechRecognizer.Builder(this)
            .setLanguage(language)
            .setMaxResults(maxResults)
            .setRecognitionListener(this)
            .setTimeoutOnSilence(silenceTimeout)
            .setMinimumUtteranceLength(minimumLength)
            .requestPermissions(this)
            .build()
    }

    private fun speak() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val bundle = Bundle()
            bundle.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "stringId")
            textToSpeech?.speak(edit_tts.text, TextToSpeech.QUEUE_FLUSH, bundle, "stringId")
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
        speechRecognizer.stopListening()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        speechRecognizer.reloadSpeechRecognizer()
        speechRecognizer.startListening()
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onPartialResults(partialResults: Bundle?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onEndOfSpeech() {}
    override fun onError(error: Int) {}
    override fun onResults(results: Bundle?) {
        val resultList = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (resultList != null) {
            val text = resultList.reduce { acc, s -> "$acc\n$s" }
            spoken_text_label.text = text
        }
    }

    companion object {
        const val TAG = "SpeechListener"
    }
}
