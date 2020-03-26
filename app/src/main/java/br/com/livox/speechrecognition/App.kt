package br.com.livox.speechrecognition

import android.app.Application
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.*

class App : Application() {
    var tts: TextToSpeech? = null
    var selectedLanguage: Locale = Locale.getDefault()
    var selectedEngine: TextToSpeech.EngineInfo? = null
    var selectedVoice: Voice? = null
}