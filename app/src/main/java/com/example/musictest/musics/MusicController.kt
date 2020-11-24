package com.example.musictest.musics

import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.widget.Toast
import com.example.musictest.activities.syncMusicController
import io.github.jeffshee.visualizer.utils.VisualizerHelper
import java.io.File


enum class Repeat {
    None, All, Once
}


private val callback = object : MediaSessionCompat.Callback() {
    override fun onPlay() {
        super.onPlay()
        syncMusicController.play()
    }

    override fun onPause() {
        super.onPause()
        syncMusicController.pause()
    }

    override fun onSkipToNext() {
        super.onSkipToNext()
        syncMusicController.next()
    }

    override fun onSkipToPrevious() {
        super.onSkipToPrevious()
        syncMusicController.prev()
    }

    override fun onSeekTo(pos: Long) {
        super.onSeekTo(pos)
        syncMusicController.player.seekTo(pos.toInt())
        syncMusicController.updateSession()
    }
}

class SyncMusicController : Application() {

    lateinit var helper: VisualizerHelper

    lateinit var c : Context
        private set

    private lateinit var db : MusicDB

    private lateinit var sharedPref: SharedPreferences

    lateinit var musics: HashMap<Int, SyncMusic>

    var currentCoverImage : Bitmap? = null

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

    private fun addIdToList(music_id: Int, list_id: Int) {
        db.addIdToListId(music_id, list_id)
    }

    private fun removeIdFromList(music_id: Int, list_id: Int) {
        db.removeIdFromListId(music_id, list_id)
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
    }
    ///////////////////////////////////////// Context init



    var initialized = false
        private set

    fun init(context: Context, sp: SharedPreferences)
    {
        if(initialized) return

        c = context

        musics = HashMap()
        lists = HashMap()

        db = MusicDB(c)
        db.open()

        initialized = true

        helper = VisualizerHelper(syncMusicController.player.audioSessionId)

        // restore queue state
        sharedPref = sp

        currentQueueId = sharedPref.getInt("currentQueueId", currentQueueId)
        shuffleMode = sharedPref.getBoolean("shuffleMode", shuffleMode)
        val r = sharedPref.getString("repeatMode", repeatMode.toString())
        if (r != null && r.isNotEmpty()) repeatMode = Repeat.valueOf(r)
        playingFrom = sharedPref.getString("playingFrom", playingFrom).toString()

        if (currentQueueId >= 0) {
            prepare(currentQueueId)
            isQueuePlaying = true
        }

        // update playing time for music
        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post(object : Runnable {
            override fun run() {
                if(isMusicPlaying && currentMusic.valid)
                {
                    db.updateStatForMusic(currentMusicId, 0, 1)
                }
                mainHandler.postDelayed(this, 1000)
            }
        })

        // update musics by getting one
        getMusic(0)

        // initialize mediasession

        mediaSessionCompat = MediaSessionCompat(c, "PlayerService")
        mediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)

        mediaSessionCompat.setCallback(callback);
    }

    fun updateSession() {

        mediaSessionCompat.setMetadata(
                MediaMetadataCompat.Builder()

                        .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, currentCoverImage)
                        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, currentMusic.artist)
                        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, currentMusic.album)
                        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, currentMusic.title)
                        .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, syncMusicController.player.hashCode().toString())
                        .putString(
                                MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI,
                                "android.resource"
                        )
                        .putLong(
                                MediaMetadataCompat.METADATA_KEY_DURATION,
                                syncMusicController.player.duration.toLong()
                        )
                        .build()
        )

        /*val audioManager = c.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
        val result = audioManager!!.requestAudioFocus({ }, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        if (result != AudioManager.AUDIOFOCUS_GAIN) {
            return  //Failed to gain audio focus
        }*/

        val state = if (isMusicPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_STOPPED

        val playbackStateCompat = PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PLAY_PAUSE or
                                PlaybackStateCompat.ACTION_PAUSE or
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                                or PlaybackStateCompat.ACTION_SEEK_TO
                )
                .setState(state, syncMusicController.player.currentPosition.toLong(), 1f)
                .build()

        mediaSessionCompat.setPlaybackState(playbackStateCompat)

    }

    ///////////////////////////////////////// music add

    fun addMusic(f: File): Boolean {
        return db.addMusicByPath(f).size > 1
    }

    ///////////////////////////////////////// Song over callback

    private fun songOverCallback()
    {
        if(repeatMode == Repeat.Once)
        {
            restartMusic()
            return
        }
        next()
    }

    ///////////////////////////////////////// Prepare / play  song

    private fun prepare(newQueueMusicId: Int)
    {
        if(0 <= newQueueMusicId && newQueueMusicId < getList(ListId.ID_MUSIC_QUEUE).list.size)
        {
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

    fun play(queueMusicId: Int)
    {
        prepare(queueMusicId)
        player.start()
        isQueuePlaying = true
        isNotificationShown = true
        updateRequired()
        db.updateStatForMusic(currentMusicId, 1, 0)
    }

    private fun restartMusic()
    {
        player.seekTo(0)
        player.start()
    }

    ///////////////////////////////////////// Update interface

    private fun updateRequired()
    {
        updateSession()
        c.sendBroadcast(
                Intent("com.example.musictest.Update_Music")
                        .putExtra("actionname", "update")
        )
    }

    ///////////////////////////////////////// Input filter not to spam media controls
    private var lastTime: Long = 0
    private fun filterInput() : Boolean
    {
        val currentTime = System.currentTimeMillis()

        if(currentTime - lastTime > 100)
        {
            lastTime = currentTime
            return false
        }
        return true
    }

    ///////////////////////////////////////// Media controls
    fun toggleShuffle()
    {
        shuffleMode = !shuffleMode
        if(shuffleMode)startShuffle()
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

        if (isMusicPlaying) player.pause()
        else player.start()
        isQueuePlaying = true
        isNotificationShown = true
        updateRequired()
    }

    fun play() {
        if (currentMusicId < 0) return
        //if(filterInput()) return
        player.start()
        isQueuePlaying = true
        isNotificationShown = true
        updateRequired()
    }

    fun pause() {
        if (currentMusicId < 0) return
        //if(filterInput()) return
        player.pause()
        updateRequired()
    }

    fun stop() {
        //if(currentMusicId < 0) return
        if (filterInput()) return
        player.pause()
        updateRequired()
        isNotificationShown = false
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

    fun next()
    {
        if(currentMusicId < 0) return

        if(filterInput()) return

        if(currentQueueId == getList(ListId.ID_MUSIC_QUEUE).list.size -1)
        {
            if(repeatMode == Repeat.All) return play(0)
            updateRequired()
            return prepare(0)
        }
        play(currentQueueId + 1)
    }

    ///////////////////////////////////////// current music interactions

    fun isCurrentMusicLiked() : Boolean
    {
        if(currentMusicId < 0) return false
        return currentMusicId in getList(ListId.ID_MUSIC_LIKED).list
    }

    fun toggleCurrentMusicLiked()
    {
        if(currentMusicId < 0) return

        if(isCurrentMusicLiked()) removeIdFromList(currentMusicId, ListId.ID_MUSIC_LIKED)
        else addIdToList(currentMusicId, ListId.ID_MUSIC_LIKED)

        updateRequired()
    }


    private fun startShuffle()
    {
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

    private fun stopShuffle()
    {
        val currentMusic = getList(ListId.ID_MUSIC_QUEUE).list[currentQueueId]
        updateList(ListId.ID_MUSIC_QUEUE, getList(ListId.ID_MUSIC_QUEUE_ORIGINAL).list)
        currentQueueId = getList(ListId.ID_MUSIC_QUEUE).list.indexOf(currentMusic)
    }

    ///////////////////////////////////////// Queue actions

    fun setQueue(ids: ArrayList<Int>, from: String, idToPlay: Int, playNow: Boolean) {
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
    }

    fun addListPlayed(lid: Int)
    {
        if(lid > 0) db.updateStatForList(lid, 1)
    }

    fun getMusicFromQueueId(queueId: Int) : SyncMusic
    {
        return getMusic(getList(ListId.ID_MUSIC_QUEUE).list[queueId])
    }

    fun getPlaylistsIds() : ArrayList<Int>
    {
        return getList(ListId.ID_MUSIC_USER_PLAYLISTS).list
    }

    fun addMusicIdsToPlaylistName(selection: ArrayList<Int>, text: String)
    {

    }

    fun addToPlaylistDialog(
            context: Context,
            selection: ArrayList<Int>,
            onSuccess: () -> Unit = {},
            onCancel: () -> Unit = {},
    )
    {
        val playlistNames : ArrayList<String> = ArrayList()
        val playlistIds : ArrayList<Int> = ArrayList()

        val userPlaylists = getList(ListId.ID_MUSIC_USER_PLAYLISTS)

        for (playlistId in userPlaylists.list) {
            val list = getList(playlistId)
            playlistIds.add(playlistId)
            playlistNames.add(list.name)
        }

        val li2 = playlistNames.toTypedArray()

        val mBuilder = AlertDialog.Builder(context)

        if(selection.size == 1)mBuilder.setTitle("Add " + getMusic(selection[0]).title + " to:")
        else mBuilder.setTitle("Add " + selection.size.toString() + " songs to:")


        mBuilder.setPositiveButton("Cancel") { dialog, _ ->
            dialog.cancel()
            onCancel()
        }

        mBuilder.setSingleChoiceItems(li2, -1) { dialogInterface, i ->

            if(selection.size == 1)Toast.makeText(
                    context,
                    getMusic(selection[0]).title + " added to " + li2[i],
                    Toast.LENGTH_LONG
            ).show()

            else Toast.makeText(
                    context,
                    selection.size.toString() + " songs added to " + li2[i],
                    Toast.LENGTH_LONG
            ).show()

            for (s in selection) addIdToList(s, i)

            dialogInterface.dismiss()
            onSuccess()
        }



        val mDialog = mBuilder.create()
        mDialog.show()
    }

    fun setQueueFiles(files: ArrayList<File>, from: String, idToPlay: Int = -2)
    {
        playingFrom = from
        var id = 0
        var nextQueueId = 0

        val nextQueue = ArrayList<Int>()
        /*Thread{
            var dialog = ProgressDialog(c)
            dialog.setMessage("Impotring musics")
            dialog.show()

            dialog.dismiss()

        }.start()*/

        files.forEach{ f ->

            if(isMusicFile(f))
            {
                val insertId = db.addMusicByPath(f)[0]
                nextQueue.add(insertId)
                if(id == idToPlay) {
                    nextQueueId = nextQueue.size -1
                }
            }
            id++
        }

        updateList(ListId.ID_MUSIC_QUEUE, nextQueue)

        prepare(nextQueueId)
        play(nextQueueId)

        if(shuffleMode)startShuffle()
    }

    override fun onTerminate() {
        super.onTerminate()
        helper.release()
        db.close()
    }
}