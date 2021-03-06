package com.arrol.amp.musics

import android.content.ContentValues
import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.arrol.amp.R
import com.arrol.amp.activities.smc
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import kotlin.jvm.Throws

//data/data/com.example.musictest/databases/musics.db

class ListId {
    companion object {
        const val ID_MUSIC_ALL = 1
        const val ID_MUSIC_QUEUE = 2
        const val ID_MUSIC_QUEUE_ORIGINAL = 3
        const val ID_MUSIC_LIKED = 4
        const val ID_MUSIC_LIST_LIKED = 5
        const val ID_MUSIC_MOST = 6
        const val ID_MUSIC_RECENT_MUSICS = 7
        const val ID_MUSIC_RECENT_LISTS = 8
        const val ID_MUSIC_SUGGEST = 9
        const val ID_MUSIC_DOWNLOAD = 10

        const val ID_MUSIC_ARTISTS = 11
        const val ID_MUSIC_ALBUMS = 12

        const val ID_MUSIC_USER_PLAYLISTS = 13

        const val ID_MUSIC_MAX_ID = 14
    }
}


class ImageId {
    companion object {
        const val ID_IMAGE_ALBUM = 1
        const val ID_IMAGE_ALL = 2
        const val ID_IMAGE_ARTIST = 3
        const val ID_IMAGE_DOWNLOAD = 4
        const val ID_IMAGE_FILE = 5
        const val ID_IMAGE_FOLDER = 6
        const val ID_IMAGE_LIKED = 7
        const val ID_IMAGE_MOST = 8
        const val ID_IMAGE_MUSIC = 9
        const val ID_IMAGE_NO_MUSIC = 10
        const val ID_IMAGE_PLAYLIST = 11
        const val ID_IMAGE_QUEUE = 12
        const val ID_IMAGE_SUGGEST = 13
        const val ID_IMAGE_VIDEO = 14
    }
}


class DBHelper(context: Context?) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    var wasCreatedNow = false

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_MUSIC)
        db.execSQL(CREATE_TABLE_LIST)
        db.execSQL(CREATE_TABLE_LINK)
        db.execSQL(CREATE_TABLE_IMAGE)
        db.execSQL(CREATE_TABLE_STAT)
        wasCreatedNow = true
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_MUSIC")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LIST")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LINK")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_IMAGE")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_STAT")
        onCreate(db)
    }

    companion object {
        // Table Name
        const val TABLE_MUSIC = "MUSICS"
        const val TABLE_LIST = "LISTS"
        const val TABLE_LINK = "LINKS"
        const val TABLE_IMAGE = "IMAGES"
        const val TABLE_STAT = "STATS"

        // stats columns
        const val STAT_ID = "id"
        const val STAT_MUSIC_ID = "music_id"
        const val STAT_PLAYED_COUNT = "played_count"
        const val STAT_PLAYED_LAST = "played_last"
        const val STAT_PLAYED_TIME = "played_time"
        const val STAT_ADDED_TIME = "added_time" // redundant with list link time in all

        // musics columns
        const val MUSIC_ID = "id"
        const val MUSIC_PATH = "path"
        const val MUSIC_HASH = "hash"
        const val MUSIC_IS_VALID = "valid"
        const val MUSIC_TITLE = "title"
        const val MUSIC_ARTIST_ID = "artist_id"
        const val MUSIC_ALBUM_ID = "album_id"
        const val MUSIC_IMAGE_ID = "image_id"

        // lists columns
        const val LIST_ID = "id"
        const val LIST_NAME = "name"
        const val LIST_CONTENT = "content"
        const val LIST_TYPE = "type"
        const val LIST_READONLY = "readonly"
        const val LIST_DELETABLE = "deletable"
        const val LIST_PLAYED_COUNT = "played_count"
        const val LIST_PLAYED_LAST = "played_last"
        const val LIST_SORT_MODE = "sort_mode"
        const val LIST_SORT_LOCKED = "sort_locked"
        const val LIST_AUTHOR_ID = "author_id" // points on another list containing author name // TODO this
        const val LIST_IMAGE_ID = "image_id"

        // links columns
        const val LINK_ID = "id"
        const val LINK_LIST_ID = "list_id"
        const val LINK_TARGET_ID = "target_id"
        const val LINK_ADD_TIME = "date"

        // images columns
        const val IMAGE_ID = "id"
        const val IMAGE_HASH = "hash"
        const val IMAGE_DATA = "data"

        // Database Information
        const val DB_NAME = "musics.db"

        // database version
        const val DB_VERSION = 1

        // Creating table query
        const val CREATE_TABLE_MUSIC =
            ("CREATE TABLE IF NOT EXISTS $TABLE_MUSIC(" +
                    "$MUSIC_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    " $MUSIC_PATH TEXT NOT NULL," +
                    " $MUSIC_HASH BLOB," +
                    " $MUSIC_IS_VALID BOOLEAN," +
                    " $MUSIC_TITLE TEXT," +
                    " $MUSIC_ARTIST_ID INTEGER NOT NULL," +
                    " $MUSIC_ALBUM_ID INTEGER NOT NULL," +
                    " $MUSIC_IMAGE_ID INTEGER);")

        const val CREATE_TABLE_LIST =
            ("CREATE TABLE IF NOT EXISTS $TABLE_LIST(" +
                    "$LIST_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    " $LIST_NAME TEXT NOT NULL," +
                    " $LIST_CONTENT TEXT NOT NULL," +
                    " $LIST_TYPE TEXT NOT NULL," +
                    " $LIST_READONLY BOOLEAN NOT NULL DEFAULT 1," +
                    " $LIST_DELETABLE BOOLEAN NOT NULL DEFAULT 1," +
                    " $LIST_PLAYED_COUNT INTEGER NOT NULL DEFAULT 0," +
                    " $LIST_PLAYED_LAST DATETIME," +
                    " $LIST_SORT_MODE TEXT NOT NULL," +
                    " $LIST_SORT_LOCKED BOOLEAN NOT NULL," +
                    " $LIST_AUTHOR_ID BOOLEAN," +
                    " $LIST_IMAGE_ID INTEGER);")

        const val CREATE_TABLE_LINK =
            ("CREATE TABLE IF NOT EXISTS $TABLE_LINK(" +
                    "$LINK_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    " $LINK_LIST_ID INTEGER NOT NULL," +
                    " $LINK_TARGET_ID INTEGER NOT NULL," +
                    " $LINK_ADD_TIME DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP);")

        const val CREATE_TABLE_IMAGE =
            ("CREATE TABLE IF NOT EXISTS $TABLE_IMAGE(" +
                    "$IMAGE_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    " $IMAGE_HASH BLOB NOT NULL," +
                    " $IMAGE_DATA BLOB NOT NULL);")

        const val CREATE_TABLE_STAT =
            ("CREATE TABLE IF NOT EXISTS $TABLE_STAT(" +
                    "$STAT_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                    " $STAT_MUSIC_ID INTEGER NOT NULL," +
                    " $STAT_PLAYED_COUNT INTEGER DEFAULT 0," +
                    " $STAT_PLAYED_LAST DATETIME," +
                    " $STAT_PLAYED_TIME NUMERIC DEFAULT 0," +
                    " $STAT_ADDED_TIME DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP);")
    }
}

class MusicDB(private val context: Context) {
    private lateinit var dbHelper: DBHelper
    private lateinit var database: SQLiteDatabase

    @Throws(SQLException::class)
    fun open(): MusicDB {

        //clear()

        dbHelper = DBHelper(context)
        database = dbHelper.writableDatabase

        // Initialize db with default playlists when created
        if (dbHelper.wasCreatedNow) {

            val options = BitmapFactory.Options()
            options.inScaled = false

            val res = context.resources
            addImageToDB(BitmapFactory.decodeResource(res, R.drawable.album, options))
            addImageToDB(BitmapFactory.decodeResource(res, R.drawable.all, options))
            addImageToDB(BitmapFactory.decodeResource(res, R.drawable.artist, options))
            addImageToDB(BitmapFactory.decodeResource(res, R.drawable.download, options))
            addImageToDB(BitmapFactory.decodeResource(res, R.drawable.file, options))
            addImageToDB(BitmapFactory.decodeResource(res, R.drawable.folder, options))
            addImageToDB(BitmapFactory.decodeResource(res, R.drawable.liked, options))
            addImageToDB(BitmapFactory.decodeResource(res, R.drawable.most, options))
            addImageToDB(BitmapFactory.decodeResource(res, R.drawable.music, options))
            addImageToDB(BitmapFactory.decodeResource(res, R.drawable.nomusic, options))
            addImageToDB(BitmapFactory.decodeResource(res, R.drawable.playlist, options))
            addImageToDB(BitmapFactory.decodeResource(res, R.drawable.queue, options))
            addImageToDB(BitmapFactory.decodeResource(res, R.drawable.suggest, options))
            addImageToDB(BitmapFactory.decodeResource(res, R.drawable.video, options))

            addList("All Songs", ListType.System, ListContent.ListOfMusics, ImageId.ID_IMAGE_ALL,
                readonly = true, deletable = false, sortMode = SortMode.Date, sortLocked = false)
            addList("Queue", ListType.System, ListContent.ListOfMusics, ImageId.ID_IMAGE_QUEUE,
                readonly = false, deletable = false, sortMode = SortMode.Date, sortLocked = true)
            addList("OriginalQueue", ListType.System, ListContent.ListOfMusics, ImageId.ID_IMAGE_QUEUE,
                readonly = true, deletable = false, sortMode = SortMode.Date, sortLocked = true)
            addList("Liked Musics", ListType.System, ListContent.ListOfMusics, ImageId.ID_IMAGE_LIKED,
                readonly = false, deletable = false, sortMode = SortMode.DateR, sortLocked = false)
            addList("Liked Lists", ListType.System, ListContent.ListOfLists, ImageId.ID_IMAGE_SUGGEST,
                readonly = false, deletable = false, sortMode = SortMode.DateR, sortLocked = false)
            addList("Most Played", ListType.System, ListContent.ListOfMusics, ImageId.ID_IMAGE_MOST,
                readonly = true, deletable = false, sortMode = SortMode.Date, sortLocked = true)
            addList("Recent Musics", ListType.System, ListContent.ListOfMusics, ImageId.ID_IMAGE_MOST,
                readonly = true, deletable = false, sortMode = SortMode.DateR, sortLocked = true)
            addList("Recent Lists", ListType.System, ListContent.ListOfLists, ImageId.ID_IMAGE_MOST,
                readonly = true, deletable = false, sortMode = SortMode.DateR, sortLocked = true)
            addList("Suggested", ListType.System, ListContent.ListOfMusics, ImageId.ID_IMAGE_SUGGEST,
                readonly = true, deletable = false, sortMode = SortMode.Date, sortLocked = true)
            addList("Downloads", ListType.System, ListContent.ListOfMusics, ImageId.ID_IMAGE_DOWNLOAD,
                readonly = true, deletable = false, sortMode = SortMode.DateR, sortLocked = false)

            addList("Artists", ListType.System, ListContent.ListOfLists, ImageId.ID_IMAGE_ARTIST,
                readonly = true, deletable = false, sortMode = SortMode.Name, sortLocked = false)
            addList("Albums", ListType.System, ListContent.ListOfLists, ImageId.ID_IMAGE_ALBUM,
                readonly = true, deletable = false, sortMode = SortMode.Name, sortLocked = false)

            addList("You", ListType.System, ListContent.ListOfLists, ImageId.ID_IMAGE_PLAYLIST,
                readonly = true, deletable = false, sortMode = SortMode.Name, sortLocked = false)

            addIdToListId(ListId.ID_MUSIC_QUEUE, ListId.ID_MUSIC_USER_PLAYLISTS)
            addIdToListId(ListId.ID_MUSIC_LIKED, ListId.ID_MUSIC_USER_PLAYLISTS)
        }
        return this
    }

    fun close() {
        dbHelper.close()
    }

    //////////////////////////////////////// QUERIES

    fun getAllMusicMaps(): HashMap<Int, SyncMusic> {
        val list = HashMap<Int, SyncMusic>()

        val query = "SELECT MUSICS.*, listArtist.name, listAlbum.name\n" +
                "FROM MUSICS JOIN LISTS as listAlbum, LISTS as listArtist \n" +
                "on listAlbum.id = album_id AND listArtist.id = artist_id"

        val cursor = database.rawQuery(query, null)

        cursor.moveToFirst()
        for (i in 0 until cursor.count) {
            val imgIndex = cursor.getInt(7)
            if (imgIndex > 0 && smc.images[imgIndex] == null) {
                val columns2 = arrayOf(DBHelper.IMAGE_DATA)
                val where2 = DBHelper.IMAGE_ID + " = " + imgIndex
                val cursor2 = database.query(
                    DBHelper.TABLE_IMAGE, columns2, where2,
                    null, null, null, null
                )
                cursor2.moveToFirst()
                if (cursor2.count > 0) {
                    val ba = cursor2.getBlob(0)
                    smc.images[imgIndex] = BitmapFactory.decodeByteArray(
                        ba,
                        0,
                        ba!!.size
                    )
                }
                cursor2.close()
            }

            list[cursor.getInt(0)] = SyncMusic(cursor)
            cursor.moveToNext()
        }
        cursor.close()
        return list
    }

    private fun updateRecentList() {
        clearListId(ListId.ID_MUSIC_RECENT_LISTS)
        database.execSQL(
            "INSERT INTO ${DBHelper.TABLE_LINK} " +
                    "(${DBHelper.LINK_LIST_ID},${DBHelper.LINK_TARGET_ID}) " +
                    "SELECT ${ListId.ID_MUSIC_RECENT_LISTS} ,${DBHelper.LINK_ID}" +
                    " FROM ${DBHelper.TABLE_LIST} " +
                    " WHERE ${DBHelper.LIST_PLAYED_COUNT} > 0" +
                    " AND ${DBHelper.LIST_ID} != ${ListId.ID_MUSIC_QUEUE}" +
                    " ORDER BY ${DBHelper.LIST_PLAYED_LAST} DESC"
        )
    }

    private fun updateRecentMusics() {
        clearListId(ListId.ID_MUSIC_RECENT_MUSICS)
        database.execSQL(
            "INSERT INTO ${DBHelper.TABLE_LINK} " +
                    "(${DBHelper.LINK_LIST_ID},${DBHelper.LINK_TARGET_ID}) " +
                    "SELECT ${ListId.ID_MUSIC_RECENT_MUSICS} ,${DBHelper.STAT_MUSIC_ID}" +
                    " FROM ${DBHelper.TABLE_STAT}" +
                    " WHERE ${DBHelper.STAT_PLAYED_LAST} is not null" +
                    " ORDER BY ${DBHelper.STAT_PLAYED_LAST} DESC"
        )
    }

    private fun updateMostMusics() {
        clearListId(ListId.ID_MUSIC_MOST)
        database.execSQL(
            "INSERT INTO ${DBHelper.TABLE_LINK} " +
                    "(${DBHelper.LINK_LIST_ID},${DBHelper.LINK_TARGET_ID}) " +
                    "SELECT ${ListId.ID_MUSIC_MOST} ,${DBHelper.STAT_MUSIC_ID}" +
                    " FROM ${DBHelper.TABLE_STAT}" +
                    " WHERE ${DBHelper.STAT_PLAYED_TIME} > 0" +
                    " ORDER BY ${DBHelper.STAT_PLAYED_TIME} DESC"
        )
    }

    fun getListFromId(list_id: Int): SyncList? {
        if (list_id == ListId.ID_MUSIC_RECENT_LISTS) updateRecentList()
        if (list_id == ListId.ID_MUSIC_RECENT_MUSICS) updateRecentMusics()
        if (list_id == ListId.ID_MUSIC_MOST) updateMostMusics()

        val columnsList = arrayOf(
            DBHelper.LIST_NAME,
            DBHelper.LIST_CONTENT,
            DBHelper.LIST_TYPE,
            DBHelper.LIST_IMAGE_ID,
            DBHelper.LIST_READONLY,
            DBHelper.LIST_DELETABLE,
            DBHelper.LIST_SORT_MODE,
            DBHelper.LIST_SORT_LOCKED,
            DBHelper.LIST_AUTHOR_ID
        )
        val whereList = "${DBHelper.LIST_ID} = $list_id"
        val cursorList = database.query(
            DBHelper.TABLE_LIST,
            columnsList,
            whereList,
            null,
            null,
            null,
            null
        )
        cursorList.moveToFirst()
        if (cursorList.count == 0) return SyncList()

        val listName = cursorList.getString(0)
        val listContent = ListContent.valueOf(cursorList.getString(1))
        val listType = ListType.valueOf(cursorList.getString(2))
        val imgId = cursorList.getInt(3)

        if (imgId > 0 && smc.images[imgId] == null) {
            val columns2 = arrayOf(DBHelper.IMAGE_DATA)
            val where2 = DBHelper.IMAGE_ID + " = " + imgId
            val cursor2 = database.query(
                DBHelper.TABLE_IMAGE,
                columns2,
                where2,
                null,
                null,
                null,
                null
            )
            cursor2.moveToFirst()
            if (cursor2.count > 0) {
                val ba = cursor2.getBlob(0)
                smc.images[imgId] = BitmapFactory.decodeByteArray(ba, 0, ba!!.size)
            }
            cursor2.close()
        }

        // SPITTED CODE
        if (listContent == ListContent.ListOfLists) {
            // get content from links

            val request = "SELECT ${DBHelper.LINK_TARGET_ID},${DBHelper.LINK_ADD_TIME},${DBHelper.TABLE_LINK}." +
                    "${DBHelper.LINK_ID}," + " ${DBHelper.STAT_PLAYED_COUNT} FROM ${DBHelper.TABLE_LINK} " +
                    "JOIN ${DBHelper.TABLE_LIST} on ${DBHelper.LINK_TARGET_ID} = ${DBHelper.TABLE_LIST}.${
                        DBHelper
                            .LIST_ID
                    }" +
                    " WHERE ${DBHelper.LINK_LIST_ID} = $list_id"

            val cursor = database.rawQuery(request, null)
            cursor.moveToFirst()
            if (cursor.count > 0) {
                val list = SyncList(cursorList,
                    cursor)//SyncList(listName, cursor, listContent,listType, imgId) // add image_id1
                cursor.close()
                return list
            }
            cursor.close()
            cursorList.close()
            return SyncList(listName, listContent, listType, imgId)
        } else if (listContent == ListContent.ListOfMusics) {
            val request = "SELECT ${DBHelper.LINK_TARGET_ID},${DBHelper.LINK_ADD_TIME},${DBHelper.TABLE_LINK}." +
                    "${DBHelper.LINK_ID}," + " ${DBHelper.STAT_PLAYED_COUNT} FROM ${DBHelper.TABLE_LINK} " +
                    "JOIN ${DBHelper.TABLE_STAT} on ${DBHelper.LINK_TARGET_ID} = ${DBHelper.TABLE_STAT}.${
                        DBHelper
                            .STAT_MUSIC_ID
                    }" +
                    " WHERE ${DBHelper.LINK_LIST_ID} = $list_id"

            val cursor = database.rawQuery(request, null)
            cursor.moveToFirst()
            if (cursor.count > 0) {
                val list = SyncList(cursorList, cursor)//SyncList(listName, cursor, listContent,listType, imgId) //
                // add image_id1
                cursor.close()
                cursorList.close()
                return list
            }
        }
        return SyncList(listName, listContent,listType , imgId)
    }

    // adders

    private fun ByteArray.toHexString(): String {
        return this.joinToString("") {
            java.lang.String.format("%02x", it)
        }
    }

    private fun addImageToDB(bitmap: Bitmap): Int {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val ba = stream.toByteArray()

        val digest: MessageDigest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(ba)
        val hashStr = hash.toHexString()

        val where = "${DBHelper.IMAGE_HASH} = x'$hashStr'"
        val columns = arrayOf(DBHelper.IMAGE_ID)
        val cursor = database.query(
            DBHelper.TABLE_IMAGE,
            columns,
            where,
            null,
            null,
            null,
            null
        )
        if (cursor.count == 0) {
            val values = ContentValues()
            values.put(DBHelper.IMAGE_HASH, hash)
            values.put(DBHelper.IMAGE_DATA, ba)
            return database.insert(DBHelper.TABLE_IMAGE, null, values).toInt()
        }
        cursor.moveToFirst()
        val ret = cursor.getInt(0)
        cursor.close()
        return ret
    }

    fun updateStatForMusic(music_id: Int, playedCounter: Int, playedTime: Int) {
        database.execSQL(
            "UPDATE ${DBHelper.TABLE_STAT} SET " +
                    "${DBHelper.STAT_PLAYED_COUNT} = ${DBHelper.STAT_PLAYED_COUNT} + ${if (playedCounter < 1) 0 else 1}," +
                    " ${DBHelper.STAT_PLAYED_LAST} = CURRENT_TIMESTAMP," +
                    " ${DBHelper.STAT_PLAYED_TIME} = ${DBHelper.STAT_PLAYED_TIME} + ${if (playedTime < 0) 0 else playedTime}" +
                    " WHERE ${DBHelper.STAT_MUSIC_ID} = $music_id"
        )

        smc.invalidateList(ListId.ID_MUSIC_RECENT_MUSICS)
        smc.invalidateList(ListId.ID_MUSIC_MOST)
    }

    fun updateStatForList(list_id: Int, playedCounter: Int) {
        database.execSQL(
            "UPDATE ${DBHelper.TABLE_LIST} SET " +
                    "${DBHelper.LIST_PLAYED_COUNT} = ${DBHelper.LIST_PLAYED_COUNT} + ${if (playedCounter < 1) 0 else 1}," +
                    " ${DBHelper.LIST_PLAYED_LAST} = CURRENT_TIMESTAMP" +
                    " WHERE ${DBHelper.LIST_ID} = $list_id"
        )

        smc.invalidateList(ListId.ID_MUSIC_RECENT_LISTS)
    }

    fun setListImage(list_id: Int, bitmap: Bitmap): Int {
        val id = addImageToDB(bitmap)
        val where = DBHelper.LIST_ID + " = " + list_id
        val contentValue = ContentValues()
        contentValue.put(DBHelper.LIST_IMAGE_ID, id)
        database.update(DBHelper.TABLE_LIST, contentValue, where, null)

        return -1
    }

    fun addMusicByPath(f: File): Array<Int> {
        val where = DBHelper.MUSIC_PATH + " = '${f.path.replace("'", "''")}'"
        val columns = arrayOf(DBHelper.MUSIC_ID)
        val cursor = database.query(
            DBHelper.TABLE_MUSIC,
            columns,
            where,
            null,
            null,
            null,
            null
        )
        if (cursor.count == 0) {
            Log.d("addMusic", f.path)
            val music = SyncMusic(f)

            val contentValue = ContentValues()
            contentValue.put(DBHelper.MUSIC_PATH, music.path)
            contentValue.put(DBHelper.MUSIC_HASH, music.hash)
            contentValue.put(DBHelper.MUSIC_IS_VALID, music.valid)
            contentValue.put(DBHelper.MUSIC_TITLE, music.title)

            var artistImageId = ImageId.ID_IMAGE_ARTIST
            var albumImageId = ImageId.ID_IMAGE_ALBUM

            if (music.image2 != null) {
                val imageId = addImageToDB(music.image2!!)
                contentValue.put(DBHelper.MUSIC_IMAGE_ID, imageId)
                artistImageId = imageId
                albumImageId = imageId
            }

            val artistId = addList(
                music.artist.toString(),
                ListType.Artist,
                ListContent.ListOfMusics,
                artistImageId,
                readonly = true,
                deletable = false,
                sortMode = SortMode.Date,
                sortLocked = false
            )
            val albumId = addList(
                music.album.toString(),
                ListType.Album,
                ListContent.ListOfMusics,
                albumImageId,
                readonly = true,
                deletable = false,
                sortMode = SortMode.Date,
                sortLocked = false,
                authorId = artistId
            )


            contentValue.put(DBHelper.MUSIC_ARTIST_ID, artistId)
            contentValue.put(DBHelper.MUSIC_ALBUM_ID, albumId)

            val id = database.insert(DBHelper.TABLE_MUSIC, null, contentValue).toInt()
            smc.invalidateMusics()

            addIdToListId(id, artistId)
            addIdToListId(id, albumId)

            if (music.valid) {
                // add to albums
                addIdToListId(artistId, ListId.ID_MUSIC_ARTISTS)
                addIdToListId(albumId, ListId.ID_MUSIC_ALBUMS)

                // add to all musics
                addIdToListId(id, ListId.ID_MUSIC_ALL)

                // create music stat
                val contentValueStat = ContentValues()
                contentValueStat.put(DBHelper.STAT_MUSIC_ID, id)
                database.insert(DBHelper.TABLE_STAT, null, contentValueStat)

                return arrayOf(id, 1)
            }
            return arrayOf(-1)
        }
        cursor.moveToFirst()
        val id = cursor.getInt(0)
        cursor.close()
        return arrayOf(id)
    }

    ////////////////////////////////////////////////////////////// Setters

    fun setListData(list_id: Int, newData: ArrayList<Int>) {
        clearListId(list_id)

        database.beginTransaction()
        for (data in newData) {
            val contentValue = ContentValues()
            contentValue.put(DBHelper.LINK_LIST_ID, list_id)
            contentValue.put(DBHelper.LINK_TARGET_ID, data)
            database.insert(DBHelper.TABLE_LINK, null, contentValue)
        }
        database.setTransactionSuccessful()
        database.endTransaction()

        smc.invalidateList(list_id)
    }

    //////////////////////////////////////////////////////////////// INSERTS

    fun addList(
        name: String, listType: ListType, listContent: ListContent, imageId: Int, readonly: Boolean,
        deletable:
        Boolean,
        sortMode: SortMode, sortLocked: Boolean, authorId: Int? = null,
    ):
            Int {
        val columns = arrayOf(DBHelper.LIST_ID)
        val where =
            " ${DBHelper.LIST_CONTENT} = '$listContent'" +
                    " AND ${DBHelper.LIST_TYPE} = '$listType'" +
                    " AND ${DBHelper.LIST_NAME} = '${name.replace("'", "''")}'"
        val cursor = database.query(
            DBHelper.TABLE_LIST,
            columns,
            where,
            null,
            null,
            null,
            null
        )
        if (cursor.count == 0) {
            val contentValue = ContentValues()
            contentValue.put(DBHelper.LIST_NAME, name)
            contentValue.put(DBHelper.LIST_CONTENT, listContent.toString())
            contentValue.put(DBHelper.LIST_TYPE, listType.toString())
            contentValue.put(DBHelper.LIST_IMAGE_ID, imageId)

            contentValue.put(DBHelper.LIST_READONLY, readonly)
            contentValue.put(DBHelper.LIST_DELETABLE, deletable)
            contentValue.put(DBHelper.LIST_AUTHOR_ID, authorId)

            contentValue.put(DBHelper.LIST_SORT_MODE, sortMode.toString())
            contentValue.put(DBHelper.LIST_SORT_LOCKED, sortLocked)

            val c = database.insert(DBHelper.TABLE_LIST, null, contentValue)
            //Log.d("MusicDB", c.toString())
            cursor.close()
            smc.invalidateList(c.toInt())
            return c.toInt()
        }
        cursor.moveToFirst()
        val id = cursor.getInt(0)
        cursor.close()
        return id
    }

    fun addIdToListId(id: Int, list_id: Int): Int {
        if (!isIdInListId(id, list_id)) {
            val contentValue = ContentValues()
            contentValue.put(DBHelper.LINK_LIST_ID, list_id)
            contentValue.put(DBHelper.LINK_TARGET_ID, id)

            val idinsert = database.insert(DBHelper.TABLE_LINK, null, contentValue)
            smc.invalidateList(list_id)
            return idinsert.toInt()
        }
        return -1
    }

    //////////////////////////////////////// LIST FUNCTIONS music scan add

    private fun isIdInListId(id: Int, list_id: Int): Boolean {
        val where = DBHelper.LINK_LIST_ID + " = " + list_id + " AND " + DBHelper.LINK_TARGET_ID + " = " + id
        val cursor = database.query(DBHelper.TABLE_LINK, null, where, null, null, null, null)
        val itIs = cursor.count > 0
        cursor.close()
        return itIs
    }

    /////////////////////////////////////////////////// delete

    fun removeIdFromListId(id: Int, list_id: Int) {
        val where = "${DBHelper.LINK_LIST_ID} = $list_id AND ${DBHelper.LINK_TARGET_ID} = $id"
        database.delete(DBHelper.TABLE_LINK, where, null)
        smc.invalidateList(list_id)
    }

    fun clearListId(list_id: Int) {
        database.delete(DBHelper.TABLE_LINK, DBHelper.LINK_LIST_ID + "=" + list_id, null)
        smc.invalidateList(list_id)
    }

    fun deleteListId(list_id: Int) {
        database.delete(DBHelper.TABLE_LIST, DBHelper.LIST_ID + "=" + list_id, null)
        smc.invalidateList(list_id)
        //syncMusicController.invalidateList(MOS)
    }

    /////////////////////////////////////////////////// clear

    private fun clear() {
        context.deleteDatabase(DBHelper.DB_NAME)
    }
}
