package com.example.musictest.activities

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import com.example.musictest.R
import com.example.musictest.SyncMusicController
import com.example.musictest.builders.CreateNotification
import com.example.musictest.fragments.CollectionFragment
import com.example.musictest.fragments.HomeFragment
import com.example.musictest.fragments.SearchFragment
import com.example.musictest.fragments.SettingsFragment
import com.example.musictest.services.OnClearFromRecentService
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

/*
todo bug fixes :
- Can select folder and files
- visualizer helper crash sometimes
- duplicates in database ?

todo add :
- manage multi selection files
- update interface on lists changed
- finish multimedia control
- shuffle mode
- image lazy load
- image database / localfiles
- notification control advanced / mediasessioncompat
- change search screen to have albums / artist
- add date in local structures
- add date to music (last played) + added + play count + time spent on this music ?
- manage file removed skip
- show recently played (liked albums playlists, artists)
- add queue playing from (search, playlist, liked, all)


 */


// global MusicController
var syncMusicController = SyncMusicController();

class MainActivity : AppCompatActivity() {

    private val CHANNEL_ID = "channel2"
    private val CHANNEL_NID = 2

    private var notificationManager: NotificationManager? = null

    lateinit var mBuilder : NotificationCompat.Builder
    lateinit var mNotifyManager : NotificationManagerCompat

    lateinit var button_back: ImageButton
    lateinit var button_settings: ImageButton
    lateinit var title: TextView

    lateinit var btn_home: ImageButton
    lateinit var btn_collection: ImageButton
    lateinit var btn_search: ImageButton

    var currentfragment : Fragment? = null

    companion object{
        fun isMusicFile(f: File) : Boolean{ // TODO modify filter
            return f.isFile
                    && (f.name.endsWith(".flac")
                    || f.name.endsWith(".mp3")
                    || f.name.endsWith(".wav")
                    || f.name.endsWith(".3gp")
                    || f.name.endsWith(".mp4")
                    || f.name.endsWith(".m4a")
                    || f.name.endsWith(".aac")
                    || f.name.endsWith(".amr")
                    || f.name.endsWith(".ota")
                    || f.name.endsWith(".mid")
                    || f.name.endsWith(".ogg")
                    || f.name.endsWith(".mkv"))
        }
    }

    fun scanMusics()
    {
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

        fun recursiveMusicScan(path: File)
        {
            //Log.w("fileScan", "scanning " + path.path)

            val listAllFiles = path.listFiles()
            if (listAllFiles != null && listAllFiles.isNotEmpty()) {
                for (currentFile in listAllFiles) {
                    if(currentFile.isDirectory)
                    {
                        recursiveMusicScan(currentFile)
                    }
                    if (isMusicFile(currentFile)) {
                        if(syncMusicController.addMusic(currentFile)) scanfilesCount ++
                        totalScanfilesCount ++
                    }
                }
            }
            mBuilder.setContentText("$totalScanfilesCount musics found ($scanfilesCount added)") // Removes the progress bar
                    .setProgress(0, 0, true)

            mNotifyManager.notify(CHANNEL_NID, mBuilder.build())
        }

        Thread {
            Log.w("fileScan", "thread ${Thread.currentThread()} started ")
            val gpath: String = Environment.getExternalStorageDirectory().absolutePath
            val spath2 = "Music"
            val fullpath2 = File(gpath + File.separator + spath2)
            recursiveMusicScan(fullpath2)

            mBuilder.setContentTitle("Music Scan Complete")
                    .setContentText("$totalScanfilesCount musics found ($scanfilesCount added)") // Removes the progress bar
                    .setProgress(0, 0, false)

            mNotifyManager.notify(CHANNEL_NID, mBuilder.build())
            //Thread.sleep(5000)

            mNotifyManager.cancel(CHANNEL_NID)

        }.start()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        val result = audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN
        )
        if (result != AudioManager.AUDIOFOCUS_GAIN) {
            return  //Failed to gain audio focus
        }

        listerTitle.isSelected = true
        textView_artist.isSelected = true

        // Require access to storage
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 0
            );
        }

        // Require access to record
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),0
            );
        }

        // init context
        syncMusicController.init(this)
        scanMusics()

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

        title = Apptitle
        button_back = imageButtonBack
        button_settings= imageButtonSettings

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

    fun updateInterface()
    {
        if (syncMusicController.isMusicPlaying)
            playBtn2.setBackgroundResource(R.drawable.ic_pause)
        else
            playBtn2.setBackgroundResource(R.drawable.ic_play)

        if (syncMusicController.currentMusic.image == null)
            imageView_cover.setImageResource(R.drawable.music)
        else
            imageView_cover.setImageBitmap(syncMusicController.currentMusic.image)

        listerTitle.text = syncMusicController.currentMusic.title
        textView_artist.text = syncMusicController.currentMusic.artist

        if(syncMusicController.isQueuePlaying)
        {
            if(syncMusicController.isMusicPlaying)
            {
                CreateNotification.createNotification(this,
                        syncMusicController.currentMusic, R.drawable.ic_pause)
            }
            else
            {
                CreateNotification.createNotification(this,
                        syncMusicController.currentMusic, R.drawable.ic_play)
            }
        }
        else
            CreateNotification.cancelNotification(this)

        if(syncMusicController.isQueuePlaying)
            linearLayoutControl.visibility = View.VISIBLE
        else
            linearLayoutControl.visibility = View.GONE
    }

    var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            val action = intent.extras!!.getString("actionname")
            when (action) {
                CreateNotification.ACTION_PREVIOUS -> syncMusicController.prev()
                CreateNotification.ACTION_PLAY -> syncMusicController.togglePlay()
                CreateNotification.ACTION_NEXT -> syncMusicController.next()
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

        unregisterReceiver(broadcastReceiver)
    }

    // Interface buttons handlers

    fun replaceFragment(newfragment: Fragment, force: Boolean = false)
    {
        if(force || currentfragment == null || currentfragment!!.javaClass != newfragment.javaClass)
        {
            supportFragmentManager.commit {
                replace(R.id.fragment, newfragment)
                setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                addToBackStack(null)
            }
            currentfragment = newfragment
        }
    }

    fun playBtnClick(v: View)
    {
        syncMusicController.togglePlay()
    }

    fun openMusicController(v: View) {
        val intent = Intent(this, MusicControllerActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivityIfNeeded(intent, 0);
    }

    fun backClick(v: View)
    {
        onBackPressed();
    }

    override fun onStart() {
        super.onStart()
        if(supportFragmentManager.backStackEntryCount == 0)
            HomeClick(findViewById<View>(android.R.id.content).getRootView())
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if(supportFragmentManager.backStackEntryCount == 0)
        {
            moveTaskToBack(true)
        }
    }

    fun HomeClick(v: View)
    {
        replaceFragment(HomeFragment())
    }
    fun SearchClick(v: View)
    {
        replaceFragment(SearchFragment())
    }

    fun ColectionClick(v: View)
    {
        replaceFragment(CollectionFragment())
    }

    fun settingsClick(v: View)
    {
        replaceFragment(SettingsFragment())
    }
}