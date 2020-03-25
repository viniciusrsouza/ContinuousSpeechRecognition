package br.com.livox.speechrecognition

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class ContinuousSpeechRecognizer
private constructor(
    private val context: Context,
    private val recognizerIntent: Intent,
    private val recognitionListener: RecognitionListener
) {
    private var isListening = false
    private var recognizer: SpeechRecognizer? = null

    fun startListening() {
        isListening = true
        recognizer?.startListening(recognizerIntent)
    }

    fun reloadSpeechRecognizer() {
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            recognizer?.setRecognitionListener(recognitionListener)
        } else throw RecognitionNotAvailableException()
    }

    fun stopListening() {
        isListening = false
        recognizer?.stopListening()
    }

    init {
        reloadSpeechRecognizer()
    }

    class Builder(private val context: Context) : RecognitionListener {
        private var timeoutOnSilence: Int = 0
        private val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        private var language: String = ""
        private var maxResults: Int = 0
        private var listener: RecognitionListener? = null
        private val recognizer: ContinuousSpeechRecognizer by lazy {
            ContinuousSpeechRecognizer(context, recognizerIntent, this)
        }

        init {
            recognizerIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
        }

        fun setLanguage(language: String): Builder {
            this.language = language
            return this
        }

        fun setMaxResults(maxResults: Int): Builder {
            this.maxResults = maxResults
            return this
        }

        fun setRecognitionListener(listener: RecognitionListener): Builder {
            this.listener = listener
            return this
        }

        fun setTimeoutOnSilence(timeoutOnSilence: Int): Builder {
            this.timeoutOnSilence = timeoutOnSilence
            return this
        }

        fun requestPermissions(activity: Activity): Builder {
            if (lacksPermissions()) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    PERMISSION_RECORD_AUDIO
                )
            }
            return this
        }

        private fun lacksPermissions(): Boolean {
            val permissionCheck =
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            return permissionCheck == PackageManager.PERMISSION_DENIED
        }

        fun build(): ContinuousSpeechRecognizer {
            if (maxResults == 0) maxResults = 3
            if (language.isEmpty()) language = "en"

            recognizerIntent.putExtra(
                RecognizerIntent.EXTRA_MAX_RESULTS,
                maxResults
            )

            recognizerIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                language
            )

            return recognizer
        }

        override fun onReadyForSpeech(params: Bundle?) {
            listener?.onReadyForSpeech(params)
        }

        override fun onRmsChanged(rmsdB: Float) {
            listener?.onRmsChanged(rmsdB)
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            listener?.onBufferReceived(buffer)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            listener?.onPartialResults(partialResults)
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            listener?.onEvent(eventType, params)
        }

        override fun onBeginningOfSpeech() {
            listener?.onBeginningOfSpeech()
        }

        override fun onEndOfSpeech() {
            listener?.onEndOfSpeech()
        }

        override fun onError(error: Int) {
            listener?.onError(error)
            logError(error)
            recognizer.reloadSpeechRecognizer()
            if (recognizer.isListening) {
                recognizer.startListening()
            }
        }

        override fun onResults(results: Bundle?) {
            listener?.onResults(results)
            if (recognizer.isListening) {
                recognizer.startListening()
            }
        }

        private fun logError(error: Int) {
            val message = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions provided"
                SpeechRecognizer.ERROR_NETWORK -> "Network Error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No match"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service is busy"
                SpeechRecognizer.ERROR_SERVER -> "Server side error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                else -> "Unknown"
            }
            Log.d("SpeechListener", message)
        }

        companion object {
            const val PERMISSION_RECORD_AUDIO = 1
        }
    }

    class RecognitionNotAvailableException(msg: String? = null) : Exception(msg)
}