package com.example.musictest.fragments

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.musictest.R
import com.example.musictest.activities.MainActivity
import com.example.musictest.activities.musicController
import kotlinx.android.synthetic.main.fragment_item_music.view.*
import java.io.File


enum class ListerMode {
    None, ListMusics, ListMusicId, ListFiles, ListPlaylists
}

class ListerFragment : Fragment()  {

    var title : String = ""
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

    fun initMusicIdList(list: ArrayList<Int>) : ListerFragment
    {
        mode = ListerMode.ListMusicId
        listMusicId = list
        return this
    }

    fun setTitle(title: String) : ListerFragment
    {
        this.title = title

        return this
    }

    fun initFile(path: String) : ListerFragment
    {
        mode = ListerMode.ListFiles
        folderPath = path

        return this
    }

    fun initPlaylist() : ListerFragment
    {
        mode = ListerMode.ListPlaylists

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

        fun longClickCallback(id : Int) {
            //Toast.makeText(v.context, "enabled", Toast.LENGTH_SHORT).show()
            val childCount: Int = listerLayout.getChildCount()
            for (i in 0 until childCount) {
                val v: View = listerLayout.getChildAt(i)
                v.itemCheckBox.isEnabled = true
                if (i == id) {
                    v.itemCheckBox.isChecked = true
                }
            }
        }

        fun onCheckboxChanged(id : Int) {

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

            // if none selected
            if(sel == 0)
            {
                /*for (i in 0 until childCount) {
                    val v: View = listerLayout.getChildAt(i)
                    v.itemCheckBox.isEnabled = false
                }*/

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

                for (i in 0 until childCount) {
                    val v: View = listerLayout.getChildAt(i)
                    v.itemCheckBox.visibility = View.GONE
                }
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

                for (i in 0 until childCount) {
                    val v: View = listerLayout.getChildAt(i)
                    v.itemCheckBox.visibility = View.VISIBLE
                }
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
            musicController.addToPlaylistDialog(musicController.c,selection, {
                // uncheck selection
                val childCount: Int = listerLayout.getChildCount()
                for (i in 0 until childCount) {
                    val v: View = listerLayout.getChildAt(i)
                    v.itemCheckBox.isChecked = false
                }
            })
        }

        queueButton.setOnClickListener{
            musicController.addToQueue(selection, true)
            Toast.makeText(v.context, selection.size.toString() + " songs added to queue", Toast.LENGTH_SHORT).show()
        }

        when(mode){
            ListerMode.ListPlaylists -> {

                //listerTitle.visibility = View.VISIBLE
                //listerTitle.text = musicController.playlist[id].name

                for (id in 0 until musicController.playlist.size) {
                    ItemMusicFragment.addItem(fm, R.id.listerLayout).initPlaylistId(id, id == 0).addClickCallback {
                        (activity as MainActivity).replaceFragment(
                                ListerFragment().initMusicIdList(musicController.playlist[id].musics)
                                        .setTitle(musicController.playlist[id].name)

                        )
                    }
                }

            }
            /*ListerMode.ListMusics -> {
                //Toast.makeText(context, "ListerMode listmusic not implemented yet!", Toast.LENGTH_SHORT).show()

                /*for (id in 0 until listMusic.size) {
                    ItemMusicFragment.addItem(fm, R.id.listerLayout).initMusicId(listMusic[id], id == 0).addClickCallback {
                        musicController.setQueueId(listMusicId)
                        musicController.play(id)
                    }.addSelectCallback {onCheckboxChanged()}
                }*/
            }*/
            ListerMode.ListMusicId -> {

                fun clickCallback(id : Int)
                {
                    val childCount: Int = listerLayout.getChildCount()

                    var selectMode = false

                    for (i in 0 until childCount) {
                        val v: View = listerLayout.getChildAt(i)
                        if (v.itemCheckBox.isChecked) {
                            selectMode = true
                            break
                        }

                    }

                    if (selectMode) {
                        for (i in 0 until childCount) {
                            val v: View = listerLayout.getChildAt(i)
                            if (i == id) {
                                v.itemCheckBox.isChecked = !v.itemCheckBox.isChecked
                            }
                        }
                    } else {
                        musicController.setQueueId(listMusicId)
                        musicController.play(id)
                    }
                }

                //Thread{
                    for (id in 0 until listMusicId.size) {
                        ItemMusicFragment.addItem(fm, R.id.listerLayout)
                                .initMusicId(listMusicId[id], id == 0)
                                .addClickCallback { clickCallback(id) }
                                .addSelectCallback { onCheckboxChanged(id) }
                                .addLongClickCallback { longClickCallback(id) }
                    }
                //}.start()


            }
            ListerMode.ListFiles -> {

                listerTitle.visibility = View.VISIBLE
                listerTitle.text = folderPath

                val musicFiles: ArrayList<File> = ArrayList()

                val directory = File(folderPath)
                val files: Array<File> = directory.listFiles()!!
                for (file in files) {
                    if (file.isDirectory) {
                        ItemMusicFragment.addItem(fm, R.id.listerLayout).initFileId(file).addClickCallback {
                            (activity as MainActivity).replaceFragment(ListerFragment().initFile(file.absolutePath), true)
                        }
                    } else if (MainActivity.isMusicFile(file)) {
                        musicFiles.add(file)
                    }
                }

                for (id in 0 until musicFiles.size) {
                    ItemMusicFragment.addItem(fm, R.id.listerLayout).initFileId(musicFiles[id]).addClickCallback {
                        musicController.setQueueFiles(musicFiles)
                        musicController.play(id)
                    }.addSelectCallback { onCheckboxChanged(id) }
                }

                //Toast.makeText(context, "ListerMode Files !", Toast.LENGTH_SHORT).show()
            }
            else ->{
                //Toast.makeText(context, "ListerMode None !", Toast.LENGTH_SHORT).show()
            }
        }

        if(title.isNotEmpty())
        {
            listerTitle.visibility = View.VISIBLE
            listerTitle.text = title
        }



        return v
    }


}