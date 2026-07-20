package com.example.myapplication.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class KavachNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val notification = sbn.notification
        val category = notification.category

        Log.d("KavachNotification", "Posted: $packageName, Category: $category")

        // Intercept incoming call notifications from any dialer (Oppo, Google, or Telecom)
        if (category == Notification.CATEGORY_CALL || category == "call" ||
            packageName.contains("incallui") || packageName.contains("telecom") || packageName.contains("dialer")
        ) {
            val title = notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            Log.d("KavachNotification", "Incoming Call Notification Detected: $title - $text")

            // Look for the "Answer" or "Accept" action button
            var answerAction: Notification.Action? = null
            if (notification.actions != null) {
                for (action in notification.actions) {
                    val actionTitle = action.title?.toString()?.lowercase() ?: ""
                    if (actionTitle.contains("answer") || actionTitle.contains("accept") || actionTitle.contains("reply")) {
                        answerAction = action
                        break
                    }
                }
            }

            if (answerAction != null) {
                try {
                    Log.d("KavachNotification", "Simulating click on Answer button...")
                    answerAction.actionIntent.send()

                    // Start the screening activity immediately
                    val intent = Intent(this, com.example.myapplication.ui.screening.ScreeningActivity::class.java).apply {
                        putExtra(com.example.myapplication.ui.screening.ScreeningActivity.EXTRA_PHONE_NUMBER, title)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    startActivity(intent)

                    // Notify engine
                    val broadcast = Intent("com.canara.kavachai.START_ENGINE")
                    LocalBroadcastManager.getInstance(this).sendBroadcast(broadcast)

                } catch (e: PendingIntent.CanceledException) {
                    Log.e("KavachNotification", "Failed to click answer", e)
                }
            } else {
                // If there's no answer action, we can try fullScreenIntent as a backup, though we can't answer it this way.
                Log.d("KavachNotification", "No Answer action found on notification")
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Handle call end if needed
    }
}
