package com.arrol.amp.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.arrol.amp.R
import com.arrol.amp.fragments.ListerRecyclerFragment
import com.arrol.amp.musics.ListId

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