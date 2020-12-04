package com.example.musictest.builders

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.text.HtmlCompat
import com.example.musictest.R
import com.example.musictest.activities.MusicControllerActivity
import com.example.musictest.activities.smc
import com.example.musictest.services.NotificationActionService

object CreateNotification {
    const val CHANNEL_ID = "channel1"
    private const val CHANNEL_NID = 1
    const val ACTION_PREVIOUS = "actionPrevious"
    const val ACTION_PLAY = "actionPlay"
    const val ACTION_NEXT = "actionNext"
    const val ACTION_STOP = "actionStop"
    const val ACTION_LIKE = "actionLike"
    private lateinit var notification: Notification

    fun cancelNotification(context: Context) {
        val ns = Context.NOTIFICATION_SERVICE
        val nMgr: NotificationManager = context.getSystemService(ns) as NotificationManager
        nMgr.cancel(CHANNEL_NID)
    }

    fun createNotification(context: Context) {
        // Intents build
        val intentLike = Intent(context, NotificationActionService::class.java).setAction(ACTION_LIKE)
        val intentPrev = Intent(context, NotificationActionService::class.java).setAction(ACTION_PREVIOUS)
        val intentPlay = Intent(context, NotificationActionService::class.java).setAction(ACTION_PLAY)
        val intentNext = Intent(context, NotificationActionService::class.java).setAction(ACTION_NEXT)
        val intentStop = Intent(context, NotificationActionService::class.java).setAction(ACTION_STOP)

        val pendingIntentLike = PendingIntent.getBroadcast(context, 0, intentLike, PendingIntent.FLAG_UPDATE_CURRENT)
        val pendingIntentPrev = PendingIntent.getBroadcast(context, 0, intentPrev, PendingIntent.FLAG_UPDATE_CURRENT)
        val pendingIntentPlay = PendingIntent.getBroadcast(context, 0, intentPlay, PendingIntent.FLAG_UPDATE_CURRENT)
        val pendingIntentNext = PendingIntent.getBroadcast(context, 0, intentNext, PendingIntent.FLAG_UPDATE_CURRENT)
        val pendingIntentStop = PendingIntent.getBroadcast(context, 0, intentStop, PendingIntent.FLAG_UPDATE_CURRENT)

        val contentIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MusicControllerActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Setup elements
        val track = smc.currentMusic
        val bmp =
            if (track.image != null) track.image else BitmapFactory.decodeResource(context.resources, R.drawable.music)
        val playButton = if (smc.isMusicPlaying) R.drawable.ic_pause2 else R.drawable.ic_play2
        val likeButton = if (smc.isCurrentMusicLiked()) R.drawable.ic_favourite else R.drawable.ic_addfavourite

        val mediaStyle = androidx.media.app.NotificationCompat.DecoratedMediaCustomViewStyle()
            .setShowActionsInCompactView(1, 2, 3)
            .setMediaSession(smc.mediaSessionCompat.sessionToken)

        // Create notification
        notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.appicon)
            .setContentTitle(track.title)
            .setSubText(HtmlCompat.fromHtml("Playing <b>${smc.playingFrom}</b>", HtmlCompat.FROM_HTML_MODE_LEGACY))
            .setContentText(track.artist)
            .setLargeIcon(bmp)
            .setShowWhen(false)
            .setColorized(true)
            .setContentIntent(contentIntent)
            .setNotificationSilent()
            .addAction(likeButton, "Like", pendingIntentLike)
            .addAction(R.drawable.ic_prev, "Previous", pendingIntentPrev)
            .addAction(playButton, "Play", pendingIntentPlay)
            .addAction(R.drawable.ic_next, "Next", pendingIntentNext)
            .addAction(R.drawable.ic_close, "Stop", pendingIntentStop)
            .setStyle(mediaStyle)
            .setOngoing(true)
            .setDeleteIntent(pendingIntentStop)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .build()

        val notificationManagerCompat = NotificationManagerCompat.from(context)
        notificationManagerCompat.notify(CHANNEL_NID, notification)
    }
}