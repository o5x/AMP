package com.example.musictest.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager

class RemoteControlService : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
            context.sendBroadcast(Intent("com.example.musictest.Update_Music")
                    .putExtra("actionname", "pause"))
        }
    }
}