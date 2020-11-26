package com.example.musictest.musics

import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.util.Log
import com.example.musictest.activities.syncMusicController
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.security.MessageDigest

class SyncMusic {
    var path: String = ""
        private set

    val id : Int = 0

    val IMAGE_SIZE = 500

    var hash: ByteArray? = null
        private set

    var title: String? = null
        get() = if (field == null) "Unknown title" else field
        private set

    var artist: String? = null
        get() = if (field == null) "Unknown artist" else field
        private set

    var artistId: Int? = null

    var album: String? = null
        get() = if (field == null) "Unknown album" else field
        private set

    var albumId: Int? = null

    private var imageId: Int? = null

    var valid = false
        private set

    private fun fileToSHA(filePath: String?): ByteArray? {
        var inputStream: InputStream? = null
        return try {
            inputStream = FileInputStream(filePath)
            val buffer = ByteArray(1024)
            val digest: MessageDigest = MessageDigest.getInstance("SHA-256")
            var numRead = 0
            while (numRead != -1) {
                numRead = inputStream.read(buffer)
                if (numRead > 0) digest.update(buffer, 0, numRead)
            }
            return digest.digest()
        } catch (e: java.lang.Exception) {
            null
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close()
                } catch (e: java.lang.Exception) {
                }
            }
        }
    }


    private fun createThumbnail(byteArray: ByteArray) : Bitmap{
        val bmp = BitmapFactory.decodeByteArray(byteArray, 0, byteArray!!.size)
        val newImage = Bitmap.createBitmap(bmp)

        val landscape: Boolean = newImage.width > newImage.height

        val scaleFactor = if (landscape) IMAGE_SIZE.toFloat() / newImage.height else IMAGE_SIZE.toFloat() / newImage.width
        val matrix = Matrix()
        matrix.postScale(scaleFactor, scaleFactor)

        return if (landscape) {
            val start: Int = (newImage.width - newImage.height) / 2
            Bitmap.createBitmap(newImage, start, 0, newImage.height, newImage.height, matrix, true)
        } else {
            val start: Int = (newImage.height - newImage.width) / 2
            Bitmap.createBitmap(newImage, 0, start, newImage.width, newImage.width, matrix, true)
        }
    }

    constructor(f: File) {
        try {
            this.path = f.path

            if (!f.exists()) {
                valid = false
                Log.w("MusicController", "Invalid file = " + f.path)
                return
            }
            val metaRetriever = MediaMetadataRetriever()

            metaRetriever.setDataSource(path)

            this.title = if (metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) != null)
                metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            else f.nameWithoutExtension

            this.artist = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            this.album = metaRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)

            val byteArray = metaRetriever.embeddedPicture

            if (byteArray != null) image2 = createThumbnail(byteArray)

            hash = fileToSHA(path)

            metaRetriever.close()

            this.valid = true

        } catch (exception: Exception) {
            this.valid = false
            Log.w("MusicController", "Invalid music = " + f.path)
        }
    }

    constructor(cursor: Cursor) {
        //id = cursor.getInt(0)
        this.valid = cursor.getInt(1) > 0
        this.path = cursor.getString(2)
        this.title = cursor.getString(3)
        this.artist = cursor.getString(4)
        this.artistId = cursor.getInt(5)
        this.album = cursor.getString(6)
        this.albumId = cursor.getInt(7)
        this.imageId = cursor.getInt(8)
    }

    constructor()

    var image2: Bitmap? = null

    val image: Bitmap?
        get() = syncMusicController.images[imageId]
}
