package com.example.musictest

import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.text.InputType
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import com.example.musictest.activities.MainActivity
import com.example.musictest.activities.syncMusicController
import com.example.musictest.databases.ListType
import com.example.musictest.databases.MusicDB
import com.example.musictest.databases.listId
import io.github.jeffshee.visualizer.utils.VisualizerHelper
import java.io.File


enum class Repeat {
    None, All, Once
}

class SyncMusic
{
    var path: String = ""
        private set

    //var hash: ByteArray = ByteArray(0)
    //    private set

    var title: String? = null
        get() = if(field == null) "Unknown title" else field
        private set

    var artist: String? = null
        get() = if(field == null) "Unknown artist" else field
        private set

    var album: String? = null
        get() = if(field == null) "Unknown album" else field
        private set

    var image_id: Int? = null
        private set

    var valid = false
        private set

    constructor(f: File)
    {
        try{
            this.path = f.path

            if(!f.exists()) {
                valid = false
                Log.w("MusicController", "Invalid file = " + f.path)
                return
            }
            val metaRetriever = MediaMetadataRetriever()

            metaRetriever.setDataSource(path)

            //hash = metaRetriever.hashCode()

            this.title = if (metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) != null)
                metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            else f.nameWithoutExtension

            this.artist = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            this.album = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)

            this.image_id = null
            this.image // load lazy image by accessing it
            this.valid = true
        }
        catch (exception: Exception)
        {
            this.valid = false
            Log.w("MusicController", "Invalid music = " + f.path)
        }
    }

    constructor(cursor: Cursor)
    {
        //id = cursor.getInt(0)
        this.valid = cursor.getInt(1) > 0
        this.path = cursor.getString(2)
        this.title = cursor.getString(3)
        this.artist = cursor.getString(4)
        this.album = cursor.getString(5)
    }

    constructor()

    // lazy Image loader

    /*private fun initImage() : Bitmap?
    {
        if(imageInitialized) return image

        imageInitialized = true

        try{
            metaRetriever.setDataSource(path)

            val imageByte = metaRetriever.embeddedPicture
            if (imageByte != null) {
                val bmp = BitmapFactory.decodeByteArray(imageByte, 0, imageByte!!.size)
                val newimage = Bitmap.createBitmap(bmp)

                val IMAGE_SIZE = 400
                val landscape: Boolean = newimage.getWidth() > newimage.getHeight()

                val scale_factor: Float
                if (landscape) scale_factor = IMAGE_SIZE.toFloat() / newimage.getHeight() else scale_factor = IMAGE_SIZE.toFloat() / newimage.getWidth()
                val matrix = Matrix()
                matrix.postScale(scale_factor, scale_factor)

                return if (landscape) {
                    val start: Int = (newimage.getWidth() - newimage.getHeight()) / 2
                    Bitmap.createBitmap(newimage, start, 0, newimage.getHeight(), newimage.getHeight(), matrix, true)
                } else {
                    val start: Int = (newimage.getHeight() - newimage.getWidth()) / 2
                    Bitmap.createBitmap(newimage, 0, start, newimage.getWidth(), newimage.getWidth(), matrix, true)
                }
            }
        }catch (exception: Exception)
        {
            this.valid = false
            Log.w("MusicController", "Invalid music load image= " + path)
        }
        return null
    }*/

    var imageInitialized = false
        private set

    val image: Bitmap? = null
    /*? by lazy {
        if(imageInitialized) image else initImage()
    }*/
}

class SyncList{
    var name: String = "Invalid list"
    var list: ArrayList<Int> = ArrayList()
    var listType = ListType.None
    var valid = false

    constructor(name_: String, listType_: ListType)
    {
        list =  ArrayList()
        name = name_
        listType = listType_
        valid = true

    }

    constructor(name_: String, cursor: Cursor, listType_: ListType)
    {
        list =  ArrayList()
        name = name_
        listType = listType_
        valid = true
        for (i in 0 until cursor.count)
        {
            list.add(cursor.getInt(0))
            cursor.moveToNext()
        }
    }

    constructor()
}

class SyncMusicController : Application() {

    lateinit var helper: VisualizerHelper

    lateinit var c : Context
        private set

    private lateinit var db : MusicDB

    private lateinit var sharedPref: SharedPreferences

    private lateinit var musics: HashMap<Int, SyncMusic>

    private var musicsListValid = false

    private lateinit var lists: HashMap<Int, SyncList>

    var player: MediaPlayer = MediaPlayer()
        private set

    var currentQueueId: Int = -1
        private set

    var currentMusicId: Int = -1
        private set

    val currentMusic: SyncMusic
        get() = if(musics.isNotEmpty() && currentMusicId >= 0 && musics[currentMusicId] != null)musics[currentMusicId]!! else SyncMusic()

    var isQueuePlaying: Boolean = false
        private set

    var isMusicPlaying : Boolean = false
        get() = player.isPlaying
        private set

    var shuffleMode: Boolean = false
        private set

    var repeatMode: Repeat = Repeat.None
        private set

    var playingFrom : String = "Phone"
        set(value){
            sharedPref.edit().putString("playingFrom", playingFrom).apply()
            field = value
        }

    ///////////////////////////////////////// Musics operations

    fun invalidateMusics(){
        musicsListValid = false
    }

    fun getMusic(id: Int) : SyncMusic
    {
        if(!musicsListValid)
        {
            musics = db.getAllMusicMaps()
            musicsListValid = true
        }
        return if (musics[id] == null) SyncMusic() else musics[id]!!
    }

    ///////////////////////////////////////// List operations

    fun getList(id: Int) : SyncList
    {
        if(lists[id] != null && lists[id]?.valid == true) return lists[id]!!
        val newList = db.getListFromId(id)
        return if(newList == null || !newList.valid) SyncList()
        else{
            lists[id] = newList
            lists[id]!!
        }
    }

    fun invalidateList(id: Int)
    {
        lists[id]?.valid = false
    }

    fun updateList(id: Int, newList: ArrayList<Int>)
    {
        db.setListData(id, newList)
    }

    fun clearList(id: Int)
    {
        db.clearListId(id)
    }

    fun addIdToList(music_id: Int, list_id: Int)
    {
        db.addIdToListId(music_id, list_id)
    }

    fun removeIdFromList(music_id: Int, list_id: Int)
    {
        db.removeIdFromListId(music_id, list_id)
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

        currentQueueId = sp.getInt("currentQueueId", currentQueueId)
        shuffleMode = sp.getBoolean("shuffleMode", shuffleMode)
        playingFrom = sp.getString("playingFrom", playingFrom).toString()

        if(currentQueueId >= 0)
        {
            prepare(currentQueueId)
            isQueuePlaying = true
        }
    }

    ///////////////////////////////////////// music add

    fun addMusic(f: File) : Boolean
    {
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
        if(0 <= newQueueMusicId && newQueueMusicId < getList(listId.ID_MUSIC_QUEUE).list.size)
        {
            currentQueueId = newQueueMusicId
            val nextMusicId = getList(listId.ID_MUSIC_QUEUE).list[currentQueueId]
            if(getMusic(nextMusicId).valid)
            {
                currentMusicId = nextMusicId
                player.reset()
                player.setDataSource(currentMusic.path)
                player.prepare()
                player.setOnCompletionListener{
                    songOverCallback()
                }
                sharedPref.edit().putInt("currentQueueId", currentQueueId).apply()
                isQueuePlaying = false
            }
        }
    }

    fun play(queueMusicId: Int)
    {
        prepare(queueMusicId)
        player.start()
        isQueuePlaying = true
        updateRequired()
    }

    private fun restartMusic()
    {
        player.seekTo(0)
        player.start()
    }

    ///////////////////////////////////////// Update interface

    private fun updateRequired()
    {
        c.sendBroadcast(
                Intent("com.example.musictest.Update_Music")
                        .putExtra("actionname", "update")
        )
    }

    ///////////////////////////////////////// Input filter not to spam media controls
    var lastTime : Long = 0
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

    fun toggleRepeat()
    {
        repeatMode = when (repeatMode) {
            Repeat.None -> Repeat.All
            Repeat.All -> Repeat.Once
            Repeat.Once -> Repeat.None
        }
        updateRequired()
    }

    fun togglePlay()
    {
        if(currentMusicId < 0) return
        if(filterInput()) return

        if(isMusicPlaying) player.pause()
        else player.start()
        isQueuePlaying = true
        updateRequired()
    }

    fun play()
    {
        if(currentMusicId < 0) return
        if(filterInput()) return
        player.start()
        isQueuePlaying = true
        updateRequired()
    }

    fun pause()
    {
        if(currentMusicId < 0) return
        if(filterInput()) return
        player.pause()
        updateRequired()
    }

    fun prev()
    {
        if(currentMusicId < 0) return

        if(filterInput()) return
        // TODO manage shuffle

        if(player.currentPosition > 4000) return restartMusic()

        if(currentQueueId == 0)
        {
            if(repeatMode == Repeat.All) return play(getList(listId.ID_MUSIC_QUEUE).list.size - 1)
            return restartMusic()
        }

        play(currentQueueId - 1)
    }

    fun next()
    {
        if(currentMusicId < 0) return

        if(filterInput()) return

        Log.w("Next", "Current = " + currentQueueId + " = " + currentMusicId)
        if(currentQueueId == getList(listId.ID_MUSIC_QUEUE).list.size -1)
        {
            if(repeatMode == Repeat.All) return play(0)
            updateRequired()
            return prepare(0)
        }
        play(currentQueueId + 1)
        Log.w("Next", "after = " + currentQueueId + " = " + currentMusicId)
    }

    ///////////////////////////////////////// current music interactions

    fun isCurrentMusicLiked() : Boolean
    {
        if(currentMusicId < 0) return false
        return currentMusicId in getList(listId.ID_MUSIC_LIKED).list
    }

    fun toggleCurrentMusicLiked()
    {
        if(currentMusicId < 0) return

        if(isCurrentMusicLiked()) removeIdFromList(currentMusicId, listId.ID_MUSIC_LIKED)
        else addIdToList(currentMusicId, listId.ID_MUSIC_LIKED)

        updateRequired()
    }


    private fun startShuffle()
    {
        val currentMusic = getList(listId.ID_MUSIC_QUEUE).list[currentQueueId]
        val currentQueue = getList(listId.ID_MUSIC_QUEUE).list
        updateList(listId.ID_MUSIC_QUEUE_ORIGINAL, currentQueue)
        currentQueue.remove(currentMusic)
        currentQueue.shuffle()
        val newQueue = ArrayList<Int>()
        newQueue.add(currentMusic)
        for (music in currentQueue) newQueue.add(music)
        updateList(listId.ID_MUSIC_QUEUE, newQueue)
        currentQueueId = getList(listId.ID_MUSIC_QUEUE).list.indexOf(currentMusic)
    }

    private fun stopShuffle()
    {
        val currentMusic = getList(listId.ID_MUSIC_QUEUE).list[currentQueueId]
        updateList(listId.ID_MUSIC_QUEUE, getList(listId.ID_MUSIC_QUEUE_ORIGINAL).list)
        currentQueueId = getList(listId.ID_MUSIC_QUEUE).list.indexOf(currentMusic)
    }

    ///////////////////////////////////////// Queue actions

    fun setQueue(ids: ArrayList<Int>, from: String)
    {
        playingFrom = from
        updateList(listId.ID_MUSIC_QUEUE, ids)
        if(shuffleMode) startShuffle()
        prepare(0)
    }

    fun getMusicFromQueueId(queueId: Int) : SyncMusic
    {
        return getMusic(getList(listId.ID_MUSIC_QUEUE).list[queueId])
    }

    fun getPlaylistsIds() : ArrayList<Int>
    {
        return getList(listId.ID_MUSIC_USER_PLAYLISTS).list
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

        fun onNew() {
            val alertDialog = AlertDialog.Builder(c).create()
            alertDialog.setTitle("Alert")
            alertDialog.setMessage("Alert message to be shown")
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK"
            ) { dialog, which -> dialog.dismiss() }
            alertDialog.show()
        }


        val playlistNames : ArrayList<String> = ArrayList()
        val playlistIds : ArrayList<Int> = ArrayList()

        val userplaylists = getList(listId.ID_MUSIC_USER_PLAYLISTS)

        for (playlistId in userplaylists.list)
        {
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

            if(MainActivity.isMusicFile(f))
            {
                val insertId = db.addMusicByPath(f)[0]
                nextQueue.add(insertId)
                if(id == idToPlay) {
                    nextQueueId = nextQueue.size -1
                }
            }
            id++
        }

        updateList(listId.ID_MUSIC_QUEUE, nextQueue)

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