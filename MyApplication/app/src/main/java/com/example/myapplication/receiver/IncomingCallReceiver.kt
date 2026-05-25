package com.example.myapplication.receiver

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.myapplication.ui.screening.ScreeningActivity

class IncomingCallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            
            if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                Log.d("IncomingCallReceiver", "Incoming call from: $incomingNumber")
                
                val numberToScreen = incomingNumber ?: "Unknown"

                // Check if number is in contacts
                if (!isNumberInContacts(context, numberToScreen)) {
                    Log.d("IncomingCallReceiver", "Unknown number detected. Auto-answering...")
                    
                    // Auto-answer the call
                    autoAnswerCall(context)

                    // Launch Screening Overlay
                    try {
                        val screeningIntent = Intent(context, ScreeningActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                            putExtra(ScreeningActivity.EXTRA_PHONE_NUMBER, numberToScreen)
                        }
                        context.startActivity(screeningIntent)
                    } catch (e: Exception) {
                        Log.e("IncomingCallReceiver", "Error starting ScreeningActivity", e)
                    }
                } else {
                    Log.d("IncomingCallReceiver", "Number in contacts. Letting it ring normally.")
                }
            }
        }
    }

    private fun isNumberInContacts(context: Context, phoneNumber: String): Boolean {
        if (phoneNumber == "Unknown") return false
        
        // Ensure we have permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return false // If no permission, treat as unknown for safety
        }

        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
        return try {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                cursor.count > 0
            } ?: false
        } catch (e: Exception) {
            Log.e("IncomingCallReceiver", "Error querying contacts database", e)
            false
        }
    }

    private fun autoAnswerCall(context: Context) {
        try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    telecomManager.acceptRingingCall()
                    Log.d("IncomingCallReceiver", "Successfully called acceptRingingCall()")
                }
            } else {
                Log.e("IncomingCallReceiver", "Missing ANSWER_PHONE_CALLS permission")
            }
        } catch (e: Exception) {
            Log.e("IncomingCallReceiver", "Failed to auto-answer call", e)
        }
    }
}
