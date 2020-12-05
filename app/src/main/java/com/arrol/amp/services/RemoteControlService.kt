package com.arrol.amp.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY

class RemoteControlService : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_AUDIO_BECOMING_NOISY) {
            context.sendBroadcast(
                Intent("com.example.musictest.Update_Music")
                    .putExtra("actionname", "pause")
            )
        }
    }
}



