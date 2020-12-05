package com.arrol.amp.fragments

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.arrol.amp.R
import com.arrol.amp.activities.smc
import com.arrol.amp.builders.BlurBuilder
import kotlinx.android.synthetic.main.fragment_image_music.*

class Image_music : Fragment() {

    private var imageId: Int? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bmp = smc.getMusicFromQueueId(imageId!!).image

        if (bmp != null) {
            val blurredBmp = BlurBuilder.blur(view.context, Bitmap.createBitmap(bmp))
            imageMusic.setImageBitmap(blurredBmp)
            imageMusic.setColorFilter(Color.rgb(123, 123, 123), PorterDuff.Mode.MULTIPLY)
        } else {
            imageMusic.visibility = View.INVISIBLE
            leftshadow.visibility = View.INVISIBLE
            rightshadow.visibility = View.INVISIBLE
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_image_music, container, false)
    }

    companion object {

        @JvmStatic
        fun newInstance(param: Int) =
            Image_music().apply {
                imageId = param
            }
    }
}