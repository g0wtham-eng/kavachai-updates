package com.example.myapplication.ui.screening

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class ScreeningActivity : ComponentActivity(), TextToSpeech.OnInitListener {

    companion object {
        const val EXTRA_PHONE_NUMBER = "EXTRA_PHONE_NUMBER"
        const val EXTRA_IS_SANDBOX = "EXTRA_IS_SANDBOX"
        private const val TAG = "ScreeningActivity"
    }

    private val viewModel: ScreeningViewModel by viewModels()
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var isSandbox = false
    private lateinit var audioManager: AudioManager

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.canara.kavachai.NEW_TRANSCRIPT" -> {
                    val msg = intent.getStringExtra("message") ?: return
                    viewModel.addTranscriptMessage(msg)
                }
                "com.canara.kavachai.NEW_TRANSCRIPT_TTS" -> {
                    val msg = intent.getStringExtra("message") ?: return
                    val isAI = intent.getBooleanExtra("isAI", true)
                    speakLine(msg, isAI)
                }
                "com.canara.kavachai.CALL_ENDED", "com.canara.kavachai.TRIGGER_DISCONNECT" -> {
                    finish()
                }
            }
        }
    }

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
        isSandbox = intent.getBooleanExtra(EXTRA_IS_SANDBOX, false)

        // Initialize Text-To-Speech for simulated voice demonstration
        tts = TextToSpeech(this, this)

        val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: "Unknown"
        viewModel.setPhoneNumber(phoneNumber)

        // Register receiver for real-time transcript sync
        val filter = IntentFilter().apply {
            addAction("com.canara.kavachai.NEW_TRANSCRIPT")
            addAction("com.canara.kavachai.NEW_TRANSCRIPT_TTS")
            addAction("com.canara.kavachai.CALL_ENDED")
            addAction("com.canara.kavachai.TRIGGER_DISCONNECT")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }

        setContent {
            MyApplicationTheme {
                val state by viewModel.uiState.collectAsState()

                ScreeningScreen(
                    state = state,
                    onClose = { finish() },
                    onSendMessage = { text -> viewModel.sendUserMessage(this@ScreeningActivity, text) }
                )
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            tts?.language = Locale.US
            Log.d(TAG, "TTS Initialization Successful")
            
            // If in sandbox mode, start the automatic voice & text simulation!
            if (isSandbox) {
                runSimulation()
            }
        } else {
            Log.e(TAG, "TTS Initialization Failed")
        }
    }

    private fun speakLine(text: String, isAI: Boolean) {
        if (!isTtsReady) return
        if (isAI) {
            tts?.setPitch(1.15f) // High-pitch, clean assistant tone
            tts?.setSpeechRate(1.05f)
        } else {
            tts?.setPitch(0.82f) // Lower-pitch, scammer/caller voice tone
            tts?.setSpeechRate(0.95f)
        }
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "SIM_${System.currentTimeMillis()}")
    }

    private fun runSimulation() {
        lifecycleScope.launch {
            // Activate speakerphone mode locally for simulation audio clarity
            activateSpeakerphone()

            delay(1500)
            val msg1 = "Hello, this is KavachAI call assistant. Please state your name and reason for calling."
            viewModel.addTranscriptMessage("KavachAI: $msg1")
            speakLine(msg1, true)
            
            delay(7000)
            val msg2 = "Yes, I am calling from Canara Bank security department. Your account has a suspicious login."
            viewModel.addTranscriptMessage("Caller: $msg2")
            speakLine(msg2, false)
            
            delay(7000)
            val msg3 = "Running registry trace... ALERT: Number is not registered to Canara Bank servers. Potential phishing threat."
            viewModel.addTranscriptMessage("KavachAI: $msg3")
            speakLine(msg3, true)
            
            delay(8000)
            val msg4 = "No, this is official call. Please share your account verification OTP code to secure it."
            viewModel.addTranscriptMessage("Caller: $msg4")
            speakLine(msg4, false)
            
            delay(7000)
            val msg5 = "WARNING: Deepfake voice synthesis probability 98 percent. Phishing signature verified. Disconnecting."
            viewModel.addTranscriptMessage("KavachAI: $msg5")
            speakLine(msg5, true)
            
            delay(3500)
            finish()
        }
    }

    @Suppress("DEPRECATION")
    private fun activateSpeakerphone() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.isSpeakerphoneOn = true
            
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
            audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxVol, 0)
            Log.d(TAG, "Speakerphone activated for sandbox simulation")
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
        try {
            unregisterReceiver(receiver)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister receiver", e)
        }
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
