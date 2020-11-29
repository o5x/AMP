package com.example.musictest.musics

import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.*
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.InputType
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.example.musictest.R
import com.example.musictest.activities.smc
import io.github.jeffshee.visualizer.utils.VisualizerHelper
import java.io.File

enum class Repeat {
    None, All, Once
}

private val callback = object : MediaSessionCompat.Callback() {
    override fun onPlay() {
        super.onPlay()
        smc.play()
    }

    override fun onPause() {
        super.onPause()
        smc.pause()
    }

    override fun onSkipToNext() {
        super.onSkipToNext()
        smc.next()
    }

    override fun onStop() {
        super.onStop()
        smc.stop()
    }

    override fun onSkipToPrevious() {
        super.onSkipToPrevious()
        smc.prev()
    }

    override fun onSeekTo(pos: Long) {
        super.onSeekTo(pos)
        smc.player.seekTo(pos.toInt())
        smc.updateSession()
    }
}

class SyncMusicController : Application() {

    lateinit var helper: VisualizerHelper

    private lateinit var c: Context

    private var audioManager: AudioManager? = null

    private lateinit var db: MusicDB

    private lateinit var sharedPref: SharedPreferences

    lateinit var musics: HashMap<Int, SyncMusic>

    private var currentCoverImage: Bitmap? = null

    var images: HashMap<Int, Bitmap> = HashMap()

    private var musicsListValid = false

    private lateinit var lists: HashMap<Int, SyncList>

    var player: MediaPlayer = MediaPlayer()
        private set

    var currentQueueId: Int = -1
        private set

    var currentMusicId: Int = -1
        private set

    val currentMusic: SyncMusic
        get() = if (musics.isNotEmpty() && currentMusicId >= 0 && musics[currentMusicId] != null) musics[currentMusicId]!! else SyncMusic()

    var isQueuePlaying: Boolean = false
        private set

    var isNotificationShown: Boolean = false
        private set

    var isMusicPlaying: Boolean = false
        get() = player.isPlaying
        private set

    var shuffleMode: Boolean = false
        private set

    var repeatMode: Repeat = Repeat.None
        private set

    var playingFrom: String = "Phone"
        set(value) {
            sharedPref.edit().putString("playingFrom", value).apply()
            field = value
        }

    var playingFromId : Int = -1
        set(value) {
            sharedPref.edit().putInt("playingFromId", value).apply()
            field = value
        }
    lateinit var mediaSessionCompat: MediaSessionCompat

    ///////////////////////////////////////// Musics operations

    fun invalidateMusics() {
        musicsListValid = false
    }

    fun getMusic(id: Int): SyncMusic {
        if (!musicsListValid) {
            musics = db.getAllMusicMaps()
            musicsListValid = true
        }
        return if (musics[id] == null) SyncMusic() else musics[id]!!
    }

    ///////////////////////////////////////// List operations

    fun getList(id: Int): SyncList {
        if (lists[id] != null && lists[id]?.valid == true) return lists[id]!!
        val newList = db.getListFromId(id)
        return if (newList == null || !newList.valid) SyncList()
        else {
            lists[id] = newList
            lists[id]!!
        }
    }

    fun invalidateList(id: Int) {
        lists[id]?.valid = false
    }

    private fun updateList(id: Int, newList: ArrayList<Int>) {
        db.setListData(id, newList)
    }

    private fun clearList(id: Int) {
        db.clearListId(id)
    }

    fun addIdToList(music_id: Int, list_id: Int) {
        db.addIdToListId(music_id, list_id)
    }

    private fun removeIdFromList(music_id: Int, list_id: Int) {
        db.removeIdFromListId(music_id, list_id)
    }

    private fun createPlaylist(name: String): Int {
        val id = db.addList(name, ListType.Playlist, ListContent.ListOfMusics, ImageId.ID_IMAGE_PLAYLIST,
                readonly = false, deletable = true, sortMode = SortMode.Date, sortLocked = false, authorId = ListId
                .ID_MUSIC_USER_PLAYLISTS)
        db.addIdToListId(id, ListId.ID_MUSIC_USER_PLAYLISTS)
        db.setListImage(id, BitmapFactory.decodeResource(c.resources, R.drawable.playlist))
        return id
    }


    //////////////

    companion object {
        fun isMusicFile(f: File): Boolean { // TODO modify filter
            return f.isFile
                    && (f.name.endsWith(".flac")
                    || f.name.endsWith(".mp3")
                    || f.name.endsWith(".wav")
                    || f.name.endsWith(".3gp")
                    || f.name.endsWith(".m4a")
                    || f.name.endsWith(".aac")
                    || f.name.endsWith(".amr")
                    || f.name.endsWith(".ota")
                    || f.name.endsWith(".mid")
                    || f.name.endsWith(".ogg")
                    || f.name.endsWith(".mkv"))
        }

        fun isVideoFile(f: File): Boolean {
            return f.isFile
                    && (f.name.endsWith(".mp4")
                    || f.name.endsWith(".avi")
                    || f.name.endsWith(".webm")
                    || f.name.endsWith(".mpeg")
                    || f.name.endsWith(".mpv")
                    || f.name.endsWith(".mov")
                    || f.name.endsWith(".flv"))
        }
    }
    ///////////////////////////////////////// Context init

    private var wasPlaying: Boolean? = null

    private val afChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Permanent loss of audio focus
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Pause playback
                Log.d("audiofocus", "AUDIOFOCUS_LOSS_TRANSIENT")
                wasPlaying = isMusicPlaying
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d("audiofocus", "AUDIOFOCUS_GAIN")
                if (wasPlaying == true) play()
                // Your app has been granted audio focus again
                // Raise volume to normal, restart playback if necessary
            }
        }
    }

    var initialized = false
        private set

    fun init(context: Context) {
        if (initialized) return

        c = context

        musics = HashMap()
        lists = HashMap()

        db = MusicDB(c)
        db.open()

        initialized = true

        helper = VisualizerHelper(smc.player.audioSessionId)

        // restore queue state
        sharedPref = PreferenceManager.getDefaultSharedPreferences(c)

        currentQueueId = sharedPref.getInt("currentQueueId", currentQueueId)
        shuffleMode = sharedPref.getBoolean("shuffleMode", shuffleMode)
        val r = sharedPref.getString("repeatMode", repeatMode.toString())
        if (r != null && r.isNotEmpty()) repeatMode = Repeat.valueOf(r)
        playingFrom = sharedPref.getString("playingFrom", playingFrom).toString()
        playingFromId = sharedPref.getInt("playingFromId", playingFromId)

        if (currentQueueId >= 0) {
            prepare(currentQueueId)
            isQueuePlaying = true
        }

        // update playing time for music
        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post(object : Runnable {
            override fun run() {
                if (isMusicPlaying && currentMusic.valid) {
                    db.updateStatForMusic(currentMusicId, 0, 1)
                }
                mainHandler.postDelayed(this, 1000)
            }
        })

        // update musics by getting one
        getMusic(0)

        // initialize mediasession

        mediaSessionCompat = MediaSessionCompat(c, "PlayerService")
        mediaSessionCompat.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )

        mediaSessionCompat.setCallback(callback)

        // audio focus
        audioManager = c.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
    }

    private fun getAudioFocus(): Boolean {
        val handler = Handler()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).run {
                setAudioAttributes(AudioAttributes.Builder().run {
                    setUsage(AudioAttributes.USAGE_MEDIA)
                    setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    build()
                })
                setAcceptsDelayedFocusGain(true)
                setOnAudioFocusChangeListener(afChangeListener, handler)
                build()
            }

            val result: Int = audioManager!!.requestAudioFocus(focusRequest)

            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                return true
            }

            Toast.makeText(c, "Unpossible to gain audio focus !", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    fun updateSession() {

        mediaSessionCompat.setMetadata(
            MediaMetadataCompat.Builder()

                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, currentCoverImage)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentMusic.artist)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, currentMusic.album)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentMusic.title)
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, smc.player.hashCode().toString())
                .putString(
                    MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
                    "android.resource"
                )
                .putLong(
                    MediaMetadataCompat.METADATA_KEY_DURATION,
                    smc.player.duration.toLong()
                )
                .build()

        )

        val state = if (isMusicPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_STOPPED

        val playbackStateCompat = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY
                        or PlaybackStateCompat.ACTION_PLAY_PAUSE
                        or PlaybackStateCompat.ACTION_PAUSE
                        or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                        or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                        or PlaybackStateCompat.ACTION_STOP
                        or PlaybackStateCompat.ACTION_SEEK_TO
                //or PlaybackStateCompat.ACTION_FAST_FORWARD
                //or PlaybackStateCompat.ACTION_REWIND
                //or PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE
                //or PlaybackStateCompat.ACTION_SET_REPEAT_MODE
            )
            .setState(state, smc.player.currentPosition.toLong(), 1f)
            .build()

        mediaSessionCompat.setPlaybackState(playbackStateCompat)

    }

    ///////////////////////////////////////// music add

    fun addMusic(f: File): Boolean {
        return db.addMusicByPath(f).size > 1
    }

    ///////////////////////////////////////// Song over callback

    private fun songOverCallback() {
        if (repeatMode == Repeat.Once) {
            restartMusic()
            return
        }
        next()
    }

    ///////////////////////////////////////// Prepare / play  song

    private fun prepare(newQueueMusicId: Int) {
        if (0 <= newQueueMusicId && newQueueMusicId < getList(ListId.ID_MUSIC_QUEUE).list.size) {
            currentQueueId = newQueueMusicId
            val nextMusicId = getList(ListId.ID_MUSIC_QUEUE).list[currentQueueId]
            if (getMusic(nextMusicId).valid) {
                currentMusicId = nextMusicId
                player.reset()
                player.setDataSource(currentMusic.path)
                player.prepare()
                player.setOnCompletionListener {
                    songOverCallback()
                }
                player.setOnErrorListener { _, _, _ ->
                    //next()
                    Log.d("MusicController", "there was an error prepare ${currentMusic.path}")
                    return@setOnErrorListener true
                }
                sharedPref.edit().putInt("currentQueueId", currentQueueId).apply()
                isQueuePlaying = false
                isNotificationShown = false

                val metaRetriever = MediaMetadataRetriever()
                metaRetriever.setDataSource(currentMusic.path)
                val byteArray = metaRetriever.embeddedPicture
                currentCoverImage = if (byteArray != null) {
                    BitmapFactory.decodeByteArray(byteArray, 0, byteArray!!.size)
                } else null
            }
        }
    }

    fun play(queueMusicId: Int) {
        prepare(queueMusicId)
        play()
        db.updateStatForMusic(currentMusicId, 1, 0)
    }

    private fun restartMusic() {
        player.seekTo(0)
        player.start()
    }

    ///////////////////////////////////////// Update interface

    private fun updateRequired() {
        updateSession()
        c.sendBroadcast(
            Intent("com.example.musictest.Update_Music")
                .putExtra("actionname", "update")
        )
    }

    ///////////////////////////////////////// Input filter not to spam media controls
    private var lastTime: Long = 0
    private fun filterInput(): Boolean {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastTime > 100) {
            lastTime = currentTime
            return false
        }
        return true
    }

    ///////////////////////////////////////// Media controls
    fun toggleShuffle() {
        shuffleMode = !shuffleMode
        if (shuffleMode) startShuffle()
        else stopShuffle()
        updateRequired()
        sharedPref.edit().putBoolean("shuffleMode", shuffleMode).apply()
    }

    fun toggleRepeat() {
        repeatMode = when (repeatMode) {
            Repeat.None -> Repeat.All
            Repeat.All -> Repeat.Once
            Repeat.Once -> Repeat.None
        }
        sharedPref.edit().putString("repeatMode", repeatMode.toString()).apply()
        updateRequired()
    }

    fun togglePlay() {
        if (currentMusicId < 0) return
        if (filterInput()) return

        if (isMusicPlaying) pause()
        else play()
        isQueuePlaying = true
        isNotificationShown = true
        updateRequired()
    }

    fun play() {
        if (currentMusicId < 0) return
        if (getAudioFocus()) {
            player.start()
            isQueuePlaying = true
            isNotificationShown = true
            updateRequired()
        }
    }

    fun pause() {
        if (currentMusicId < 0) return
        player.pause()
        updateRequired()
    }

    fun stop() {
        if (filterInput()) return
        player.pause()
        isNotificationShown = false
        updateRequired()
    }

    fun prev() {
        if (currentMusicId < 0) return

        if (filterInput()) return

        if (player.currentPosition > 4000) return restartMusic()

        if (currentQueueId == 0) {
            if (repeatMode == Repeat.All) return play(getList(ListId.ID_MUSIC_QUEUE).list.size - 1)
            return restartMusic()
        }

        play(currentQueueId - 1)
    }

    fun next() {
        if (currentMusicId < 0) return

        if (filterInput()) return

        if (currentQueueId == getList(ListId.ID_MUSIC_QUEUE).list.size - 1) {
            if (repeatMode == Repeat.All) return play(0)
            updateRequired()
            return prepare(0)
        }
        play(currentQueueId + 1)
    }

    ///////////////////////////////////////// current music interactions

    fun isListLiked(currentListId :Int): Boolean {
        if (currentListId < 0) return false
        return currentListId in getList(ListId.ID_MUSIC_LIST_LIKED).list
    }

    fun toggleListLiked(currentListId :Int) {
        if (currentListId < 0) return
        if (isListLiked(currentListId)) removeIdFromList(currentListId, ListId.ID_MUSIC_LIST_LIKED)
        else addIdToList(currentListId, ListId.ID_MUSIC_LIST_LIKED)
        updateRequired()
    }

    fun isMusicLiked(currentListId :Int): Boolean {
        if (currentListId < 0) return false
        return currentListId in getList(ListId.ID_MUSIC_LIKED).list
    }

    fun toggleMusicLiked(currentListId :Int) {
        if (currentListId < 0) return
        if (isMusicLiked(currentListId)) removeIdFromList(currentListId, ListId.ID_MUSIC_LIKED)
        else addIdToList(currentListId, ListId.ID_MUSIC_LIKED)
        updateRequired()
    }

    fun isCurrentMusicLiked(): Boolean {
        return isMusicLiked(currentMusicId)
    }

    fun toggleCurrentMusicLiked() {
        return toggleMusicLiked(currentMusicId)
    }

    private fun startShuffle() {
        val currentMusic = getList(ListId.ID_MUSIC_QUEUE).list[currentQueueId]
        val currentQueue = getList(ListId.ID_MUSIC_QUEUE).list
        updateList(ListId.ID_MUSIC_QUEUE_ORIGINAL, currentQueue)
        currentQueue.remove(currentMusic)
        currentQueue.shuffle()
        val newQueue = ArrayList<Int>()
        newQueue.add(currentMusic)
        for (music in currentQueue) newQueue.add(music)
        updateList(ListId.ID_MUSIC_QUEUE, newQueue)
        currentQueueId = getList(ListId.ID_MUSIC_QUEUE).list.indexOf(currentMusic)
    }

    private fun stopShuffle() {
        val currentMusic = getList(ListId.ID_MUSIC_QUEUE).list[currentQueueId]
        updateList(ListId.ID_MUSIC_QUEUE, getList(ListId.ID_MUSIC_QUEUE_ORIGINAL).list)
        currentQueueId = getList(ListId.ID_MUSIC_QUEUE).list.indexOf(currentMusic)
    }

    ///////////////////////////////////////// Queue actions

    /*fun setQueue(ids: ArrayList<Int>, from: String, idToPlay: Int, playNow: Boolean) {
        playingFrom = from
        updateList(ListId.ID_MUSIC_QUEUE, ids)
        currentQueueId = idToPlay
        if (shuffleMode) {
            startShuffle()
            if (playNow) play(0)
            else prepare(0)
        } else {
            if (playNow) play(idToPlay)
            else prepare(idToPlay)
        }
    }*/


    fun setQueue(ids: ArrayList<Int>, from: Int?, idToPlay: Int, playNow: Boolean) {
        playingFrom = if (from != null) {
            playingFromId = from
            addListPlayed(from)
            getList(from).name
        } else "Search"
        updateList(ListId.ID_MUSIC_QUEUE, ids)
        currentQueueId = idToPlay
        if (shuffleMode) {
            startShuffle()
            if (playNow) play(0)
            else prepare(0)
        } else {
            if (playNow) play(idToPlay)
            else prepare(idToPlay)
        }
    }

    private fun addListPlayed(lid: Int) {
        if (lid > 0) db.updateStatForList(lid, 1)
    }

    fun getMusicFromQueueId(queueId: Int): SyncMusic {
        return getMusic(getList(ListId.ID_MUSIC_QUEUE).list[queueId])
    }

    fun addPlaylistMenu(sm: Menu) {
        sm.add(0, 9, 9, "Create new playlist")
        val playlists = smc.getList(ListId.ID_MUSIC_USER_PLAYLISTS)
        for (i in 0 until playlists.list.size)
            sm.add(0, i + 10, i + 10, smc.getList(playlists.list[i]).name)
    }

    fun processPlaylistMenu(context: Context, msuciIs: Int, music: SyncMusic, menuid: MenuItem) {
        if (menuid.itemId == 9) {

            val builder = AlertDialog.Builder(context)
            builder.setTitle("New Playlist")

            val input = EditText(context)
            input.inputType = InputType.TYPE_CLASS_TEXT
            builder.setView(input)


            builder.setPositiveButton("Save") { _, _ ->
                val newPlaylistName = input.text.toString()
                if (newPlaylistName.length < 2) {
                    Toast.makeText(context, "Playlist name too short", Toast.LENGTH_SHORT).show()
                } else {
                    val id = smc.createPlaylist(newPlaylistName)
                    smc.addIdToList(msuciIs, id)
                    Toast.makeText(context, "Adding ${music.title} to " + newPlaylistName, Toast.LENGTH_SHORT).show()
                }
            }
            builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

            builder.show()
        } else {
            val playlists = smc.getList(ListId.ID_MUSIC_USER_PLAYLISTS)
            smc.addIdToList(msuciIs, playlists.list[menuid.itemId - 10])
            Toast.makeText(context, "Adding ${music.title} to " + menuid.title, Toast.LENGTH_SHORT).show()
        }

    }

    fun processPlaylistMenu(context: Context, msuciIs: ArrayList<Int>, menuid: MenuItem) {
        if (menuid.itemId == 9) {

            val builder = AlertDialog.Builder(context)
            builder.setTitle("New Playlist")

            val input = EditText(context)
            input.inputType = InputType.TYPE_CLASS_TEXT
            builder.setView(input)


            builder.setPositiveButton("Save") { _, _ ->
                val newPlaylistName = input.text.toString()
                if (newPlaylistName.length < 2) {
                    Toast.makeText(context, "Playlist name too short", Toast.LENGTH_SHORT).show()
                } else {
                    val id = smc.createPlaylist(newPlaylistName)

                    for (mid in msuciIs) addIdToList(mid, id)
                    Toast.makeText(context, "Adding ${msuciIs.size} songs to " + newPlaylistName, Toast.LENGTH_SHORT)
                        .show()
                }
            }
            builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

            builder.show()
        } else {
            val playlists = smc.getList(ListId.ID_MUSIC_USER_PLAYLISTS)
            for (mid in msuciIs) addIdToList(mid, playlists.list[menuid.itemId - 10])
            Toast.makeText(context, "Adding ${msuciIs.size} songs to " + menuid.title, Toast.LENGTH_SHORT).show()
        }

    }

    fun deletePlaylist(id: Int) {
        db.clearListId(id)
        db.deleteListId(id)
        db.removeIdFromListId(id, ListId.ID_MUSIC_USER_PLAYLISTS)
    }

    fun setQueueFiles(files: ArrayList<File>, from: String, idToPlay: Int = -2) {
        playingFrom = from
        playingFromId = -1
        var id = 0
        var nextQueueId = 0

        val nextQueue = ArrayList<Int>()
        /*Thread{
            var dialog = ProgressDialog(c)
            dialog.setMessage("Impotring musics")
            dialog.show()

            dialog.dismiss()

        }.start()*/

        files.forEach { f ->

            if (isMusicFile(f)) {
                val insertId = db.addMusicByPath(f)[0]
                nextQueue.add(insertId)
                if (id == idToPlay) {
                    nextQueueId = nextQueue.size - 1
                }
            }
            id++
        }

        updateList(ListId.ID_MUSIC_QUEUE, nextQueue)

        prepare(nextQueueId)
        play(nextQueueId)

        if (shuffleMode) startShuffle()
    }

    override fun onTerminate() {
        super.onTerminate()
        helper.release()
        db.close()
    }
}