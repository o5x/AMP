package com.example.musictest.fragments

import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.musictest.activities.MainActivity
import com.example.musictest.R
import com.example.musictest.activities.musicController

class HomeFragment : Fragment() {

    override fun onStart() {
        super.onStart()

        (activity as MainActivity).currentfragment = this

        (activity as MainActivity).button_back.visibility = View.INVISIBLE
        (activity as MainActivity).button_settings.visibility = View.VISIBLE
        (activity as MainActivity).title.text = "Home"

        (activity as MainActivity).btn_home.setColorFilter(R.color.th)
        (activity as MainActivity).btn_search.setColorFilter(null)
        (activity as MainActivity).btn_collection.setColorFilter(null)
    }

    override fun onDestroy() {
        super.onDestroy()
        (activity as MainActivity).currentfragment = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        val v = inflater.inflate(R.layout.fragment_home, container, false)

        val fm = fragmentManager

        // FILL FAVOURITES
        val tabFav = arrayOf("Liked Songs","All Songs","Local Files","Most Listened")
        val tabFav_i = arrayOf(R.drawable.liked, R.drawable.all, R.drawable.folder, R.drawable.most)
        val tabFav_d = arrayOf(musicController.playlist[0].musics.size.toString() + " songs",musicController.musics.size.toString() + " songs", "", "")
        val tabFav_c = arrayOf(
                { (activity as MainActivity).replaceFragment(ListerRecyclerFragment().initMusicIdList(musicController.playlist[0].musics).setTitle("Liked")) },
                {
                    val allMusicsIds = ArrayList<Int>()
                    for (i in 0 until musicController.musics.size) { allMusicsIds.add(i) }
                    (activity as MainActivity).replaceFragment(ListerRecyclerFragment().initMusicIdList(allMusicsIds).setTitle("All"))
                },{  (activity as MainActivity).replaceFragment(ListerFragment().initFile(Environment.getExternalStorageDirectory().toString() + "/Music")) },
                {})

        for(i in 0 until tabFav.size)
        {
            ItemSquare.addItem(fm, R.id.layout_favourites, tabFav[i], "", tabFav_i[i], i == 0).setClickCallback { tabFav_c[i]() }
        }

        var id = 0

        // FILL Recent
        val tabRec = arrayOf("TRON","Relax","Sail","Weshly Arms")
        val tabRec_d = arrayOf("Daft Punk","Playlist","AWNLOATION","Artist")
        val tabRec_i = arrayOf(
            R.drawable.album,
            R.drawable.playlist,
            R.drawable.music,
                R.drawable.artist
        )
        id = 0

        for(item in tabRec)
        {
            ItemSquare.addItem(fm, R.id.layout_recent, item, tabRec_d[id], tabRec_i[id], id++ == 0)
        }

        // FILL suggested
        val tabSuggest = arrayOf("Suggested","No place is home")
        val tabSuggest_d = arrayOf("","Weshly Arms")
        val tabSuggest_i = arrayOf(R.drawable.suggest, R.drawable.album)
        id = 0

        for(item in tabSuggest)
        {
            ItemSquare.addItem(
                fm,
                R.id.layout_suggest,
                item,
                tabSuggest_d[id],
                tabSuggest_i[id],
                id++ == 0
            )
        }

        return v
    }

    fun settingsClick(v: View)
    {
        (activity as MainActivity).settingsClick(v)
    }
}