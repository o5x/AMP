package com.example.musictest.musics

import android.database.Cursor
import android.graphics.Bitmap
import androidx.core.database.getIntOrNull
import com.example.musictest.activities.smc

enum class SortMode {
    None, Name, NameR, Random, Date, DateR, Played, PlayedR
}

enum class ListContent {
    None, ListOfLists, ListOfMusics
}

enum class ListType {
    None, System, Album, Artist, Playlist
}

/*
class ListElement{
    var target : Int = 0
    var date: String = ""
    var count : Int = 0
}*/

class SyncList {

    val id : Int? = null
    var name: String = "Invalid list"
    var listContent = ListContent.None
    var listType = ListType.None
    var playedLast = ""
    private var imgId: Int? = null

    var valid = false

    var list: ArrayList<Int> = ArrayList()

    private var listOrigin: ArrayList<Int> = ArrayList()

    private var date: ArrayList<String> = ArrayList()
    private var count: ArrayList<Int> = ArrayList()

    //var sortMode = SortMode.None// TODO implement save

    fun sort(sortMode: SortMode)
    {
        list = listOrigin

        when (sortMode) {
            SortMode.Name -> {
                if(listContent == ListContent.ListOfMusics)
                    list = ArrayList(list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { smc.getMusic(it).title.toString() }))
                else if(listContent == ListContent.ListOfLists)
                    list = ArrayList(list.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { smc.getList(it).name }))
            }
            SortMode.NameR -> {
                if(listContent == ListContent.ListOfMusics)
                    list = ArrayList(list.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { smc.getMusic(it).title.toString() }))
                else if(listContent == ListContent.ListOfLists)
                    list = ArrayList(list.sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { smc.getList(it).name }))
            }
            SortMode.Date -> {
                list = ArrayList(list.sortedWith(compareBy {date[list.indexOf(it)] }))
            }
            SortMode.DateR -> {
                list = ArrayList(list.sortedWith(compareByDescending {date[list.indexOf(it)] }))
            }
            SortMode.Played -> {
                list = ArrayList(list.sortedWith(compareByDescending {count[list.indexOf(it)] }))
            }
            SortMode.PlayedR -> {
                list = ArrayList(list.sortedWith(compareBy {count[list.indexOf(it)] }))
            }
            SortMode.Random -> {
                list.shuffle()
            }
            else -> {}
        }
    }

    constructor(name_: String, listContent_: ListContent, list_: ArrayList<Int>) {
        list = list_
        listOrigin = list
        name = name_
        listContent = listContent_
        valid = true
    }

    constructor(name_: String, listContent_: ListContent, imid: Int) {
        list = ArrayList()
        listOrigin = list
        name = name_
        listContent = listContent_
        imgId = imid
        valid = true
    }

    var readonly = true
    var deletable = false
    var sortMode = SortMode.None
    var sortLocked = true
    var author_id : Int? = null

    constructor(cursorList: Cursor, cursorData: Cursor) {

        list = ArrayList()
        valid = true

        name = cursorList.getString(0)
        listContent = ListContent.valueOf(cursorList.getString(1))
        listType = ListType.valueOf(cursorList.getString(2))
        imgId = cursorList.getInt(3)

        readonly = cursorList.getInt(4) > 0
        deletable = cursorList.getInt(5) > 0
        sortMode = SortMode.valueOf(cursorList.getString(6))
        sortLocked = cursorList.getInt(7) > 0
        author_id = cursorList.getIntOrNull(8)

        for (i in 0 until cursorData.count) {
            list.add(cursorData.getInt(0))

            if(listContent == ListContent.ListOfLists)
            {
                date.add(cursorData.getString(1) + ":" +
                        cursorData.getInt(2).toString().padStart(6, '0'))
                count.add(cursorData.getInt(3))
            }
            else if (listContent == ListContent.ListOfMusics)
            {
                date.add(/*cursor.getString(1) + ":" +*/
                    cursorData.getString(1)+ ":" +
                            cursorData.getInt(2).toString().padStart(6, '0'))
                count.add(cursorData.getInt(3))
            }

            // to differentiate same time
            cursorData.moveToNext()
        }

        listOrigin = list
    }
/*
    constructor(name_: String, cursor: Cursor, listContent_: ListContent,listType_ : ListType, imid: Int) {
        list = ArrayList()
        listOrigin = list
        name = name_
        listContent = listContent_
        listType = listType_
        valid = true
        imgId = imid

        for (i in 0 until cursor.count) {
            list.add(cursor.getInt(0))

            if(listContent == ListContent.ListOfLists)
            {
                date.add(cursor.getString(1) + ":" +
                        cursor.getInt(2).toString().padStart(6, '0'))
                count.add(cursor.getInt(3))
            }
            else if (listContent == ListContent.ListOfMusics)
            {
                date.add(/*cursor.getString(1) + ":" +*/
                        cursor.getString(1)+ ":" +
                                cursor.getInt(2).toString().padStart(6, '0'))
                count.add(cursor.getInt(3))
            }

            // to differentiate same time
            cursor.moveToNext()
        }
    }*/

    val image: Bitmap?
        get() = smc.images[imgId]

    val author: String?
        get() = author_id?.let { smc.getList(it).name }

    constructor()
}
