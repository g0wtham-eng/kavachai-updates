package com.example.myapplication.ui.screening

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.myapplication.ui.theme.MyApplicationTheme
import java.util.Locale

class ScreeningActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    companion object {
        const val EXTRA_PHONE_NUMBER = "EXTRA_PHONE_NUMBER"
        private const val TAG = "ScreeningActivity"
    }

    private val viewModel: ScreeningViewModel by viewModels()
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private val pendingMessages = mutableListOf<String>()
    private var lastSpokenIndex = -1
    private lateinit var audioManager: AudioManager

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show over lock screen
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

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        // Initialize TTS FIRST before anything else (Moved to AIVoiceService)
        // tts = TextToSpeech(this, this)

        val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: "Unknown"
        viewModel.setPhoneNumber(phoneNumber)

        // Listen for transcripts from InCallService
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
                if (intent?.action == "com.canara.kavachai.NEW_TRANSCRIPT") {
                    val msg = intent.getStringExtra("message") ?: return
                    viewModel.addTranscriptMessage(msg)
                } else if (intent?.action == "com.canara.kavachai.CALL_ENDED") {
                    finish()
                }
            }
        }
        val filter = android.content.IntentFilter().apply {
            addAction("com.canara.kavachai.NEW_TRANSCRIPT")
            addAction("com.canara.kavachai.CALL_ENDED")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }

        setContent {
            MyApplicationTheme {
                val state by viewModel.uiState.collectAsState()

                // UI automatically updates from StateFlow


                ScreeningScreen(
                    state = state,
                    onClose = { finish() }
                )
            }
        }
    }

    override fun onInit(status: Int) {
        // TTS logic moved to AIVoiceService
    }



    @Suppress("DEPRECATION")
    private fun activateSpeakerphone() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Modern API: Request audio focus with USAGE_VOICE_COMMUNICATION
                val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .build()
                audioManager.requestAudioFocus(focusRequest)
            }
            // Force speakerphone ON so the caller hears the TTS
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.isSpeakerphoneOn = true
            
            // Maximize voice call volume to ensure mic picks it up
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
            audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxVol, 0)
            
            Log.d(TAG, "Speakerphone activated. Mode: ${audioManager.mode}")
        } catch (e: Exception) {
            Log.e(TAG, "Error activating speakerphone", e)
        }
    }

    @Suppress("DEPRECATION")
    private fun deactivateSpeakerphone() {
        try {
            audioManager.isSpeakerphoneOn = false
            audioManager.mode = AudioManager.MODE_NORMAL
        } catch (e: Exception) {
            Log.e(TAG, "Error deactivating speakerphone", e)
        }
    }

    override fun onDestroy() {
        deactivateSpeakerphone()
        super.onDestroy()
    }
}
