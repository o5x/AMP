package com.example.musictest.fragments

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.musictest.R
import com.example.musictest.activities.syncMusicController
import com.example.musictest.musics.ListId
import com.example.musictest.musics.ListType
import com.example.musictest.musics.SyncMusicController.Companion.isMusicFile


class ListAdapter(private val listerRecyclerFragment: ListerRecyclerFragment)
    : RecyclerView.Adapter<MovieViewHolder>() {

    private var checkBoxVisibility = View.GONE

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return MovieViewHolder(inflater, parent)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.bind(checkBoxVisibility, listerRecyclerFragment)
    }

    fun selectMode(layoutManager: RecyclerView.LayoutManager, visibility: Int)
    {
        checkBoxVisibility = visibility
        for (i in 0 until layoutManager.childCount)
        {
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
    var imageButtonMore : ImageButton? = null

    init {
        mTitleView = itemView.findViewById(R.id.list_title)
        mYearView = itemView.findViewById(R.id.list_description)
        mImageView = itemView.findViewById(R.id.list_image)
        mCheckBox = itemView.findViewById(R.id.list_checkBox)
        imageButtonMore = itemView.findViewById(R.id.imageButtonMore)
    }

    fun bind(visibility: Int, lrf: ListerRecyclerFragment) {

        if(adapterPosition >= lrf.childSelected.size) return

        imageButtonMore?.visibility = View.GONE

        var onclick = {
            lrf.clickCallback(adapterPosition)
        }

        when(lrf.listerMode) {

            ListerMode.ListFiles -> {

                mCheckBox?.isEnabled = false
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
                            syncMusicController.setQueueFiles(lrf.files, lrf.folderPath, adapterPosition)
                        }
                    }
                }
            }

            ListerMode.syncList -> {
                if (lrf.syncList!!.listType == ListType.listOfMusics) {

                    val music = syncMusicController.getMusic(lrf.syncList!!.list[adapterPosition])

                    mTitleView?.text = music.title
                    mYearView?.text = music.artist


                    // menu options
                    imageButtonMore?.visibility = View.VISIBLE
                    //imageButtonMore?.isEnabled = false
                    imageButtonMore?.setOnClickListener {
                        val popup = PopupMenu(lrf.context, imageButtonMore)

                        val sm = popup.menu.addSubMenu(0, 1, 1, "Add to playlist")
                        sm.add(0, 11, 1, "liked ")
                        for (i in 1 until 7)
                            sm.add(0, 11 + i, i + 1, "playlist $i")

                        popup.menu.add(0, 2, 2, "View Album")
                        popup.menu.add(0, 3, 3, "View Artist")
                        popup.menu.add(0, 4, 4, "Info")
                        popup.menu.add(0, 5, 5, "Delete music").isEnabled = false

                        popup.setOnMenuItemClickListener { item ->
                            when(item.itemId)
                            {
                                1 -> {}
                                2 -> {Toast.makeText(lrf.context, "Go album ", Toast.LENGTH_SHORT).show()}
                                3 -> {Toast.makeText(lrf.context, "Go artist ", Toast.LENGTH_SHORT).show()}
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
                                in 11..50 -> {Toast.makeText(lrf.context, "Saving song to " + item.title , Toast.LENGTH_SHORT).show()}
                            }

                            return@setOnMenuItemClickListener true
                        }
                        popup.show();
                    }


                    mImageView?.setImageResource(R.drawable.music)
                    if (music.image != null) mImageView?.setImageBitmap(music.image)

                    onclick = {
                        syncMusicController.setQueue(lrf.syncList!!.list, lrf.syncListId, adapterPosition, true)
                    }


                } else if (lrf.syncList!!.listType == ListType.listOfLists) {

                    val sublist = syncMusicController.getList(lrf.syncList!!.list[adapterPosition])

                    mTitleView?.text = sublist.name

                    if(sublist.listType == ListType.listOfMusics)mYearView?.text = sublist.list.size.toString() + " Songs"
                    else mYearView?.text = sublist.list.size.toString() + " Lists"

                    when (lrf.syncList!!.list[adapterPosition]) {
                        ListId.ID_MUSIC_LIKED -> mImageView?.setImageResource(R.drawable.liked)
                        ListId.ID_MUSIC_QUEUE -> mImageView?.setImageResource(R.drawable.queue)
                        else -> mImageView?.setImageResource(R.drawable.playlist)
                    }

                    onclick = {
                        lrf.replaceFragment(
                                ListerRecyclerFragment().initSyncListById(lrf.syncList!!.list[adapterPosition]), // TODO change origin
                                true
                        )
                    }

                }
            }
            else -> {

            }
        }

        mCheckBox?.visibility = visibility
        mCheckBox?.isChecked = lrf.childSelected[adapterPosition]

        itemView.setOnClickListener {
            if(mCheckBox!!.isEnabled && mCheckBox?.visibility == View.VISIBLE)
            {
                mCheckBox?.isChecked = !mCheckBox?.isChecked!!
                lrf.childSelected[adapterPosition] = mCheckBox!!.isChecked
                lrf.callbackCheckBox(adapterPosition)
            }
            else onclick()
        }

        if(mCheckBox!!.isEnabled)
        {
            itemView.setOnLongClickListener {
                mCheckBox?.isChecked = true
                lrf.childSelected[adapterPosition] = true
                lrf.callbackCheckBox(adapterPosition)
                return@setOnLongClickListener true
            }
            mCheckBox!!.setOnClickListener {
                lrf.childSelected[adapterPosition] = mCheckBox!!.isChecked
                lrf.callbackCheckBox(adapterPosition)
            }
        }
    }
}