package br.com.livox.speechrecognition

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.util.Log

/**
 * Receiver for list of supported languages of a SpeechRecognizer
 * @param callback: callback function invoked with the list of
 * supported languages
 */
class LanguageReceiver(val callback: (List<String>?) -> Unit): BroadcastReceiver() {
    private var supportedLanguages: List<String>? = null

    private var languagePreference: String? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "Receiving broadcast")
        val results = getResultExtras(true)
        if (results.containsKey(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE)) {
            languagePreference = results.getString(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE)
        }
        if (results.containsKey(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES)) {
            supportedLanguages = results.getStringArrayList(
                RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES
            )
        }
        callback(supportedLanguages)
    }

    companion object {
        const val TAG = "LanguageReceiver"
    }
}