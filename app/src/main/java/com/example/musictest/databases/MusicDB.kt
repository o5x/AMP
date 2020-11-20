package com.example.musictest.databases

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.musictest.Music
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
        wasCreatedNow = true
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $MUSIC_TABLE")
        db.execSQL("DROP TABLE IF EXISTS $LIST_TABLE")
        db.execSQL("DROP TABLE IF EXISTS $LINK_TABLE")
        onCreate(db)
    }

    companion object {
        // Table Name
        const val MUSIC_TABLE = "MUSICS"
        const val LIST_TABLE = "LISTS"
        const val LINK_TABLE = "LINK"

        // musics columns
        const val MUSIC_ID = "id"
        const val MUSIC_PATH = "path"
        const val MUSIC_TITLE = "title"
        const val MUSIC_ARTIST = "artist"
        const val MUSIC_ALBUM = "album"
        const val MUSIC_IMAGE_ID = "image_id"

        // lists columns
        const val LIST_ID = "id"
        const val LIST_NAME = "name"
        const val LIST_TYPE = "type"
        const val LIST_LASTPLAYED = "last_played"

        // lists links
        const val LINK_ID = "id"
        const val LINK_LIST_ID = "list_id"
        const val LINK_MUSIC_ID = "music_id"
        const val LINK_ADD_TIME = "date"

        // Database Information
        const val DB_NAME = "musics.db"

        // database version
        const val DB_VERSION = 1

        // Creating table query
        const val CREATE_TABLE_MUSIC =
                ("CREATE TABLE IF NOT EXISTS $MUSIC_TABLE(" +
                        "$MUSIC_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                        " $MUSIC_PATH TEXT NOT NULL," +
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
    }
}

class MusicDB(private val context: Context) {
    private lateinit var dbHelper: DBMusicHelper
    private lateinit var database: SQLiteDatabase

    @Throws(SQLException::class)
    fun open(): MusicDB {
        // Initialize db with default playlists when created

        //clear()

        dbHelper = DBMusicHelper(context)
        database = dbHelper.writableDatabase

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

    // MUSICS FUNCTIONS
    fun addMusic(music : Music) : Boolean
    {
        val where = "${DBMusicHelper.MUSIC_PATH} = \"${music.path}\""
        val cursor = database.query(DBMusicHelper.MUSIC_TABLE, null, where, null, null, null, null)
        if(cursor.count == 0)
        {
            val contentValue = ContentValues()
            contentValue.put(DBMusicHelper.MUSIC_PATH, music.path)
            contentValue.put(DBMusicHelper.MUSIC_TITLE, music.title)
            contentValue.put(DBMusicHelper.MUSIC_ARTIST, music.artist)
            contentValue.put(DBMusicHelper.MUSIC_ALBUM, music.album)
            //contentValue.put(DBMusicHelper.MUSIC_IMAGE, music.image)
            val id = database.insert(DBMusicHelper.MUSIC_TABLE, null, contentValue)
            cursor.close()
            addMusicToList(0, id.toInt())
            return true
        }
        cursor.close()
        return false
    }

    fun getMusic(id : Int) : Music?
    {
        val columns = arrayOf(DBMusicHelper.MUSIC_ID, DBMusicHelper.MUSIC_PATH, DBMusicHelper.MUSIC_TITLE,DBMusicHelper.MUSIC_ARTIST,DBMusicHelper.MUSIC_ALBUM)
        val where = "${DBMusicHelper.MUSIC_ID} = $id"
        val cursor = database.query(DBMusicHelper.MUSIC_TABLE, columns, where, null, null, null, null)

        if(cursor.count > 0)
        {
            cursor.moveToFirst()

            val id = cursor.getInt(0)
            val path = cursor.getString(1)
            val title = cursor.getString(2)
            val artist = cursor.getString(3)
            val album = cursor.getString(4)

            return Music(path, title, artist, album)
        }

        return null
    }

    // LIST FUNCTIONS
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

    fun isMusicIdInList(list_id : Int, music_id : Int) : Boolean
    {
        val where = "${DBMusicHelper.LINK_LIST_ID} = $list_id AND ${DBMusicHelper.LINK_MUSIC_ID} = $music_id"
        val cursor = database.query(DBMusicHelper.LINK_TABLE, null, where, null, null, null, null)
        val itIs = cursor.count > 0
        cursor.close()
        return itIs
    }

    fun addMusicToList(list_id : Int, music_id : Int) : Boolean
    {
        if(!isMusicIdInList(list_id , music_id))
        {
            val contentValue = ContentValues()
            contentValue.put(DBMusicHelper.LINK_LIST_ID, list_id)
            contentValue.put(DBMusicHelper.LINK_MUSIC_ID, music_id)
            database.insert(DBMusicHelper.LINK_TABLE, null, contentValue)
            return true
        }
        return false
    }

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

    fun getListNameFromId(list_id: ListType) : String?
    {
        val columns = arrayOf(DBMusicHelper.LIST_NAME)
        val where = "${DBMusicHelper.LIST_ID} = $list_id"
        val cursor = database.query(DBMusicHelper.LIST_TABLE, columns, where, null, null, null, null)
        cursor.moveToFirst()
        val name = if(cursor.count > 0) cursor.getString(0)
        else null
        cursor.close()
        return name
    }

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

    // MISCELLANEOUS

    fun cursorToMusic(cursor : Cursor) : Music
    {
        return Music(cursor.getString(1),
                cursor.getString(2),
                cursor.getString(3),
                cursor.getString(4), )
    }

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
