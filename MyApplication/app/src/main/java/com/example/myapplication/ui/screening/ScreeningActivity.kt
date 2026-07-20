package com.example.myapplication.ui.screening

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    private val viewModel: ScreeningViewModel by viewModels()
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var isSandbox = false
    private lateinit var audioManager: AudioManager
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

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

        // Initialize Text-To-Speech
        tts = TextToSpeech(this, this)

        val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: "Unknown"
        viewModel.setPhoneNumber(phoneNumber)

        // Register receiver
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

        if (!isSandbox) {
            checkPermissionsAndStart()
        }
    }

    private fun checkPermissionsAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSION_REQUEST_CODE)
        } else {
            setupRealCall()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupRealCall()
            } else {
                Log.e(TAG, "Microphone permission denied. Cannot transcribe caller.")
                viewModel.addTranscriptMessage("KavachAI: Microphone access denied. Transcribing disabled.")
            }
        }
    }

    private fun setupRealCall() {
        // Run speakerphone loopback and speech recognition
        activateSpeakerphone()
        initSpeechRecognizer()
        
        lifecycleScope.launch {
            delay(1000)
            val msg = "Hello, you have reached Mr. Gowtham's phone. This is KavachAI, his personal call assistant. May I know who is calling and what is the reason for calling Mr. Gowtham?"
            viewModel.addTranscriptMessage("KavachAI: $msg")
            speakLine(msg, true)
        }
    }

    private fun initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e(TAG, "Speech recognition not available")
            return
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                Log.e(TAG, "Speech recognition error: $error")
                // Restart listening after error (like timeout)
                if (isListening) startListening()
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    Log.d(TAG, "Recognized caller speech: $text")
                    viewModel.processCallerVoice(this@ScreeningActivity, text)
                }
                if (isListening) startListening()
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        isListening = true
        startListening()
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toString())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.startListening(intent)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            tts?.language = Locale.US
            Log.d(TAG, "TTS Initialization Successful")
            
            if (isSandbox) {
                runSimulation()
            }
        } else {
            Log.e(TAG, "TTS Initialization Failed")
        }
    }

    private fun speakLine(text: String, isAI: Boolean) {
        if (!isTtsReady) return
        
        // Temporarily pause listening while AI is speaking so it doesn't transcribe itself
        val wasListening = isListening
        if (wasListening) {
            isListening = false
            speechRecognizer?.stopListening()
        }

        if (isAI) {
            tts?.setPitch(1.15f)
            tts?.setSpeechRate(1.05f)
        } else {
            tts?.setPitch(0.82f)
            tts?.setSpeechRate(0.95f)
        }
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "SIM_${System.currentTimeMillis()}")
        
        // Resume listening after TTS finishes (simplified delay here, ideally via UtteranceProgressListener)
        if (wasListening) {
            lifecycleScope.launch {
                val delayTime = (text.length * 75L) + 1000 // Approximate speaking time
                delay(delayTime)
                isListening = true
                startListening()
            }
        }
    }

    private fun runSimulation() {
        lifecycleScope.launch {
            activateSpeakerphone()
            delay(1500)
            val msg1 = "Hello, this is KavachAI call assistant. Please state your name and reason for calling."
            viewModel.addTranscriptMessage("KavachAI: $msg1")
            speakLine(msg1, true)
            
            delay(7000)
            val msg2 = "Yes, I am calling from Amazon. I have a delivery for Mr. Gowtham."
            viewModel.addTranscriptMessage("Caller: $msg2")
            speakLine(msg2, false)
            
            delay(5000)
            val msg3 = "I see. Could you please confirm the order number or delivery address?"
            viewModel.addTranscriptMessage("KavachAI: $msg3")
            speakLine(msg3, true)
            
            delay(6000)
            val msg4 = "It's arriving today at 5 PM."
            viewModel.addTranscriptMessage("Caller: $msg4")
            speakLine(msg4, false)
            
            delay(4000)
            val msg5 = "Understood. I will notify Mr. Gowtham to collect the parcel. Thank you."
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
            
            // Blast volume so mic picks it up
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
            audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxVol, 0)
            Log.d(TAG, "Speakerphone activated for Loopback")
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
        isListening = false
        speechRecognizer?.destroy()
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
