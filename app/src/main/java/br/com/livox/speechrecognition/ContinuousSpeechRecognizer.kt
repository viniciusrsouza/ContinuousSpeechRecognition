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

/**
 * Class that wraps a [SpeechRecognizer] to make it
 * listen continuously.
 */
class ContinuousSpeechRecognizer
private constructor(
    private val context: Context,
    private val recognizerIntent: Intent,
    private val recognitionListener: RecognitionListener
) {
    private var isListening = false
    private var recognizer: SpeechRecognizer? = null

    /**
     * starts the recognition service
     */
    fun startListening() {
        isListening = true
        recognizer?.startListening(recognizerIntent)
    }

    /**
     * destroys the [SpeechRecognizer] instance and
     * recreates it.
     */
    fun reloadSpeechRecognizer() {
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            recognizer?.setRecognitionListener(recognitionListener)
        } else throw RecognitionNotAvailableException()
    }

    /**
     * destroy the [SpeechRecognizer] instance
     */
    fun destroy() {
        isListening = false
        recognizer?.destroy()
    }

    /**
     * stops the [SpeechRecognizer] instance but
     * keeps it to restart listening later
     */
    fun stopListening() {
        isListening = false
        recognizer?.stopListening()
    }

    init {
        reloadSpeechRecognizer()
    }

    /**
     * BuilderClass to wrap all the settings required
     * for the [SpeechRecognizer]
     */
    class Builder(private val context: Context) : RecognitionListener {
        private var minimumUtteranceLength: Int = 0
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

        /**
         * sets the listening language of the [SpeechRecognizer]
         * @param language: language in the format ll-RR where
         * l = language, R = Region
         */
        fun setLanguage(language: String): Builder {
            this.language = language
            return this
        }

        /**
         * sets the maximum results provided by the [SpeechRecognizer]
         */
        fun setMaxResults(maxResults: Int): Builder {
            this.maxResults = maxResults
            return this
        }

        /**
         * sets the recognition listener.
         */
        fun setRecognitionListener(listener: RecognitionListener): Builder {
            this.listener = listener
            return this
        }

        /**
         * sets the time, in milliseconds, that the [SpeechRecognizer]
         * will wait when there's no speech to understand that the
         * speech has finished.
         * Experimental
         */
        fun setTimeoutOnSilence(timeoutOnSilence: Int): Builder {
            this.timeoutOnSilence = timeoutOnSilence
            return this
        }

        /**
         * sets the minimum time that the [SpeechRecognizer] will
         * record.
         * Experimental
         */
        fun setMinimumUtteranceLength(minimumUtteranceLength: Int): Builder {
            this.minimumUtteranceLength = minimumUtteranceLength
            return this
        }

        /**
         * Requests permissions for recording audio if it's
         * not already provided.
         */
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

        /**
         * checks if Audio recording permissions were already
         * provided
         * @return true if it lacks permissions
         */
        private fun lacksPermissions(): Boolean {
            val permissionCheck =
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            return permissionCheck == PackageManager.PERMISSION_DENIED
        }

        /**
         * assemble preset parameters into an instance of
         * a [ContinuousSpeechRecognizer]
          */
        fun build(): ContinuousSpeechRecognizer {
            Log.d(
                "SpeechListener",
                "maxResults: $maxResults, language: $language, timeoutOnSilence: $timeoutOnSilence, minimumLength: $minimumUtteranceLength"
            )
            if (maxResults == 0) maxResults = 3
            if (language.isEmpty()) language = "en"

            recognizerIntent.putExtra(
                RecognizerIntent.EXTRA_MAX_RESULTS,
                maxResults
            )

            recognizerIntent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                language
            )

            if (timeoutOnSilence != 0) {
                recognizerIntent.putExtra(
                    RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                    timeoutOnSilence
                )
            }

            if (minimumUtteranceLength != 0) {
                recognizerIntent.putExtra(
                    RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,
                    minimumUtteranceLength
                )
            }

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

        /**
         * reloads and restarts the recognizer onError.
         */
        override fun onError(error: Int) {
            listener?.onError(error)
            logError(error)
            recognizer.reloadSpeechRecognizer()
            if (recognizer.isListening) {
                recognizer.startListening()
            }
        }

        /**
         * restarts the recognizer onResult.
         */
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