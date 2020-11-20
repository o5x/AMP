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

    var hash: ByteArray = ByteArray(0)
        private set

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

    var image: Bitmap? = null
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

                this.image = if (landscape) {
                    val start: Int = (newimage.getWidth() - newimage.getHeight()) / 2
                    Bitmap.createBitmap(newimage, start, 0, newimage.getHeight(), newimage.getHeight(), matrix, true)
                } else {
                    val start: Int = (newimage.getHeight() - newimage.getWidth()) / 2
                    Bitmap.createBitmap(newimage, 0, start, newimage.getWidth(), newimage.getWidth(), matrix, true)
                }
            }
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
}

class SyncList{
    var name: String
    var list: ArrayList<Int>

    constructor(name_ : String)
    {
        list =  ArrayList()
        name = name_
    }

    constructor(name_ : String, cursor: Cursor)
    {
        list =  ArrayList()
        name = name_
        for (i in 0 until cursor.count)
        {
            list.add(cursor.getInt(0))
            cursor.moveToNext()
        }
    }
}


class SyncMusicController : Application() {

    private val ID_MUSIC_ALL = 0
    private val ID_MUSIC_QUEUE = 1
    private val ID_MUSIC_LIKED = 2
    private val ID_MUSIC_MOST = 3
    private val ID_MUSIC_SUGGEST = 4
    private val ID_MUSIC_DOWNLOAD = 5

    private val forbiddenPlaylistsIds = arrayOf(ID_MUSIC_ALL,
            ID_MUSIC_MOST,
            ID_MUSIC_SUGGEST,
            ID_MUSIC_DOWNLOAD)

    lateinit var c : Context
        private set

    lateinit var db : MusicDB

    lateinit var musics: ArrayList<SyncMusic>
        private set

    lateinit var lists: ArrayList<SyncList>
        private set

    var player: MediaPlayer = MediaPlayer()
        private set

    var currentQueueId: Int = -1
        private set
    var currentMusicId: Int = -1
        private set

    var currentMusic: SyncMusic = SyncMusic()
        get() = if(musics.isNotEmpty() && currentMusicId >= 0)musics[currentMusicId] else SyncMusic()

    var list_all : ArrayList<Int> = ArrayList()
        get() = lists[ID_MUSIC_ALL].list
        private set

    var list_queue : ArrayList<Int> = ArrayList()
        get() = lists[ID_MUSIC_QUEUE].list
        private set

    var list_liked : ArrayList<Int> = ArrayList()
        get() = lists[ID_MUSIC_LIKED].list
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

    fun init(context: Context)
    {
        c = context

        db = MusicDB(c)
        db.open()

        musics = db.getAllMusics()

        val listsId = db.getLists()

        lists = ArrayList()

        for(listId in listsId)
        {
            val list = db.getListFromId(listId)
            if(list != null)
            {
                Log.d("SyncMusicController", "addlist ${list.name} size ${list.list.size}")
                lists.add(list)
            }
        }

        if(musics.size > 3)
            for (i in 0 until 3)
            {
                addMusicIdToListId(i, ID_MUSIC_QUEUE)
                prepare(0)
            }
    }

    ///////////////////////////////////////// music add

    fun addMusic(f: File) : Boolean
    {
        val newMusic = SyncMusic(f)

        if(db.addMusic(newMusic)) {
            musics.add(newMusic)
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
            if(0 <= nextMusicId && nextMusicId < musics.size)
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
        return currentMusicId in lists[ID_MUSIC_LIKED].list
    }

    fun toggleCurrentMusicLiked()
    {
        if(currentMusicId < 0) return

        if(isCurrentMusicLiked()) removeMusicIdFromListId(currentMusicId,ID_MUSIC_LIKED)
        else addMusicIdToListId(currentMusicId,ID_MUSIC_LIKED)

        updateRequired()
    }

    // DB interface list

    fun addMusicIdToListId(music_id : Int, list_id : Int)
    {
        db.addMusicIdToListId(music_id,list_id)
        lists[list_id].list.add(music_id)
    }

    fun removeMusicIdFromListId(music_id : Int, list_id : Int)
    {
        db.removeMusicIdFromListId(music_id,list_id)
        lists[list_id].list.remove(music_id)
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
        if(musics.isEmpty()) return SyncMusic()
        return musics[list_queue[currentQueueId]]
    }

    fun addToQueue(selection: ArrayList<Int>, duplicates: Boolean = false)
    {
        if(duplicates)
            selection.forEach { s -> list_queue.add(s) }
        else
            selection.forEach { s -> if(s !in list_queue) list_queue.add(s) }
    }

    fun getPlaylistsIds() : ArrayList<Int>
    {
        val playlistIds : ArrayList<Int> = ArrayList()

        for (i in 0 until lists.size)
        {
            if(i in forbiddenPlaylistsIds) continue
            playlistIds.add(i)
        }

        return playlistIds
    }

    fun addToPlaylistDialog(context: Context, selection: ArrayList<Int>, onSuccess: () -> Unit = {}, onCancel: () -> Unit = {})
    {
        val playlistNames : ArrayList<String> = ArrayList()
        val playlistIds : ArrayList<Int> = ArrayList()

        for (i in 0 until lists.size)
        {
            if(i in forbiddenPlaylistsIds) continue
            playlistIds.add(i)
            playlistNames.add(lists[i].name)
        }

        val li2 = playlistNames.toTypedArray()

        val mBuilder = AlertDialog.Builder(context)

        if(selection.size == 1)mBuilder.setTitle("Add " + musics[selection[0]].title + " to:")
        else mBuilder.setTitle("Add " + selection.size.toString() + " songs to:")

        mBuilder.setSingleChoiceItems(li2, -1) { dialogInterface, i ->

            if(selection.size == 1)Toast.makeText(context,
                    musics[selection[0]].title + " added to " + li2[i],
                    Toast.LENGTH_LONG).show()

            else Toast.makeText(context,
                    selection.size.toString() + " songs added to " + li2[i],
                    Toast.LENGTH_LONG).show()

            for (s in selection)
            {
                addMusicIdToListId(s,i)
            }

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
}

class Music {

    // TODO initialize image only when required ?

    var path: String = ""
    var artist: String = "Unknown Artist"
    var title: String = "Unknown Title"
    var album: String = "Unknown Album"
    var imageByte: ByteArray? = null
    var image: Bitmap? = null

    private fun initImage() : Bitmap?
    {
        metaRetriever.setDataSource(path)

        imageByte = metaRetriever.embeddedPicture
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
        return null
    }

    val imageAfter: Bitmap? by lazy {
        initImage() as Bitmap?
    }


    var valid: Boolean = false
    var initialized = false
// TODO postinint lazy
    /*var image2 by Lazy<Bitmap>{

    }// post init*/
    constructor(path_:String, title_:String, artist_:String, album_:String, valid_ : Boolean)
    {
        path = path_
        title = title_
        artist = artist_
        album = album_
        valid = valid_
    }

    constructor(f: File) {
        try {

            path = f.path

            metaRetriever.setDataSource(path)
            //metaRetriever.hashCode()
            artist = if (metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) != null)
                metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST).toString()
            else "Unknown Artist"
            album = if (metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) != null)
                metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM).toString()
            else "Unknown Album"
            title = if (metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) != null)
                metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE).toString()
            else f.nameWithoutExtension

            imageByte = metaRetriever.embeddedPicture
            if (imageByte != null) {
                val bmp = BitmapFactory.decodeByteArray(imageByte, 0, imageByte!!.size)
                val newimage = Bitmap.createBitmap(bmp)

                val IMAGE_SIZE = 400
                val landscape: Boolean = newimage.getWidth() > newimage.getHeight()

                val scale_factor: Float
                if (landscape) scale_factor = IMAGE_SIZE.toFloat() / newimage.getHeight() else scale_factor = IMAGE_SIZE.toFloat() / newimage.getWidth()
                val matrix = Matrix()
                matrix.postScale(scale_factor, scale_factor)

                image = if (landscape) {
                    val start: Int = (newimage.getWidth() - newimage.getHeight()) / 2
                    Bitmap.createBitmap(newimage, start, 0, newimage.getHeight(), newimage.getHeight(), matrix, true)
                } else {
                    val start: Int = (newimage.getHeight() - newimage.getWidth()) / 2
                    Bitmap.createBitmap(newimage, 0, start, newimage.getWidth(), newimage.getWidth(), matrix, true)
                }
            }

            initialized = true
            valid = true
        }
        catch (exception: Exception)
        {
            Log.w("fileScan", "Invalid Music = " + f.path)
        }
    }
}

class Playlist(var name: String) {

    val musics: ArrayList<Int> = ArrayList()

    fun add(id: Int)
    {
        if(id !in musics) musics.add(id)
    }

    fun add(ids: ArrayList<Int>)
    {
        ids.forEach{ id -> add(id) }
    }

    fun remove(id: Int)
    {
        if(musics.indexOf(id) != -1) musics.remove(id)
    }

    fun contains(id: Int) : Boolean{
        return id in musics
    }

    fun toggle(id: Int) {
        if(contains(id)) remove(id)
        else add(id)
    }
}


// TODO add home variables (recently, suggested)



class MusicController : Application() {

    lateinit var c : Context

    // Player variables
    var player: MediaPlayer = MediaPlayer()
        private set

    var isQueuePlaying: Boolean = false
        private set

    var currentMusicId: Int = 0
        private set

    val currentMusic: Music
        get() = musics[currentMusicId]

    var shuffleMode: Boolean = false
        private set

    var repeatMode: Repeat = Repeat.None
        private set

    var isPlaying : Boolean = false
        get() = player.isPlaying
        private set

    var playingFrom = "queue"

    // All musics array
    var musics: ArrayList<Music> = ArrayList()
    var musicsPaths: ArrayList<String> = ArrayList()

    // Queue musics array
    val queue: ArrayList<Int> = ArrayList()
    var queueMusicId: Int = 0;

    // all playlist containint liked songs
    val playlist : ArrayList<Playlist> = ArrayList()

    // InitContext
    fun init(context: Context)
    {
        c = context

        playlist.add(Playlist("liked")) // initial playlist
        playlist.add(Playlist("myPlaylist")) // to remove
    }

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

    // Song over callback
    private fun songOver()
    {
        if(repeatMode == Repeat.Once)
        {
            restartMusic()
            return
        }
        next()
    }

    private fun updateRequired()
    {
        c.sendBroadcast(Intent("com.example.musictest.Update_Music")
                .putExtra("actionname", "update"))
    }

    fun setQueueId(ids: ArrayList<Int>)
    {
        queue.clear()

        ids.forEach{ f ->
            queue.add(f)
        }

        prepare(0)
    }

    fun addToQueue(selection: ArrayList<Int>, duplicates: Boolean = false)
    {
        if(duplicates)
        {
            selection.forEach { s -> queue.add(s) }
        }
        else
        {
            selection.forEach { s -> if(s !in queue) queue.add(s) }
        }
    }

    fun setQueueFiles(files: ArrayList<File>)
    {
        queue.clear()

        files.forEach{ f ->

            if(f.path in musicsPaths)
            {
                val musicIndex = musicsPaths.indexOf(f.path)
                queue.add(musicIndex)
            }
            else
            {
                queue.add(musics.size)
                musics.add(Music(f))
                musicsPaths.add(f.path)
            }
        }

        Log.w("LoadMusics", "musics = " + musics.size.toString())
        Log.w("LoadMusics", "queue = " + queue.size.toString())

        prepare(0)
    }

    /*fun addMusic(f: File) : Boolean
    {
        if(f.path !in musicsPaths)
        {

            val newMusic = Music(f)
            if(newMusic.valid)
            {
                musics.add(newMusic)

                val musicDB = MusicDB(c)
                musicDB.open()
                musicDB.addMusic(newMusic)
                musicDB.close()

                musicsPaths.add(f.path)
                return true
            }
        }
        return false
    }*/

    private fun prepare(newQueueMusicId: Int)
    {
        if(0 <= newQueueMusicId && newQueueMusicId < queue.size)
        {
            queueMusicId = newQueueMusicId
            val nextMusicId = queue[queueMusicId]
            if(0 <= nextMusicId && nextMusicId < musics.size)
            {
                currentMusicId = nextMusicId
                player.reset()
                player.setDataSource(currentMusic.path)
                player.prepare()
                player.setOnCompletionListener{
                    songOver()
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


    fun next()
    {
        if(filterInput()) return

        Log.w("Next", "Current = " + queueMusicId + " = " + currentMusicId)
        if(queueMusicId == queue.size -1)
        {
            if(repeatMode == Repeat.All) return play(0)
            updateRequired()
            return prepare(0)
        }
        play(queueMusicId + 1)
        Log.w("Next", "after = " + queueMusicId + " = " + currentMusicId)
    }

    private fun restartMusic()
    {
        player.seekTo(0)
        player.start()
    }



    fun addToPlaylistDialog(context: Context, selection: ArrayList<Int>, onSuccess: () -> Unit = {}, onCancel: () -> Unit = {})
    {
        val playlistNames : ArrayList<String> = ArrayList()

        for (p in playlist)
        {
            playlistNames.add(p.name)
        }

        val li2 = playlistNames.toTypedArray()

        val mBuilder = AlertDialog.Builder(context)

        if(selection.size == 1)mBuilder.setTitle("Add" + musics[selection[0]].title + " to:")
        else mBuilder.setTitle("Add" + selection.size.toString() + " to:")

        mBuilder.setSingleChoiceItems(li2, -1) { dialogInterface, i ->

            if(selection.size == 1)Toast.makeText(context,
                    musics[selection[0]].title + " added to " + li2[i],
                    Toast.LENGTH_LONG).show()

            else Toast.makeText(context,
                    selection.size.toString() + " songs added to " + li2[i],
                    Toast.LENGTH_LONG).show()

            playlist[i].add(selection)

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


}