package com.example.musictest.fragments

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.musictest.Music
import com.example.musictest.R
import com.example.musictest.activities.MainActivity
import com.example.musictest.activities.musicController
import java.io.File

enum class ListerMode {
    None, ListMusics, ListMusicId, ListFiles
}

class ListerFragment : Fragment() {

    //var listMusic : ArrayList<Music> = ArrayList()
    var listMusicId : ArrayList<Int> = ArrayList()
    var folderPath : String = ""
    var mode : ListerMode = ListerMode.None

    lateinit var tab: ArrayList<Int>
    var selection: ArrayList<Int> = ArrayList()
    lateinit var listerLayout : LinearLayout
    lateinit var playlistBtn : Button
    lateinit var queueButton : Button
    lateinit var listerTitle : TextView
    lateinit var listerOptions : LinearLayout

    /*fun initMusicList(list: ArrayList<Music>) : Fragment
    {
        mode = ListerMode.ListMusics
        listMusic = list
        return this
    }*/

    fun initMusicIdList(list: ArrayList<Int>) : Fragment
    {
        mode = ListerMode.ListMusicId
        listMusicId = list
        return this
    }

    fun initFile(path: String) : Fragment
    {
        mode = ListerMode.ListFiles
        folderPath = path

        return this
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        val v = inflater.inflate(R.layout.fragment_lister, container, false)

        listerLayout = v.findViewById(R.id.listerLayout);

        tab = ArrayList()

        val fm = fragmentManager

        fun onCheckboxChanged() {

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

            Toast.makeText(v.context, selection.size.toString() + " songs added to playlist", Toast.LENGTH_SHORT).show()
        }

        queueButton.setOnClickListener{
            Toast.makeText(v.context, selection.size.toString() + " songs added to queue", Toast.LENGTH_SHORT).show()
        }

        when(mode){
            ListerMode.ListMusics -> {
                //Toast.makeText(context, "ListerMode listmusic not implemented yet!", Toast.LENGTH_SHORT).show()

                /*for (id in 0 until listMusic.size) {
                    ItemMusicFragment.addItem(fm, R.id.listerLayout).initMusicId(listMusic[id], id == 0).addClickCallback {
                        musicController.setQueueId(listMusicId)
                        musicController.play(id)
                    }.addSelectCallback {onCheckboxChanged()}
                }*/

            }
            ListerMode.ListMusicId -> {

                for (id in 0 until listMusicId.size) {
                    ItemMusicFragment.addItem(fm, R.id.listerLayout).initMusicId(listMusicId[id], id == 0).addClickCallback {
                        musicController.setQueueId(listMusicId)
                        musicController.play(id)
                    }.addSelectCallback {onCheckboxChanged()}
                }
            }
            ListerMode.ListFiles -> {

                listerTitle.visibility = View.VISIBLE
                listerTitle.text = folderPath

                val musicFiles : ArrayList<File> = ArrayList()

                val directory = File(folderPath)
                val files: Array<File> = directory.listFiles()!!
                for (file in files) {
                    if (file.isDirectory) {
                        ItemMusicFragment.addItem(fm, R.id.listerLayout).initFileId(file).addClickCallback {
                            (activity as MainActivity).replaceFragment(ListerFragment().initFile(file.absolutePath), true)
                        }
                    }
                    else if(MainActivity.isMusicFile(file)){
                        musicFiles.add(file)
                    }
                }

                for (id in 0 until musicFiles.size) {
                    ItemMusicFragment.addItem(fm, R.id.listerLayout).initFileId(musicFiles[id]).addClickCallback {
                        musicController.setQueueFiles(musicFiles)
                        musicController.play(id)
                    }.addSelectCallback {onCheckboxChanged()}
                }

                //Toast.makeText(context, "ListerMode Files !", Toast.LENGTH_SHORT).show()
            }
            else ->{
                //Toast.makeText(context, "ListerMode None !", Toast.LENGTH_SHORT).show()
            }
        }

        return v
    }
}