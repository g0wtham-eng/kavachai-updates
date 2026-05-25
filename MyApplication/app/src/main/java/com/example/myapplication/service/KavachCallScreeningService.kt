package com.example.myapplication.service

import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import com.example.myapplication.ui.screening.ScreeningActivity

class KavachCallScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        Log.d("KavachService", "onScreenCall triggered!")
        val handle = callDetails.handle
        Log.d("KavachService", "Call handle: $handle")
        
        val phoneNumber = handle?.schemeSpecificPart
        Log.d("KavachService", "Extracted phoneNumber: $phoneNumber")
        
        if (phoneNumber == null) {
            Log.d("KavachService", "Phone number is null, letting call proceed.")
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        val inContacts = isNumberInContacts(phoneNumber)
        Log.d("KavachService", "Is number in contacts? $inContacts")

        if (!inContacts) {
            Log.d("KavachService", "Unknown number detected! Launching ScreeningActivity overlay...")
            try {
                val intent = Intent(this, ScreeningActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
                    putExtra(ScreeningActivity.EXTRA_PHONE_NUMBER, phoneNumber)
                }
                startActivity(intent)
                Log.d("KavachService", "ScreeningActivity started successfully from service.")
            } catch (e: Exception) {
                Log.e("KavachService", "Failed to start ScreeningActivity overlay", e)
            }

            respondToCall(callDetails, CallResponse.Builder().build())
        } else {
            Log.d("KavachService", "Number is in contacts, letting call proceed normally.")
            respondToCall(callDetails, CallResponse.Builder().build())
        }
    }

    private fun isNumberInContacts(phoneNumber: String): Boolean {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
        return try {
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val count = cursor.count
                Log.d("KavachService", "Contacts query count: $count")
                cursor.moveToFirst()
            } ?: false
        } catch (e: Exception) {
            Log.e("KavachService", "Error querying contacts database", e)
            false
        }
    }
}
