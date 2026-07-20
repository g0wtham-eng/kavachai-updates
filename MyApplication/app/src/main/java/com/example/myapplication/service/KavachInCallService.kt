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

class KavachInCallService : InCallService() {

    private val TAG = "KavachInCallService"
    private var activeCall: Call? = null
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

    override fun onDestroy() {
        cleanupCall()
        try {
            unregisterReceiver(disconnectReceiver)
        } catch (e: java.lang.Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }
        Log.d(TAG, "InCallService destroyed")
        super.onDestroy()
    }
}
