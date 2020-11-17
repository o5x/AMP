package com.example.musictest.activities

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import com.example.musictest.MusicController
import com.example.musictest.R
import com.example.musictest.activities.MusicControllerActivity
import com.example.musictest.builders.CreateNotification
import com.example.musictest.fragments.CollectionFragment
import com.example.musictest.fragments.HomeFragment
import com.example.musictest.fragments.SearchFragment
import com.example.musictest.fragments.SettingsFragment
import com.example.musictest.services.*
import java.io.File

// global MusicController
var musicController = MusicController();


class MainActivity : AppCompatActivity() {

    lateinit var playBtn2 : Button

    lateinit var notificationManager: NotificationManager

    lateinit var imageView_cover : ImageView
    lateinit var textView_title  : TextView
    lateinit var textView_artist : TextView

    lateinit var button_back: ImageButton
    lateinit var button_settings: ImageButton
    lateinit var title: TextView

    lateinit var btn_home: ImageButton
    lateinit var btn_collection: ImageButton
    lateinit var btn_search: ImageButton

    var currentfragment : Fragment? = null

    private fun musicReader(root: File) : ArrayList<File>{
        val fileList: ArrayList<File> = ArrayList()
        val listAllFiles = root.listFiles()
        if (listAllFiles != null && listAllFiles.isNotEmpty()) {
            for (currentFile in listAllFiles) {
                if (currentFile.name.endsWith(".flac") || currentFile.name.endsWith(".mp3")) { // TODO modify filter
                    fileList.add(currentFile.absoluteFile)
                }
            }
            fileList.sortedWith(compareBy{ it.name })
        }
        return fileList;
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

        // Require access to storage
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(
                this,
                arrayOf<String>(Manifest.permission.READ_EXTERNAL_STORAGE),
                0
            );
        }

        // Require access to record
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(
                this,
                arrayOf<String>(Manifest.permission.RECORD_AUDIO),
                0
            );
        }

        // init context
        musicController.init(this);

        // check intent filter
        var fileList: ArrayList<File> = ArrayList()
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
            val gpath: String = Environment.getExternalStorageDirectory().absolutePath
            val spath = "MusicTest"
            val fullpath = File(gpath + File.separator + spath)
            Log.w("fileread", "" + fullpath)
            fileList = musicReader(fullpath)

            musicController.setMusics(fileList);
       // }

        title = findViewById(R.id.Apptitle)
        button_back = findViewById(R.id.imageButtonBack)
        button_settings= findViewById(R.id.imageButtonSettings)
        imageView_cover = findViewById(R.id.imageView_cover)
        textView_title = findViewById(R.id.listerTitle)
        textView_artist = findViewById(R.id.textView_artist)
        playBtn2 = findViewById(R.id.playBtn2)

        btn_home = findViewById(R.id.imageButtonHome)
        btn_search = findViewById(R.id.imageButtonSearch)
        btn_collection = findViewById(R.id.imageButtonCollection)

        // Notification controller
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
            registerReceiver(broadcastReceiver, IntentFilter("com.example.musictest.Control_Music"))
            startService(Intent(baseContext, OnClearFromRecentService::class.java))
        }

        registerReceiver(broadcastReceiver2, IntentFilter("com.example.musictest.Update_Music"))

        updateInterface()

        // set default fragment
        HomeClick(findViewById<View>(android.R.id.content).getRootView())
        //SearchClick(findViewById<View>(android.R.id.content).getRootView())
    }

    fun updateInterface()
    {
        if (musicController.player.isPlaying)
            playBtn2.setBackgroundResource(R.drawable.ic_pause)
        else
            playBtn2.setBackgroundResource(R.drawable.ic_play)

        if (musicController.getCurrentMusic().image == null)
            imageView_cover.setImageResource(R.drawable.music)
        else
            imageView_cover.setImageBitmap(musicController.getCurrentMusic().image)

        textView_title.text = musicController.getCurrentMusic().title
        textView_artist.text = musicController.getCurrentMusic().artist

        if(musicController.nothingPlaying)
        {
            CreateNotification.cancelNotification(this)
        }
        else
        {

            if(musicController.isPlaying())
            {
                CreateNotification.createNotification(
                    this, musicController.musics.get(musicController.currentMusic),
                    R.drawable.ic_pause, musicController.currentMusic, musicController.musics.size - 1
                )
            }
            else
            {
                CreateNotification.createNotification(
                    this, musicController.musics.get(musicController.currentMusic),
                    R.drawable.ic_play, musicController.currentMusic, musicController.musics.size - 1
                )
            }
        }


    }

    var broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {

            val action = intent.extras!!.getString("actionname")
            when (action) {
                CreateNotification.ACTION_PREVIOUS -> musicController.prev()
                CreateNotification.ACTION_PLAY -> musicController.toggle()
                CreateNotification.ACTION_NEXT -> musicController.next()
            }
        }
    }

    var broadcastReceiver2: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateInterface()
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CreateNotification.CHANNEL_ID,
                "Playback", NotificationManager.IMPORTANCE_HIGH
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

    fun replaceFragment(newfragment: Fragment)
    {
        if(currentfragment == null || currentfragment!!.javaClass != newfragment.javaClass)
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
        musicController.toggle()
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