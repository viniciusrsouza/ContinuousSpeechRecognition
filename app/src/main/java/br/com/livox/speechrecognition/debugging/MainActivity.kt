package br.com.livox.speechrecognition.debugging

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import br.com.livox.speechrecognition.ContinuousSpeechRecognizer
import br.com.livox.speechrecognition.LanguageReceiver
import br.com.livox.speechrecognition.R
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity(), RecognitionListener {

    private var selectedLanguage: String = "default"
    private lateinit var speechRecognizer: ContinuousSpeechRecognizer

    private var textToSpeech: TextToSpeech?
        get() = (application as App).tts
        set(value) {
            (application as App).tts = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        speechRecognizer = buildRecognizer()
        if (textToSpeech == null) textToSpeech = buildTTS()


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
        }

        button.setOnClickListener {
            speak()
        }

        btn_edit_tts.setOnClickListener {
            startActivity(Intent(this, TTSActivity::class.java))
        }
        val languageReceiver = LanguageReceiver { supportedLanguages ->
            Log.d(TAG, supportedLanguages.toString())
            if (supportedLanguages != null)
                initLanguageSpinner(supportedLanguages)
        }
        sendOrderedBroadcast(
            RecognizerIntent.getVoiceDetailsIntent(this),
            null,
            languageReceiver,
            null,
            Activity.RESULT_OK,
            null,
            null
        )

        sw_mute_while_listening.setOnCheckedChangeListener { _, isChecked ->
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if(isChecked) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
            } else {
                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val index = (0.5 * maxVolume).toInt()
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index, AudioManager.FLAG_VIBRATE)
            }
        }
    }

    private fun buildTTS(): TextToSpeech {
        return TextToSpeech(applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.getDefault()
            }
        }
    }

    private fun initLanguageSpinner(list: List<String>) {
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

    // get field values and build recognizer
    private fun buildRecognizer(): ContinuousSpeechRecognizer {
        val maxResultsText = edit_amount_of_results.text.toString()
        val maxResults = if (maxResultsText.isNotEmpty()) maxResultsText.toInt() else 0

        val silenceTimeoutText = edit_silence_timeout.text.toString()
        val silenceTimeout = if (silenceTimeoutText.isNotEmpty()) silenceTimeoutText.toInt() else 0

        val minimumLengthText = edit_minimum_length.text.toString()
        val minimumLength = if (minimumLengthText.isNotEmpty()) minimumLengthText.toInt() else 0

        Log.d(
            TAG,
            "Reloading: { Language: $selectedLanguage, maxResults: $maxResults, silenceTimeout: $silenceTimeout, minimumLength: $minimumLength}"
        )

        return ContinuousSpeechRecognizer.Builder(this)
            .setLanguage(selectedLanguage)
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
            val audioAttributes = if (sw_use_frontal_speaker.isChecked) {
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            } else {
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            }
            textToSpeech?.setAudioAttributes(audioAttributes)
            textToSpeech?.setOnUtteranceProgressListener(utteranceListener)
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

    private val utteranceListener = object : UtteranceProgressListener() {
        val TAG = "UtterListener"
        override fun onAudioAvailable(utteranceId: String?, audio: ByteArray?) {}
        override fun onError(utteranceId: String?) {
            Log.d(TAG,"onError $utteranceId")
        }
        override fun onDone(utteranceId: String?) {
            Log.d(TAG,"onDone $utteranceId")
            if(sw_mute_while_listening.isChecked) {
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_VIBRATE)
            }
            runOnUiThread {
                speechRecognizer.startListening()
            }
        }

        override fun onStart(utteranceId: String?) {
            Log.d(TAG,"onStart $utteranceId")
            runOnUiThread {
                speechRecognizer.stopListening()
            }
            if(sw_mute_while_listening.isChecked) {
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val index = (0.5 * maxVolume).toInt()
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index, AudioManager.FLAG_VIBRATE)
            }
        }

        override fun onStop(utteranceId: String?, interrupted: Boolean) {
            Log.d(TAG,"onStop $utteranceId, $interrupted")
            if(!interrupted){
                if(sw_mute_while_listening.isChecked) {
                    val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_VIBRATE)
                }
                runOnUiThread {
                    speechRecognizer.startListening()
                }
            }
        }
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
