package com.example.musictest.musics

import android.content.ContentValues
import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.musictest.activities.syncMusicController
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest

//data/data/com.example.musictest/databases/musics.db

enum class ListType {
    None, SystemR, SystemRW, Album, Artist, User, SystemRList, SystemRWList
}

class ListId{
    companion object
    {
        const val ID_MUSIC_ALL = 1
        const val ID_MUSIC_QUEUE = 2
        const val ID_MUSIC_QUEUE_ORIGINAL = 3
        const val ID_MUSIC_LIKED = 4
        const val ID_MUSIC_MOST = 5
        const val ID_MUSIC_RECENT = 6
        const val ID_MUSIC_SUGGEST = 7
        const val ID_MUSIC_DOWNLOAD = 8

        const val ID_MUSIC_ARTISTS = 9
        const val ID_MUSIC_ALBUMS = 10

        const val ID_MUSIC_USER_PLAYLISTS = 11
    }
}


class DBMusicHelper(context: Context?) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

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
        const val STAT_ADDED_TIME = "added_time"

        // musics columns
        const val MUSIC_ID = "id"
        const val MUSIC_PATH = "path"
        const val MUSIC_HASH = "hash"
        const val MUSIC_ISVALID = "valid"
        const val MUSIC_TITLE = "title"
        const val MUSIC_ARTIST = "artist"
        const val MUSIC_ALBUM = "album"
        const val MUSIC_IMAGE_ID = "image_id"

        // lists columns
        const val LIST_ID = "id"
        const val LIST_NAME = "name"
        const val LIST_TYPE = "type"
        const val LIST_PLAYED_COUNT = "played_count"
        const val LIST_PLAYED_LAST = "played_last"

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
                        " $MUSIC_ISVALID BOOLEAN," +
                        " $MUSIC_TITLE TEXT," +
                        " $MUSIC_ARTIST TEXT," +
                        " $MUSIC_ALBUM TEXT," +
                        " $MUSIC_IMAGE_ID INTEGER);")

        const val CREATE_TABLE_LIST =
                ("CREATE TABLE IF NOT EXISTS $TABLE_LIST(" +
                        "$LIST_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                        " $LIST_NAME TEXT NOT NULL," +
                        " $LIST_TYPE TEXT NOT NULL," +
                        " $LIST_PLAYED_COUNT INTEGER DEFAULT 0," +
                        " $LIST_PLAYED_LAST DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP);")

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
    private lateinit var dbHelper: DBMusicHelper
    private lateinit var database: SQLiteDatabase

    @Throws(SQLException::class)
    fun open(): MusicDB {

        //clear()

        dbHelper = DBMusicHelper(context)
        database = dbHelper.writableDatabase

        // Initialize db with default playlists when created
        if(dbHelper.wasCreatedNow)
        {
            addList("all", ListType.SystemR)
            addList("queue", ListType.SystemRW)
            addList("originalQueue", ListType.SystemR)
            addList("liked", ListType.SystemRW)
            addList("most", ListType.SystemR)
            addList("recent", ListType.SystemR)
            addList("suggest", ListType.SystemR)
            addList("download", ListType.SystemR)

            addList("artists", ListType.SystemRList)
            addList("albums", ListType.SystemRList)

            addList("userPlaylists", ListType.SystemRWList)
            addIdToListId(ListId.ID_MUSIC_QUEUE, ListId.ID_MUSIC_USER_PLAYLISTS)
            addIdToListId(ListId.ID_MUSIC_LIKED, ListId.ID_MUSIC_USER_PLAYLISTS)
        }
        return this
    }

    fun close() {
        dbHelper.close()
    }

    //////////////////////////////////////// QUERIES

    fun getAllMusicMaps() : HashMap<Int, SyncMusic>
    {
        val list = HashMap<Int, SyncMusic>()
        val columns = arrayOf(DBMusicHelper.MUSIC_ID, DBMusicHelper.MUSIC_ISVALID, DBMusicHelper.MUSIC_PATH, DBMusicHelper.MUSIC_TITLE, DBMusicHelper.MUSIC_ARTIST, DBMusicHelper.MUSIC_ALBUM, DBMusicHelper.MUSIC_IMAGE_ID)

        val cursor = database.query(DBMusicHelper.TABLE_MUSIC, columns, null, null, null, null, null)
        cursor.moveToFirst()
        for (i in 0 until cursor.count)
        {
            ///*var ba : ByteArray?= null
            val imgIndex = cursor.getInt(6)
            if(imgIndex > 0 && syncMusicController.images[imgIndex] == null)
            {
                val columns2 = arrayOf(DBMusicHelper.IMAGE_DATA)
                val where2 = DBMusicHelper.IMAGE_ID + " = " + cursor.getInt(6).toString()
                val cursor2 = database.query(DBMusicHelper.TABLE_IMAGE, columns2, where2, null, null, null, null)
                cursor2.moveToFirst()
                if(cursor2.count > 0)
                {
                    val ba = cursor2.getBlob(0)
                    syncMusicController.images[imgIndex] = BitmapFactory.decodeByteArray(ba, 0, ba!!.size)
                }
            }//*/

            list[cursor.getInt(0)] = SyncMusic(cursor)
            cursor.moveToNext()
        }
        cursor.close()
        return list
    }

    fun getListFromId(list_id: Int) : SyncList?
    {
        // get info from lists
        val columnsList = arrayOf(DBMusicHelper.LIST_NAME,DBMusicHelper.LIST_TYPE)
        val whereList = "${DBMusicHelper.LIST_ID} = $list_id"
        val cursorList = database.query(DBMusicHelper.TABLE_LIST, columnsList, whereList, null, null, null, null)
        cursorList.moveToFirst()
        if(cursorList.count == 0) return SyncList()
        val listName = cursorList.getString(0)
        val listType: ListType = ListType.valueOf(cursorList.getString(1))

        // get content from links
        val columns = arrayOf(DBMusicHelper.LINK_TARGET_ID)
        val where = "${DBMusicHelper.LINK_LIST_ID} = $list_id"
        val cursor = database.query(DBMusicHelper.TABLE_LINK, columns, where, null, null, null, null)
        cursor.moveToFirst()
        if(cursor.count > 0)
        {
            val list = SyncList(listName, cursor, listType)
            cursor.close()
            return list
        }
        cursor.close()
        return SyncList(listName, listType)
    }

    // adders

    private fun ByteArray.toHexString() : String {
        return this.joinToString("") {
            java.lang.String.format("%02x", it)
        }
    }

    private fun addImageToDB(bitmap: Bitmap) : Int
    {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val ba = stream.toByteArray()

        val digest: MessageDigest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(ba)
        val hashStr = hash.toHexString()

        val where = "${DBMusicHelper.IMAGE_HASH} = x'$hashStr'"
        val columns = arrayOf(DBMusicHelper.IMAGE_ID)
        val cursor = database.query(DBMusicHelper.TABLE_IMAGE, columns, where, null, null, null, null)
        if(cursor.count == 0)
        {
            val values = ContentValues()
            values.put(DBMusicHelper.IMAGE_HASH, hash)
            values.put(DBMusicHelper.IMAGE_DATA, ba)
            return database.insert(DBMusicHelper.TABLE_IMAGE, null, values).toInt()
        }
        cursor.moveToFirst()
        return cursor.getInt(0)
    }

    fun updateStatForMusic(music_id : Int, playedCounter: Int, playedTime: Int)
    {
        database.execSQL("UPDATE STATS SET " +
                "played_count = played_count + ${if (playedCounter < 1) 0 else 1}," +
                " played_last = CURRENT_TIMESTAMP," +
                " played_time = played_time + ${if(playedTime < 0) 0 else playedTime}" +
                " WHERE music_id = $music_id")
    }

    fun updateStatForList(list_id : Int, playedCounter: Int)
    {
        database.execSQL("UPDATE LISTS SET " +
                "played_count = played_count + ${if (playedCounter < 1) 0 else 1}," +
                " played_last = CURRENT_TIMESTAMP," +
                " WHERE id = $list_id")
    }

    fun addMusicByPath(f: File) : Array<Int>
    {
        val where = "${DBMusicHelper.MUSIC_PATH} = '${f.path.replace("'", "''")}'"
        val columns = arrayOf(DBMusicHelper.MUSIC_ID)
        val cursor = database.query(DBMusicHelper.TABLE_MUSIC, columns, where, null, null, null, null)
        if(cursor.count == 0)
        {
            Log.d("addMusic", f.path)
            val music = SyncMusic(f)

            val contentValue = ContentValues()
            contentValue.put(DBMusicHelper.MUSIC_PATH, music.path)
            contentValue.put(DBMusicHelper.MUSIC_HASH, music.hash)
            contentValue.put(DBMusicHelper.MUSIC_ISVALID, music.valid)
            contentValue.put(DBMusicHelper.MUSIC_TITLE, music.title)
            contentValue.put(DBMusicHelper.MUSIC_ARTIST, music.artist)
            contentValue.put(DBMusicHelper.MUSIC_ALBUM, music.album)
            if(music.image2 != null)
            {
                val imageId = addImageToDB(music.image2!!)
                contentValue.put(DBMusicHelper.MUSIC_IMAGE_ID, imageId)
            }
            val id = database.insert(DBMusicHelper.TABLE_MUSIC, null, contentValue).toInt()
            syncMusicController.invalidateMusics()

            if(music.valid)
            {
                // add to all musics
                addIdToListId(id, ListId.ID_MUSIC_ALL)
                val aid = addIdToListNameType(id, music.artist.toString(), ListType.Artist)
                val abid = addIdToListNameType(id, music.album.toString(), ListType.Album)
                addIdToListId(aid, ListId.ID_MUSIC_ARTISTS)
                addIdToListId(abid, ListId.ID_MUSIC_ALBUMS)

                // create music stat
                val contentValueStat = ContentValues()
                contentValueStat.put(DBMusicHelper.STAT_MUSIC_ID, id)
                database.insert(DBMusicHelper.TABLE_STAT, null, contentValueStat)

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

    fun setListData(list_id: Int, newData: ArrayList<Int>)
    {
        clearListId(list_id)

        database.beginTransaction()
        for (data in newData)
        {
            val contentValue = ContentValues()
            contentValue.put(DBMusicHelper.LINK_LIST_ID, list_id)
            contentValue.put(DBMusicHelper.LINK_TARGET_ID, data)
            database.insert(DBMusicHelper.TABLE_LINK, null, contentValue)
        }
        database.setTransactionSuccessful()
        database.endTransaction()

        syncMusicController.invalidateList(list_id)
    }

    //////////////////////////////////////////////////////////////// INSERTS

    private fun addList(name: String, listType: ListType) : Int
    {
        val columns = arrayOf(DBMusicHelper.LIST_ID)
        val where = " ${DBMusicHelper.LIST_TYPE} = '$listType' AND ${DBMusicHelper.LIST_NAME} = '${name.replace("'", "''")}' "
        val cursor = database.query(DBMusicHelper.TABLE_LIST, columns, where, null, null, null, null)
        if(cursor.count == 0)
        {
            val contentValue = ContentValues()
            contentValue.put(DBMusicHelper.LIST_NAME, name)
            contentValue.put(DBMusicHelper.LIST_TYPE, listType.toString())
            val c = database.insert(DBMusicHelper.TABLE_LIST, null, contentValue)
            //Log.d("MusicDB", c.toString())
            cursor.close()
            syncMusicController.invalidateList(c.toInt())
            return c.toInt()
        }
        cursor.moveToFirst()
        val id = cursor.getInt(0)
        cursor.close()
        return id
    }

    fun addIdToListId(id: Int, list_id: Int) : Int
    {
        if(!isIdInListId(id, list_id))
        {
            val contentValue = ContentValues()
            contentValue.put(DBMusicHelper.LINK_LIST_ID, list_id)
            contentValue.put(DBMusicHelper.LINK_TARGET_ID, id)

            val idinsert = database.insert(DBMusicHelper.TABLE_LINK, null, contentValue)
            syncMusicController.invalidateList(list_id)
            return idinsert.toInt()
        }
        return -1
    }

    //////////////////////////////////////// LIST FUNCTIONS music scan add

    private fun addIdToListNameType(id: Int, listName: String, listType: ListType) : Int
    {
        val list_id = addList(listName, listType)

        if(!isIdInListId(id, list_id))
        {
            val contentValue = ContentValues()
            contentValue.put(DBMusicHelper.LINK_LIST_ID, list_id)
            contentValue.put(DBMusicHelper.LINK_TARGET_ID, id)
            database.insert(DBMusicHelper.TABLE_LINK, null, contentValue)
            syncMusicController.invalidateList(list_id)
        }
        return list_id
    }

    private fun isIdInListId(id: Int, list_id: Int) : Boolean
    {
        val where = "${DBMusicHelper.LINK_LIST_ID} = $list_id AND ${DBMusicHelper.LINK_TARGET_ID} = $id"
        val cursor = database.query(DBMusicHelper.TABLE_LINK, null, where, null, null, null, null)
        val itIs = cursor.count > 0
        cursor.close()
        return itIs
    }

    /////////////////////////////////////////////////// delete

    fun removeIdFromListId(id: Int, list_id: Int)
    {
        val where = "${DBMusicHelper.LINK_LIST_ID} = $list_id AND ${DBMusicHelper.LINK_TARGET_ID} = $id"
        database.delete(DBMusicHelper.TABLE_LINK, where, null)
        syncMusicController.invalidateList(list_id)
    }

    fun clearListId(list_id: Int)
    {
        database.delete(DBMusicHelper.TABLE_LINK, DBMusicHelper.LINK_LIST_ID + "=" + list_id, null);
        syncMusicController.invalidateList(list_id)
    }

    /////////////////////////////////////////////////// clear

    private fun clear()
    {
        context.deleteDatabase(DBMusicHelper.DB_NAME);
    }
}
