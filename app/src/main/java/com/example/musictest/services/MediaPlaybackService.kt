package com.example.musictest.services

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import androidx.media.MediaBrowserServiceCompat
import com.example.musictest.R
import com.example.musictest.activities.smc

class MediaPlaybackService : MediaBrowserServiceCompat() {

    override fun onGetRoot(clientPackageName: String, clientUid: Int, rootHints: Bundle?): BrowserRoot? {
        return BrowserRoot(getString(R.string.app_name), null) // Bundle of optional extras
    }

    override fun onCreate() {
        super.onCreate()
        if (smc.initialized) {
            val mediaSession = smc.mediaSessionCompat
            sessionToken = mediaSession.sessionToken
            mediaSession.isActive = true
        }
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<MediaBrowserCompat.MediaItem>>) {
        result.sendResult(null);
    }
}