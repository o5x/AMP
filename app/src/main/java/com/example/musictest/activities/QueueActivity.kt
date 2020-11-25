package com.example.musictest.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.musictest.R
import com.example.musictest.fragments.ListerRecyclerFragment
import com.example.musictest.musics.ListId

class QueueActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_queue)

        // listerLayout
        val fm = supportFragmentManager
        ListerRecyclerFragment().addItem(fm, R.id.listerLayout)
                .initSyncListById(ListId.ID_MUSIC_QUEUE)
    }
}