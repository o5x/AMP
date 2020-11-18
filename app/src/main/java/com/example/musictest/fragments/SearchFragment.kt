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
import com.example.musictest.activities.musicController

class SearchFragment : Fragment() {

    lateinit var search : EditText;

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        val v = inflater.inflate(R.layout.fragment_search, container, false)

        search = v.findViewById(R.id.editTextSearch);

        val fm = fragmentManager

        var id = 0

        var tab = ArrayList<Int>()

        for(i in musicController.musics)
        {
            tab.add(id++)
        }

        // init with all ids
        addListItem(fm, R.id.searchResultLayout).initMusicIdList(tab)

        search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                val newtab: ArrayList<Int> = ArrayList()

                id = 0

                if (search.text.toString().length != 0) {

                    for (m in musicController.musics) {
                        var music = musicController.musics[id]
                        if (music.title.toLowerCase().contains(
                                search.text.toString().toLowerCase()
                            )
                            || music.artist.toLowerCase().contains(
                                search.text.toString().toLowerCase()
                            )
                            || music.album.toLowerCase().contains(
                                search.text.toString().toLowerCase()
                            )
                            || music.path.toLowerCase().contains(
                                search.text.toString().toLowerCase()
                            )
                        ) {
                            newtab.add(id)
                        }

                        id++
                    }
                }

                if (tab.hashCode() != newtab.hashCode()) {
                    v.findViewById<LinearLayout>(R.id.searchResultLayout).removeAllViews();
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