package com.example.musictest.databases

import android.content.ContentValues
import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.musictest.SyncList
import com.example.musictest.SyncMusic
import com.example.musictest.databases.DBMusicHelper.Companion.DB_NAME

// //data/data/com.example.musictest/databases/musics.db


enum class ListType {
    None, SystemR, SystemRW, Album, Artist, User, SystemRList
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

    companion object{
        const val ID_MUSIC_ALL = 1
        const val ID_MUSIC_QUEUE = 2
        const val ID_MUSIC_QUEUE_SHUFFLED = 3
        const val ID_MUSIC_LIKED = 4
        const val ID_MUSIC_MOST = 5
        const val ID_MUSIC_SUGGEST = 6
        const val ID_MUSIC_DOWNLOAD = 7

        const val ID_MUSIC_ARTISTS = 8
        const val ID_MUSIC_ALBUMS = 9
    }

    @Throws(SQLException::class)
    fun open(): MusicDB {

        clear()

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
        }
        return this
    }

    fun close() {
        dbHelper.close()
    }

    //////////////////////////////////////// LIST FUNCTIONS Music Controller Start functions

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

    fun  getAllListMaps() : HashMap<Int,SyncList>
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

    fun getListFromId(list_id: Int) : SyncList?
    {
        val listName: String = getListNameFromId(list_id) ?: return null
        val listType: ListType = getListTypeFromId(list_id) ?: return null

        val columns = arrayOf(DBMusicHelper.LINK_MUSIC_ID)
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

    fun getListContentFromId(list_id: Int) : ArrayList<Int>
    {
        val list = ArrayList<Int>()
        val columns = arrayOf(DBMusicHelper.LINK_MUSIC_ID)
        val where = "${DBMusicHelper.LINK_LIST_ID} = $list_id"
        val cursor = database.query(DBMusicHelper.LINK_TABLE, columns, where, null, null, null, null)
        cursor.moveToFirst()
        //Log.d("MusicDB", "getListFromId($list_id) = name $listName size ${cursor.count}")
        for (i in 0 until cursor.count)
        {
            list.add(cursor.getInt(0))
            cursor.moveToNext()
        }
        cursor.close()
        return list
    }

    //////////////////////////////////////// LIST FUNCTIONS music scan add

    private fun addMusicIdToListNameType(music_id: Int, listName: String, listType: ListType) : Int
    {
        val playlistId = addList(listName, listType)

        if(!isListIdInListId(music_id, playlistId))
        {
            val contentValue = ContentValues()
            contentValue.put(DBMusicHelper.LINK_LIST_ID, playlistId)
            contentValue.put(DBMusicHelper.LINK_MUSIC_ID, music_id)
            database.insert(DBMusicHelper.LINK_TABLE, null, contentValue)
        }
        return playlistId
    }

    private fun isListIdInListId(source_list: Int, list_id: Int) : Boolean
    {
        val where = "${DBMusicHelper.LINK_LIST_ID} = $list_id AND ${DBMusicHelper.LINK_MUSIC_ID} = $source_list"
        val cursor = database.query(DBMusicHelper.LINK_TABLE, null, where, null, null, null, null)
        val itIs = cursor.count > 0
        //Log.d("MusicDB", "isMusicIdInListId($list_id,$source_list) = $itIs")
        cursor.close()
        return itIs
    }



    private fun processNewMusic(music_id : Int, artist : String, album : String)
    {
        // add to all musics
        addIdToListId(music_id, ID_MUSIC_ALL)

        // process artist grouping
        val aid = addMusicIdToListNameType(music_id, artist, ListType.Artist)

        // process album grouping
        val abid = addMusicIdToListNameType(music_id, album, ListType.Album)

        //addList("artists", ListType.SystemRList)

        addIdToListId(aid, ID_MUSIC_ARTISTS)
        addIdToListId(abid, ID_MUSIC_ALBUMS)
    }

    fun addMusic(music : SyncMusic) : Array<Int>
    {
        val where = "${DBMusicHelper.MUSIC_PATH} = '${music.path.replace('\'', 31.toChar())}'"
        val cursor = database.query(DBMusicHelper.MUSIC_TABLE, null, where, null, null, null, null)
        var id: Int;
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
            id = database.insert(DBMusicHelper.MUSIC_TABLE, null, contentValue).toInt()
            processNewMusic(id, music.artist.toString(), music.album.toString())
            return arrayOf(id,1)
        }
        cursor.moveToFirst()
        id = cursor.getInt(0)
        cursor.close()
        return arrayOf(id)
    }

    //////////////////////////////////////// LIST FUNCTIONS

    private fun addList(name: String, listType: ListType) : Int
    {
        val columns = arrayOf(DBMusicHelper.LIST_ID)
        val where = " ${DBMusicHelper.LIST_TYPE} = '${listType.toString().replace('\'', 31.toChar())}' AND ${DBMusicHelper.LIST_NAME} = '${name.replace('\'', 31.toChar())}' "
        val cursor = database.query(DBMusicHelper.LIST_TABLE, columns, where, null, null, null, null)
        if(cursor.count == 0)
        {
            val contentValue = ContentValues()
            contentValue.put(DBMusicHelper.LIST_NAME, name)
            contentValue.put(DBMusicHelper.LIST_TYPE, listType.toString())
            val c = database.insert(DBMusicHelper.LIST_TABLE, null, contentValue)
            //Log.d("MusicDB", c.toString())
            cursor.close()
            return c.toInt()
        }
        cursor.moveToFirst()
        val id = cursor.getInt(0)
        cursor.close()
        return id
    }

    fun addIdToListId(source_list: Int, list_id: Int) : Int
    {
        // is album in albumlist
        if(!isListIdInListId(source_list, list_id))
        {
            val contentValue = ContentValues()
            contentValue.put(DBMusicHelper.LINK_LIST_ID, list_id)
            contentValue.put(DBMusicHelper.LINK_MUSIC_ID, source_list)
            val id = database.insert(DBMusicHelper.LINK_TABLE, null, contentValue)
            return id.toInt()
        }
        return -1
    }

    fun removeIdFromListId(id: Int, list_id: Int)
    {
        //Log.d("MusicDB", "removeMusicIdFromListId($list_id,$music_id)")
        val where = "${DBMusicHelper.LINK_LIST_ID} = ${list_id} AND ${DBMusicHelper.LINK_MUSIC_ID} = ${id}"
        database.delete(DBMusicHelper.LINK_TABLE, where,null)
    }

    private fun getListNameFromId(list_id: Int) : String?
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

    // Database clear function

    private fun clear()
    {
        context.deleteDatabase(DB_NAME);
    }
}
