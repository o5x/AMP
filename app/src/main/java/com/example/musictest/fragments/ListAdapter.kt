package com.example.musictest.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.musictest.R
import com.example.musictest.activities.MainActivity
import com.example.musictest.activities.syncMusicController


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
/*
fun ImageViewAnimatedChange(c: Context?, v: ImageView, new_image: Bitmap?) {
    val anim_out: Animation = AnimationUtils.loadAnimation(c, R.anim.fade_out)
    val anim_in: Animation = AnimationUtils.loadAnimation(c, R.anim.fade_in)
    anim_out.setAnimationListener(object : Animation.AnimationListener {
        override fun onAnimationStart(animation: Animation?) {}
        override fun onAnimationRepeat(animation: Animation?) {}
        override fun onAnimationEnd(animation: Animation?) {
            v.setImageBitmap(new_image)
            anim_in.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationRepeat(animation: Animation?) {}
                override fun onAnimationEnd(animation: Animation?) {}
            })
            v.startAnimation(anim_in)
        }
    })
    v.startAnimation(anim_out)
}

fun ImageViewAnimatedChange2(c: Context?, v: ImageView, new_image: Bitmap?) {

    val anim_in: Animation = AnimationUtils.loadAnimation(c, R.anim.fade_in)

    v.setImageBitmap(new_image!!)
    anim_in.setAnimationListener(object : Animation.AnimationListener {
        override fun onAnimationStart(animation: Animation?) {}
        override fun onAnimationRepeat(animation: Animation?) {}
        override fun onAnimationEnd(animation: Animation?) {}
    })
    v.startAnimation(anim_in)

}*/

class MovieViewHolder(inflater: LayoutInflater, parent: ViewGroup) :
    RecyclerView.ViewHolder(inflater.inflate(R.layout.list_item, parent, false)) {
    var mTitleView: TextView? = null
    var mYearView: TextView? = null
    var mImageView: ImageView? = null
    var mImageView2: ImageView? = null
    var mCheckBox: CheckBox? = null

    init {
        mTitleView = itemView.findViewById(R.id.list_title)
        mYearView = itemView.findViewById(R.id.list_description)
        mImageView = itemView.findViewById(R.id.list_image)
        //mImageView2 = itemView.findViewById(R.id.list_image2)
        mCheckBox = itemView.findViewById(R.id.list_checkBox)
    }

    fun bind(visibility: Int, lrf: ListerRecyclerFragment) {

        if(adapterPosition >= lrf.childSelected.size) return

        var onclick = {
            lrf.clickCallback(adapterPosition)
        }

        when(lrf.listerMode) {
            ListerMode.ListMusicId -> {

                val music = syncMusicController.getMusic(lrf.listIds[adapterPosition])

                mTitleView?.text = music.title
                mYearView?.text = music.artist

                //mImageView2?.setImageResource(R.drawable.music)
                //if(music.image != null)mImageView?.setImageBitmap(music.image)
                // else mImageView?.setImageResource(R.drawable.music)

                mImageView?.setImageResource(R.drawable.music)

                /*if(music.imageInitialized)
                {
                    if(music.image != null)mImageView?.setImageBitmap(music.image)
                }
                else
                {
                    Thread(Runnable(fun() {
                        if (music.image != null) {
                            (lrf.context as Activity).runOnUiThread(Runnable {
                                mImageView?.setImageBitmap(music.image)
                                //ImageViewAnimatedChange2(lrf.context,mImageView!!,music.image)
                            })
                        }
                    })).start()
                }*/

                // erase onclick with custom one
                onclick = {
                    syncMusicController.setQueue(lrf.listIds)
                    syncMusicController.play(adapterPosition)
                }
            }
            ListerMode.ListPlaylists -> {

                val list = syncMusicController.getList(lrf.listIds[adapterPosition])
                mTitleView?.text = list.name
                mYearView?.text = "${list.list.size} songs"
                mImageView?.setImageResource(R.drawable.playlist)

                onclick = {
                    lrf.replaceFragment(
                        ListerRecyclerFragment().initMusicIdList(list.list),
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

                    if (MainActivity.isMusicFile(file)) {
                        mCheckBox?.isEnabled = true
                        mYearView?.text = "Music"
                        mImageView?.setImageResource(R.drawable.music)

                        onclick = {
                            syncMusicController.setQueueFiles(lrf.files, adapterPosition)
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