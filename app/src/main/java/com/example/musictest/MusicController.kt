package com.example.musictest

import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.content.Intent
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
    constructor(path_:String, title_:String, artist_:String, album_:String)
    {
        path = path_
        title = title_
        artist = artist_
        album = album_
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

    fun remove(ids: ArrayList<Int>)
    {
        ids.forEach{ id -> remove(id) }
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

    fun toggleShuffle()
    {
        shuffleMode = !shuffleMode
        updateRequired()
    }

    fun getMusicFromQueueId(queueId: Int) : Music
    {
        return musics[queue[queueId]]
    }

    fun setMusicsTest(files: ArrayList<File>)
    {
        musics.clear()
        queue.clear()

        var id = 0

        files.forEach{ f ->

            if(id % 2 == 0) playlist[0].add(musics.size)
            if(id % 3 != 0) queue.add(musics.size)
            if(id < 7) playlist[1].add(musics.size)
            musics.add(Music(f))
            musicsPaths.add(f.path)

            id++
        }

        prepare(0)
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

    fun addMusic(f: File) : Boolean
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
    }

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

    fun isFavourite() : Boolean
    {
        return playlist[0].contains(currentMusicId)
    }

    fun toggleFavourite()
    {
        if(filterInput()) return

        playlist[0].toggle(currentMusicId)

        updateRequired()
    }

    fun togglePlay()
    {
        if(filterInput()) return

        if(isPlaying) player.pause()
        else player.start()
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

    fun toggleRepeat()
    {
        repeatMode = when (repeatMode) {
            Repeat.None -> Repeat.All
            Repeat.All -> Repeat.Once
            Repeat.Once -> Repeat.None
        }
        updateRequired()
    }

    fun prev()
    {
        if(filterInput()) return
        // TODO manage shuffle

        if(player.currentPosition > 4000) return restartMusic()

        if(queueMusicId == 0)
        {
            if(repeatMode == Repeat.All) return play(queue.size - 1)
            return restartMusic()
        }

        play(queueMusicId - 1)
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

    fun restore() : Boolean
    {
        val musicDB = MusicDB(c)
        musicDB.open()

        val list = musicDB.getMusicsIdFromList(0)

        Log.w("DBmusic", "${list.size} musics to restore ")

        for(i in list)
        {
            musics.add(musicDB.getMusic(i)!!)
        }

        musicDB.close()

        return true
    }
}