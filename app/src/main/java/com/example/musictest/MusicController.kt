package com.example.musictest

import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.util.Log
import android.widget.Toast
import com.example.musictest.activities.MainActivity
import com.example.musictest.databases.ListType
import com.example.musictest.databases.MusicDB
import java.io.File

enum class Repeat {
    None, All, Once
}

val metaRetriever = MediaMetadataRetriever()

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

    constructor(f : File)
    {
        try{
            this.path = f.path

            if(!f.exists()) {
                valid = false
                Log.w("MusicController", "Invalid file = " + f.path)
                return
            }

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

    constructor(cursor : Cursor)
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

    private fun initImage() : Bitmap?
    {
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

                imageInitialized = true

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
    }

    var imageInitialized = false
        private set

    val image: Bitmap? by lazy {
        initImage()
    }
}

class SyncList{
    var name: String = "Invalid list"
    var list: ArrayList<Int> = ArrayList()
    var listType = ListType.None

    constructor(name_ : String, listType_ : ListType)
    {
        list =  ArrayList()
        name = name_
        listType = listType_
    }

    constructor(name_ : String, cursor: Cursor, listType_ : ListType)
    {
        list =  ArrayList()
        name = name_
        listType = listType_
        for (i in 0 until cursor.count)
        {
            list.add(cursor.getInt(0))
            cursor.moveToNext()
        }
    }

    constructor()
}


class SyncMusicController : Application() {

    lateinit var c : Context
        private set

    lateinit var db : MusicDB

    // TODO implement map instead of arrayList
    lateinit var musicsM: HashMap<Int, SyncMusic>
    lateinit var listsM: HashMap<Int, SyncList>

    fun getMusic(index : Int) : SyncMusic
    {
        return if (musicsM[index] == null) SyncMusic() else musicsM[index]!!
    }

    fun getList(index : Int) : SyncList
    {
        return if (listsM[index] == null) SyncList() else listsM[index]!!
    }

    //lateinit var musics: ArrayList<SyncMusic>
    //    private set

    //lateinit var lists: ArrayList<SyncList>
    //    private set

    var player: MediaPlayer = MediaPlayer()
        private set

    var currentQueueId: Int = -1
        private set
    var currentMusicId: Int = -1
        private set

    var currentMusic: SyncMusic = SyncMusic()
        get() = if(musicsM.isNotEmpty() && currentMusicId >= 0 && musicsM[currentMusicId] != null)musicsM[currentMusicId]!! else SyncMusic()

    var list_all : ArrayList<Int> = ArrayList()
        get() = getList(MusicDB.ID_MUSIC_ALL).list
        private set

    var list_queue : ArrayList<Int> = ArrayList()
        get() = getList(MusicDB.ID_MUSIC_QUEUE).list
        private set

    var list_liked : ArrayList<Int> = ArrayList()
        get() = getList(MusicDB.ID_MUSIC_LIKED).list
        private set

    var isQueuePlaying: Boolean = false
        private set

    var isMusicPlaying : Boolean = false
        get() = player.isPlaying
        private set

    var shuffleMode: Boolean = false
        get() = field

    var repeatMode: Repeat = Repeat.None
        private set

    ///////////////////////////////////////// Context init


    fun retrieveAllFromDB()
    {
        musicsM = db.getAllMusicMaps()
        listsM = db.getAllListMaps()
    }

    fun init(context: Context)
    {
        c = context

        db = MusicDB(c)
        db.open()

        // new impl
        retrieveAllFromDB()

        // restore queue state ?
    }

    ///////////////////////////////////////// music add

    fun addMusic(f: File) : Boolean
    {
        val newMusic = SyncMusic(f)

        if(db.addMusic(newMusic)) {
            //musics.add(newMusic)
            retrieveAllFromDB()
            return true
        }

        return false
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

    ///////////////////////////////////////// Prepare song

    private fun prepare(newQueueMusicId: Int)
    {
        if(0 <= newQueueMusicId && newQueueMusicId < list_queue.size)
        {
            currentQueueId = newQueueMusicId
            val nextMusicId = list_queue[currentQueueId]
            if(musicsM[nextMusicId] != null)
            {
                currentMusicId = nextMusicId
                player.reset()
                player.setDataSource(currentMusic.path)
                player.prepare()
                player.setOnCompletionListener{
                    songOverCallback()
                }
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

    ///////////////////////////////////////// Update interface

    private fun updateRequired()
    {
        c.sendBroadcast(Intent("com.example.musictest.Update_Music")
                .putExtra("actionname", "update"))
    }

    ///////////////////////////////////////// Input filter not to spam media controls
    var lastTime : Long = 0
    private fun filterInput() : Boolean
    {
        val currenttime = System.currentTimeMillis()

        if(currenttime - lastTime > 100)
        {
            lastTime = currenttime
            return false
        }
        return true
    }

    ///////////////////////////////////////// Media controls
    fun toggleShuffle()
    {
        shuffleMode = !shuffleMode
        updateRequired()
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

    private fun restartMusic()
    {
        player.seekTo(0)
        player.start()
    }

    fun prev()
    {
        if(currentMusicId < 0) return

        if(filterInput()) return
        // TODO manage shuffle

        if(player.currentPosition > 4000) return restartMusic()

        if(currentQueueId == 0)
        {
            if(repeatMode == Repeat.All) return play(list_queue.size - 1)
            return restartMusic()
        }

        play(currentQueueId - 1)
    }

    fun next()
    {
        if(currentMusicId < 0) return

        if(filterInput()) return

        Log.w("Next", "Current = " + currentQueueId + " = " + currentMusicId)
        if(currentQueueId == list_queue.size -1)
        {
            if(repeatMode == Repeat.All) return play(0)
            updateRequired()
            return prepare(0)
        }
        play(currentQueueId + 1)
        Log.w("Next", "after = " + currentQueueId + " = " + currentMusicId)
    }

    fun isCurrentMusicLiked() : Boolean
    {
        if(currentMusicId < 0) return false
        return currentMusicId in getList(MusicDB.ID_MUSIC_LIKED).list
    }

    fun toggleCurrentMusicLiked()
    {
        if(currentMusicId < 0) return

        if(isCurrentMusicLiked()) removeMusicIdFromListId(currentMusicId,MusicDB.ID_MUSIC_LIKED)
        else addMusicIdToListId(currentMusicId,MusicDB.ID_MUSIC_LIKED)

        updateRequired()
    }

    // DB interface list

    fun addMusicIdToListId(music_id : Int, list_id : Int)
    {
        db.addMusicIdToListId(music_id, list_id)
        retrieveAllFromDB()
    }

    fun removeMusicIdFromListId(music_id : Int, list_id : Int)
    {
        db.removeMusicIdFromListId(music_id, list_id)
        retrieveAllFromDB()
    }

    fun setQueue(ids: ArrayList<Int>)
    {
        list_queue.clear()

        ids.forEach{ f ->
            list_queue.add(f)
        }

        prepare(0)
    }

    fun getMusicFromQueueId(queueId: Int) : SyncMusic
    {
        return getMusic(list_queue[queueId])
    }

    /*fun addToQueue(selection: ArrayList<Int>, duplicates: Boolean = false)
    {
        if(duplicates)
            selection.forEach { s -> list_queue.add(s) }
        else
            selection.forEach { s -> if(s !in list_queue) list_queue.add(s) }
    }*/

    fun getPlaylistsIds() : ArrayList<Int>
    {
        val playlistIds = ArrayList<Int>()

        for (i in listsM)
        {
            if(i.value.listType == ListType.SystemRW || i.value.listType == ListType.User)
                playlistIds.add(i.key)
        }

        return playlistIds
    }

    fun addToPlaylistDialog(context: Context, selection: ArrayList<Int>, onSuccess: () -> Unit = {}, onCancel: () -> Unit = {})
    {
        val playlistNames : ArrayList<String> = ArrayList()
        val playlistIds : ArrayList<Int> = ArrayList()

        for (i in listsM)
        {
            if(i.value.listType == ListType.SystemRW || i.value.listType == ListType.User)
            {
                playlistIds.add(i.key)
                playlistNames.add(i.value.name)
            }
        }

        val li2 = playlistNames.toTypedArray()

        val mBuilder = AlertDialog.Builder(context)

        if(selection.size == 1)mBuilder.setTitle("Add " + getMusic(selection[0]).title + " to:")
        else mBuilder.setTitle("Add " + selection.size.toString() + " songs to:")

        mBuilder.setSingleChoiceItems(li2, -1) { dialogInterface, i ->

            if(selection.size == 1)Toast.makeText(context,
                    getMusic(selection[0]).title + " added to " + li2[i],
                    Toast.LENGTH_LONG).show()

            else Toast.makeText(context,
                    selection.size.toString() + " songs added to " + li2[i],
                    Toast.LENGTH_LONG).show()

            for (s in selection) addMusicIdToListId(s, i)

            dialogInterface.dismiss()
            onSuccess()
        }

        mBuilder.setNeutralButton("Cancel") { dialog, which ->
            dialog.cancel()
            onCancel()
        }

        val mDialog = mBuilder.create()
        mDialog.show()
    }

    fun setQueueFiles(files: ArrayList<File>, idToPlay : Int = -1)
    {
        var id = 0
        var nextQueueId = 0

        // TODO Synchronize with db

        list_queue.clear()

        files.forEach{ f ->

            if(MainActivity.isMusicFile(f))
            {
                val newMusic = SyncMusic(f)

                val insertId = db.addMusicEx(newMusic)

                list_queue.add(insertId)
                if(id == idToPlay) {
                    nextQueueId = list_queue.size -1
                }
            }

            id++
        }

        //retrieveAllFromDB()

        prepare(nextQueueId)
        play(nextQueueId)
    }

}