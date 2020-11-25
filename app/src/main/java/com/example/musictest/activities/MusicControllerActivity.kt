package com.example.musictest.activities

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.View
import android.widget.PopupMenu
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.musictest.R
import com.example.musictest.fragments.Image_music
import com.example.musictest.musics.ListId
import com.example.musictest.musics.Repeat
import io.github.jeffshee.visualizer.painters.fft.FftCLine
import io.github.jeffshee.visualizer.painters.misc.Icon
import io.github.jeffshee.visualizer.painters.modifier.Compose
import io.github.jeffshee.visualizer.painters.modifier.Scale
import kotlinx.android.synthetic.main.activity_music_controller.*


class MusicControllerActivity : AppCompatActivity() {

    private var currentSeek: Int = 0

    var ignoreFirst: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_controller)

        visual.fps = false

        registerReceiver(broadcastReceiver, IntentFilter("com.example.musictest.Update_Music"))

        textViewTitle.isSelected = true
        textViewArtist.isSelected = true
        //volumeBar = findViewById(R.id.volumeBar)

        // Position Bar
        positionBar.setOnSeekBarChangeListener(
                object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(
                            seekBar: SeekBar?,
                            progress: Int,
                            fromUser: Boolean,
                    ) {
                        if (fromUser) {
                            currentSeek = progress
                        }
                    }

                    override fun onStartTrackingTouch(p0: SeekBar?) {
                    }

                    override fun onStopTrackingTouch(p0: SeekBar?) {
                        syncMusicController.player.seekTo(currentSeek)
                    }
                }
        )

        val pagerAdapter = ScreenSlidePagerAdapter(this)
        pager.adapter = pagerAdapter

        pager.isUserInputEnabled = false
        /* pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
             override fun onPageSelected(position: Int) {
                 super.onPageSelected(position)
                 if (ignoreFirst) {
                     ignoreFirst = false
                     return
                 }
                 Toast.makeText(applicationContext, "scrolled by user", Toast.LENGTH_SHORT).show()
                 syncMusicController.play(position)
             }
         })*/

        pager.setCurrentItem(syncMusicController.currentQueueId, false)

        // Thread
        Thread(Runnable(fun() {
            while (true) {
                try {
                    val msg = Message()
                    msg.what = syncMusicController.player.currentPosition
                    handler.sendMessage(msg)
                    Thread.sleep(100)
                } catch (e: InterruptedException) {
                    break
                }
            }
        })).start()

        imageButtonMore.setOnClickListener {

            val popup = PopupMenu(applicationContext, imageButtonMore)

            val sm = popup.menu.addSubMenu(0, 1, 1, "Add to playlist")
            sm.add(0, 11, 1, "liked ")
            for (i in 1 until 7)
                sm.add(0, 11 + i, i + 1, "playlist $i")

            popup.menu.add(0, 2, 2, "View Album")
            popup.menu.add(0, 3, 3, "View Artist")
            popup.menu.add(0, 4, 4, "Info")
            popup.menu.add(0, 5, 5, "Delete music").isEnabled = false

            popup.setOnMenuItemClickListener { item ->

                when(item.itemId)
                {
                    1 -> {}
                    2 -> {
                        Toast.makeText(applicationContext, "Go album ", Toast.LENGTH_SHORT).show()}
                    3 -> {
                        Toast.makeText(applicationContext, "Go artist ", Toast.LENGTH_SHORT).show()}
                    4 -> {
                        val builder1 = AlertDialog.Builder(applicationContext)
                        builder1.setTitle(syncMusicController.currentMusic.title)
                        builder1.setMessage("\nName : ${syncMusicController.currentMusic.title}\n\n" +
                                "Album : ${syncMusicController.currentMusic.album}\n\n" +
                                "Artist : ${syncMusicController.currentMusic.artist}\n\n" +
                                "Path : ${syncMusicController.currentMusic.path}")
                        builder1.setCancelable(true)
                        builder1.setIcon(R.drawable.ic_music)
                        builder1.setPositiveButton("Done") { dialog, _ -> dialog.cancel() }
                        val alert11 = builder1.create()
                        alert11.show()

                    }
                    in 11..50 -> {
                        Toast.makeText(applicationContext, "Saving song to " + item.title , Toast.LENGTH_SHORT).show()}
                }

                return@setOnMenuItemClickListener true
            }
            popup.show();
        }

        // update interface before showing
        updateInterface()
    }

    // Build carousel
    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = syncMusicController.getList(ListId.ID_MUSIC_QUEUE).list.size
        override fun createFragment(position: Int): Fragment = Image_music.newInstance(
                position
        )
    }

    private var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateInterface()
        }
    }

    private fun updateInterface() {
        // Update visualizer
        var bitmap = BitmapFactory.decodeResource(resources, R.drawable.music)
        if (syncMusicController.currentMusic.image != null)
            bitmap = syncMusicController.currentMusic.image

        visual.setup(syncMusicController.helper, Scale(Compose(FftCLine(),
                Scale(Icon(Icon.getCircledBitmap(bitmap)), scaleX = 1.2f, scaleY = 1.2f)
        ), scaleX = 1.3f, scaleY = 1.3f)
        )

        // playing from
        playingFrom.text = syncMusicController.playingFrom
        playingFrom.isSelected = true

        // Update metadata
        if (textViewTitle.text != syncMusicController.currentMusic.title) // avoid reset scroll position
            textViewTitle.text = syncMusicController.currentMusic.title
        if (textViewArtist.text != syncMusicController.currentMusic.artist)
            textViewArtist.text = syncMusicController.currentMusic.artist

        val totalTime = syncMusicController.player.duration
        positionBar.max = totalTime
        remainingTimeLabel.text = createTimeLabel(totalTime)

        // Update Play button
        if (syncMusicController.isMusicPlaying) playBtn.setBackgroundResource(R.drawable.ic_pause)
        else playBtn.setBackgroundResource(R.drawable.ic_play)

        // Update shuffle
        if (syncMusicController.shuffleMode) buttonShuffle.setColorFilter(R.color.th)
        else buttonShuffle.colorFilter = null

        // update repeat mode
        when (syncMusicController.repeatMode) {
            Repeat.None -> {
                buttonRepeat.colorFilter = null
                buttonRepeat.setImageResource(R.drawable.ic_repeat)
            }
            Repeat.All -> {
                buttonRepeat.setColorFilter(R.color.th)
                buttonRepeat.setImageResource(R.drawable.ic_repeat)
            }
            Repeat.Once -> {
                buttonRepeat.setColorFilter(R.color.th)
                buttonRepeat.setImageResource(R.drawable.ic_repeat_one)
            }
        }

        // Update shuffle
        if (syncMusicController.shuffleMode) buttonShuffle.setColorFilter(R.color.th)
        else buttonShuffle.colorFilter = null

        // Update favourites

        if (syncMusicController.isCurrentMusicLiked()) {
            favBtn.setColorFilter(R.color.th)
            favBtn.setImageResource(R.drawable.ic_favourite)
        } else {
            favBtn.colorFilter = null
            favBtn.setImageResource(R.drawable.ic_addfavourite)
        }

        // update background cover
        if (pager.currentItem != syncMusicController.currentQueueId) {
            ignoreFirst = true
            pager.currentItem = syncMusicController.currentQueueId
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

        if (min < 0) return "0:00"

        var timeLabel = "$min:"
        if (sec < 10) timeLabel += "0"
        timeLabel += sec

        return timeLabel
    }

    // Button click actions

    fun playBtnClick(v: View) {
        syncMusicController.togglePlay()
    }

    fun nextBtnClick(v: View) {
        syncMusicController.next()
    }

    fun prevBtnClick(v: View) {
        syncMusicController.prev()
    }

    fun shuffleClick(v: View) {
        syncMusicController.toggleShuffle()
    }

    fun repeatClick(v: View) {
        syncMusicController.toggleRepeat()
    }

    fun backClick(v: View) {
        onBackPressed()
    }

    fun favouriteClick(v: View) {
        syncMusicController.toggleCurrentMusicLiked()
    }

    fun playlistClick(v: View) {
        if (syncMusicController.currentMusicId < 0) return
        val ids = ArrayList<Int>()
        ids.add(syncMusicController.currentMusicId)
        syncMusicController.addToPlaylistDialog(this, ids, onSuccess = {
            updateInterface()
        })
    }

    fun showlistClick(v: View) {
        intent = Intent(this, QueueActivity::class.java)
        startActivity(intent)
    }

    override fun onDestroy() {

        super.onDestroy()
    }
}