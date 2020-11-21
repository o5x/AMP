package com.example.musictest.fragments

import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.musictest.activities.MainActivity
import com.example.musictest.R
import com.example.musictest.activities.syncMusicController
import com.example.musictest.listId

data class HomeItem(
        val name : String,
        val desc : String,
        val image : Int,
        val onclick : () -> Unit)

class HomeFragment : Fragment() {

    override fun onStart() {
        super.onStart()

        (activity as MainActivity).currentfragment = this

        (activity as MainActivity).button_back.visibility = View.INVISIBLE
        (activity as MainActivity).button_settings.visibility = View.VISIBLE
        (activity as MainActivity).title.text = "Home"

        (activity as MainActivity).btn_home.setColorFilter(R.color.th)
        (activity as MainActivity).btn_search.colorFilter = null
        (activity as MainActivity).btn_collection.colorFilter = null
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

        val fm = childFragmentManager

       /* var favs = arrayOf(
                HomeItem("Liked Songs", "", R.drawable.liked
                ) {
                    (activity as MainActivity).replaceFragment(ListerRecyclerFragment()
                            .initMusicIdList(syncMusicController.list_liked)
                            .setTitle("Liked"))
                },
                HomeItem("All Songs", "", R.drawable.all
                ) {
                    (activity as MainActivity).replaceFragment(ListerRecyclerFragment()
                            .initMusicIdList(syncMusicController.list_all)
                            .setTitle("All"))
                },
                HomeItem("Local Files", "", R.drawable.folder
                ) {
                    (activity as MainActivity).replaceFragment(ListerRecyclerFragment()
                            .initFile(Environment.getExternalStorageDirectory().toString() + "/Music"))
                },
                HomeItem("Most Listened", "", R.drawable.most
                ) {
                    (activity as MainActivity).replaceFragment(ListerRecyclerFragment()
                            .initMusicIdList(syncMusicController.getList(listId.ID_MUSIC_MOST).list)
                            .setTitle("Most Listened"))
                },
                HomeItem("Downloads", "", R.drawable.download
                ) {
                    (activity as MainActivity).replaceFragment(ListerRecyclerFragment()
                            .initMusicIdList(syncMusicController.getList(listId.ID_MUSIC_DOWNLOAD).list)
                            .setTitle("Downloaded"))
                }
        )


        var id = 0
        for(fav in favs)
        {
            ItemSquare.addItem(fm, id, fav.name, fav.desc, fav.image, id == 0).setClickCallback { fav.onclick() }
            id++
        }
*/

        // FILL FAVOURITES
        val tabFav = arrayOf("Liked Songs","Local Files","Most Listened","Suggested")
        val tabFav_i = arrayOf(R.drawable.liked,  R.drawable.folder, R.drawable.most,R.drawable.suggest)
        val tabFav_c = arrayOf(
                { (activity as MainActivity).replaceFragment(ListerRecyclerFragment().initMusicIdList(syncMusicController.getList(listId.ID_MUSIC_LIKED).list).setTitle("Liked")) },
                { (activity as MainActivity).replaceFragment(ListerRecyclerFragment().initFile(Environment.getExternalStorageDirectory().toString() + "/Music")) },
                { (activity as MainActivity).replaceFragment(ListerRecyclerFragment().initMusicIdList(syncMusicController.getList(listId.ID_MUSIC_MOST).list).setTitle("Most Listened")) },
                { (activity as MainActivity).replaceFragment(ListerRecyclerFragment().initMusicIdList(syncMusicController.getList(listId.ID_MUSIC_SUGGEST).list).setTitle("Suggested")) }

        )

        for(i in tabFav.indices)
        {
            ItemSquare.addItem(fm, R.id.layout_favourites, tabFav[i], "", tabFav_i[i], i == 0).setClickCallback { tabFav_c[i]() }
        }

        //var id = 0

        // FILL Recent

        val tabFav2 = arrayOf("All Songs","Albums","Artists","Downloads")
        val tabFav2_i = arrayOf(R.drawable.all,R.drawable.album, R.drawable.artist, R.drawable.download)
        val tabFav2_c = arrayOf(
                { (activity as MainActivity).replaceFragment(ListerRecyclerFragment().initMusicIdList(syncMusicController.getList(listId.ID_MUSIC_ALL).list).setTitle("All Songs")) },
                { (activity as MainActivity).replaceFragment(ListerRecyclerFragment().initPlaylistList(syncMusicController.getList(listId.ID_MUSIC_ALBUMS).list).setTitle("All albums")) },
                { (activity as MainActivity).replaceFragment(ListerRecyclerFragment().initPlaylistList(syncMusicController.getList(listId.ID_MUSIC_ARTISTS).list).setTitle("All Artists")) },
                { (activity as MainActivity).replaceFragment(ListerRecyclerFragment().initMusicIdList(syncMusicController.getList(listId.ID_MUSIC_DOWNLOAD).list).setTitle("Downloaded")) }
        )

        var id = 0
        for(i in 0 until tabFav2.size)
        {
            ItemSquare.addItem(fm, R.id.layout_onyourphone, tabFav2[i], "", tabFav2_i[i], id == 0).setClickCallback { tabFav2_c[i]() }
            id++
        }

        // FILL suggested
        val tabSuggest = arrayOf("No place is home")
        val tabSuggest_d = arrayOf("Weshly Arms")
        val tabSuggest_i = arrayOf(
                R.drawable.album)
        id = 0

        for(item in tabSuggest)
        {
            ItemSquare.addItem(
                fm,
                R.id.layout_recent,
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