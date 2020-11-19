package com.example.musictest.fragments

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.example.musictest.R
import com.example.musictest.activities.MainActivity
import com.example.musictest.activities.musicController
import java.io.File

enum class ItemMode {
    None, ItemMusicId, ItemFiles, ItemPlaylist
}

class ItemMusicFragment : Fragment() {

    private var first: Boolean? = null
    private var id: Int? = null

    private var clickCallback: () -> Unit = {}
    private var selectCallback: () -> Unit = {}
    private var longClickCallback: () -> Unit = {}

    private var file: File? = null

    lateinit var name: TextView
    lateinit var desc: TextView
    lateinit var img: ImageView
    lateinit var ckeckbox: CheckBox
    lateinit var ItemMainLayout: LinearLayout

    var mode: ItemMode = ItemMode.None

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment

        // (ItemMainLayout.parent as LinearLayout).orientation // do depend on orientation

        var v = inflater.inflate(R.layout.fragment_item_music, container, false)

        img = v.findViewById(R.id.imageView_item)
        name = v.findViewById(R.id.textView_Name)
        desc = v.findViewById(R.id.textView_desc)
        ckeckbox = v.findViewById(R.id.itemCheckBox)
        ItemMainLayout = v.findViewById(R.id.ItemMainLayout)

        ckeckbox.visibility = View.GONE

        /*if (first!!) {
            val param = v.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, dpToPx(5F), 0, 0)
            v.layoutParams = param
        }*/

        if (mode == ItemMode.ItemPlaylist) {

            name.text = musicController.playlist[id!!].name

            desc.text = musicController.playlist[id!!].musics.size.toString() + " Songs"

            if(id == 0)
            {
                img.setImageResource(R.drawable.liked)
            }
            else
            {
                img.setImageResource(R.drawable.playlist)
            }
        }
        else if (mode == ItemMode.ItemMusicId) {
            /*var music = musicController.musics[id!!]

            name.text = music.title
            desc.text = music.artist
            img.setImageResource(R.drawable.music)

            if (music.image != null) img.setImageBitmap(music.imageMini)*/
        } else if (mode == ItemMode.ItemFiles) {

            name.text = file!!.name

            if (file!!.isDirectory) {
                desc.text = "directory"
                img.setImageResource(R.drawable.folder)
            }
            if (file!!.isFile) {
                desc.text = "File"
                img.setImageResource(R.drawable.folder)

                if (MainActivity.isMusicFile(file!!)) {
                    desc.text = "Music"
                    img.setImageResource(R.drawable.music)
                }
            }
        }

        // Setup callbacks

        ckeckbox.setOnCheckedChangeListener { _, _ ->
            selectCallback()
        }

        ItemMainLayout.setOnClickListener {
            clickCallback()
        }

        ItemMainLayout.setOnLongClickListener {
            longClickCallback()
            return@setOnLongClickListener true
        }


        /*ckeckbox.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                ckeckbox.isChecked = !ckeckbox.isChecked
            }
            Toast.makeText(context,"focus",Toast.LENGTH_SHORT).show()
        }

        ItemMainLayout.setOnHoverListener { view: View, motionEvent: MotionEvent ->

            itemCheckBox.isChecked = true

            Toast.makeText(context,"focus",Toast.LENGTH_SHORT).show()
            return@setOnHoverListener true
        }*/


        /*ItemMainLayout.setOnClickListener {

            var empty = true

            var listerLayout = v.parent as LinearLayout

            val childCount: Int = listerLayout.getChildCount()
            for (i in 0 until childCount) {
                val v: View = listerLayout.getChildAt(i)
                if(v.findViewById<CheckBox>(R.id.itemCheckBox).isChecked)
                {
                    empty = false
                    break
                }
            }

            if(empty)
            {
                clickCallback()
            }
            else
            {
                ckeckbox.isChecked = !ckeckbox.isChecked
            }

        }

        ItemMainLayout.setOnLongClickListener {
            itemCheckBox.isChecked = true
            return@setOnLongClickListener true
        }*/

        return v;
    }

    fun dpToPx(dp: Float): Int = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            resources.displayMetrics
    ).toInt()


    fun initMusicId(id: Int, f: Boolean): ItemMusicFragment {

        first = f
        this.id = id
        mode = ItemMode.ItemMusicId

        return this
    }

    fun initFileId(f: File): ItemMusicFragment {

        file = f
        mode = ItemMode.ItemFiles

        return this
    }

    fun initPlaylistId(id: Int, f: Boolean): ItemMusicFragment {

        this.id = id
        first = f
        mode = ItemMode.ItemPlaylist

        return this
    }

    fun addClickCallback(f: () -> Unit): ItemMusicFragment {
        clickCallback = f
        return this
    }

    fun addSelectCallback(f: () -> Unit): ItemMusicFragment {
        selectCallback = f
        return this
    }

    fun addLongClickCallback(f: () -> Unit): ItemMusicFragment {
        longClickCallback = f
        return this
    }

    companion object {

        fun addItem(
                fm: androidx.fragment.app.FragmentManager?,
                layout_id: Int,
        ): ItemMusicFragment {
            val fragOne: Fragment = ItemMusicFragment()
            val tr = fm!!.beginTransaction()
            tr.add(layout_id, fragOne)
            tr.commitAllowingStateLoss()
            tr.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)

            return fragOne as ItemMusicFragment
        }
    }
}

