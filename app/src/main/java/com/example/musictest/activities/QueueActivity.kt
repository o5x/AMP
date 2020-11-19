package com.example.musictest.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.example.musictest.R
import com.example.musictest.fragments.ListerFragment

class QueueActivity : AppCompatActivity() {

    fun addListItem(
            fm: androidx.fragment.app.FragmentManager?,
            layout_id: Int
    ) : ListerFragment
    {
        val fragOne: Fragment = ListerFragment()
        val tr = fm!!.beginTransaction()
        tr.add(layout_id, fragOne)
        tr.commitAllowingStateLoss()
        tr.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)

        return fragOne as ListerFragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_queue)

        // listerLayout
        var fm = supportFragmentManager
        addListItem(fm, R.id.listerLayout).initMusicIdList(musicController.queue)
    }
}