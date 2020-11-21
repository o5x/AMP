package com.example.musictest

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.example.musictest.activities.syncMusicController
import com.example.musictest.builders.BlurBuilder

class Image_music : Fragment() {

    private var param1: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getInt("id")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val v = inflater.inflate(R.layout.fragment_image_music, container, false)
        val imageMusic = v.findViewById<ImageView>(R.id.imageMusic)

        val left = v.findViewById<View>(R.id.leftshadow)
        val right = v.findViewById<View>(R.id.rightshadow)

        /* imageMusic.setImageResource(R.drawable.music)

         if(syncMusicController.getMusicFromQueueId(param1!!).imageInitialized)
         {
             if(syncMusicController.getMusicFromQueueId(param1!!).image != null)imageMusic.setImageBitmap(syncMusicController.getMusicFromQueueId(param1!!).image)
         }
         else
         {
             Thread(Runnable(fun() {
                 if (syncMusicController.getMusicFromQueueId(param1!!).image != null) {
                     (this.context as Activity).runOnUiThread(Runnable {
                         imageMusic.setImageBitmap(syncMusicController.getMusicFromQueueId(param1!!).image)
                         //ImageViewAnimatedChange2(lrf.context,mImageView!!,music.image)
                     })
                 }
             })).start()
         }*/

        val bmp = syncMusicController.getMusicFromQueueId(param1!!).image

        if(bmp != null) {
            val blurredBmp = BlurBuilder.blur(v.context, Bitmap.createBitmap(bmp))
            imageMusic.setImageBitmap(blurredBmp)
            imageMusic.setColorFilter(Color.rgb(123, 123, 123), PorterDuff.Mode.MULTIPLY)
        }
        else {
            imageMusic.visibility = View.INVISIBLE
            left.visibility = View.INVISIBLE
            right.visibility = View.INVISIBLE
        }

        return v
    }

    companion object {

        @JvmStatic
        fun newInstance(param1: Int) =
            Image_music().apply {
                arguments = Bundle().apply {
                    putInt("id", param1)
                }
            }
    }
}