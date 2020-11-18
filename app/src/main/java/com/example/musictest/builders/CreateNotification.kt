package com.example.musictest.builders

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.musictest.Music
import com.example.musictest.R
import com.example.musictest.services.NotificationActionService

object CreateNotification {
    const val CHANNEL_ID = "channel1"
    const val ACTION_PREVIOUS = "actionprevious"
    const val ACTION_PLAY = "actionplay"
    const val ACTION_NEXT = "actionnext"
    lateinit var notification: Notification

    fun cancelNotification(context: Context)
    {
        val ns = Context.NOTIFICATION_SERVICE
        val nMgr: NotificationManager = context.getSystemService(ns) as NotificationManager
        nMgr.cancel(1)
    }

    fun createNotification(context: Context, track: Music, playbutton: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManagerCompat = NotificationManagerCompat.from(context)
            //val mediaSessionCompat = MediaSessionCompat(context, "tag")

            val mediaSessionCompat = MediaSessionCompat(context, "tag")
            mediaSessionCompat.setMetadata(
                MediaMetadataCompat.Builder()
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, track.image)
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.artist)
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, track.album)
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.title)
                    .build()
            )

            // Intents build
            val intentPrevious = Intent(context, NotificationActionService::class.java)
                    .setAction(ACTION_PREVIOUS)
            val pendingIntentPrevious = PendingIntent.getBroadcast(
                context, 0,
                intentPrevious, PendingIntent.FLAG_UPDATE_CURRENT
            )

            val intentPlay = Intent(context, NotificationActionService::class.java)
                    .setAction(ACTION_PLAY)
            val pendingIntentPlay = PendingIntent.getBroadcast(
                context, 0,
                intentPlay, PendingIntent.FLAG_UPDATE_CURRENT
            )

            val intentNext = Intent(context, NotificationActionService::class.java)
                    .setAction(ACTION_NEXT)
            val pendingIntentNext = PendingIntent.getBroadcast(
                context, 0,
                intentNext, PendingIntent.FLAG_UPDATE_CURRENT
            )

            val token: MediaSessionCompat.Token = mediaSessionCompat.sessionToken

            var bmp = BitmapFactory.decodeResource(context.resources, R.drawable.music)

            if(track.image != null) bmp = track!!.image

            //create notification
            notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setProgress(100,50,true)
                .setSmallIcon(R.drawable.appiconrot)
                .setContentTitle(track.title)
                .setContentText(track.artist)
                .setLargeIcon(bmp)
                .setOnlyAlertOnce(true) //show notification for only first time
                .setShowWhen(false)
                .addAction(R.drawable.ic_prev, "Previous", pendingIntentPrevious)
                .addAction(playbutton, "Play", pendingIntentPlay)
                .addAction(R.drawable.ic_next, "Next", pendingIntentNext)
                .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
                    .setMediaSession(token)
                )
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            notificationManagerCompat.notify(1, notification)
        }
    }
}