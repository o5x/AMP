package com.example.musictest.databases

import android.content.ContentValues
import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.musictest.SyncList
import com.example.musictest.SyncMusic
import com.example.musictest.databases.DBMusicHelper.Companion.DB_NAME

// //data/data/com.example.musictest/databases/musics.db

enum class ListType {
     System, Album, Artist, User
}

class DBMusicHelper(context: Context?) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    var wasCreatedNow = false

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_MUSIC)
        db.execSQL(CREATE_TABLE_LIST)
        db.execSQL(CREATE_TABLE_LINK)
        db.execSQL(CREATE_TABLE_IMAGE)
        wasCreatedNow = true
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $MUSIC_TABLE")
        db.execSQL("DROP TABLE IF EXISTS $LIST_TABLE")
        db.execSQL("DROP TABLE IF EXISTS $LINK_TABLE")
        db.execSQL("DROP TABLE IF EXISTS $IMAGE_TABLE")
        onCreate(db)
    }

    companion object {
        // Table Name
        const val MUSIC_TABLE = "MUSICS"
        const val LIST_TABLE = "LISTS"
        const val LINK_TABLE = "LINKS"
        const val IMAGE_TABLE = "IMAGES"

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
        const val LIST_LASTPLAYED = "last_played"

        // links columns
        const val LINK_ID = "id"
        const val LINK_LIST_ID = "list_id"
        const val LINK_MUSIC_ID = "music_id"
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
                ("CREATE TABLE IF NOT EXISTS $MUSIC_TABLE(" +
                        "$MUSIC_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                        " $MUSIC_PATH TEXT NOT NULL," +
                        " $MUSIC_HASH TEXT," +
                        " $MUSIC_ISVALID BOOLEAN," +
                        " $MUSIC_TITLE TEXT," +
                        " $MUSIC_ARTIST TEXT," +
                        " $MUSIC_ALBUM TEXT," +
                        " $MUSIC_IMAGE_ID INTEGER);")

        const val CREATE_TABLE_LIST =
                ("CREATE TABLE IF NOT EXISTS $LIST_TABLE(" +
                        "$LIST_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                        " $LIST_NAME TEXT NOT NULL," +
                        " $LIST_TYPE TEXT NOT NULL," +
                        " $LIST_LASTPLAYED DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP);")

        const val CREATE_TABLE_LINK =
                ("CREATE TABLE IF NOT EXISTS $LINK_TABLE(" +
                        "$LINK_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                        " $LINK_LIST_ID INTEGER NOT NULL," +
                        " $LINK_MUSIC_ID INTEGER NOT NULL," +
                        " $LINK_ADD_TIME DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP);")

        const val CREATE_TABLE_IMAGE =
                ("CREATE TABLE IF NOT EXISTS $IMAGE_TABLE(" +
                        "$IMAGE_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                        " $IMAGE_HASH TEXT NOT NULL," +
                        " $IMAGE_DATA TEXT NOT NULL);")
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
            addList("all", ListType.System)
            addList("queue", ListType.System)
            addList("liked", ListType.System)
            addList("most", ListType.System)
            addList("suggest", ListType.System)
            addList("download", ListType.System)
        }
        return this
    }

    fun close() {
        dbHelper.close()
    }

    //////////////////////////////////////// LIST FUNCTIONS Music Controller Start functions

    fun getAllMusics() : ArrayList<SyncMusic>
    {
        val list = ArrayList<SyncMusic>()
        val columns = arrayOf(DBMusicHelper.MUSIC_ID,DBMusicHelper.MUSIC_ISVALID, DBMusicHelper.MUSIC_PATH, DBMusicHelper.MUSIC_TITLE,DBMusicHelper.MUSIC_ARTIST,DBMusicHelper.MUSIC_ALBUM)

        val cursor = database.query(DBMusicHelper.MUSIC_TABLE, columns, null, null, null, null, null)
        cursor.moveToFirst()
        for (i in 0 until cursor.count)
        {
            list.add(SyncMusic(cursor))
            cursor.moveToNext()
        }
        cursor.close()
        return list
    }

    fun getLists() : ArrayList<Int>
    {
        val list = ArrayList<Int>()
        val columns = arrayOf(DBMusicHelper.LIST_ID)
        val cursor = database.query(DBMusicHelper.LIST_TABLE, columns, null, null, null, null, null)
        cursor.moveToFirst()
        for (i in 0 until cursor.count)
        {
            list.add(cursor.getInt(0) - 1)
            cursor.moveToNext()
        }
        cursor.close()
        return list
    }

    fun getListFromId(list_id: Int) : SyncList?
    {
        val listName: String = getListNameFromId(list_id) ?: return null

        val columns = arrayOf(DBMusicHelper.LINK_MUSIC_ID)
        val where = "${DBMusicHelper.LINK_LIST_ID} = ${list_id+1}"
        val cursor = database.query(DBMusicHelper.LINK_TABLE, columns, where, null, null, null, null)
        cursor.moveToFirst()
        if(cursor.count > 0)
        {
            val list = SyncList(listName, cursor)
            cursor.close()
            return list
        }
        cursor.close()
        return SyncList(listName)
    }

    //////////////////////////////////////// LIST FUNCTIONS music scan add

    fun addMusic(music : SyncMusic) : Boolean
    {
        val where = "${DBMusicHelper.MUSIC_PATH} = \"${music.path}\""
        val cursor = database.query(DBMusicHelper.MUSIC_TABLE, null, where, null, null, null, null)
        if(cursor.count == 0)
        {
            cursor.close()
            val contentValue = ContentValues()
            contentValue.put(DBMusicHelper.MUSIC_PATH, music.path)
            //contentValue.put(DBMusicHelper.MUSIC_HASH, music.hash)
            contentValue.put(DBMusicHelper.MUSIC_ISVALID, music.valid)
            contentValue.put(DBMusicHelper.MUSIC_TITLE, music.title)
            contentValue.put(DBMusicHelper.MUSIC_ARTIST, music.artist)
            contentValue.put(DBMusicHelper.MUSIC_ALBUM, music.album)
            val id = database.insertOrThrow(DBMusicHelper.MUSIC_TABLE, null, contentValue)
            addMusicIdToListId(0, id.toInt() - 1)
            return true
        }
        cursor.close()
        return false
    }

    //////////////////////////////////////// LIST FUNCTIONS

    fun addList(name: String, listType: ListType) : Boolean
    {
        val columns = arrayOf(DBMusicHelper.LIST_ID)
        val where = " ${DBMusicHelper.LIST_TYPE} = \"${listType}\" AND ${DBMusicHelper.LIST_NAME} = \"$name\" "
        val cursor = database.query(DBMusicHelper.LIST_TABLE, columns, where, null, null, null, null)
        if(cursor.count == 0)
        {
            val contentValue = ContentValues()
            contentValue.put(DBMusicHelper.LIST_NAME, name)
            contentValue.put(DBMusicHelper.LIST_TYPE, listType.toString())
            val c = database.insert(DBMusicHelper.LIST_TABLE, null, contentValue)
            Log.d("MusicDB", c.toString())
            cursor.close()
            return true
        }
        cursor.close()
        return false
    }

    fun isMusicIdInListId(list_id : Int, music_id : Int) : Boolean
    {
        val where = "${DBMusicHelper.LINK_LIST_ID} = ${list_id + 1} AND ${DBMusicHelper.LINK_MUSIC_ID} = ${music_id + 1}"
        val cursor = database.query(DBMusicHelper.LINK_TABLE, null, where, null, null, null, null)
        val itIs = cursor.count > 0
        cursor.close()
        return itIs
    }

    fun addMusicIdToListId(list_id : Int, music_id : Int) : Boolean
    {
        if(!isMusicIdInListId(list_id , music_id))
        {
            val contentValue = ContentValues()
            contentValue.put(DBMusicHelper.LINK_LIST_ID, list_id + 1)
            contentValue.put(DBMusicHelper.LINK_MUSIC_ID, music_id + 1)
            var id = database.insert(DBMusicHelper.LINK_TABLE, null, contentValue)
            Log.d("v", id.toString())
            return true
        }
        return false
    }

    ////////////// NOT TESTED

    fun removeMusicIdFromListId(list_id : Int, music_id : Int)
    {
        val where = "${DBMusicHelper.LINK_LIST_ID} = ${list_id + 1} AND ${DBMusicHelper.LINK_MUSIC_ID} = ${music_id + 1}"
        database.delete(DBMusicHelper.LINK_TABLE, where,null)
    }

    /////////////////

/*
    fun getMusicsFromList(list_id : Int) : ArrayList<SyncMusic>
    {
        val list = ArrayList<SyncMusic>()
        val columns = arrayOf(DBMusicHelper.MUSIC_ID,DBMusicHelper.MUSIC_ISVALID, DBMusicHelper.MUSIC_PATH, DBMusicHelper.MUSIC_TITLE,DBMusicHelper.MUSIC_ARTIST,DBMusicHelper.MUSIC_ALBUM)

        val cursor = database.query(DBMusicHelper.LINK_TABLE, columns, null, null, null, null, null)
        cursor.moveToFirst()
        for (i in 0 until cursor.count)
        {
            list.add(SyncMusic(cursor))
            cursor.moveToNext()
        }
        cursor.close()
        return list
    }*/

    // MUSICS FUNCTIONS

   /* fun addMusic(music : Music) : Boolean
    {
        val where = "${DBMusicHelper.MUSIC_PATH} = \"${music.path}\""
        val cursor = database.query(DBMusicHelper.MUSIC_TABLE, null, where, null, null, null, null)
        if(cursor.count == 0)
        {
            val contentValue = ContentValues()
            contentValue.put(DBMusicHelper.MUSIC_PATH, music.path)
            contentValue.put(DBMusicHelper.MUSIC_ISVALID, music.valid)
            contentValue.put(DBMusicHelper.MUSIC_TITLE, music.title)
            contentValue.put(DBMusicHelper.MUSIC_ARTIST, music.artist)
            contentValue.put(DBMusicHelper.MUSIC_ALBUM, music.album)
            //contentValue.put(DBMusicHelper.MUSIC_IMAGE, music.image)
            //val id = database.insert(DBMusicHelper.MUSIC_TABLE, null, contentValue)
            cursor.close()
            //addMusicToList(0, id.toInt() - 0)
            return true
        }
        cursor.close()
        return false
    }*/

    /*fun getMusic(id : Int) : Music?
    {
        val columns = arrayOf(DBMusicHelper.MUSIC_ID,DBMusicHelper.MUSIC_ISVALID, DBMusicHelper.MUSIC_PATH, DBMusicHelper.MUSIC_TITLE,DBMusicHelper.MUSIC_ARTIST,DBMusicHelper.MUSIC_ALBUM)
        val where = "${DBMusicHelper.MUSIC_ID} = ${id + 1}"
        val cursor = database.query(DBMusicHelper.MUSIC_TABLE, columns, where, null, null, null, null)

        if(cursor.count > 0)
        {
            cursor.moveToFirst()

            //val id = cursor.getInt(0)
            val valid = cursor.getInt(1)
            val path = cursor.getString(2)
            val title = cursor.getString(3)
            val artist = cursor.getString(4)
            val album = cursor.getString(5)

            //return SyncMusic(cursor)

            return Music(path, title, artist, album, valid > 0)
        }

        return null
    }*/




    /*

    fun removeMusicFromList(list_id : Int, music_id : Int)
    {
        val where = "${DBMusicHelper.LINK_LIST_ID} = $list_id AND ${DBMusicHelper.LINK_MUSIC_ID} = $music_id"
        database.delete(DBMusicHelper.LINK_TABLE, where,null)
    }




    fun getListsIdFromType(listType: ListType) : ArrayList<Int>
    {
        val list = ArrayList<Int>()
        val columns = arrayOf(DBMusicHelper.LIST_ID)
        val where = "${DBMusicHelper.LIST_TYPE} = \"${listType}\""
        val cursor = database.query(DBMusicHelper.LIST_TABLE, columns, where, null, null, null, null)
        cursor.moveToFirst()
        for (i in 0 until cursor.count)
        {
            list.add(cursor.getInt(0))
            cursor.moveToNext()
        }
        cursor.close()
        return list
    }
*/
    fun getListNameFromId(list_id: Int) : String?
    {
        val columns = arrayOf(DBMusicHelper.LIST_NAME)
        val where = "${DBMusicHelper.LIST_ID} = ${list_id+1}"
        val cursor = database.query(DBMusicHelper.LIST_TABLE, columns, where, null, null, null, null)
        cursor.moveToFirst()
        val name = if(cursor.count > 0) cursor.getString(0)
        else null
        cursor.close()
        return name
    }

    /*
    fun getMusicsIdFromList(list_id : Int) : ArrayList<Int>
    {
        val list = ArrayList<Int>()
        val columns = arrayOf(DBMusicHelper.LINK_MUSIC_ID)
        val where = "${DBMusicHelper.LINK_LIST_ID} = $list_id"
        val cursor = database.query(DBMusicHelper.LINK_TABLE, columns, where, null, null, null, null)
        cursor.moveToFirst()
        for (i in 0 until cursor.count)
        {
            list.add(cursor.getInt(0))
            cursor.moveToNext()
        }
        cursor.close()
        return list
    }
*/


    // MISCELLANEOUS

    /*
    fun insert(music : Music) {
        val contentValue = ContentValues()
        contentValue.put(DBMusicHelper.PATH, music.path)
        contentValue.put(DBMusicHelper.TITLE, music.title)
        contentValue.put(DBMusicHelper.ARTIST, music.artist)
        contentValue.put(DBMusicHelper.ALBUM, music.album)
        database.insert(DBMusicHelper.TABLE_NAME, null, contentValue)
    }*/

    /*fun fetch(): Cursor {
        val columns = arrayOf(DBMusicHelper._ID, DBMusicHelper.PATH, DBMusicHelper.TITLE,DBMusicHelper.ARTIST,DBMusicHelper.ALBUM)
        val cursor = database.query(DBMusicHelper.TABLE_NAME, columns, null, null, null, null, null)

        cursor.moveToFirst()
        return cursor
    }

    */

    /*
    fun update(_id: Long, name: String?, desc: String?): Int {
        val contentValues = ContentValues()
        contentValues.put(DBMusicHelper.SUBJECT, name)
        contentValues.put(DBMusicHelper.DESC, desc)
        return database!!.update(DBMusicHelper.TABLE_NAME, contentValues, DBMusicHelper._ID + " = " + _id, null)
    }*/

    /*fun delete(_id: Long) {
        database.delete(DBMusicHelper.TABLE_NAME, DBMusicHelper._ID + "=" + _id, null)
    }*/

    fun clear()
    {
        context.deleteDatabase(DB_NAME);
    }
}
