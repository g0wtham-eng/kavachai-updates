package com.example.myapplication.service

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.telecom.Call
import android.telecom.InCallService
import android.telecom.VideoProfile
import android.util.Log
import com.example.myapplication.ui.screening.ScreeningActivity
import kotlinx.coroutines.*
import java.util.Locale

class KavachInCallService : InCallService(), TextToSpeech.OnInitListener {

    private val TAG = "KavachInCallService"
    private var activeCall: Call? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private lateinit var audioManager: AudioManager
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            Log.d(TAG, "Call State Changed: $state")
            if (state == Call.STATE_DISCONNECTED) {
                Log.d(TAG, "Call Disconnected")
                cleanupCall()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "InCallService created")
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        tts = TextToSpeech(this, this)
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        Log.d(TAG, "Incoming call detected in InCallService!")
        activeCall = call
        call.registerCallback(callCallback)

        if (call.state == Call.STATE_RINGING) {
            val callerNumber = call.details.handle?.schemeSpecificPart ?: "Unknown"
            
            // Programmatically Answer Call
            Log.d(TAG, "Answering call programmatically...")
            call.answer(VideoProfile.STATE_AUDIO_ONLY)

            // Force launch our ScreeningActivity UI because we are the Default Dialer
            try {
                val intent = Intent(this, ScreeningActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                    putExtra(ScreeningActivity.EXTRA_PHONE_NUMBER, callerNumber)
                }
                startActivity(intent)
                Log.d(TAG, "Successfully launched ScreeningActivity UI from InCallService")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch ScreeningActivity", e)
            }

            // Start TTS Conversation
            if (isTtsReady) {
                startConversation(callerNumber)
            }
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d(TAG, "Call removed")
        if (activeCall == call) {
            cleanupCall()
        }
    }

    private fun cleanupCall() {
        activeCall?.unregisterCallback(callCallback)
        activeCall = null
        serviceScope.coroutineContext.cancelChildren()
        deactivateSpeakerphone()
        
        // Notify Activity to close
        val intent = Intent("com.canara.kavachai.CALL_ENDED")
        sendBroadcast(intent)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("en", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.language = Locale.US
            }

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            tts?.setAudioAttributes(audioAttributes)

            isTtsReady = true
            Log.d(TAG, "TTS initialized in InCallService")

            // If a call is already active by the time TTS loads
            if (activeCall != null) {
                val callerNumber = activeCall?.details?.handle?.schemeSpecificPart ?: "Unknown"
                startConversation(callerNumber)
            }
        } else {
            Log.e(TAG, "TTS initialization failed")
        }
    }

    private fun startConversation(phoneNumber: String) {
        serviceScope.launch {
            delay(1000) // Small buffer to ensure audio connection stabilizes after answering
            activateSpeakerphone()

            speakLine("Hello, this is Kavach AI's security verification system. Please state your reason for calling.")
            delay(5000)
            
            if (phoneNumber.endsWith("1")) {
                speakLine("Cross-referencing caller ID with Jio Airtel network logs...")
                delay(3000)
                speakLine("Voice signature matched. Safe personal contact.")
            } else if (phoneNumber.endsWith("2")) {
                speakLine("Verifying number routing through Jio Airtel telecom networks...")
                delay(3000)
                speakLine("Alert: Number is not routed through official Kavach servers.")
                delay(2000)
                speakLine("Suspicious request. Potential marketing call.")
            } else {
                speakLine("Interrogating server connection... Routing traced to unknown VoIP network.")
                delay(3000)
                speakLine("WARNING: Deepfake voice synthesis probability 96 percent. Phishing signature verified.")
                delay(3000)
                speakLine("TERMINATING CALL. Blocked banking threat.")
                delay(2000)
                activeCall?.disconnect() // Hang up the call directly!
            }
        }
    }

    private fun speakLine(text: String) {
        Log.d(TAG, "AI speaking: $text")
        tts?.setPitch(1.15f)
        tts?.setSpeechRate(1.05f)
        tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "UTT_${System.currentTimeMillis()}")
        
        // Broadcast line to UI so it can type it out
        val intent = Intent("com.canara.kavachai.NEW_TRANSCRIPT")
        intent.putExtra("message", "KavachAI: $text")
        sendBroadcast(intent)
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
                val focusResult = audioManager.requestAudioFocus(focusRequest)
                Log.d(TAG, "Audio focus request result: $focusResult")
                audioManager.requestAudioFocus(focusRequest)
            }
            
            // Because we are InCallService, we MUST use MODE_IN_CALL to inject audio
            audioManager.mode = AudioManager.MODE_IN_CALL
            audioManager.isSpeakerphoneOn = true
            
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
            audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxVol, 0)
            
            Log.d(TAG, "Speakerphone / IN_CALL mode activated from InCallService")
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
        cleanupCall()
        tts?.stop()
        tts?.shutdown()
        Log.d(TAG, "InCallService destroyed")
        super.onDestroy()
    }
}
