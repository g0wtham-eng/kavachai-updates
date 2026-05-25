package com.example.myapplication.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.example.myapplication.ui.screening.ScreeningActivity

class IncomingCallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            
            // We only want to trigger the UI when the phone starts ringing
            if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                Log.d("IncomingCallReceiver", "Incoming call from: $incomingNumber")
                
                // If the number is null, we can't screen it. On newer Androids, reading the number 
                // requires READ_CALL_LOG, but READ_PHONE_STATE works for many devices.
                val numberToScreen = incomingNumber ?: "Unknown"

                try {
                    val screeningIntent = Intent(context, ScreeningActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                        putExtra(ScreeningActivity.EXTRA_PHONE_NUMBER, numberToScreen)
                    }
                    Log.d("IncomingCallReceiver", "Starting ScreeningActivity as fallback for Oppo...")
                    context.startActivity(screeningIntent)
                } catch (e: Exception) {
                    Log.e("IncomingCallReceiver", "Error starting ScreeningActivity", e)
                }
            }
        }
    }
}
