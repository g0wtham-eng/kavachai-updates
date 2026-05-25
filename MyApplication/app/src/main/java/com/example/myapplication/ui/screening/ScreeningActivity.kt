package com.example.myapplication.ui.screening

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.myapplication.ui.theme.MyApplicationTheme

import android.speech.tts.TextToSpeech
import java.util.Locale
import androidx.compose.runtime.LaunchedEffect

class ScreeningActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    companion object {
        const val EXTRA_PHONE_NUMBER = "EXTRA_PHONE_NUMBER"
    }

    private val viewModel: ScreeningViewModel by viewModels()
    private var tts: TextToSpeech? = null
    private var lastSpokenMessageIndex = -1

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Text-To-Speech engine
        tts = TextToSpeech(this, this)
        
        // Handle showing over lock screen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        enableEdgeToEdge()

        val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: "Unknown"
        viewModel.startScreening(phoneNumber)

        setContent {
            MyApplicationTheme {
                val state by viewModel.uiState.collectAsState()
                
                // Observe the dynamic AI screening dialogue transcript and speak new lines out loud!
                LaunchedEffect(state.transcript) {
                    speakNewMessages(state.transcript)
                }

                ScreeningScreen(
                    state = state,
                    onClose = { finish() }
                )
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
        }
    }

    private fun speakNewMessages(messages: List<String>) {
        if (tts == null) return
        if (messages.size > lastSpokenMessageIndex + 1) {
            for (i in (lastSpokenMessageIndex + 1) until messages.size) {
                val message = messages[i]
                
                // Clean the dialogue label prefixes for standard natural reading
                val speakText = message
                    .replace("KavachAI:", "Kavach A I says:")
                    .replace("Caller (Robo-AI):", "The automated caller says:")
                    .replace("Caller:", "The caller says:")
                
                // Differentiate voices: high pitch for KavachAI security, deep/robotic pitch for scammer!
                if (message.startsWith("KavachAI:")) {
                    tts?.setPitch(1.2f)       // High-pitched protective AI voice
                    tts?.setSpeechRate(1.05f)
                } else {
                    tts?.setPitch(0.75f)      // Deep, metallic robo-scammer voice
                    tts?.setSpeechRate(0.9f)
                }
                
                tts?.speak(speakText, TextToSpeech.QUEUE_ADD, null, "MSG_$i")
            }
            lastSpokenMessageIndex = messages.size - 1
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
