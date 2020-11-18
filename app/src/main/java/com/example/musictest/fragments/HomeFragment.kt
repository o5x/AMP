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
        val tabFav = arrayOf("Liked Songs","Local Files","Suggested")
        val tabFav_d = arrayOf("","", "just for you")
        val tabFav_i = arrayOf(R.drawable.liked, R.drawable.folder, R.drawable.suggest)
        var id = 0

        for(item in tabFav)
        {
            if(id == 0)
                ItemSquare.addItem(fm, R.id.layout_favourites, item, tabFav_d[id], tabFav_i[id], id == 0).setClickCallback {
                    (activity as MainActivity).replaceFragment(ListerFragment().initMusicIdList(musicController.favourites))
                }
            else if(id == 1)
                ItemSquare.addItem(fm, R.id.layout_favourites, item, tabFav_d[id], tabFav_i[id], id == 0).setClickCallback {
                    (activity as MainActivity).replaceFragment(ListerFragment().initFile(Environment.getExternalStorageDirectory().toString() + "/Music"))
                }
            else
            {
                ItemSquare.addItem(fm, R.id.layout_favourites, item, tabFav_d[id], tabFav_i[id], id == 0)
            }
            id++

        }

        // FILL Recent
        val tabRec = arrayOf("TRON","Relax","Most Listened songs","Sail")
        val tabRec_d = arrayOf("Daft Punk","Playlist","","AWNLOATION")
        val tabRec_i = arrayOf(
            R.drawable.album,
            R.drawable.playlist,
            R.drawable.most,
            R.drawable.music
        )
        id = 0

        for(item in tabRec)
        {
            ItemSquare.addItem(fm, R.id.layout_recent, item, tabRec_d[id], tabRec_i[id], id++ == 0)
        }

        // FILL suggested
        val tabSuggest = arrayOf("Weshly Arms","No place is home")
        val tabSuggest_d = arrayOf("Artist","Weshly Arms")
        val tabSuggest_i = arrayOf(R.drawable.artist, R.drawable.album)
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