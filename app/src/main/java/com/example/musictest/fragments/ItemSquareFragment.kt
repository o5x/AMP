package com.example.musictest.fragments

import android.os.Bundle
import android.util.TypedValue
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import com.example.musictest.R

class ItemSquare : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var param3: ByteArray? = null
    private var param4: Int? = null
    private var param5: Boolean? = null

    lateinit var name : TextView
    lateinit var desc : TextView
    lateinit var img : ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString("param1")
            param2 = it.getString("param2")
            param3 = it.getByteArray("param3")
            param4 = it.getInt("param4")
            param5 = it.getBoolean("param5")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        var v =  inflater.inflate(R.layout.fragment_item_square, container, false)

        img = v.findViewById(R.id.imageView_item)
        name = v.findViewById(R.id.textView_Name)
        desc = v.findViewById(R.id.textView_desc)

        name.text = param1
        desc.text = param2
        img.setImageResource(param4!!)

        if(param5!!)
        {
            val param = v.layoutParams as ViewGroup.MarginLayoutParams
            param.setMargins(dpToPx(10F),0,0,0)
            v.layoutParams = param
        }

        return v;
    }

    fun dpToPx(dp: Float): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()


    companion object{

        fun addItem(fm : FragmentManager?, layout_id:Int, name:String, description: String, img_id : Int, first: Boolean)
        {
            val ft = fm!!.beginTransaction()
            val fragOne: Fragment = ItemSquare()
            val arguments = Bundle()
            arguments.putString("param1", name)
            arguments.putString("param2", description)
            //arguments.putByteArray()"param3", item)
            arguments.putInt("param4", img_id)
            arguments.putBoolean("param5", first)
            fragOne.arguments = arguments
            ft.add(layout_id, fragOne)
            ft.commit()
        }

    }
}