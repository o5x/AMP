package com.example.musictest.fragments

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musictest.R
import com.example.musictest.activities.MainActivity
import com.example.musictest.activities.syncMusicController

import kotlinx.android.synthetic.main.fragment_lister_recycler.*
import java.io.File

enum class ListerMode{
    None,ListMusicId,ListPlaylists, ListFiles
}

class ListerRecyclerFragment : Fragment()  {

    var title : String = ""
    var listIds : ArrayList<Int> = ArrayList()
    var folderPath : String = ""
    var listerMode : ListerMode = ListerMode.None
    var files : ArrayList<File> = ArrayList()

    var callbackCheckBox : (id : Int) -> Unit = {}
    var clickCallback : (id : Int) -> Unit = {}

    var childSelected : ArrayList<Boolean> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    fun initMusicIdList(list: ArrayList<Int>) : ListerRecyclerFragment
    {
        listerMode = ListerMode.ListMusicId
        listIds = list
        return this
    }

    fun initPlaylistList(list: ArrayList<Int>) : ListerRecyclerFragment
    {
        listerMode = ListerMode.ListPlaylists
        listIds = list
        return this
    }

    fun initFile(path: String) : ListerRecyclerFragment
    {
        listerMode = ListerMode.ListFiles
        folderPath = path
        return this
    }

    fun setTitle(title: String) : ListerRecyclerFragment
    {
        this.title = title
        return this
    }

    private fun getSelection() : ArrayList<Int>
    {
        val selection: ArrayList<Int> = ArrayList()
        for(i in 0 until childSelected.size) if(childSelected[i]) selection.add(listIds[i])
        return selection
    }

    fun replaceFragment(fragment: Fragment, force : Boolean)
    {
        (activity as MainActivity).replaceFragment(fragment, force)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Global init
        listerButtonAddTo.isEnabled = false
        listerButtonPlay.isEnabled = false

        listerOptions.visibility = View.GONE

        if(title.isNotEmpty()) listerTitle.text = "$title (${listIds.size})"
        else listerTitle.visibility = View.GONE

        val lra = this

        list_recycler_view.apply {

            layoutManager = LinearLayoutManager(activity)

            lra.callbackCheckBox = {
                var count = 0
                for(i in 0 until childSelected.size)
                {
                    if(childSelected[i]) count ++
                }
                if(count > 0)
                {
                    (adapter as ListAdapter).selectMode(layoutManager!!, View.VISIBLE)
                    listerOptions.visibility = View.VISIBLE
                    listerButtonAddTo.isEnabled = true
                    listerButtonPlay.isEnabled = true
                    listerOptions.animate()
                            .alpha(1f)
                            .setDuration(100)
                            .setListener(null)

                    if(title.isNotEmpty()) listerTitle.text = "$title (${listIds.size}) • $count selected"
                }
                else
                {
                    (adapter as ListAdapter).selectMode(layoutManager!!, View.GONE)

                    listerButtonAddTo.isEnabled = false
                    listerButtonPlay.isEnabled = false
                    listerOptions.animate()
                            .alpha(0f)
                            .setDuration(100)
                            .setListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator) {
                                    listerOptions.visibility = View.GONE
                                }
                            })
                    if(title.isNotEmpty()) listerTitle.text = "$title (${listIds.size})"
                }
            }

            // MODES

            when(listerMode)
            {
                ListerMode.ListMusicId -> {
                    childSelected.clear()
                    for(i in 0 until listIds.size) childSelected.add(false);

                    listerButtonPlay.setOnClickListener{
                        syncMusicController.setQueue(getSelection())
                        syncMusicController.play(0)
                    }

                    listerButtonAddTo.setOnClickListener{
                        syncMusicController.addToPlaylistDialog(syncMusicController.c, getSelection())
                    }
                }
                ListerMode.ListPlaylists ->{
                    childSelected.clear()
                    for(i in 0 until listIds.size) childSelected.add(false);

                    lra.clickCallback = {
                        (activity as MainActivity).replaceFragment(
                                ListerRecyclerFragment().initMusicIdList(syncMusicController.lists[listIds[it]].list)
                                        .setTitle(syncMusicController.lists[listIds[it]].name))
                    }

                }
                ListerMode.ListFiles ->{

                    listerTitle.visibility = View.VISIBLE
                    listerTitle.text = folderPath

                    files.clear()

                    val directory = File(folderPath)
                    val filesList: Array<File> = directory.listFiles()!!

                    for (file in filesList) {
                        if (file.isDirectory) files.add(file)
                    }
                    for (file in filesList) {
                        if (file.isFile) files.add(file)
                    }

                    childSelected.clear()
                    for(i in 0 until files.size) childSelected.add(false);
                }
                else -> {
                    Toast.makeText(context, "ListerMode None !", Toast.LENGTH_SHORT).show()
                }
            }


            adapter = ListAdapter(lra)
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_lister_recycler, container, false)
    }


}