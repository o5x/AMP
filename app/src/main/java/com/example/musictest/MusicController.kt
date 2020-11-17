package com.example.musictest

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import java.io.File

enum class Repeat {
    None, All, Once
}

class Music
{
    var file: File? = null

    var path : String = ""
    var artist: String = "No Artist"
    var title: String = "No Title"
    var album: String = "No Album"
    var image: Bitmap? = null
    var imageByte: ByteArray? = null
    var isFavourite : Boolean = false

    constructor()

    constructor(f: File)
    {
        file = f

        path = f.path

        val metaRetriever = MediaMetadataRetriever()
        metaRetriever.setDataSource(f.path)

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

        image = if(imageByte != null){
            val bmp = BitmapFactory.decodeByteArray(imageByte, 0, imageByte!!.size)
            Bitmap.createBitmap(bmp)
        }
        else null
    }
}

class MusicController : Application() {

    // notification
    lateinit var context : Context

    var nothingPlaying = true;

    val musics: ArrayList<Music> = ArrayList()
    var currentMusic : Int = -1

    private var shuffle : Boolean = false
    var repeat : Repeat = Repeat.None

    var player: MediaPlayer = MediaPlayer()

    private fun songOver()
    {
        if(repeat == Repeat.Once)
        {
            restartMusic()
            return
        }

        next()
    }

    private fun updateRequired()
    {
        context.sendBroadcast(Intent("com.example.musictest.Update_Music")
                .putExtra("actionname", "update"))
    }

    fun shuffleToggle()
    {
        shuffle = !shuffle
        updateRequired()
    }

    fun getShuffle() : Boolean
    {
        return shuffle // see with get sets
    }

    fun getCurrentMusic() : Music
    {
        if(currentMusic >= 0) return musics[currentMusic]
        return Music()
    }

    fun init(context_: Context)
    {
        context = context_
    }

    fun setMusics(files: ArrayList<File>)
    {
        musics.clear()
        files.forEach{ n -> musics.add(Music(n)) }
        prepareMusic(0)
    }

   /* fun changeMusic(uri : Uri)
    {
        player.reset()
        player.setDataSource(context, uri)
        player.prepare()
        player.start()
    }*/

    private fun prepareMusic(id: Int)
    {
        if(0 <= id && id < musics.size)
        {
            currentMusic = id
            player.reset()
            player.setDataSource(getCurrentMusic().path)
            player.prepare()
            player.setOnCompletionListener{
                songOver()
            }
            nothingPlaying = true
        }
    }

    fun changeMusic(id: Int)
    {
        prepareMusic(id)
        player.start()
        updateRequired()
        nothingPlaying = false;
    }

    fun isPlaying() : Boolean
    {
        return player.isPlaying
    }

    fun toggleFavourite()
    {
        musics[currentMusic].isFavourite = !musics[currentMusic].isFavourite
        updateRequired()
    }

    fun toggle()
    {
        if(player.isPlaying) player.pause()
        else player.start()
        nothingPlaying = false
        updateRequired()
    }

    fun next()
    {
        if(currentMusic == musics.size -1)
        {
            if(repeat == Repeat.All) return changeMusic(0)
            updateRequired()
            return prepareMusic(0)
        }

        changeMusic(currentMusic + 1)
    }

    fun restartMusic()
    {
        player.seekTo(0)
        player.start()
    }

    fun repeatToggle()
    {
        repeat = when (repeat) {
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

        if(currentMusic == 0)
        {
            if(repeat == Repeat.All) return changeMusic(musics.size -1)
            return restartMusic()
        }

        changeMusic(currentMusic - 1)
    }
}