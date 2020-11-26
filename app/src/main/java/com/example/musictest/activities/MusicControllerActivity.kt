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

        // Position Bar
        positionBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean,
                ) {
                    if (fromUser) currentSeek = progress
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {
                }

                override fun onStopTrackingTouch(p0: SeekBar?) {
                    smc.player.seekTo(currentSeek)
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

        pager.setCurrentItem(smc.currentQueueId, false)

        // Thread
        Thread(Runnable(fun() {
            while (true) {
                try {
                    val msg = Message()
                    msg.what = smc.player.currentPosition
                    handler.sendMessage(msg)
                    Thread.sleep(100)
                } catch (e: InterruptedException) {
                    break
                }
            }
        })).start()

        imageButtonMore.setOnClickListener {

            val music = smc.currentMusic

            if (music.valid) {

                val popup = PopupMenu(applicationContext, imageButtonMore)

                val sm = popup.menu.addSubMenu(0, 1, 1, "Add to playlist")
                val playlists = smc.getList(ListId.ID_MUSIC_USER_PLAYLISTS)
                for (i in 0 until playlists.list.size)
                    sm.add(0, i + 10, i + 10, smc.getList(playlists.list[i]).name)

                popup.menu.add(0, 2, 2, "View Album")
                popup.menu.add(0, 3, 3, "View Artist")
                //popup.menu.add(0, 4, 4, "Info")
                //popup.menu.add(0, 5, 5, "Delete music").isEnabled = false

                popup.setOnMenuItemClickListener { item ->

                    when (item.itemId) {
                        1 -> {
                        }
                        2 -> {
                            val i = Intent(this, MainActivity::class.java)
                            val b = Bundle()
                            b.putBoolean("showList", true)
                            b.putInt("listId", music.albumId!!)
                            i.putExtras(b)
                            i.action = Intent.ACTION_MAIN
                            i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                            startActivity(i)
                        }
                        3 -> {
                            val i = Intent(this, MainActivity::class.java)
                            val b = Bundle()
                            b.putBoolean("showList", true)
                            b.putInt("listId", music.artistId!!)
                            i.putExtras(b)
                            i.action = Intent.ACTION_MAIN
                            i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                            startActivity(i)
                        }
                        /*4 -> {

                            val builder1 = AlertDialog.Builder(applicationContext)
                            builder1.setTitle(music.title)
                            builder1.setMessage("\nName : ${music.title}\n\nAlbum : ${music.album}\n\nArtist : ${music.artist}\n\nPath : ${music.path}")
                            builder1.setCancelable(true)
                            builder1.setIcon(R.drawable.ic_music)
                            builder1.setPositiveButton("Done") { dialog, _ -> dialog.cancel() }
                            val alert11 = builder1.create()
                            alert11.show()
                        }*/
                        in 10..(playlists.list.size + 10) -> {
                            smc.addIdToList(smc.currentMusicId, playlists.list[item.itemId - 10])
                            Toast.makeText(
                                applicationContext,
                                "Adding ${music.title} to " + item.title,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        //else -> {Toast.makeText(lrf.context, "Saving song to ${item.itemId}" + item.title , Toast.LENGTH_SHORT).show()}
                    }

                    return@setOnMenuItemClickListener true
                }
                popup.show();
            }
        }

        // update interface before showing
        updateInterface()
    }

    // Build carousel
    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = smc.getList(ListId.ID_MUSIC_QUEUE).list.size
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
        if (smc.currentMusic.image != null)
            bitmap = smc.currentMusic.image

        visual.setup(
            smc.helper, Scale(
                Compose(
                    FftCLine(),
                    Scale(Icon(Icon.getCircledBitmap(bitmap)), scaleX = 1.2f, scaleY = 1.2f)
                ), scaleX = 1.3f, scaleY = 1.3f
            )
        )

        // playing from
        playingFrom.text = smc.playingFrom
        playingFrom.isSelected = true

        // Update metadata
        if (textViewTitle.text != smc.currentMusic.title) // avoid reset scroll position
            textViewTitle.text = smc.currentMusic.title
        if (textViewArtist.text != smc.currentMusic.artist)
            textViewArtist.text = smc.currentMusic.artist

        val totalTime = smc.player.duration
        positionBar.max = totalTime
        remainingTimeLabel.text = createTimeLabel(totalTime)

        // Update Play button
        if (smc.isMusicPlaying) playBtn.setBackgroundResource(R.drawable.ic_pause)
        else playBtn.setBackgroundResource(R.drawable.ic_play)

        // Update shuffle
        if (smc.shuffleMode) buttonShuffle.setColorFilter(R.color.th)
        else buttonShuffle.colorFilter = null

        // update repeat mode
        when (smc.repeatMode) {
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
        if (smc.shuffleMode) buttonShuffle.setColorFilter(R.color.th)
        else buttonShuffle.colorFilter = null

        // Update favourites

        if (smc.isCurrentMusicLiked()) {
            favBtn.setColorFilter(R.color.th)
            favBtn.setImageResource(R.drawable.ic_favourite)
        } else {
            favBtn.colorFilter = null
            favBtn.setImageResource(R.drawable.ic_addfavourite)
        }

        // update background cover
        if (pager.currentItem != smc.currentQueueId) {
            ignoreFirst = true
            pager.currentItem = smc.currentQueueId
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
        smc.togglePlay()
    }

    fun nextBtnClick(v: View) {
        smc.next()
    }

    fun prevBtnClick(v: View) {
        smc.prev()
    }

    fun shuffleClick(v: View) {
        smc.toggleShuffle()
    }

    fun repeatClick(v: View) {
        smc.toggleRepeat()
    }

    fun backClick(v: View) {
        onBackPressed()
    }

    fun favouriteClick(v: View) {
        smc.toggleCurrentMusicLiked()
    }

    fun playlistClick(v: View) {
        if (smc.currentMusic.valid) {
            val cid = smc.currentMusicId
            val cur = smc.currentMusic

            val popup = PopupMenu(applicationContext, v)
            smc.addPlaylistMenu(popup.menu)
            popup.setOnMenuItemClickListener { item ->
                smc.processPlaylistMenu(v.context, cid, cur, item)
                updateInterface()
                return@setOnMenuItemClickListener true
            }
            popup.show();
        }
    }

    fun showlistClick(v: View) {
        intent = Intent(this, QueueActivity::class.java)
        startActivity(intent)
    }
}