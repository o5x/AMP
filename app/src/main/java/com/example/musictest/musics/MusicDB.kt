package com.example.musictest.musics

import android.content.ContentValues
import android.content.Context
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.musictest.R
import com.example.musictest.activities.syncMusicController
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest

//data/data/com.example.musictest/databases/musics.db

enum class ListContent {
    None, ListOfLists, ListOfMusics
}

enum class ListType {
    None, System, Album, Artist, Playlist
}

class ListId{
    companion object
    {
        const val ID_MUSIC_ALL = 1
        const val ID_MUSIC_QUEUE = 2
        const val ID_MUSIC_QUEUE_ORIGINAL = 3
        const val ID_MUSIC_LIKED = 4
        const val ID_MUSIC_MOST = 5
        const val ID_MUSIC_RECENT_MUSICS = 6
        const val ID_MUSIC_RECENT_LISTS = 7
        const val ID_MUSIC_SUGGEST = 8
        const val ID_MUSIC_DOWNLOAD = 9

        const val ID_MUSIC_ARTISTS = 10
        const val ID_MUSIC_ALBUMS = 11

        const val ID_MUSIC_USER_PLAYLISTS = 12

        const val ID_MUSIC_MAXID = 13
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
        const val STAT_ADDED_TIME = "added_time" // redundant with list link time in all

        // musics columns
        const val MUSIC_ID = "id"
        const val MUSIC_PATH = "path"
        const val MUSIC_HASH = "hash"
        const val MUSIC_ISVALID = "valid"
        const val MUSIC_TITLE = "title"
        const val MUSIC_ARTIST = "artist"
        const val MUSIC_ARTIST_ID = "artist_id"
        const val MUSIC_ALBUM = "album"
        const val MUSIC_ALBUM_ID = "album_id"
        const val MUSIC_IMAGE_ID = "image_id"

        // lists columns
        const val LIST_ID = "id"
        const val LIST_NAME = "name"
        const val LIST_CONTENT = "content"
        const val LIST_TYPE = "type"
        const val LIST_PLAYED_COUNT = "played_count"
        const val LIST_PLAYED_LAST = "played_last"
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
                        " $MUSIC_ISVALID BOOLEAN," +
                        " $MUSIC_TITLE TEXT," +
                        " $MUSIC_ARTIST TEXT," +
                        " $MUSIC_ARTIST_ID INTEGER," + // TODO INTEGER NOT NULL
                        " $MUSIC_ALBUM TEXT," +
                        " $MUSIC_ALBUM_ID INTEGER," +
                        " $MUSIC_IMAGE_ID INTEGER);")

        const val CREATE_TABLE_LIST =
                ("CREATE TABLE IF NOT EXISTS $TABLE_LIST(" +
                        "$LIST_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                        " $LIST_NAME TEXT NOT NULL," +
                        " $LIST_CONTENT TEXT NOT NULL," +
                        " $LIST_TYPE TEXT NOT NULL," +
                        " $LIST_PLAYED_COUNT INTEGER DEFAULT 0," +
                        " $LIST_PLAYED_LAST DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
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
            addList("all", ListType.System, ListContent.ListOfMusics)
            addList("queue", ListType.System,ListContent.ListOfMusics)
            addList("originalQueue", ListType.System,ListContent.ListOfMusics)
            addList("liked", ListType.System,ListContent.ListOfMusics)
            addList("most played",ListType.System, ListContent.ListOfMusics)
            addList("recent musics",ListType.System, ListContent.ListOfMusics)
            addList("recent lists",ListType.System, ListContent.ListOfLists)
            addList("suggest", ListType.System,ListContent.ListOfMusics)
            addList("download",ListType.System, ListContent.ListOfMusics)

            addList("artists",ListType.System, ListContent.ListOfLists)
            addList("albums",ListType.System, ListContent.ListOfLists)

            addList("userPlaylists", ListType.System,ListContent.ListOfLists)

            val res = context.resources
            setListImage(ListId.ID_MUSIC_ALL, BitmapFactory.decodeResource(res, R.drawable.all))
            setListImage(ListId.ID_MUSIC_QUEUE, BitmapFactory.decodeResource(res, R.drawable.queue))
            setListImage(ListId.ID_MUSIC_QUEUE_ORIGINAL, BitmapFactory.decodeResource(res, R.drawable.queue))
            setListImage(ListId.ID_MUSIC_LIKED, BitmapFactory.decodeResource(res, R.drawable.liked))
            setListImage(ListId.ID_MUSIC_MOST, BitmapFactory.decodeResource(res, R.drawable.most))
            setListImage(ListId.ID_MUSIC_RECENT_MUSICS, BitmapFactory.decodeResource(res, R.drawable.suggest))
            setListImage(ListId.ID_MUSIC_RECENT_LISTS, BitmapFactory.decodeResource(res, R.drawable.suggest))
            setListImage(ListId.ID_MUSIC_SUGGEST, BitmapFactory.decodeResource(res, R.drawable.suggest))
            setListImage(ListId.ID_MUSIC_DOWNLOAD, BitmapFactory.decodeResource(res, R.drawable.download))

            setListImage(ListId.ID_MUSIC_ARTISTS, BitmapFactory.decodeResource(res, R.drawable.artist))
            setListImage(ListId.ID_MUSIC_ALBUMS, BitmapFactory.decodeResource(res, R.drawable.album))

            setListImage(ListId.ID_MUSIC_USER_PLAYLISTS, BitmapFactory.decodeResource(res, R.drawable.playlist))

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
        val columns = arrayOf(DBMusicHelper.MUSIC_ID,
                DBMusicHelper.MUSIC_ISVALID,
                DBMusicHelper.MUSIC_PATH,
                DBMusicHelper.MUSIC_TITLE,
                DBMusicHelper.MUSIC_ARTIST,
                DBMusicHelper.MUSIC_ARTIST_ID,
                DBMusicHelper.MUSIC_ALBUM,
                DBMusicHelper.MUSIC_ALBUM_ID,
                DBMusicHelper.MUSIC_IMAGE_ID)

        val cursor = database.query(DBMusicHelper.TABLE_MUSIC, columns, null, null, null, null, null)
        cursor.moveToFirst()
        for (i in 0 until cursor.count)
        {
            val imgIndex = cursor.getInt(8)
            if(imgIndex > 0 && syncMusicController.images[imgIndex] == null)
            {
                val columns2 = arrayOf(DBMusicHelper.IMAGE_DATA)
                val where2 = DBMusicHelper.IMAGE_ID + " = " + imgIndex
                val cursor2 = database.query(DBMusicHelper.TABLE_IMAGE, columns2, where2, null, null, null, null)
                cursor2.moveToFirst()
                if(cursor2.count > 0)
                {
                    val ba = cursor2.getBlob(0)
                    syncMusicController.images[imgIndex] = BitmapFactory.decodeByteArray(ba, 0, ba!!.size)
                }
            }

            list[cursor.getInt(0)] = SyncMusic(cursor)
            cursor.moveToNext()
        }
        cursor.close()
        return list
    }

    private fun updateRecentList()
    {
        clearListId(ListId.ID_MUSIC_RECENT_LISTS)
        database.execSQL("INSERT INTO LINKS (list_id,target_id) " +
                "SELECT ${ListId.ID_MUSIC_RECENT_LISTS} ,id" +
                "\t\tFROM LISTS " +
                "\tWHERE played_count > 0" +
                "\tAND id != ${ListId.ID_MUSIC_QUEUE}" +
                "\tORDER BY played_last DESC")
    }

    private fun updateRecentMusics()
    {
        clearListId(ListId.ID_MUSIC_RECENT_MUSICS)
        database.execSQL("INSERT INTO LINKS (list_id,target_id) \n" +
                "SELECT ${ListId.ID_MUSIC_RECENT_MUSICS} ,music_id\n" +
                "\t\tFROM STATS \n" +
                "\tWHERE played_last is not null\n" +
                "\tORDER BY played_last DESC")
    }

    private fun updateMostMusics()
    {
        clearListId(ListId.ID_MUSIC_MOST)
        database.execSQL("INSERT INTO LINKS (list_id,target_id) \n" +
                "SELECT ${ListId.ID_MUSIC_MOST} ,music_id\n" +
                "\t\tFROM STATS \n" +
                "\tWHERE played_time > 0\n" +
                "\tORDER BY played_time DESC")
    }

    fun getListFromId(list_id: Int) : SyncList?
    {
        if(list_id == ListId.ID_MUSIC_RECENT_LISTS) updateRecentList()
        if(list_id == ListId.ID_MUSIC_RECENT_MUSICS) updateRecentMusics()
        if(list_id == ListId.ID_MUSIC_MOST) updateMostMusics()
        // get info from lists
        val columnsList = arrayOf(DBMusicHelper.LIST_NAME,DBMusicHelper.LIST_CONTENT,DBMusicHelper.LIST_IMAGE_ID)
        val whereList = "${DBMusicHelper.LIST_ID} = $list_id"
        val cursorList = database.query(DBMusicHelper.TABLE_LIST, columnsList, whereList, null, null, null, null)
        cursorList.moveToFirst()
        if(cursorList.count == 0) return SyncList()
        val listName = cursorList.getString(0)
        val listContent: ListContent = ListContent.valueOf(cursorList.getString(1))
        val img_id = cursorList.getInt(2)

        if(img_id > 0 && syncMusicController.images[img_id] == null)
        {
            val columns2 = arrayOf(DBMusicHelper.IMAGE_DATA)
            val where2 = DBMusicHelper.IMAGE_ID + " = " + img_id
            val cursor2 = database.query(DBMusicHelper.TABLE_IMAGE, columns2, where2, null, null, null, null)
            cursor2.moveToFirst()
            if(cursor2.count > 0)
            {
                val ba = cursor2.getBlob(0)
                syncMusicController.images[img_id] = BitmapFactory.decodeByteArray(ba, 0, ba!!.size)
            }
        }

        // get content from links
        val columns = arrayOf(DBMusicHelper.LINK_TARGET_ID,DBMusicHelper.LINK_ADD_TIME)
        val where = "${DBMusicHelper.LINK_LIST_ID} = $list_id"
        val cursor = database.query(DBMusicHelper.TABLE_LINK, columns, where, null, null, null, null)
        cursor.moveToFirst()
        if(cursor.count > 0)
        {
            val list = SyncList(listName, cursor, listContent, img_id) // add image_id1
            cursor.close()
            return list
        }
        cursor.close()
        return SyncList(listName, listContent, img_id)
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
        database.execSQL("UPDATE ${DBMusicHelper.TABLE_STAT} SET " +
                "${DBMusicHelper.STAT_PLAYED_COUNT} = ${DBMusicHelper.STAT_PLAYED_COUNT} + ${if (playedCounter < 1) 0 else 1}," +
                " ${DBMusicHelper.STAT_PLAYED_LAST} = CURRENT_TIMESTAMP," +
                " ${DBMusicHelper.STAT_PLAYED_TIME} = ${DBMusicHelper.STAT_PLAYED_TIME} + ${if(playedTime < 0) 0 else playedTime}" +
                " WHERE ${DBMusicHelper.STAT_MUSIC_ID} = $music_id")

        syncMusicController.invalidateList(ListId.ID_MUSIC_RECENT_MUSICS)
        syncMusicController.invalidateList(ListId.ID_MUSIC_MOST)
    }

    fun updateStatForList(list_id : Int, playedCounter: Int)
    {
        database.execSQL("UPDATE ${DBMusicHelper.TABLE_LIST} SET " +
                "${DBMusicHelper.LIST_PLAYED_COUNT} = ${DBMusicHelper.LIST_PLAYED_COUNT} + ${if (playedCounter < 1) 0 else 1}," +
                " ${DBMusicHelper.LIST_PLAYED_LAST} = CURRENT_TIMESTAMP" +
                " WHERE ${DBMusicHelper.LIST_ID} = $list_id")

        syncMusicController.invalidateList(ListId.ID_MUSIC_RECENT_LISTS)
    }

    fun setListImage(list_id: Int, bitmap: Bitmap) : Int
    {
        val id = addImageToDB(bitmap)
        val where = DBMusicHelper.LIST_ID + " = " + list_id
        val contentValue = ContentValues()
        contentValue.put(DBMusicHelper.LIST_IMAGE_ID, id)
        database.update(DBMusicHelper.TABLE_LIST,contentValue,where,null)

        return -1
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

            val aid = addIdToListNameTypeContent(id, music.artist.toString(), ListType.Artist,  ListContent.ListOfMusics)
            val abid = addIdToListNameTypeContent(id, music.album.toString(), ListType.Album, ListContent.ListOfMusics)


            addIdToListId(aid, ListId.ID_MUSIC_ARTISTS)
            addIdToListId(abid, ListId.ID_MUSIC_ALBUMS)

            setListImage(aid, BitmapFactory.decodeResource(context.resources, R.drawable.artist))
            setListImage(abid, BitmapFactory.decodeResource(context.resources, R.drawable.album))

            database.execSQL("UPDATE ${DBMusicHelper.TABLE_MUSIC} SET " +
                    "${DBMusicHelper.MUSIC_ALBUM_ID} = $abid," +
                    " ${DBMusicHelper.MUSIC_ARTIST_ID} = $aid " +
                    "WHERE ${DBMusicHelper.MUSIC_ID} = $id")

            if(music.valid)
            {
                // add to all musics
                addIdToListId(id, ListId.ID_MUSIC_ALL)

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

    fun addList(name: String, listType: ListType, listContent: ListContent) : Int
    {
        val columns = arrayOf(DBMusicHelper.LIST_ID)
        val where = " ${DBMusicHelper.LIST_CONTENT} = '$listContent' AND ${DBMusicHelper.LIST_NAME} = '${name.replace("'", "''")}' "
        val cursor = database.query(DBMusicHelper.TABLE_LIST, columns, where, null, null, null, null)
        if(cursor.count == 0)
        {
            val contentValue = ContentValues()
            contentValue.put(DBMusicHelper.LIST_NAME, name)
            contentValue.put(DBMusicHelper.LIST_CONTENT, listContent.toString())
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

    private fun addIdToListNameTypeContent(id: Int, listName: String, listType: ListType, listContent: ListContent) : Int
    {
        val list_id = addList(listName,listType, listContent)

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

    fun deleteListId(list_id: Int)
    {
        database.delete(DBMusicHelper.TABLE_LIST, DBMusicHelper.LIST_ID + "=" + list_id, null);
        syncMusicController.invalidateList(list_id)
        //syncMusicController.invalidateList(MOS)
    }

    /////////////////////////////////////////////////// clear

    private fun clear()
    {
        context.deleteDatabase(DBMusicHelper.DB_NAME);
    }
}
