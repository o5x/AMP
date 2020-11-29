package com.example.musictest.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.example.musictest.R
import com.example.musictest.activities.MainActivity
import com.example.musictest.activities.smc
import com.example.musictest.musics.ListContent
import com.example.musictest.musics.ListId
import com.example.musictest.musics.SyncList
import kotlinx.android.synthetic.main.fragment_search.*
import java.util.*
import kotlin.collections.ArrayList

class SearchFragment : Fragment() {

    fun addListItem(
        fm: androidx.fragment.app.FragmentManager?,
        layout_id: Int,
    ): ListerRecyclerFragment {
        val fragOne = ListerRecyclerFragment()
        val tr = fm!!.beginTransaction()
        tr.add(layout_id, fragOne)
        tr.commitAllowingStateLoss()
        tr.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
        return fragOne
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fm = childFragmentManager
        var tab = ArrayList<Int>()

        editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                val newTab: ArrayList<Int> = ArrayList()
                var id = 0

                val currentSearch = editTextSearch.text.toString().toLowerCase(Locale.ROOT)

                if (currentSearch.isNotEmpty()) {
                    for (m in smc.getList(ListId.ID_MUSIC_ALL).list) {
                        val music = smc.getMusic(id)
                        if (music.title.toString().toLowerCase(Locale.ROOT).contains(currentSearch)
                            || music.artist.toString().toLowerCase(Locale.ROOT).contains(currentSearch)
                            || music.album.toString().toLowerCase(Locale.ROOT).contains(currentSearch)
                            || music.path.toLowerCase(Locale.ROOT).contains(currentSearch)
                        ) {
                            newTab.add(id)
                        }
                        id++
                    }
                }

                if (tab.hashCode() != newTab.hashCode()) {
                    view.findViewById<LinearLayout>(R.id.searchResultLayout).removeAllViews()
                    tab = newTab

                    if (tab.size > 0)
                        addListItem(fm, R.id.searchResultLayout).initSyncList(
                            SyncList("Search", ListContent.ListOfMusics, tab), header = false
                        )
                }
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onResume() {
        super.onResume()
        (activity as MainActivity).apply {
            tvTitle.text = "Search"
            btnHome.colorFilter = null
            btnSearch.setColorFilter(R.color.th)
            btnColleceion.colorFilter = null
            btnBack.visibility = View.INVISIBLE
        }
    }
}