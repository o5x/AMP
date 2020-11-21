package com.example.musictest.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.example.musictest.activities.MainActivity
import com.example.musictest.R
import com.example.musictest.activities.syncMusicController
import com.example.musictest.databases.MusicDB
import com.example.musictest.listId
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_music_controller.*
import java.util.*
import kotlin.collections.ArrayList

class SearchFragment : Fragment() {

    lateinit var search : EditText;

    fun addListItem(
            fm: androidx.fragment.app.FragmentManager?,
            layout_id: Int
    ) : ListerRecyclerFragment
    {
        val fragOne: Fragment = ListerRecyclerFragment()
        val tr = fm!!.beginTransaction()
        tr.add(layout_id, fragOne)
        tr.commitAllowingStateLoss()
        tr.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)

        return fragOne as ListerRecyclerFragment
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        val v = inflater.inflate(R.layout.fragment_search, container, false)

        search = v.findViewById(R.id.editTextSearch)

        val fm = childFragmentManager

        var id: Int

        var tab = ArrayList<Int>()

        /*for(i in syncMusicController.musics)
        {
            tab.add(id++)
        }*/

        // init with all ids

        //addListItem(fm, R.id.searchResultLayout).initMusicIdList(tab)

        search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                val newtab: ArrayList<Int> = ArrayList()

                id = 0

                if (search.text.toString().isNotEmpty()) {

                    for (m in syncMusicController.getList(listId.ID_MUSIC_ALL).list) {
                        val music = syncMusicController.getMusic(id)
                        if (music.title.toString().toLowerCase(Locale.ROOT).contains(
                                search.text.toString().toLowerCase(Locale.ROOT)
                            )
                            || music.artist.toString().toLowerCase(Locale.ROOT).contains(
                                search.text.toString().toLowerCase(Locale.ROOT)
                            )
                            || music.album.toString().toLowerCase(Locale.ROOT).contains(
                                search.text.toString().toLowerCase(Locale.ROOT)
                            )
                            || music.path.toString().toLowerCase(Locale.ROOT).contains(
                                search.text.toString().toLowerCase(Locale.ROOT)
                            )
                        ) {
                            newtab.add(id)
                        }

                        id++
                    }
                }

                if (tab.hashCode() != newtab.hashCode()) {
                    v.findViewById<LinearLayout>(R.id.searchResultLayout).removeAllViews()
                    tab = newtab

                    if(tab.size > 0)
                        addListItem(fm, R.id.searchResultLayout).initMusicIdList(tab)
                }
            }
        })

        return v
    }

    override fun onStart() {
        super.onStart()

        (activity as MainActivity).currentfragment = this

        (activity as MainActivity).button_back.visibility = View.INVISIBLE
        (activity as MainActivity).button_settings.visibility = View.VISIBLE
        (activity as MainActivity).title.text = "Search"

        (activity as MainActivity).btn_home.setColorFilter(null)
        (activity as MainActivity).btn_search.setColorFilter(R.color.th)
        (activity as MainActivity).btn_collection.setColorFilter(null)
    }
}