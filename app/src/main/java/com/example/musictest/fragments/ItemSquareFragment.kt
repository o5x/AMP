package com.example.musictest.fragments

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.musictest.R
import kotlinx.android.synthetic.main.fragment_item_square.*

class ItemSquare : Fragment() {

    private var name: String? = null
    private var description: String? = null
    private var size: Float? = null
    private var imgId: Int? = null
    private var first: Boolean? = null

    var callback: () -> Unit = {}

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        iv_item.layoutParams.height = dpToPx(size!!)
        iv_item.layoutParams.width = dpToPx(size!!)

        tv_name.text = name
        tv_desc.text = description
        iv_item.setImageResource(imgId!!)
        iv_item.setColorFilter(Color.rgb(220, 220, 220), PorterDuff.Mode.MULTIPLY)

        if (first!!) {
            (view.layoutParams as ViewGroup.MarginLayoutParams).leftMargin = dpToPx(10F)
        }

        view.setOnClickListener {
            callback()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_item_square, container, false)
    }

    fun setClickCallback(f: () -> Unit) {
        callback = f
    }

    private fun dpToPx(dp: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()

    companion object {

        fun addItem(
            fm: FragmentManager?,
            layout_id: Int,
            name: String,
            description: String,
            img_id: Int,
            first: Boolean,
            size: Float = 110f
        ): ItemSquare {
            val ft = fm!!.beginTransaction()
            val fragOne = ItemSquare()
            ft.add(layout_id, fragOne)
            ft.commit()

            fragOne.name = name
            fragOne.description = description
            fragOne.size = size
            fragOne.imgId = img_id
            fragOne.first = first

            return fragOne
        }
    }
}