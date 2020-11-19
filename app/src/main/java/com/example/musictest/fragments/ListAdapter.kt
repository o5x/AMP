package com.example.musictest.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.musictest.R
import com.example.musictest.activities.musicController
import kotlinx.android.synthetic.main.fragment_lister_recycler.*

class ListAdapter(private val list: Array<MusicItem>)
    : RecyclerView.Adapter<MovieViewHolder>() {

    private var checkBoxVisibility = View.GONE;

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return MovieViewHolder(inflater, parent)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.bind(list, position , checkBoxVisibility)
    }

    fun selectMode(layoutManager: RecyclerView.LayoutManager, visibility: Int)
    {
        checkBoxVisibility = visibility
        for (i in 0 until layoutManager.childCount)
        {
            var cb = layoutManager.getChildAt(i)!!.findViewById<CheckBox>(R.id.list_checkBox)
            cb.visibility = visibility

            layoutManager.getChildAt(i)!!.findViewById<CheckBox>(R.id.list_checkBox)

            /*(cb as ListAdapter).selectMode(layoutManager!!, View.VISIBLE)
            listerOptions.visibility = View.VISIBLE
            listerButtonPlaylist.isEnabled = true
            listerButtonQueue.isEnabled = true
            listerOptions.animate()
                    .alpha(1f)
                    .setDuration(100)
                    .setListener(null)*/
        }
    }

    override fun getItemCount(): Int = list.size
}

class MovieViewHolder(inflater: LayoutInflater, parent: ViewGroup) :
    RecyclerView.ViewHolder(inflater.inflate(R.layout.list_item, parent, false)) {
    var mTitleView: TextView? = null
    var mYearView: TextView? = null
    var mImageView: ImageView? = null
    var mCheckBox: CheckBox? = null

    init {
        mTitleView = itemView.findViewById(R.id.list_title)
        mYearView = itemView.findViewById(R.id.list_description)
        mImageView = itemView.findViewById(R.id.list_image)
        mCheckBox = itemView.findViewById(R.id.list_checkBox)
    }

    fun bind(list: Array<MusicItem>, position: Int, visibility: Int) {

        val music = musicController.musics[list[position].id]

        mTitleView?.text = music.title
        mYearView?.text = music.artist

        mCheckBox?.visibility = visibility
        mCheckBox?.isChecked = list[position].selected

        if(music.image != null)mImageView?.setImageBitmap(music.image)
        else mImageView?.setImageResource(R.drawable.music)

        itemView.setOnClickListener {
            if(mCheckBox?.visibility == View.VISIBLE)
            {
                mCheckBox?.isChecked = !mCheckBox?.isChecked!!
                list[position].selected = mCheckBox!!.isChecked
                list[position].callbackCheckBox()
            }
            else list[position].clickCallback()
        }

        itemView.setOnLongClickListener {
            mCheckBox?.isChecked = true
            list[position].selected = true
            list[position].callbackCheckBox()
            return@setOnLongClickListener true
        }

        mCheckBox!!.setOnClickListener {
            list[position].selected = mCheckBox!!.isChecked
            list[position].callbackCheckBox()
        }
    }

}