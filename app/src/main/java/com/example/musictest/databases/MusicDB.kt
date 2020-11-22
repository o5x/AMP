package com.example.musictest.databases

import android.content.ContentValues
import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.musictest.SyncList
import com.example.musictest.SyncMusic
import com.example.musictest.activities.syncMusicController
import java.io.File

// //data/data/com.example.musictest/databases/musics.db


enum class ListType {
    None, SystemR, SystemRW, Album, Artist, User, SystemRList, SystemRWList
}

class listId{
    companion object
    {
        const val ID_MUSIC_ALL = 1
        const val ID_MUSIC_QUEUE = 2
        const val ID_MUSIC_QUEUE_ORIGINAL = 3
        const val ID_MUSIC_LIKED = 4
        const val ID_MUSIC_MOST = 5
        const val ID_MUSIC_SUGGEST = 6
        const val ID_MUSIC_DOWNLOAD = 7

        const val ID_MUSIC_ARTISTS = 8
        const val ID_MUSIC_ALBUMS = 9

        const val ID_MUSIC_USER_PLAYLISTS = 10
    }
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
                        " $LINK_TARGET_ID INTEGER NOT NULL," +
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

       // clear()

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
            addList("suggest", ListType.SystemR)
            addList("download", ListType.SystemR)

            addList("artists", ListType.SystemRList)
            addList("albums", ListType.SystemRList)

            addList("userPlaylists", ListType.SystemRWList)
            addIdToListId(listId.ID_MUSIC_QUEUE, listId.ID_MUSIC_USER_PLAYLISTS)
            addIdToListId(listId.ID_MUSIC_LIKED, listId.ID_MUSIC_USER_PLAYLISTS)
        }
        return this
    }

    fun close() {
        dbHelper.close()
    }

    //////////////////////////////////////// QUERIES

    fun getAllMusicMaps() : HashMap<Int,SyncMusic>
    {
        val list = HashMap<Int,SyncMusic>()
        val columns = arrayOf(DBMusicHelper.MUSIC_ID,DBMusicHelper.MUSIC_ISVALID, DBMusicHelper.MUSIC_PATH, DBMusicHelper.MUSIC_TITLE,DBMusicHelper.MUSIC_ARTIST,DBMusicHelper.MUSIC_ALBUM)

        val cursor = database.query(DBMusicHelper.MUSIC_TABLE, columns, null, null, null, null, null)
        cursor.moveToFirst()
        for (i in 0 until cursor.count)
        {
            list[cursor.getInt(0)] = SyncMusic(cursor)
            cursor.moveToNext()
        }
        cursor.close()
        return list
    }
/*
    fun getAllListMaps() : HashMap<Int,SyncList>
    {
        val listsIds = getLists()

        val lists = HashMap<Int,SyncList>()

        for(listId in listsIds)
        {
            lists[listId] = getListFromId(listId)!!
        }

        return lists
    }

    fun getLists() : ArrayList<Int>
    {
        val list = ArrayList<Int>()
        val columns = arrayOf(DBMusicHelper.LIST_ID)
        val cursor = database.query(DBMusicHelper.LIST_TABLE, columns, null, null, null, null, null)
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
    fun getListFromId(list_id: Int) : SyncList?
    {
        val listName: String = getListNameFromId(list_id) ?: return null
        val listType: ListType = getListTypeFromId(list_id) ?: return null

        val columns = arrayOf(DBMusicHelper.LINK_TARGET_ID)
        val where = "${DBMusicHelper.LINK_LIST_ID} = $list_id"
        val cursor = database.query(DBMusicHelper.LINK_TABLE, columns, where, null, null, null, null)
        cursor.moveToFirst()
        //Log.d("MusicDB", "getListFromId($list_id) = name $listName size ${cursor.count}")
        if(cursor.count > 0)
        {
            val list = SyncList(listName, cursor, listType)
            cursor.close()
            return list
        }
        cursor.close()
        return SyncList(listName, listType)
    }

    fun getListNameFromId(list_id: Int) : String?
    {
        val columns = arrayOf(DBMusicHelper.LIST_NAME)
        val where = "${DBMusicHelper.LIST_ID} = ${list_id}"
        val cursor = database.query(DBMusicHelper.LIST_TABLE, columns, where, null, null, null, null)
        cursor.moveToFirst()
        val name = if(cursor.count > 0) cursor.getString(0)
        else null
        cursor.close()
        //Log.d("MusicDB", "getListNameFromId($list_id) = \"$name\"")
        return name
    }

    private fun getListTypeFromId(list_id: Int) : ListType?
    {
        val columns = arrayOf(DBMusicHelper.LIST_TYPE)
        val where = "${DBMusicHelper.LIST_ID} = ${list_id}"
        val cursor = database.query(DBMusicHelper.LIST_TABLE, columns, where, null, null, null, null)
        cursor.moveToFirst()
        val name = if(cursor.count > 0) cursor.getString(0)
        else null
        cursor.close()
        return ListType.valueOf(name!!)
    }

    // adders

    fun addMusicByPath(f : File) : Array<Int>
    {
        val where = "${DBMusicHelper.MUSIC_PATH} = '${f.path.replace("'", "''")}'"
        val columns = arrayOf(DBMusicHelper.MUSIC_ID)
        val cursor = database.query(DBMusicHelper.MUSIC_TABLE, columns, where, null, null, null, null)
        if(cursor.count == 0)
        {
            Log.d("addMusic", f.path)
            val music = SyncMusic(f)
            cursor.close()
            val contentValue = ContentValues()
            contentValue.put(DBMusicHelper.MUSIC_PATH, music.path)
            //contentValue.put(DBMusicHelper.MUSIC_HASH, music.hash)
            contentValue.put(DBMusicHelper.MUSIC_ISVALID, music.valid)
            contentValue.put(DBMusicHelper.MUSIC_TITLE, music.title)
            contentValue.put(DBMusicHelper.MUSIC_ARTIST, music.artist)
            contentValue.put(DBMusicHelper.MUSIC_ALBUM, music.album)
            val id = database.insert(DBMusicHelper.MUSIC_TABLE, null, contentValue).toInt()
            syncMusicController.invalidateMusics()

            // add to all musics
            addIdToListId(id, listId.ID_MUSIC_ALL)
            val aid = addIdToListNameType(id,  music.artist.toString(), ListType.Artist)
            val abid = addIdToListNameType(id, music.album.toString(), ListType.Album)
            addIdToListId(aid, listId.ID_MUSIC_ARTISTS)
            addIdToListId(abid, listId.ID_MUSIC_ALBUMS)

            return arrayOf(id,1)
        }
        cursor.moveToFirst()
        val id = cursor.getInt(0)
        cursor.close()
        return arrayOf(id)
    }

    ////////////////////////////////////////////////////////////// Setters

    fun setListData(list_id : Int, newData : ArrayList<Int>)
    {
        clearListId(list_id)

        database.beginTransaction()
        for (data in newData)
        {
            val contentValue = ContentValues()
            contentValue.put(DBMusicHelper.LINK_LIST_ID, list_id)
            contentValue.put(DBMusicHelper.LINK_TARGET_ID, data)
            database.insert(DBMusicHelper.LINK_TABLE, null, contentValue)
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
        val cursor = database.query(DBMusicHelper.LIST_TABLE, columns, where, null, null, null, null)
        if(cursor.count == 0)
        {
            val contentValue = ContentValues()
            contentValue.put(DBMusicHelper.LIST_NAME, name.replace("'", "''"))
            contentValue.put(DBMusicHelper.LIST_TYPE, listType.toString())
            val c = database.insert(DBMusicHelper.LIST_TABLE, null, contentValue)
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

            val idinsert = database.insert(DBMusicHelper.LINK_TABLE, null, contentValue)
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
            database.insert(DBMusicHelper.LINK_TABLE, null, contentValue)
            syncMusicController.invalidateList(list_id)
        }
        return list_id
    }

    private fun isIdInListId(id: Int, list_id: Int) : Boolean
    {
        val where = "${DBMusicHelper.LINK_LIST_ID} = $list_id AND ${DBMusicHelper.LINK_TARGET_ID} = $id"
        val cursor = database.query(DBMusicHelper.LINK_TABLE, null, where, null, null, null, null)
        val itIs = cursor.count > 0
        cursor.close()
        return itIs
    }

    /////////////////////////////////////////////////// delete

    fun removeIdFromListId(id: Int, list_id: Int)
    {
        val where = "${DBMusicHelper.LINK_LIST_ID} = $list_id AND ${DBMusicHelper.LINK_TARGET_ID} = $id"
        database.delete(DBMusicHelper.LINK_TABLE, where,null)
        syncMusicController.invalidateList(list_id)
    }

    fun clearListId(list_id : Int)
    {
        database.delete(DBMusicHelper.LINK_TABLE, DBMusicHelper.LINK_LIST_ID + "=" + list_id, null);
        syncMusicController.invalidateList(list_id)
    }

    /////////////////////////////////////////////////// clear

    private fun clear()
    {
        context.deleteDatabase(DBMusicHelper.DB_NAME);
    }
}
