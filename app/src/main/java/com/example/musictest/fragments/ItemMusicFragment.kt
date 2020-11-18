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
    None, ItemMusicId, ItemFiles
}

class ItemMusicFragment : Fragment() {

    private var first: Boolean? = null
    private var musicId: Int? = null
    //private var fct: () -> Unit = {}

    private var clickCallback : () -> Unit = {}
    private var selectCallback : () -> Unit = {}

    private var file : File? = null

    lateinit var name : TextView
    lateinit var desc : TextView
    lateinit var img : ImageView
    lateinit var ckeckbox : CheckBox
    lateinit var ItemMainLayout : LinearLayout

    var mode : ItemMode = ItemMode.None

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment

        // (ItemMainLayout.parent as LinearLayout).orientation // do depend on orientation

        var v =  inflater.inflate(R.layout.fragment_item_music, container, false)

        img = v.findViewById(R.id.imageView_item)
        name = v.findViewById(R.id.textView_Name)
        desc = v.findViewById(R.id.textView_desc)
        ckeckbox = v.findViewById(R.id.itemCheckBox)
        ItemMainLayout = v.findViewById(R.id.ItemMainLayout)

        /*if (first!!) {
            val param = v.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, dpToPx(5F), 0, 0)
            v.layoutParams = param
        }*/

        if(mode == ItemMode.ItemMusicId) {
            var music = musicController.musics[musicId!!]

            name.text = music.title
            desc.text = music.artist
            img.setImageResource(R.drawable.music)

            if (music.image != null) img.setImageBitmap(music.image)
        }
        else  if(mode == ItemMode.ItemFiles) {

            name.text = file!!.name

            if(file!!.isDirectory)
            {
                desc.text = "directory"
                img.setImageResource(R.drawable.folder)


            }
            if(file!!.isFile)
            {
                desc.text = "File"
                img.setImageResource(R.drawable.folder)

                if(MainActivity.isMusicFile(file!!))
                {
                    desc.text = "Music"
                    img.setImageResource(R.drawable.music)
                }
            }
        }

        // Setup callbacks

        ckeckbox.setOnCheckedChangeListener { buttonView, isChecked ->
            selectCallback()
        }

        ItemMainLayout.setOnClickListener {
            clickCallback()
        }

        return v;
    }

    fun dpToPx(dp: Float): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp,
        resources.displayMetrics
    ).toInt()


    fun initMusicId(id: Int, f: Boolean) : ItemMusicFragment{

        first = f
        musicId = id
        mode = ItemMode.ItemMusicId

        return this
    }

    fun initFileId(f : File) : ItemMusicFragment{

        file = f
        mode = ItemMode.ItemFiles

        return this
    }

    fun addClickCallback(f : () -> Unit) : ItemMusicFragment
    {
       clickCallback = f
        return this
    }

    fun addSelectCallback(f : () -> Unit) : ItemMusicFragment
    {
        selectCallback = f
        return this
    }

    companion object{

        fun addItem(
                fm: androidx.fragment.app.FragmentManager?,
                layout_id: Int
        ) : ItemMusicFragment
        {
            val fragOne: Fragment = ItemMusicFragment()
            val tr = fm!!.beginTransaction()
            tr.add(layout_id, fragOne)
            tr.commitAllowingStateLoss()
            tr.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)

            return fragOne as ItemMusicFragment
        }
    }

}