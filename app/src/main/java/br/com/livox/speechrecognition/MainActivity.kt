package br.com.livox.speechrecognition

import android.media.AudioAttributes
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity(), RecognitionListener {

    private val speechRecognizer: ContinuousSpeechRecognizer by lazy {
        ContinuousSpeechRecognizer.Builder(this)
            .setLanguage("en-US")
            .setMaxResults(3)
            .setRecognitionListener(this)
            .requestPermissions(this)
            .build()
    }

    var textToSpeech: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.ENGLISH
            }
        }

        Handler().postDelayed({
            //speechRecognizer.startListening()
        }, 3000)
        Log.d("SpeechListener", "onCreate")
        Log.d("SpeechListener", "${Locale.getDefault()}")

        button.setOnClickListener {
            speak()
        }
    }

    private fun speak() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val bundle = Bundle()
            bundle.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "stringId")
            textToSpeech?.speak("Hello, World!", TextToSpeech.QUEUE_FLUSH, bundle, "stringId")
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d("SpeechListener", "onPause")
        speechRecognizer.stopListening()
    }

    override fun onResume() {
        super.onResume()
        Log.d("SpeechListener", "onResume")
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
}
