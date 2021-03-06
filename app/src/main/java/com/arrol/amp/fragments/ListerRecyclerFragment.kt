package com.arrol.amp.fragments

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Paint
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.recyclerview.widget.LinearLayoutManager
import com.arrol.amp.R
import com.arrol.amp.activities.MainActivity
import com.arrol.amp.activities.smc
import com.arrol.amp.musics.*
import kotlinx.android.synthetic.main.fragment_lister_recycler.*
import java.io.File

enum class ListerMode {
    None, ListFiles, SyncList
}

class ListerRecyclerFragment : Fragment() {

    var syncList: SyncList? = null
    var syncListId: Int? = null

    private var showHeader = false

    var folderPath: String = ""
    var listerMode: ListerMode = ListerMode.None
    var files: ArrayList<File> = ArrayList()

    var callbackCheckBox: (id: Int) -> Unit = {}

    var childSelected: ArrayList<Boolean> = ArrayList()
    var checkboxVisibility = View.GONE

    var refreshCallback = {}
    var removeCallback = { i: Int -> }

    private var sortMode: SortMode = SortMode.None

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    override fun onResume() {
        super.onResume()
        /*try {
            if ((activity as MainActivity).currentfragment == this)
                //(activity as MainActivity).btnBack.visibility = View.VISIBLE
        } catch (e: Exception) {
        }*/
    }

    fun initSyncList(sl: SyncList, header: Boolean = true) {
        syncList = sl
        listerMode = ListerMode.SyncList
        showHeader = header
    }

    fun reload() {
        if (listerMode == ListerMode.SyncList) {
            syncList = smc.getList(syncListId!!)
            apply()
        }
    }

    fun initSyncListById(id: Int, header: Boolean = true): ListerRecyclerFragment {
        syncList = smc.getList(id)
        listerMode = ListerMode.SyncList
        syncListId = id
        showHeader = header
        return this
    }

    fun initFile(path: String): ListerRecyclerFragment {
        listerMode = ListerMode.ListFiles
        folderPath = path
        showHeader = true
        return this
    }

    fun replaceFragment(fragment: Fragment, force: Boolean) {
        (activity as MainActivity).replaceFragment(fragment, force)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // setup default sort mode
        if (syncList != null)
            sortMode = syncList!!.sortMode

        btn_sort.setOnClickListener {
            val popup = PopupMenu(context, btn_sort)
            popup.menu.add(0, 3, 3, "\uD835\uDC00\uD835\uDC33  Name ▼")
            popup.menu.add(0, 4, 4, "\uD835\uDC00\uD835\uDC33  Name ▲")
            popup.menu.add(0, 5, 5, "\uD83D\uDD50  Date ▼")
            popup.menu.add(0, 6, 6, "\uD83D\uDD50  Date ▲")
            //popup.menu.add(0, 7, 7, "⭐  Favourites ▼")
            //popup.menu.add(0, 8, 8, "⭐  Favourites ▲")
            popup.menu.add(0, 9, 9, "\uD83D\uDD00  Random")
            popup.setOnMenuItemClickListener { item ->
                sortMode = when (item.itemId) {
                    3 -> SortMode.Name
                    4 -> SortMode.NameR
                    5 -> SortMode.Date
                    6 -> SortMode.DateR
                    7 -> SortMode.Played
                    8 -> SortMode.PlayedR
                    9 -> SortMode.Random
                    else -> SortMode.Name
                }
                apply()
                return@setOnMenuItemClickListener true
            }
            popup.show()
        }

        apply()
        activity?.registerReceiver(broadcastReceiver3, IntentFilter("com.example.musictest.Update_Music"))
    }

    private var broadcastReceiver3: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            refreshCallback()
        }
    }

    private fun apply() {

        if (listerButtonAddTo == null) return
        // Global init
        listerButtonAddTo.visibility = View.VISIBLE
        listerButtonPlay.visibility = View.VISIBLE
        listerButtonRemove.visibility = View.GONE

        listerOptions.visibility = View.GONE

        ll_options.visibility = View.GONE
        ll_header.visibility = View.GONE

        btn_sort.visibility = View.GONE
        btn_playall.visibility = View.GONE
        btn_fav.visibility = View.GONE

        val lra = this

        list_recycler_view.apply {

            layoutManager = LinearLayoutManager(activity)

            refreshCallback = {
                //for(i in 0 until childSelected.size)
                //   (adapter as ListAdapter).notifyItemChanged(i)
                (adapter as ListAdapter).notifyDataSetChanged()
            }

            fun rem(position: Int) {
                smc.removeIdFromList(syncList!!.list[position], syncListId!!)
                syncList!!.list.remove(syncList!!.list[position])
                (adapter as ListAdapter).notifyItemRemoved(position);
                (adapter as ListAdapter).notifyItemRangeChanged(0, syncList!!.list.size);
                childSelected.removeAt(position)
            }

            removeCallback = { position: Int ->
                rem(position)
                syncList = smc.getList(syncListId!!) // not mandatory but we just make sure
                callbackCheckBox(position)

                if (!syncList!!.sortLocked) {
                    syncList!!.sort(sortMode)
                }

            }

            lra.callbackCheckBox = {
                var count = 0
                for (i in 0 until childSelected.size) if (childSelected[i]) count++
                if (count > 0) {
                    if (checkboxVisibility == View.GONE) refreshCallback()
                    checkboxVisibility = View.VISIBLE
                    listerOptions.visibility = View.VISIBLE
                    listerOptions.animate()
                        .alpha(1f)
                        .setDuration(100)
                        .setListener(null)
                } else {
                    if (checkboxVisibility == View.VISIBLE) refreshCallback()
                    checkboxVisibility = View.GONE
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


                    ll_header.visibility = View.VISIBLE
                    iv_list.setImageResource(R.drawable.folder)
                    tv_title.text = "Local Files"


                    tv_subtitle.text = folderPath

                    //(activity as MainActivity).tvTitle.text = "Files"

                    files.clear()

                    val directory = File(Environment.getExternalStorageDirectory().toString() + folderPath)
                    val filesList: Array<File> = directory.listFiles()!!

                    var filecount = 0
                    var foldercount = 0

                    for (file in filesList) {
                        if (file.isDirectory) {
                            files.add(file)
                            foldercount++
                        }
                    }
                    for (file in filesList) {
                        if (file.isFile) {
                            files.add(file)
                            filecount++
                        }
                    }

                    tv_subsubtitle.text = "$foldercount Folders, $filecount files" // todo manage s

                    childSelected.clear()
                    for (i in 0 until files.size) childSelected.add(false)
                }
                ListerMode.SyncList -> {

                    if (syncListId != null && syncListId!! > 0)
                        syncList = smc.getList(syncListId!!)

                    // Sort Button

                    if (!syncList!!.sortLocked) {
                        btn_sort.visibility = View.VISIBLE

                        btn_sort.text = "Sort by " + when (sortMode) {
                            SortMode.None -> "None"
                            SortMode.Name -> "name ▼"
                            SortMode.NameR -> "name ▲"
                            SortMode.Date -> "date ▼"
                            SortMode.DateR -> "date ▲"
                            SortMode.Played -> "played ▼"
                            SortMode.PlayedR -> "played ▲"
                            SortMode.Random -> "random"
                        }

                        syncList!!.sort(sortMode)
                    }

                    if (showHeader) {

                        ll_options.visibility = if (syncList?.list!!.isNotEmpty()) View.VISIBLE else View.GONE

                        if(syncListId == ListId.ID_MUSIC_QUEUE) ll_options.visibility = View.GONE;

                        ll_header.visibility = View.VISIBLE
                        iv_list.setImageBitmap(syncList!!.image)
                        tv_title.text = syncList!!.name

                        // Favourite btn
                        if (syncList!!.listType in arrayOf(ListType.Album, ListType.Artist)) {
                            btn_fav.visibility = View.VISIBLE

                            if (smc.isListLiked(syncListId!!)) {
                                btn_fav.setColorFilter(R.color.th)
                                btn_fav.setImageResource(R.drawable.ic_favourite)
                            } else {
                                btn_fav.colorFilter = null
                                btn_fav.setImageResource(R.drawable.ic_addfavourite)
                            }

                            btn_fav.setOnClickListener {
                                smc.toggleListLiked(syncListId!!)
                                if (smc.isListLiked(syncListId!!)) {
                                    btn_fav.setColorFilter(R.color.th)
                                    btn_fav.setImageResource(R.drawable.ic_favourite)
                                } else {
                                    btn_fav.colorFilter = null
                                    btn_fav.setImageResource(R.drawable.ic_addfavourite)
                                }
                            }
                        }

                        if (syncList!!.author_id != null) {
                            tv_subtitle.paintFlags = tv_subtitle.paintFlags or Paint.UNDERLINE_TEXT_FLAG
                            tv_subtitle.setOnClickListener {
                                replaceFragment(
                                    ListerRecyclerFragment().initSyncListById(syncList!!.author_id!!),
                                    true
                                )
                            }
                        }

                        tv_subtitle.text =
                            when (syncList!!.listType) {
                                ListType.Album -> "Album" + if (syncList!!.author != null) " by " + syncList!!.author else ""
                                ListType.Artist -> "Artist"
                                ListType.System -> {
                                    tv_subtitle.visibility = View.GONE
                                    ""
                                }
                                ListType.Playlist -> "Playlist" + if (syncList!!.author != null) " by " + syncList!!.author
                                else ""
                                ListType.None -> "Not valid"
                            }

                        val size = syncList!!.list.size

                        tv_subsubtitle.text =
                            when (syncList!!.listContent) {
                                ListContent.ListOfLists -> {
                                    size.toString() + when (syncList!!.listType) {
                                        ListType.Artist -> " Artist"
                                        ListType.Album -> " Album"
                                        else -> " Playlist"
                                    } + if (size == 1) "" else "s"
                                }
                                ListContent.ListOfMusics -> size.toString() + " Music" + if (size == 1) "" else "s"
                                ListContent.None -> "Not valid"
                            }


                        btn_playall.visibility = View.VISIBLE
                    }

                    if (syncList!!.listContent == ListContent.ListOfLists) {
                        btn_playall.visibility = View.GONE
                    }



                    childSelected.clear()
                    for (i in 0 until syncList!!.list.size) childSelected.add(false)

                    listerButtonPlay.setOnClickListener {
                        val selection: ArrayList<Int> = ArrayList()
                        for (i in 0 until childSelected.size) if (childSelected[i]) selection.add(syncList!!.list[i])
                        smc.setQueue(selection, syncListId, 0, true)
                    }

                    if (!syncList!!.readonly) {
                        listerButtonRemove.visibility = View.VISIBLE
                        listerButtonRemove.setOnClickListener {

                            val listToRemove = ArrayList<Int>()
                            for (i in 0 until childSelected.size)
                                if (childSelected[i]) listToRemove.add(i)

                            for ((i, v) in listToRemove.iterator().withIndex())
                                rem(v - i)


                            syncList = smc.getList(syncListId!!)

                            if (!syncList!!.sortLocked)
                                syncList!!.sort(sortMode)

                            callbackCheckBox(0)
                        }
                    }

                    listerButtonAddTo.setOnClickListener {
                        val selection: ArrayList<Int> = ArrayList()
                        for (i in 0 until childSelected.size) if (childSelected[i]) selection.add(syncList!!.list[i])
                        val popup = PopupMenu(context, listerButtonAddTo)
                        smc.addPlaylistMenu(popup.menu)
                        popup.setOnMenuItemClickListener { item ->
                            smc.processPlaylistMenu(requireContext(), selection, item)
                            return@setOnMenuItemClickListener true
                        }
                        popup.show()
                    }

                    btn_playall.setOnClickListener {
                        if (syncList!!.list.isNotEmpty())
                            smc.setQueue(syncList!!.list, syncListId, 0, true)
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