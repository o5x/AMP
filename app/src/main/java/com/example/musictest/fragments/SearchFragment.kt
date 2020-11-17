package com.example.musictest.fragments

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.musictest.activities.MainActivity
import com.example.musictest.R
import com.example.musictest.activities.musicController

class SearchFragment : Fragment() {

    lateinit var search : EditText;
    lateinit var tab: ArrayList<Int>
    var selection: ArrayList<Int> = ArrayList()
    lateinit var listerLayout : LinearLayout
    lateinit var playlistBtn : Button
    lateinit var queueButton : Button
    lateinit var listerTitle : TextView
    lateinit var listerOptions : LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        var v = inflater.inflate(R.layout.fragment_search, container, false)

        search = v.findViewById(R.id.editTextSearch);
        listerLayout = v.findViewById(R.id.listerLayout);

        tab = ArrayList()

        val fm = fragmentManager

        fun oncheckboxchanged(): () -> Unit = {

            selection.clear()

            var sel = 0

            val childCount: Int = listerLayout.getChildCount()
            for (i in 0 until childCount) {
                val v: View = listerLayout.getChildAt(i)
                if(v.findViewById<CheckBox>(R.id.itemCheckBox).isChecked)
                {
                    sel ++
                    selection.add(i)
                }
            }

            if(sel == 0)
            {
                playlistBtn.isEnabled = false
                queueButton.isEnabled = false
                listerOptions.animate()
                    .alpha(0f)
                    .setDuration(100)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            listerOptions.visibility = View.GONE
                        }
                    })
            }
            else
            {
                listerOptions.visibility = View.VISIBLE
                playlistBtn.isEnabled = true
                queueButton.isEnabled = true
                listerOptions.animate()
                    .alpha(1f)
                    .setDuration(100)
                    .setListener(null)
            }
        }

        queueButton = v.findViewById(R.id.listerButtonQueue)
        playlistBtn = v.findViewById(R.id.listerButtonPlaylist)
        listerOptions = v.findViewById(R.id.listerOptions)
        listerTitle = v.findViewById(R.id.listerTitle)

        playlistBtn.isEnabled = false
        queueButton.isEnabled = false
        listerOptions.visibility = View.GONE
        listerTitle.visibility = View.GONE

        playlistBtn.setOnClickListener{

            Toast.makeText(v.context, selection.size.toString()+ " songs added to playlist", Toast.LENGTH_SHORT).show()
        }

        queueButton.setOnClickListener{
            Toast.makeText(v.context, selection.size.toString()+ " songs added to queue", Toast.LENGTH_SHORT).show()
        }

        var id = 0

        for(mid in musicController.musics)
        {
            ItemMusicFragment.addItem2(fm, R.id.listerLayout, id, id++ == 0, oncheckboxchanged())
        }

        search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                var newtab: ArrayList<Int> = ArrayList()

                var id = 0

                if (search.text.toString().length != 0) {
                    for (music in musicController.musics) {
                        if (music.title.toString().toLowerCase().contains(
                                search.text.toString().toLowerCase()
                            )
                            || music.artist.toString().toLowerCase().contains(
                                search.text.toString().toLowerCase()
                            )
                            || music.album.toString().toLowerCase().contains(
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

                id = 0

                if (tab.hashCode() != newtab.hashCode()) {
                    v.findViewById<LinearLayout>(R.id.listerLayout).removeAllViews();
                    tab = newtab
                    for (mid in tab) {
                        ItemMusicFragment.addItem(fm, R.id.listerLayout, mid, id++ == 0)
                    }
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