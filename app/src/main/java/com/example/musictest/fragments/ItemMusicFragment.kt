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
import com.example.musictest.activities.musicController

class ItemMusicFragment : Fragment() {

    private var first: Boolean? = null
    private var musicId: Int? = null
    //private var fct: () -> Unit = {}

    lateinit var name : TextView
    lateinit var desc : TextView
    lateinit var img : ImageView
    lateinit var ckeckbox : CheckBox
    lateinit var ItemMainLayout : LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            first = it.getBoolean("first")
            musicId = it.getInt("musicId")
            //fct = it.getSerializable("callback") as () -> Unit
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        var v =  inflater.inflate(R.layout.fragment_item_music, container, false)

        var music = musicController.musics[musicId!!]

        img = v.findViewById(R.id.imageView_item)
        name = v.findViewById(R.id.textView_Name)
        desc = v.findViewById(R.id.textView_desc)
        ckeckbox = v.findViewById(R.id.itemCheckBox)
        ItemMainLayout = v.findViewById(R.id.ItemMainLayout)

        ckeckbox.setOnCheckedChangeListener { buttonView, isChecked ->
            //Toast.makeText(v.context,isChecked.toString(),Toast.LENGTH_SHORT).show()
            //fct()
        }

        name.text = music.title
        desc.text = music.artist
        img.setImageResource(R.drawable.music)

        if(music.image != null)
        {
            img.setImageBitmap(music.image)
        }

        if(first!!)
        {
            val param = v.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(0, dpToPx(5F), 0, 0)
            v.layoutParams = param
        }

        ItemMainLayout.setOnClickListener{
            musicController.changeMusic(musicId!!)
        }

        return v;
    }

    fun dpToPx(dp: Float): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        dp,
        resources.displayMetrics
    ).toInt()

    companion object{

        fun addItem(
            fm: androidx.fragment.app.FragmentManager?,
            layout_id: Int,
            id: Int,
            first: Boolean
        )
        {
            val fragOne: Fragment = ItemMusicFragment()
            val arguments = Bundle()

            arguments.putInt("musicId", id)
            arguments.putBoolean("first", first)
            //arguments.putSerializable("callback", {} as Serializable)

            fragOne.arguments = arguments

            val tr = fm!!.beginTransaction()
            tr.add(layout_id, fragOne)
            tr.commitAllowingStateLoss()
            tr.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
        }

        fun addItem2(
            fm: androidx.fragment.app.FragmentManager?,
            layout_id: Int,
            id: Int,
            first: Boolean,
            fct: () -> Unit
        )
        {
            val fragOne: Fragment = ItemMusicFragment()
            val arguments = Bundle()

            arguments.putInt("musicId", id)
            arguments.putBoolean("first", first)
            //arguments.putSerializable("callback", fct as Serializable)

            fragOne.arguments = arguments

            val tr = fm!!.beginTransaction()
            tr.add(layout_id, fragOne)
            tr.commitAllowingStateLoss()
            tr.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
        }
    }

}