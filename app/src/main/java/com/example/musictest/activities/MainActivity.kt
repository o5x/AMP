package com.example.musictest.activities

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import com.example.musictest.R
import com.example.musictest.builders.CreateNotification
import com.example.musictest.fragments.*
import com.example.musictest.musics.SyncMusicController
import com.example.musictest.musics.SyncMusicController.Companion.isMusicFile
import com.example.musictest.services.MediaPlaybackService
import com.example.musictest.services.OnClearFromRecentService
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

/*
todo bug fixes :
- nothing

todo add :
- make db updatable ?

todo optional (or almost done) :
- in artists, put albums instead of musics ? or both ?
- make visualizer facilitative ?
- change search screen to have albums / artist s
- image lazy load
- manage multi selection files
- add date to music (last played) + added + play count + time spent on this music ? - music stats table ?
- manage file removed skip ? (is it managed ?)
- playlist show time, songs count, year, artist

todo done (and / or improvable):
- update interface on lists changed (remove liked live with scroll top)
- add date in local structures
- add go to album artist on track
- remove musics / playlists from lists
- improve sort by name, date, songs count

*/

// global MusicController
var smc = SyncMusicController();

class MainActivity : AppCompatActivity() {

    private val CHANNEL_ID = "channel2"
    private val CHANNEL_NID = 2

    private var backToMusicControllerActivity = false

    private var notificationManager: NotificationManager? = null

    lateinit var mBuilder: NotificationCompat.Builder
    lateinit var mNotifyManager: NotificationManagerCompat

    lateinit var btnBack: ImageButton
    lateinit var btnSettings: ImageButton
    lateinit var tvTitle: TextView

    lateinit var btnHome: ImageButton
    lateinit var btnColleceion: ImageButton
    lateinit var btnSearch: ImageButton

    var currentfragment: Fragment? = null

    private fun scanMusics() {

        createNotificationChannel(CHANNEL_ID, "Music Scan")

        // Setup notification progress
        mNotifyManager = NotificationManagerCompat.from(this)
        mBuilder = NotificationCompat.Builder(this, this.CHANNEL_ID)
        mBuilder.setContentTitle("Music Scan")
            .setContentText("Scan starting")
            .setSmallIcon(R.drawable.appicon)
            .setNotificationSilent()

        var scanfilesCount = 0
        var totalScanfilesCount = 0

        fun recursiveMusicScan(path: File) {
            val listAllFiles = path.listFiles()
            if (listAllFiles != null && listAllFiles.isNotEmpty()) {
                for (currentFile in listAllFiles) {
                    if (currentFile.isDirectory) {
                        recursiveMusicScan(currentFile)
                    }
                    if (isMusicFile(currentFile)) {
                        if (smc.addMusic(currentFile)) scanfilesCount++
                        totalScanfilesCount++
                    }
                }
            }
            mBuilder.setContentText("$totalScanfilesCount musics found ($scanfilesCount added)") // Removes the progress bar
                .setProgress(0, 0, true)

            mNotifyManager.notify(CHANNEL_NID, mBuilder.build())
        }

        Thread {
            Log.w("fileScan", "thread ${Thread.currentThread()} started ")
            val storagePath: String = Environment.getExternalStorageDirectory().absolutePath
            recursiveMusicScan(File(storagePath))

            mBuilder.setContentTitle("Music Scan Complete")
                .setContentText("$totalScanfilesCount musics found ($scanfilesCount added)") // Removes the progress bar
                .setProgress(0, 0, false)

            mNotifyManager.notify(CHANNEL_NID, mBuilder.build())
            mNotifyManager.cancel(CHANNEL_NID)

        }.start()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        val b = intent?.extras
        if (b != null && b.getBoolean("showList")) {
            replaceFragment(ListerRecyclerFragment().initSyncListById(b.getInt("listId")), true)
            backToMusicControllerActivity = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        // enable auto text scroll
        tv_path.isSelected = true
        textView_artist.isSelected = true

        // init context
        smc.init(this)
        scanMusics()

        startService(Intent(this@MainActivity, MediaPlaybackService::class.java))

        tvTitle = Apptitle
        btnBack = imageButtonBack
        btnSettings = imageButtonSettings
        btnHome = imageButtonHome
        btnSearch = imageButtonSearch
        btnColleceion = imageButtonCollection

        Thread.currentThread().uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, _ ->
            object : Thread() {
                override fun run() {
                    CreateNotification.cancelNotification(applicationContext)
                }
            }.start()
        }

        // Notification controller
        createNotificationChannel(CreateNotification.CHANNEL_ID, "Playback")
        registerReceiver(broadcastReceiver, IntentFilter("com.example.musictest.Control_Music"))
        startService(Intent(baseContext, OnClearFromRecentService::class.java))

        registerReceiver(broadcastReceiver2, IntentFilter("com.example.musictest.Update_Music"))

        updateInterface()
    }

    fun updateInterface() {
        if (smc.isMusicPlaying)
            playBtn2.setBackgroundResource(R.drawable.ic_pause)
        else
            playBtn2.setBackgroundResource(R.drawable.ic_play)

        if (smc.currentMusic.image == null)
            imageView_cover.setImageResource(R.drawable.music)
        else imageView_cover.setImageBitmap(smc.currentMusic.image)

        // do not break the scroll unless name changed
        if (tv_path.text != smc.currentMusic.title)
            tv_path.text = smc.currentMusic.title

        if (textView_artist.text != smc.currentMusic.artist)
            textView_artist.text = smc.currentMusic.artist

        if (smc.isQueuePlaying && smc.isNotificationShown)
            CreateNotification.createNotification(this)
        else
            CreateNotification.cancelNotification(this)

        if (smc.isQueuePlaying && smc.currentMusic.valid)
            linearLayoutControl.visibility = View.VISIBLE
        else
            linearLayoutControl.visibility = View.GONE
    }

    private var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.extras!!.getString("actionname")) {
                CreateNotification.ACTION_PREVIOUS -> smc.prev()
                CreateNotification.ACTION_PLAY -> smc.togglePlay()
                CreateNotification.ACTION_NEXT -> smc.next()
                CreateNotification.ACTION_STOP -> smc.stop()
                CreateNotification.ACTION_LIKE -> smc.toggleCurrentMusicLiked()
            }
        }
    }

    var broadcastReceiver2: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateInterface()
        }
    }

    private fun createNotificationChannel(channelId: String, channelName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName, NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager = getSystemService(NotificationManager::class.java)
            if (notificationManager != null) {
                notificationManager!!.createNotificationChannel(channel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager!!.cancelAll()
        }

        mNotifyManager.cancel(CHANNEL_NID)
        unregisterReceiver(broadcastReceiver)
    }

    // Interface buttons handlers

    fun replaceFragment(newfragment: Fragment, force: Boolean = false) {
        if (force || currentfragment == null || currentfragment!!.javaClass != newfragment.javaClass) {
            supportFragmentManager.commit {
                replace(R.id.fragment, newfragment)
                setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                addToBackStack(null)
            }
            currentfragment = newfragment

        }
    }

    fun playBtnClick(v: View) {
        smc.togglePlay()
    }

    fun openMusicController(v: View) {
        val intent = Intent(this, MusicControllerActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT;
        startActivityIfNeeded(intent, 0);
    }

    fun backClick(v: View) {
        onBackPressed();
    }

    override fun onResume() {
        super.onResume()

        if (currentfragment is ListerRecyclerFragment)
            (currentfragment as ListerRecyclerFragment).reload()
    }

    override fun onStart() {
        super.onStart()
        if (supportFragmentManager.backStackEntryCount == 0)
            homeClick(findViewById<View>(android.R.id.content).rootView)
    }

    private fun getPreviousVisibleFragment(): Fragment? {
        val fragmentManager: FragmentManager = this@MainActivity.supportFragmentManager
        val fragments: List<Fragment> = fragmentManager.fragments
        var prevFrag: Fragment? = null
        for (fragment in fragments) {
            if (fragment.isVisible) return prevFrag
            prevFrag = fragment
        }
        return null
    }

    override fun onBackPressed() {
        if (backToMusicControllerActivity) {
            openMusicController(View(applicationContext))
            backToMusicControllerActivity = false
            Handler().postDelayed({
                super.onBackPressed()
            }, 100)
        } else {
            super.onBackPressed()
            if (supportFragmentManager.backStackEntryCount == 0) {
                moveTaskToBack(true)
            }
            currentfragment = getPreviousVisibleFragment()
        }
    }

    fun homeClick(v: View) {
        replaceFragment(HomeFragment())
    }

    fun searchClick(v: View) {
        replaceFragment(SearchFragment())
    }

    fun collectionClick(v: View) {
        replaceFragment(CollectionFragment())
    }

    fun settingsClick(v: View) {
        replaceFragment(SettingsFragment())
    }
}