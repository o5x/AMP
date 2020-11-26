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
- is this an empty bug flx list ?

todo add :
- manage multi selection files
- update interface on lists changed
- add date to music (last played) + added + play count + time spent on this music ? - musicstats table ?
- show recently played (liked albums playlists, artists)
- improve sort by name, date, songs count

todo optional :
- make visualizer facultative ?
- add go to album artist on track
- change search screen to have albums / artist s
- image lazy load
- add date in local structures
- manage file removed skip ? (is it managed ?)
*/

// global MusicController
var syncMusicController = SyncMusicController();

class MainActivity : AppCompatActivity() {

    private val CHANNEL_ID = "channel2"
    private val CHANNEL_NID = 2

    private var backToMusicControllerActivity = false

    private var notificationManager: NotificationManager? = null

    lateinit var mBuilder: NotificationCompat.Builder
    lateinit var mNotifyManager: NotificationManagerCompat

    lateinit var btn_back: ImageButton
    lateinit var btn_settings: ImageButton
    lateinit var tv_title: TextView

    lateinit var btn_home: ImageButton
    lateinit var btn_collection: ImageButton
    lateinit var btn_search: ImageButton

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
            //Log.w("fileScan", "scanning " + path.path)

            val listAllFiles = path.listFiles()
            if (listAllFiles != null && listAllFiles.isNotEmpty()) {
                for (currentFile in listAllFiles) {
                    if (currentFile.isDirectory) {
                        recursiveMusicScan(currentFile)
                    }
                    if (isMusicFile(currentFile)) {
                        if (syncMusicController.addMusic(currentFile)) scanfilesCount++
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
        if (b != null)
        {
            if(b.getBoolean("showList"))
            {
                replaceFragment(
                        ListerRecyclerFragment().initSyncListById(b.getInt("listId")),
                        true)
                backToMusicControllerActivity = true
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        // enable auto text scroll
        listerTitle.isSelected = true
        textView_artist.isSelected = true

        // init context
        syncMusicController.init(this)
        scanMusics()

        startService(Intent(this@MainActivity, MediaPlaybackService::class.java))

        // check intent filter
        /*if (intent.action!!.compareTo(Intent.ACTION_VIEW) == 0) {

            if (intent.scheme!!.compareTo(ContentResolver.SCHEME_CONTENT) == 0) {
                val uri: Uri? = intent.data
                Log.w("loader", "content : " + uri!!.toString())
            }
            else if (intent.scheme!!.compareTo(ContentResolver.SCHEME_FILE) == 0)
            {
                val uri: Uri? = intent.data
                //val file:File = File(uri!!.lastPathSegment)
                fileList = ArrayList()
                Log.w("loader", "file" + uri!!.toString())
                //fileList.add(file)

                musicController.changeMusic(uri);
            }
            else
            {
                Log.w("loader", "nope")
            }
        }
        else
        {*/

        tv_title = Apptitle
        btn_back = imageButtonBack
        btn_settings = imageButtonSettings

        btn_home = imageButtonHome
        btn_search = imageButtonSearch
        btn_collection = imageButtonCollection

        // Notification controller
        createNotificationChannel(CreateNotification.CHANNEL_ID, "Playback")
        registerReceiver(broadcastReceiver, IntentFilter("com.example.musictest.Control_Music"))
        startService(Intent(baseContext, OnClearFromRecentService::class.java))

        registerReceiver(broadcastReceiver2, IntentFilter("com.example.musictest.Update_Music"))

        updateInterface()
    }

    fun updateInterface() {
        if (syncMusicController.isMusicPlaying)
            playBtn2.setBackgroundResource(R.drawable.ic_pause)
        else
            playBtn2.setBackgroundResource(R.drawable.ic_play)

        if (syncMusicController.currentMusic.image == null)
            imageView_cover.setImageResource(R.drawable.music)
        else
            imageView_cover.setImageBitmap(syncMusicController.currentMusic.image)

        // do not break the scroll unless name changed
        if(listerTitle.text != syncMusicController.currentMusic.title)
            listerTitle.text = syncMusicController.currentMusic.title

        if(textView_artist.text != syncMusicController.currentMusic.artist)
            textView_artist.text = syncMusicController.currentMusic.artist

        if (syncMusicController.isQueuePlaying && syncMusicController.isNotificationShown)
            CreateNotification.createNotification(this)
        else
            CreateNotification.cancelNotification(this)

        if (syncMusicController.isQueuePlaying && syncMusicController.currentMusic.valid)
            linearLayoutControl.visibility = View.VISIBLE
        else
            linearLayoutControl.visibility = View.GONE
    }

    private var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.extras!!.getString("actionname")) {
                CreateNotification.ACTION_PREVIOUS -> syncMusicController.prev()
                CreateNotification.ACTION_PLAY -> syncMusicController.togglePlay()
                CreateNotification.ACTION_NEXT -> syncMusicController.next()
                CreateNotification.ACTION_STOP -> syncMusicController.stop()
                CreateNotification.ACTION_LIKE -> syncMusicController.toggleCurrentMusicLiked()
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
        syncMusicController.togglePlay()
    }

    fun openMusicController(v: View) {
        val intent = Intent(this, MusicControllerActivity::class.java)
        //intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivityIfNeeded(intent, 0);
    }

    fun backClick(v: View) {
        onBackPressed();
    }

    override fun onResume() {
        super.onResume()

        if(currentfragment is ListerRecyclerFragment)
        {
            (currentfragment as ListerRecyclerFragment).reload()
        }
    }

    override fun onStart() {
        super.onStart()
        if (supportFragmentManager.backStackEntryCount == 0)
            HomeClick(findViewById<View>(android.R.id.content).rootView)
    }

    fun getPrevVisibleFragment(): Fragment? {
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

        if(backToMusicControllerActivity)
        {
            openMusicController(View(applicationContext))
            backToMusicControllerActivity = false
            Handler().postDelayed({
                super.onBackPressed()
            }, 100)

        }
        else{
            super.onBackPressed()
            if (supportFragmentManager.backStackEntryCount == 0) {
                moveTaskToBack(true)
            }
            currentfragment = getPrevVisibleFragment()
        }
    }

    fun HomeClick(v: View) {
        replaceFragment(HomeFragment())
    }

    fun SearchClick(v: View) {
        replaceFragment(SearchFragment())
    }

    fun ColectionClick(v: View) {
        replaceFragment(CollectionFragment())
    }

    fun settingsClick(v: View) {
        replaceFragment(SettingsFragment())
    }
}