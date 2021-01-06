package com.arrol.amp.fragments

import android.app.AlertDialog
import android.graphics.Color
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.arrol.amp.R
import com.arrol.amp.activities.smc
import com.arrol.amp.musics.ListContent
import com.arrol.amp.musics.ListId
import com.arrol.amp.musics.ListType
import com.arrol.amp.musics.SyncMusicController.Companion.isMusicFile
import com.arrol.amp.musics.SyncMusicController.Companion.isVideoFile
import kotlinx.android.synthetic.main.fragment_lister_recycler.*


class ListAdapter(private val listerRecyclerFragment: ListerRecyclerFragment) :
    RecyclerView.Adapter<MovieViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return MovieViewHolder(inflater, parent)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.bind(this, listerRecyclerFragment)
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
    var imageButtonLiked: ImageButton? = null

    init {
        mTitleView = itemView.findViewById(R.id.list_title)
        mYearView = itemView.findViewById(R.id.list_description)
        mImageView = itemView.findViewById(R.id.list_image)
        mCheckBox = itemView.findViewById(R.id.list_checkBox)
        imageButtonMore = itemView.findViewById(R.id.imageButtonMore)
        imageButtonLiked = itemView.findViewById(R.id.btn_fav2)
    }

    fun bind(la: ListAdapter, lrf: ListerRecyclerFragment) {

        if (adapterPosition >= lrf.childSelected.size) return
        mCheckBox?.isEnabled = false
        mCheckBox?.visibility = lrf.checkboxVisibility
        mCheckBox?.isChecked = lrf.childSelected[adapterPosition]

        imageButtonMore?.visibility = View.GONE
        imageButtonLiked?.visibility = View.GONE

        var onclick = {
            //lrf.clickCallback(adapterPosition)
        }

        when (lrf.listerMode) {

            ListerMode.ListFiles -> {

                val file = lrf.files[adapterPosition]
                mTitleView?.text = file.name

                val len = Environment.getExternalStorageDirectory().toString().length
                val fileNewPath = file.path.substring(len)

                if (file.isDirectory) {
                    mYearView?.text = "directory"
                    mImageView?.setImageResource(R.drawable.folder)

                    onclick = {
                        lrf.replaceFragment(
                            ListerRecyclerFragment().initFile(fileNewPath),
                            true
                        )
                    }
                }
                if (file.isFile) {
                    mYearView?.text = "File"
                    mImageView?.setImageResource(R.drawable.file)

                    if (isMusicFile(file)) {
                        //mCheckBox?.isEnabled = true
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

                    if(adapterPosition >= lrf.syncList!!.list.size)
                        return

                    val musicId = lrf.syncList!!.list[adapterPosition]
                    val music = smc.getMusic(musicId)

                    imageButtonLiked?.visibility = View.VISIBLE

                    if (smc.isMusicLiked(musicId)) {
                        imageButtonLiked?.setColorFilter(R.color.th)
                        imageButtonLiked?.setImageResource(R.drawable.ic_favourite)
                    } else {
                        imageButtonLiked?.colorFilter = null
                        imageButtonLiked?.setImageResource(R.drawable.ic_addfavourite)
                    }

                    imageButtonLiked?.setOnClickListener {
                        if (!smc.isMusicLiked(musicId)) {
                            smc.toggleMusicLiked(musicId)
                            la.notifyItemChanged(adapterPosition)
                        } else Toast.makeText(lrf.context, "Long press to remove from liked", Toast.LENGTH_SHORT).show()
                    }

                    imageButtonLiked?.setOnLongClickListener {
                        if (smc.isMusicLiked(musicId)) {
                            smc.toggleMusicLiked(musicId)
                            la.notifyItemChanged(adapterPosition)
                            return@setOnLongClickListener true
                        }
                        la.notifyItemChanged(adapterPosition)
                        return@setOnLongClickListener false
                    }

                    if (musicId == smc.currentMusicId && (lrf.syncListId == smc.playingFromId || lrf.syncListId == ListId.ID_MUSIC_QUEUE)) {
                        mTitleView?.setTextColor(Color.parseColor("#FFBB86FC"))
                        //mTitleView?.paintFlags = mTitleView?.paintFlags!! or Paint.UNDERLINE_TEXT_FLAG
                        //mTitleView?.isSelected = true
                        onclick = {
                            smc.togglePlay()
                        }
                    } else {
                        mTitleView?.setTextColor(Color.WHITE)
                        //mTitleView?.paintFlags = mTitleView?.paintFlags!! and Paint.UNDERLINE_TEXT_FLAG.inv()
                        //mTitleView?.isSelected = false
                        onclick = {
                            smc.setQueue(lrf.syncList!!.list, lrf.syncListId, adapterPosition, true)
                        }
                    }

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
                        popup.menu.add(0, 5, 5, "Remove from playlist")
                            .isEnabled = !lrf.syncList!!.readonly

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
                                5 -> {
                                    //smc.removeIdFromList(musicId, lrf.syncListId!!)
                                    //lrf.apply()
                                    lrf.removeCallback(adapterPosition);
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
                } else if (lrf.syncList!!.listContent == ListContent.ListOfLists) {

                    val sublistId = lrf.syncList!!.list[adapterPosition]
                    val sublist = smc.getList(sublistId)

                    if (sublistId == smc.playingFromId) {
                        mTitleView?.setTextColor(Color.parseColor("#FFBB86FC"))
                    } else {
                        mTitleView?.setTextColor(Color.WHITE)
                    }

                    if (sublist.listType in arrayOf(ListType.Artist, ListType.Album)) {
                        imageButtonLiked?.visibility = View.VISIBLE

                        if (smc.isListLiked(sublistId)) {
                            imageButtonLiked?.setColorFilter(R.color.th)
                            imageButtonLiked?.setImageResource(R.drawable.ic_favourite)
                        } else {
                            imageButtonLiked?.colorFilter = null
                            imageButtonLiked?.setImageResource(R.drawable.ic_addfavourite)
                        }
                        imageButtonLiked?.setOnClickListener {
                            smc.toggleListLiked(sublistId)
                            la.notifyItemChanged(adapterPosition)
                        }
                    }

                    mTitleView?.text = sublist.name

                    if (sublist.listContent == ListContent.ListOfMusics) mYearView?.text =
                        if (sublist.author_id != null && sublist.listType == ListType.Album) sublist.author
                        else sublist.list.size.toString() + " Song" + if (sublist.list.size == 1) "" else "s"
                    else mYearView?.text =
                        sublist.list.size.toString() + " List" + if (sublist.list.size == 1) "" else "s"

                    imageButtonMore?.visibility = View.VISIBLE

                    if (sublist.image != null) mImageView?.setImageBitmap(sublist.image)

                    imageButtonMore?.setOnClickListener {
                        val popup = PopupMenu(lrf.context, imageButtonMore)

                        popup.menu.add(0, 1, 1, "View")
                        popup.menu.add(0, 2, 2, "Play")
                        popup.menu.add(0, 3, 3, "Delete playlist").isEnabled = sublist.deletable
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
                                    smc.setQueue(list.list, lrf.syncList!!.list[adapterPosition], 0, true)
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
            else -> {
            }
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