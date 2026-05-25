package com.example.myapplication.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.myapplication.ui.screening.ScreeningActivity

class CallSimulationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("CallSimulationReceiver", "onReceive: received action = ${intent.action}")
        if (intent.action == "com.canara.kavachai.SIMULATE_CALL") {
            val phoneNumber = intent.getStringExtra("phoneNumber") ?: "Unknown Remote"
            Log.d("CallSimulationReceiver", "onReceive: phoneNumber = $phoneNumber")
            
            try {
                val simulationIntent = Intent(context, ScreeningActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                    putExtra(ScreeningActivity.EXTRA_PHONE_NUMBER, phoneNumber)
                }
                Log.d("CallSimulationReceiver", "onReceive: Starting ScreeningActivity...")
                context.startActivity(simulationIntent)
                Log.d("CallSimulationReceiver", "onReceive: ScreeningActivity started successfully!")
            } catch (e: Exception) {
                Log.e("CallSimulationReceiver", "onReceive: Error starting ScreeningActivity", e)
            }
        }
    }
}
