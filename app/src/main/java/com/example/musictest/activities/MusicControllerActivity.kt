package com.example.musictest.activities

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.musictest.Image_music
import com.example.musictest.R
import com.example.musictest.Repeat
import com.example.musictest.activities.musicController
import io.github.jeffshee.visualizer.painters.fft.FftCLine
import io.github.jeffshee.visualizer.painters.misc.Icon
import io.github.jeffshee.visualizer.painters.modifier.*
import io.github.jeffshee.visualizer.utils.VisualizerHelper
import io.github.jeffshee.visualizer.views.VisualizerView


class MusicControllerActivity : AppCompatActivity() {

    private lateinit var helper: VisualizerHelper

    private var currentSeek: Int = 0

    //var ignoreFirst : Boolean = true

    private lateinit var positionBar: SeekBar
    private lateinit var elapsedTimeLabel: TextView
    private lateinit var remainingTimeLabel: TextView
    private lateinit var playBtn: Button
    private lateinit var shuffle: ImageButton
    private lateinit var repeat: ImageButton
    private lateinit var favourite: ImageButton
    private lateinit var mPager: ViewPager2
    private lateinit var textViewTitle: TextView
    private lateinit var textViewArtist: TextView

    private lateinit var visual: VisualizerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_controller)

        helper = VisualizerHelper(musicController.player.audioSessionId)

        visual = findViewById(R.id.visual)
        visual.fps = false

        registerReceiver(broadcastReceiver, IntentFilter("com.example.musictest.Update_Music"))

        favourite = findViewById(R.id.favBtn)

        textViewTitle = findViewById(R.id.textViewTitle)
        textViewTitle.isSelected = true

        textViewArtist = findViewById(R.id.textViewArtist)
        textViewArtist.isSelected = true

        shuffle = findViewById(R.id.buttonShuffle)
        repeat = findViewById(R.id.buttonRepeat)

        //volumeBar = findViewById(R.id.volumeBar)
        positionBar = findViewById(R.id.positionBar)
        elapsedTimeLabel = findViewById(R.id.elapsedTimeLabel)
        remainingTimeLabel = findViewById(R.id.remainingTimeLabel)
        playBtn = findViewById(R.id.playBtn)


        // Position Bar

        positionBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    if (fromUser) {
                        currentSeek = progress
                    }
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {
                }

                override fun onStopTrackingTouch(p0: SeekBar?) {
                    musicController.player.seekTo(currentSeek)
                }
            }
        )

        mPager = findViewById(R.id.pager)
        val pagerAdapter = ScreenSlidePagerAdapter(this)
        mPager.adapter = pagerAdapter

        mPager.isUserInputEnabled = false
        /*mPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (ignoreFirst) {
                    return
                }
                //Toast.makeText(applicationContext, "Scrolled", Toast.LENGTH_SHORT).show()
                //ignoreFirst = true
                //musicController.changeMusic(position)
            }
            /*
            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)

                //ignoreFirst = mPager.isFakeDragging()

            }*/
        })*/

        mPager.setCurrentItem(musicController.currentMusic, false)

        // Thread
        Thread(Runnable(fun() {
            while (true) {
                try {
                    val msg = Message()
                    msg.what = musicController.player.currentPosition
                    handler.sendMessage(msg)
                    Thread.sleep(100)
                } catch (e: InterruptedException) {
                    break
                }
            }
        })).start()

        // update interface before showing
        updateInterface()
    }

    // Build carousel
    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = musicController.musics.size
        override fun createFragment(position: Int): Fragment = Image_music.newInstance(
            position
        )
    }

    private var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateInterface()
        }
    }

    private fun updateInterface()
    {
        // Update visualizer
        var bitmap = BitmapFactory.decodeResource(resources, R.drawable.music)
        if(musicController.getCurrentMusic().image != null)
            bitmap = musicController.getCurrentMusic().image

        visual.setup(helper, Scale(Compose(FftCLine(),
            Scale(Icon(Icon.getCircledBitmap(bitmap)), scaleX = 1.2f, scaleY = 1.2f)
        ), scaleX = 1.3f, scaleY = 1.3f)
        )

        // Update metadata
        textViewTitle.text = musicController.getCurrentMusic().title
        textViewArtist.text = musicController.getCurrentMusic().artist
        val totalTime = musicController.player.duration
        positionBar.max = totalTime
        val totalTimeTime = createTimeLabel(totalTime)
        remainingTimeLabel.text = "$totalTimeTime"

        // Update Play button
        if (musicController.player.isPlaying) playBtn.setBackgroundResource(R.drawable.ic_pause)
        else playBtn.setBackgroundResource(R.drawable.ic_play)

        // Update shuffle
        if(musicController.getShuffle())  shuffle.setColorFilter(R.color.th)
        else shuffle.colorFilter = null

        // update repeat mode
        when (musicController.repeat) {
            Repeat.None -> {
                repeat.colorFilter = null
                repeat.setImageResource(R.drawable.ic_repeat)
            }
            Repeat.All -> {
                repeat.setColorFilter(R.color.th)
                repeat.setImageResource(R.drawable.ic_repeat)
            }
            Repeat.Once -> {
                repeat.setColorFilter(R.color.th)
                repeat.setImageResource(R.drawable.ic_repeat_one)
            }
        }

        // Update shuffle
        if(musicController.getShuffle())shuffle.setColorFilter(R.color.th)
        else shuffle.colorFilter = null

        // Update favourites
        if(musicController.getCurrentMusic().isFavourite)
        {
            favourite.setColorFilter(R.color.th)
            favourite.setImageResource(R.drawable.ic_favourite)
        }
        else
        {
            favourite.colorFilter = null
            favourite.setImageResource(R.drawable.ic_addfavourite)
        }

        // update background cover
        if(mPager.currentItem != musicController.currentMusic)
        {
            mPager.currentItem = musicController.currentMusic
        }
    }

    @SuppressLint("HandlerLeak")
    private var handler = object : Handler() {

        override fun handleMessage(msg: Message) {
            val currentPosition = msg.what

            positionBar.progress = currentPosition

            val elapsedTime = createTimeLabel(currentPosition)
            elapsedTimeLabel.text = elapsedTime
        }
    }

    private fun createTimeLabel(time: Int): String {

        val min = time / 1000 / 60
        val sec = time / 1000 % 60

        if(min < 0) return "0:00"

        var timeLabel = "$min:"
        if (sec < 10) timeLabel += "0"
        timeLabel += sec

        return timeLabel
    }

    // Button click actions

    fun playBtnClick(v: View) {
        musicController.toggle()
    }

    fun nextBtnClick(v: View) {
        musicController.next()
    }

    fun prevBtnClick(v: View) {
        musicController.prev()
    }

    fun shuffleClick(v: View)
    {
        musicController.shuffleToggle()
    }

    fun repeatClick(v: View)
    {
        musicController.repeatToggle()
    }

    fun backClick(v: View)
    {
        onBackPressed()
    }

    fun favouriteClick(v: View)
    {
        musicController.toggleFavourite()
    }

    fun playlistClick(v: View)
    {
        Toast.makeText(getApplicationContext(), "add Playlist to implement", Toast.LENGTH_SHORT).show()
    }

    fun showlistClick(v: View)
    {
        intent = Intent(this, QueueActivity::class.java)
        startActivity(intent)
    }

    override fun onDestroy() {
        helper.release()
        super.onDestroy()
    }
}