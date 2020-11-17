package com.example.musictest.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.musictest.fragments.ItemMusicFragment
import com.example.musictest.R

class QueueActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_queue)

        var id = 0;

        val fm = supportFragmentManager

        for(item in musicController.musics)
        {
            ItemMusicFragment.addItem(fm, R.id.listerLayout, id, id++ == 0)
        }
    }
}