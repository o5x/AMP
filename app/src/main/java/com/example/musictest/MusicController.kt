package com.example.musictest

import android.R.attr.bitmap
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.util.Log
import kotlinx.android.synthetic.main.activity_music_controller.view.*
import java.io.File


enum class Repeat {
    None, All, Once
}


class Music
{
    var path: String
    var artist: String
    var title: String
    var album: String
    var imageByte: ByteArray?  = null
    var image: Bitmap? = null

    constructor(f: File)
    {
        path = f.path

        val metaRetriever = MediaMetadataRetriever()
        metaRetriever.setDataSource(path)

        metaRetriever.hashCode()

        artist = if(metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) != null)
            metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST).toString()
        else "Unknown Artist"

        album = if(metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) != null)
            metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM).toString()
        else "Unknown Album"

        title = if(metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) != null)
            metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE).toString()
        else f.nameWithoutExtension

        imageByte = metaRetriever.embeddedPicture

        if(imageByte != null)
        {
            val bmp = BitmapFactory.decodeByteArray(imageByte, 0, imageByte!!.size)
            var newimage = Bitmap.createBitmap(bmp)

            val IMAGE_SIZE = 200
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
    }
}

class MusicController : Application() {

    private lateinit var c : Context

    // Player variables
    var player: MediaPlayer = MediaPlayer()
        private set

    var isQueuePlaying: Boolean = false
        private set

    private var currentMusicId: Int = 0
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

    // All musics array
    val musics: ArrayList<Music> = ArrayList()
    val musicsPaths: ArrayList<String> = ArrayList()

    // Queue musics array
    val queue: ArrayList<Int> = ArrayList()
    var queueMusicId: Int = 0;

    // Favourite musics array
    val favourites: ArrayList<Int> = ArrayList()

    // Playlist 1 music array
    val playlist1: ArrayList<Int> = ArrayList()

    // InitContext
    fun init(context: Context)
    {
        c = context
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
        playlist1.clear()
        favourites.clear()

        var id = 0

        files.forEach{ f ->

            if(id % 2 == 0) playlist1.add(musics.size)
            if(id % 3 != 0) queue.add(musics.size)
            if(id % 2 != 0)favourites.add(musics.size)
            musics.add(Music(f))
            musicsPaths.add(f.path)

            id++
        }

        Log.w("LoadMusics", "musics = " + musics.size.toString())
        Log.w("LoadMusics", "queue = " + queue.size.toString())
        Log.w("LoadMusics", "favourites = " + favourites.size.toString())
        Log.w("LoadMusics", "playlist1 = " + playlist1.size.toString())

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

    fun setQueueFiles(files: ArrayList<File>)
    {
        queue.clear()

        files.forEach{ f ->

            if(f.path in musicsPaths)
            {
                var musicIndex = musicsPaths.indexOf(f.path)
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

    private fun prepare(newQueueMusicId: Int)
    {
        if(0 <= newQueueMusicId && newQueueMusicId < queue.size)
        {
            queueMusicId = newQueueMusicId
            var nextMusicId = queue[queueMusicId]
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
        return currentMusicId in favourites
    }

    fun toggleFavourite()
    {
        if(currentMusicId in favourites)
        {
            favourites.remove(Integer.valueOf(currentMusicId))
        }
        else
        {
            favourites.add(currentMusicId)
        }
        updateRequired()
    }

    fun togglePlay()
    {
        if(isPlaying) player.pause()
        else player.start()
        isQueuePlaying = true
        updateRequired()
    }

    fun next()
    {
        if(queueMusicId == queue.size -1)
        {
            if(repeatMode == Repeat.All) return play(0)
            updateRequired()
            return prepare(0)
        }
        play(queueMusicId + 1)
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
        // TODO manage shuffle

        //if(player.currentPosition > 4000) return restartMusic()

        if(queueMusicId == 0)
        {
            if(repeatMode == Repeat.All) return play(queue.size - 1)
            return restartMusic()
        }

        play(queueMusicId - 1)
    }
}