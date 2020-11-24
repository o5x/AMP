package com.example.musictest.fragments

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.musictest.R
import com.example.musictest.activities.MainActivity
import com.example.musictest.activities.syncMusicController
import com.example.musictest.musics.ListId
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
            ListerMode.ListMusicId -> {

                val music = syncMusicController.getMusic(lrf.listIds[adapterPosition])

                imageButtonMore?.visibility = View.VISIBLE
                imageButtonMore?.setOnClickListener {
                    val builder1 = AlertDialog.Builder(lrf.context)
                    builder1.setTitle(music.title)
                    builder1.setMessage("\nName : ${music.title}\n\nAlbum : ${music.album}\n\nArtist : ${music.artist}\n\nPath : ${music.path}")
                    builder1.setCancelable(true)
                    builder1.setIcon(R.drawable.ic_music)
                    //builder1.setNeutralButton("Add to", { dialog, id -> dialog.cancel()})
                    //builder1.setNeutralButton("Go to Album", { dialog, id -> dialog.cancel()})
                    //builder1.setNegativeButton("Go to Artist", { dialog, id -> dialog.cancel()})
                    //builder1.setPositiveButton("Cancel", { dialog, id -> dialog.cancel()})

                    builder1.setPositiveButton("Done", { dialog, id -> dialog.cancel() })
                    val alert11 = builder1.create()
                    alert11.show()
                }

                mTitleView?.text = music.title
                mYearView?.text = music.artist

                mImageView?.setImageResource(R.drawable.music)
                if (music.image != null) mImageView?.setImageBitmap(music.image)

                // erase onclick with custom one
                onclick = {
                    syncMusicController.setQueue(lrf.listIds, lrf.title, adapterPosition, true)
                }
            }
            ListerMode.ListPlaylists -> {

                val list = syncMusicController.getList(lrf.listIds[adapterPosition])
                mTitleView?.text = list.name
                mYearView?.text = "${list.list.size} songs"

                when(lrf.listIds[adapterPosition])
                {
                    ListId.ID_MUSIC_LIKED -> mImageView?.setImageResource(R.drawable.liked)
                    ListId.ID_MUSIC_QUEUE -> mImageView?.setImageResource(R.drawable.queue)
                    else -> mImageView?.setImageResource(R.drawable.playlist)
                }

                onclick = {
                    lrf.replaceFragment(
                            ListerRecyclerFragment().initMusicIdList(list.list).setTitle(lrf.title + " - " + list.name),
                            true
                    )
                }
            }
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