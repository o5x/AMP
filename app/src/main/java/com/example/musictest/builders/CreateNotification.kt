package com.example.musictest.builders

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.MediaMetadata
import android.os.Build
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.musictest.R
import com.example.musictest.SyncMusic
import com.example.musictest.activities.syncMusicController
import com.example.musictest.services.NotificationActionService


object CreateNotification {
    const val CHANNEL_ID = "channel1"
    const val ACTION_PREVIOUS = "actionprevious"
    const val ACTION_PLAY = "actionplay"
    const val ACTION_NEXT = "actionnext"
    lateinit var notification: Notification
    lateinit var mediaSessionCompat : MediaSessionCompat

    fun cancelNotification(context: Context)
    {
        val ns = Context.NOTIFICATION_SERVICE
        val nMgr: NotificationManager = context.getSystemService(ns) as NotificationManager
        nMgr.cancel(1)
    }

    fun createNotification(context: Context, track: SyncMusic, playbutton: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManagerCompat = NotificationManagerCompat.from(context)
            //val mediaSessionCompat = MediaSessionCompat(context, "tag")

            mediaSessionCompat = MediaSessionCompat(context, "PlayerService")
            mediaSessionCompat.setMetadata(
                MediaMetadataCompat.Builder()
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, track.image)
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.artist)
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, track.album)
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.title)
                        .putString( MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, "android.resource")
                        .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, syncMusicController.player.duration.toLong())
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



            var bmp = BitmapFactory.decodeResource(context.resources, R.drawable.music)

            if(track.image != null) bmp = track!!.image

            val meciastyle = androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
                    .setMediaSession(mediaSessionCompat.sessionToken)


            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
            val result = audioManager!!.requestAudioFocus({ },
                    AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
            if (result != AudioManager.AUDIOFOCUS_GAIN) {
                return  //Failed to gain audio focus
            }


            //create notification
            notification = NotificationCompat.Builder(context, CHANNEL_ID)
                //.setProgress(100,50,true)
                .setSmallIcon(R.drawable.appiconrot)
                .setContentTitle(track.title)
                .setContentText(track.artist)
                .setLargeIcon(bmp)
                .setOnlyAlertOnce(true) //show notification for only first time
                .setShowWhen(false)
                    .setNotificationSilent()
                .addAction(R.drawable.ic_prev, "Previous", pendingIntentPrevious)
                .addAction(playbutton, "Play", pendingIntentPlay)
                .addAction(R.drawable.ic_next, "Next", pendingIntentNext)
                .setStyle(meciastyle)
                .setOngoing(true)

                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

            notificationManagerCompat.notify(1, notification)
        }
    }
}