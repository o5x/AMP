package com.arrol.amp.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.arrol.amp.R
import com.arrol.amp.activities.MainActivity
import com.arrol.amp.musics.ListId
import kotlinx.android.synthetic.main.fragment_home.*

class HomeFragment : Fragment() {

    lateinit var lrf: ListerRecyclerFragment

    override fun onResume() {
        super.onResume()

        (activity as MainActivity).apply {
            btnHome.setColorFilter(R.color.th)
            btnSearch.colorFilter = null
            btnCollection.colorFilter = null
            currentfragment = this@HomeFragment
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val fm = childFragmentManager

        // FILL FAVOURITES
        val tabFav = arrayOf("Liked Songs", "Liked lists", "Local Files", "Most Listened")
        val tabFav_i = arrayOf(R.drawable.liked, R.drawable.suggest, R.drawable.folder, R.drawable.most)
        val tabFav_c = arrayOf(
            { (activity as MainActivity).replaceFragment(ListerRecyclerFragment().initSyncListById(ListId.ID_MUSIC_LIKED)) },
            {
                (activity as MainActivity).replaceFragment(ListerRecyclerFragment().initSyncListById(ListId.ID_MUSIC_LIST_LIKED)
                )
            },
            {
                (activity as MainActivity).replaceFragment(
                    ListerRecyclerFragment().initFile(
                        "/"
                    )
                )
            },
            { (activity as MainActivity).replaceFragment(ListerRecyclerFragment().initSyncListById(ListId.ID_MUSIC_MOST)) }
        )

        for (i in tabFav.indices)
            ItemSquare.addItem(fm, R.id.layout_favourites, tabFav[i], "", tabFav_i[i], i == 0, 110f)
                .setClickCallback { tabFav_c[i]() }

        // FILL Recent
        val tabFav2 = arrayOf("All Songs", "Albums", "Artists", "Downloads")
        val tabFav2_i = arrayOf(R.drawable.all, R.drawable.album, R.drawable.artist, R.drawable.download)
        val tabFav2_c = arrayOf(
            { (activity as MainActivity).replaceFragment(ListerRecyclerFragment().initSyncListById(ListId.ID_MUSIC_ALL)) },
            { (activity as MainActivity).replaceFragment(ListerRecyclerFragment().initSyncListById(ListId.ID_MUSIC_ALBUMS)) },
            { (activity as MainActivity).replaceFragment(ListerRecyclerFragment().initSyncListById(ListId.ID_MUSIC_ARTISTS)) },
            { (activity as MainActivity).replaceFragment(ListerRecyclerFragment().initSyncListById(ListId.ID_MUSIC_DOWNLOAD)) }
        )

        for ((id, i) in tabFav2.indices.withIndex()) {
            ItemSquare.addItem(fm, R.id.layout_onyourphone, tabFav2[i], "", tabFav2_i[i], id == 0, 85.5f)
                .setClickCallback { tabFav2_c[i]() }
        }

        // FILL suggested
        lrf = ListerRecyclerFragment().addItem(fm, R.id.layout_recent)
            .initSyncListById(ListId.ID_MUSIC_RECENT_LISTS, false)
    }

    override fun onStart() {
        super.onStart()
        lrf.reload()
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }
}