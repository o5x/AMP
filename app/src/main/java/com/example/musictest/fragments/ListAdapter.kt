package com.example.musictest.fragments

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.musictest.R
import com.example.musictest.activities.smc
import com.example.musictest.musics.ListContent
import com.example.musictest.musics.ListId
import com.example.musictest.musics.SyncMusicController.Companion.isMusicFile
import com.example.musictest.musics.SyncMusicController.Companion.isVideoFile


class ListAdapter(private val listerRecyclerFragment: ListerRecyclerFragment) :
    RecyclerView.Adapter<MovieViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return MovieViewHolder(inflater, parent)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.bind(listerRecyclerFragment)
    }

    fun selectMode(layoutManager: RecyclerView.LayoutManager, visibility: Int) {
        listerRecyclerFragment.checkboxVisibility = visibility
        for (i in 0 until layoutManager.childCount) {
            val cb = layoutManager.getChildAt(i)!!.findViewById<CheckBox>(R.id.list_checkBox)
            cb.visibility = visibility
        }
    }

    override fun getItemCount(): Int = listerRecyclerFragment.childSelected.size
}

class MovieViewHolder(inflater: LayoutInflater, parent: ViewGroup) :
    RecyclerView.ViewHolder(inflater.inflate(R.layout.list_item, parent, false)) {
    var mTitleView: TextView? = null
    var mYearView: TextView? = null
    var mImageView: ImageView? = null
    var mCheckBox: CheckBox? = null
    var imageButtonMore: ImageButton? = null

    init {
        mTitleView = itemView.findViewById(R.id.list_title)
        mYearView = itemView.findViewById(R.id.list_description)
        mImageView = itemView.findViewById(R.id.list_image)
        mCheckBox = itemView.findViewById(R.id.list_checkBox)
        imageButtonMore = itemView.findViewById(R.id.imageButtonMore)
    }

    fun bind(lrf: ListerRecyclerFragment) {

        if (adapterPosition >= lrf.childSelected.size) return
        mCheckBox?.isEnabled = false
        mCheckBox?.visibility = lrf.checkboxVisibility
        mCheckBox?.isChecked = lrf.childSelected[adapterPosition]

        imageButtonMore?.visibility = View.GONE

        var onclick = {
            lrf.clickCallback(adapterPosition)
        }

        when (lrf.listerMode) {

            ListerMode.ListFiles -> {

                val file = lrf.files[adapterPosition]
                mTitleView?.text = file.name

                if (file.isDirectory) {
                    mYearView?.text = "directory"
                    mImageView?.setImageResource(R.drawable.folder)

                    onclick = {
                        lrf.replaceFragment(
                            ListerRecyclerFragment().initFile(file.absolutePath),
                            true
                        )
                    }
                }
                if (file.isFile) {
                    mYearView?.text = "File"
                    mImageView?.setImageResource(R.drawable.file)

                    if (isMusicFile(file)) {
                        mCheckBox?.isEnabled = true
                        mYearView?.text = "Music"
                        mImageView?.setImageResource(R.drawable.music)

                        onclick = {
                            smc.setQueueFiles(lrf.files, lrf.folderPath, adapterPosition)
                        }
                    } else if (isVideoFile(file)) {
                        mImageView?.setImageResource(R.drawable.video)
                    }
                }
            }

            ListerMode.SyncList -> {
                if (lrf.syncList!!.listContent == ListContent.ListOfMusics) {

                    mCheckBox?.isEnabled = true

                    val music = smc.getMusic(lrf.syncList!!.list[adapterPosition])

                    mTitleView?.text = music.title
                    mYearView?.text = music.artist

                    // menu options
                    imageButtonMore?.visibility = View.VISIBLE
                    //imageButtonMore?.isEnabled = false
                    imageButtonMore?.setOnClickListener {
                        val popup = PopupMenu(lrf.context, imageButtonMore)

                        val sm = popup.menu.addSubMenu(0, 1, 1, "Add to playlist")
                        smc.addPlaylistMenu(sm)
                        popup.menu.add(0, 2, 2, "View Album")
                        popup.menu.add(0, 3, 3, "View Artist")
                        popup.menu.add(0, 4, 4, "Info")
                        //popup.menu.add(0, 5, 5, "Delete music").isEnabled = false

                        popup.setOnMenuItemClickListener { item ->
                            when (item.itemId) {
                                1 -> {
                                }
                                2 -> {
                                    lrf.replaceFragment(
                                        ListerRecyclerFragment().initSyncListById(music.albumId!!),
                                        true
                                    )
                                }
                                3 -> {
                                    lrf.replaceFragment(
                                        ListerRecyclerFragment().initSyncListById(music.artistId!!),
                                        true
                                    )
                                }
                                4 -> {
                                    val builder1 = AlertDialog.Builder(lrf.context)
                                    builder1.setTitle(music.title)
                                    builder1.setMessage("\nName : ${music.title}\n\nAlbum : ${music.album}\n\nArtist : ${music.artist}\n\nPath : ${music.path}")
                                    builder1.setCancelable(true)
                                    builder1.setIcon(R.drawable.ic_music)
                                    builder1.setPositiveButton("Done") { dialog, _ -> dialog.cancel() }
                                    val alert11 = builder1.create()
                                    alert11.show()
                                }
                                else -> smc.processPlaylistMenu(
                                    lrf.requireContext(),
                                    lrf.syncList!!.list[adapterPosition],
                                    music,
                                    item
                                )
                            }
                            return@setOnMenuItemClickListener true
                        }
                        popup.show();
                    }

                    mImageView?.setImageResource(R.drawable.music)
                    if (music.image != null) mImageView?.setImageBitmap(music.image)

                    onclick = {
                        smc.setQueue(lrf.syncList!!.list, lrf.syncListId, adapterPosition, true)
                    }


                } else if (lrf.syncList!!.listContent == ListContent.ListOfLists) {

                    val sublist = smc.getList(lrf.syncList!!.list[adapterPosition])

                    mTitleView?.text = sublist.name

                    if (sublist.listContent == ListContent.ListOfMusics) mYearView?.text =
                        sublist.list.size.toString() + " Songs"
                    else mYearView?.text = sublist.list.size.toString() + " Lists"

                    if (lrf.syncList!!.list[adapterPosition] >= ListId.ID_MUSIC_MAX_ID)
                        imageButtonMore?.visibility = View.VISIBLE

                    if (sublist.image != null) mImageView?.setImageBitmap(sublist.image)

                    imageButtonMore?.setOnClickListener {
                        val popup = PopupMenu(lrf.context, imageButtonMore)

                        popup.menu.add(0, 1, 1, "View")
                        popup.menu.add(0, 2, 2, "Play")
                        popup.menu.add(0, 3, 3, "Delete playlist")
                        popup.menu.add(0, 4, 4, "Info")
                        //popup.menu.add(0, 5, 5, "Delete music").isEnabled = false

                        popup.setOnMenuItemClickListener { item ->
                            val list = smc.getList(lrf.syncList!!.list[adapterPosition])
                            when (item.itemId) {
                                1 -> {
                                    lrf.replaceFragment(
                                        ListerRecyclerFragment().initSyncListById(lrf.syncList!!.list[adapterPosition]),
                                        true
                                    )
                                }
                                2 -> {
                                    smc.setQueue( list.list,lrf.syncList!!.list[adapterPosition],0,true)
                                }
                                3 -> {
                                    smc.deletePlaylist(lrf.syncList!!.list[adapterPosition])
                                    Toast.makeText(lrf.context, "Playlist deleted", Toast.LENGTH_SHORT).show()
                                    lrf.reload()
                                }
                                4 -> {
                                    val builder1 = AlertDialog.Builder(lrf.context)
                                    builder1.setTitle(list.name)
                                    builder1.setMessage("\nName : ${list.name}\n\nType : ${list.listContent}\n\nContains : ${list.list.size} elements")
                                    builder1.setCancelable(true)
                                    builder1.setIcon(R.drawable.ic_list)
                                    builder1.setPositiveButton("Done") { dialog, _ -> dialog.cancel() }
                                    val alert11 = builder1.create()
                                    alert11.show()
                                }
                                //else -> {Toast.makeText(lrf.context, "Saving song to ${item.itemId}" + item.title , Toast.LENGTH_SHORT).show()}
                            }
                            return@setOnMenuItemClickListener true
                        }
                        popup.show()
                    }

                    onclick = {
                        lrf.replaceFragment(
                            ListerRecyclerFragment().initSyncListById(lrf.syncList!!.list[adapterPosition]), // TODO change origin
                            true
                        )
                    }
                }
            }
            else -> { }
        }

        itemView.setOnClickListener {
            if (mCheckBox!!.isEnabled && mCheckBox?.visibility == View.VISIBLE) {
                mCheckBox?.isChecked = !mCheckBox?.isChecked!!
                lrf.childSelected[adapterPosition] = mCheckBox!!.isChecked
                lrf.callbackCheckBox(adapterPosition)
            } else onclick()
        }

        itemView.setOnLongClickListener {
            if (mCheckBox!!.isEnabled) {
                mCheckBox?.isChecked = true
                lrf.childSelected[adapterPosition] = true
                lrf.callbackCheckBox(adapterPosition)
                return@setOnLongClickListener true
            }
            return@setOnLongClickListener false
        }
        mCheckBox!!.setOnClickListener {
            if (mCheckBox!!.isEnabled) {
                lrf.childSelected[adapterPosition] = mCheckBox!!.isChecked
                lrf.callbackCheckBox(adapterPosition)
            }
        }
    }
}