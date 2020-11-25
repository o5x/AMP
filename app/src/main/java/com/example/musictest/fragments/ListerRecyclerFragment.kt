package com.example.musictest.fragments

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musictest.R
import com.example.musictest.activities.MainActivity
import com.example.musictest.activities.syncMusicController
import com.example.musictest.musics.ListType
import com.example.musictest.musics.SyncList
import kotlinx.android.synthetic.main.activity_music_controller.*
import kotlinx.android.synthetic.main.fragment_lister_recycler.*
import java.io.File
import java.lang.Exception

enum class ListerMode {
    None, ListMusicId, ListPlaylists, ListFiles, syncList
}

enum class SortMode {
    Id, IdR, Name, NameR, Random//(alphabetical), Date, DateR(added), Played, PlayedR (mostplayed)
}

class ListerRecyclerFragment : Fragment() {

    var syncList : SyncList? = null
    var syncListId : Int? = null

    //var title: String = ""
    //var listIds: ArrayList<Int> = ArrayList()
    //var listIdsOrigin : Int? = null; //undefined
    var folderPath: String = ""
    var listerMode: ListerMode = ListerMode.None
    var files: ArrayList<File> = ArrayList()

    var callbackCheckBox: (id: Int) -> Unit = {}
    var clickCallback: (id: Int) -> Unit = {}

    var childSelected: ArrayList<Boolean> = ArrayList()

    var sortMode: SortMode = SortMode.Id

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onResume() {
        super.onResume()

        try{
            if((activity as MainActivity).currentfragment == this)
                (activity as MainActivity).btn_back.visibility = View.VISIBLE
        }
        catch (e : Exception)
        {

        }


    }

    /*fun initMusicIdList(list: ArrayList<Int>): ListerRecyclerFragment {
        listerMode = ListerMode.ListMusicId
        listIds = list
        return this
    }*/

    fun initSyncList(sl : SyncList){
        syncList = sl
        listerMode = ListerMode.syncList
    }

    fun initSyncListById(id : Int) : ListerRecyclerFragment{
        syncList = syncMusicController.getList(id)
        listerMode = ListerMode.syncList
        syncListId = id
        return this
    }

    /*fun initMusicIdList(origin : Int): ListerRecyclerFragment {
        listerMode = ListerMode.ListMusicId
        listIds = syncMusicController.getList(origin).list
        listIdsOrigin = origin
        return this
    }

    fun initPlaylistList(list: ArrayList<Int>) : ListerRecyclerFragment
    {
        listerMode = ListerMode.ListPlaylists
        listIds = list
        return this
    }*/

    fun initFile(path: String) : ListerRecyclerFragment
    {
        listerMode = ListerMode.ListFiles
        folderPath = path
        return this
    }

    fun setTitle(title: String) : ListerRecyclerFragment
    {
        //this.title = title
        return this
    }

    /*private fun getSelection() : ArrayList<Int>
    {
        val selection: ArrayList<Int> = ArrayList()
        for(i in 0 until childSelected.size) if(childSelected[i]) selection.add(i) // TODO GET THAT
        return selection
    }*/

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
        ll_options.visibility = View.GONE
        listerTitle.visibility = View.GONE

        btn_sort.setOnClickListener {
            val popup = PopupMenu(context, btn_sort)
            popup.menu.add(0, 1, 1, "Add date desc")
            popup.menu.add(0, 2, 2, "Add date asc")
            popup.menu.add(0, 3, 3, "Alphabetical desc")
            popup.menu.add(0, 4, 4, "Alphabetical asc")
            popup.menu.add(0, 5, 5, "Most listened desc")
            popup.menu.add(0, 6, 6, "Most listened asc")
            popup.menu.add(0, 7, 7, "Random")
            popup.setOnMenuItemClickListener { item ->
                sortMode = when (item.itemId) {
                    1 -> SortMode.Id
                    2 -> SortMode.IdR
                    3 -> SortMode.Name
                    4 -> SortMode.NameR
                    7 -> SortMode.Random
                    else -> SortMode.Id
                }
                apply()
                return@setOnMenuItemClickListener true
            }
            popup.show();
        }

        //if (title.isNotEmpty()) listerTitle.text = "$title (${listIds.size})"
        //else listerTitle.visibility = View.GONE

        apply()
    }

    private fun apply() {

        btn_sort.text = "Sort by " + when(sortMode){
            SortMode.Id -> "date ▼"
            SortMode.IdR -> "date ▲"
            SortMode.Name -> "name ▼"
            SortMode.NameR -> "name ▲"
            SortMode.Random-> "random"
        }

        val lra = this

        list_recycler_view.apply {

            layoutManager = LinearLayoutManager(activity)

            lra.callbackCheckBox = {
                var count = 0
                for (i in 0 until childSelected.size) {
                    if (childSelected[i]) count++
                }
                if (count > 0) {
                    (adapter as ListAdapter).selectMode(layoutManager!!, View.VISIBLE)
                    listerOptions.visibility = View.VISIBLE
                    listerButtonAddTo.isEnabled = true
                    listerButtonPlay.isEnabled = true
                    listerOptions.animate()
                            .alpha(1f)
                            .setDuration(100)
                            .setListener(null)


                } else {
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
                }
            }

            // MODES

            when (listerMode) {
                ListerMode.ListFiles -> {

                    (activity as MainActivity).tv_title.text = "Files"
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
                    for (i in 0 until files.size) childSelected.add(false)
                }
                ListerMode.syncList -> {

                    try {
                        (activity as MainActivity).tv_title .text = syncList!!.name
                    }catch (e : Exception){

                    }


                    if(syncList!!.listType == ListType.listOfMusics){

                        // show play options
                        ll_options.visibility = View.VISIBLE

                        // SORT LIST
                        val cp = syncMusicController.musics
                        when (sortMode) {
                            SortMode.Id -> {
                                syncList!!.list = ArrayList(syncList!!.list.sortedWith(compareBy { it }))
                            }
                            SortMode.IdR -> {
                                syncList!!.list = ArrayList(syncList!!.list.sortedWith(compareByDescending { it }))
                            }
                            SortMode.Name -> {
                                syncList!!.list = ArrayList(syncList!!.list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { cp[it]?.title.toString() }))
                            }
                            SortMode.NameR -> {
                                syncList!!.list = ArrayList(syncList!!.list.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { cp[it]?.title.toString() }))
                            }
                            SortMode.Random -> {
                                syncList!!.list.shuffle()
                            }
                        }
                    }

                    childSelected.clear()
                    for (i in 0 until syncList!!.list.size) childSelected.add(false);

                    listerButtonPlay.setOnClickListener {
                        val selection: ArrayList<Int> = ArrayList()
                        for(i in 0 until childSelected.size) if(childSelected[i]) selection.add(syncList!!.list[i])
                        syncMusicController.setQueue(selection, syncListId, 0, true)
                    }

                    listerButtonAddTo.setOnClickListener {
                        val selection: ArrayList<Int> = ArrayList()
                        for(i in 0 until childSelected.size) if(childSelected[i]) selection.add(syncList!!.list[i])
                        syncMusicController.addToPlaylistDialog(syncMusicController.c, selection)
                    }

                    btn_playall.setOnClickListener {
                        syncMusicController.setQueue(syncList!!.list, syncListId, 0, true)
                    }

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

    fun addItem(
            fm: androidx.fragment.app.FragmentManager?,
            layout_id: Int,
    ): ListerRecyclerFragment {
        val fragOne: Fragment = ListerRecyclerFragment()
        val tr = fm!!.beginTransaction()
        tr.add(layout_id, fragOne)
        tr.commitAllowingStateLoss()
        tr.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)

        return fragOne as ListerRecyclerFragment
    }


}