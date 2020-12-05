package com.arrol.amp.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationActionService : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        context.sendBroadcast(
            Intent("com.example.musictest.Control_Music")
                .putExtra("actionname", intent.action)
        )
    }
}