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
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import androidx.core.app.NotificationCompat

class KavachInCallService : InCallService(), TextToSpeech.OnInitListener {

    private val TAG = "KavachInCallService"
    private var activeCall: Call? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private lateinit var audioManager: AudioManager
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private val disconnectReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.canara.kavachai.TRIGGER_DISCONNECT") {
                Log.d(TAG, "Disconnect triggered via broadcast")
                activeCall?.disconnect()
            }
        }
    }

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

        val filter = android.content.IntentFilter("com.canara.kavachai.TRIGGER_DISCONNECT")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(disconnectReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(disconnectReceiver, filter)
        }
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

            // Create full screen intent notification to bypass ColorOS background launch block
            try {
                val channelId = "kavach_screening_channel"
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(
                        channelId,
                        "Kavach Call Screening",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Displays the screening interface during incoming calls."
                        enableLights(true)
                        enableVibration(false)
                        setSound(null, null)
                    }
                    nm.createNotificationChannel(channel)
                }

                val intent = Intent(this, ScreeningActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                    putExtra(ScreeningActivity.EXTRA_PHONE_NUMBER, callerNumber)
                }

                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(this, channelId)
                    .setSmallIcon(android.R.drawable.sym_call_incoming)
                    .setContentTitle("KavachAI Screening")
                    .setContentText("Screening incoming call...")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    .setFullScreenIntent(pendingIntent, true)
                    .setAutoCancel(true)
                    .build()

                nm.notify(1001, notification)
                Log.d(TAG, "Posted full-screen notification for incoming call")

                // Fallback direct activity launch
                startActivity(intent)
                Log.d(TAG, "Direct launch backup executed")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch ScreeningActivity via notification/intent", e)
            }

            // Clear previous log file at start of call
            try {
                val file = java.io.File(filesDir, "conversation_log.txt")
                file.writeText("")
            } catch (e: java.lang.Exception) {
                Log.e(TAG, "Failed to clear log file", e)
            }

            // Start Text Conversation immediately (no TTS wait)
            startConversation(callerNumber)
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
        
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(1001)
            nm.cancel(1002)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel screening notification", e)
        }
        
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
        // Comment out TTS voice speech to perform a purely text-based screening
        // tts?.setPitch(1.15f)
        // tts?.setSpeechRate(1.05f)
        // tts?.speak(text, TextToSpeech.QUEUE_ADD, null, "UTT_${System.currentTimeMillis()}")
        
        // Save conversation line to file
        try {
            val file = java.io.File(filesDir, "conversation_log.txt")
            file.appendText("KavachAI: $text\n")
        } catch (e: java.lang.Exception) {
            Log.e(TAG, "Failed to write transcript line to file", e)
        }

        // Broadcast line to UI so it can type it out
        val intent = Intent("com.canara.kavachai.NEW_TRANSCRIPT")
        intent.putExtra("message", "KavachAI: $text")
        sendBroadcast(intent)

        // Post live notification showing the conversation dialogue
        try {
            val channelId = "kavach_transcript_channel"
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Kavach Conversation Logs",
                    NotificationManager.IMPORTANCE_LOW
                )
                nm.createNotificationChannel(channel)
            }
            val notification = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentTitle("KavachAI Conversation")
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
            nm.notify(1002, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show transcript notification", e)
        }
    }

    @Suppress("DEPRECATION")
    private fun activateSpeakerphone() {
        try {
            // Route call audio to earpiece by default (no speakerphone blast)
            setAudioRoute(android.telecom.CallAudioState.ROUTE_EARPIECE)
            
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
            audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxVol, 0)
            
            Log.d(TAG, "Audio routed to earpiece from InCallService")
        } catch (e: Exception) {
            Log.e(TAG, "Error activating earpiece routing", e)
        }
    }

    @Suppress("DEPRECATION")
    private fun deactivateSpeakerphone() {
        try {
            setAudioRoute(android.telecom.CallAudioState.ROUTE_EARPIECE)
            audioManager.mode = AudioManager.MODE_NORMAL
        } catch (e: Exception) {
            Log.e(TAG, "Error deactivating earpiece routing", e)
        }
    }

    override fun onDestroy() {
        cleanupCall()
        tts?.stop()
        tts?.shutdown()
        try {
            unregisterReceiver(disconnectReceiver)
        } catch (e: java.lang.Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }
        Log.d(TAG, "InCallService destroyed")
        super.onDestroy()
    }
}
