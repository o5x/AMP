package com.arrol.amp.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.arrol.amp.R
import com.arrol.amp.fragments.ListerRecyclerFragment
import com.arrol.amp.musics.ListId
import kotlinx.android.synthetic.main.activity_queue.*

class QueueActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_queue)

        // listerLayout
        val fm = supportFragmentManager
        ListerRecyclerFragment().addItem(fm, R.id.listerLayout)
            .initSyncListById(ListId.ID_MUSIC_QUEUE)

        registerReceiver(broadcastReceiver, IntentFilter("com.example.musictest.Update_Music"))

        updateInterface()
    }

    private var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateInterface()
        }
    }

    fun updateInterface()
    {
        // Update Play button
        if (smc.isMusicPlaying) playBtn3.setBackgroundResource(R.drawable.ic_pause)
        else playBtn3.setBackgroundResource(R.drawable.ic_play)
    }

    fun backClick(v: View) {
        onBackPressed();
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    // Button click actions
    fun playBtnClick(v: View) {
        smc.togglePlay()
    }

    fun nextBtnClick(v: View) {
        smc.next()
    }

    fun prevBtnClick(v: View) {
        smc.prev()
    }
}