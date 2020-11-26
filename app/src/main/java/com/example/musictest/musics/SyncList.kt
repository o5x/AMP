package com.example.musictest.musics

import android.database.Cursor
import android.graphics.Bitmap
import com.example.musictest.activities.smc

enum class SortMode {
    Id, IdR, Name, NameR, Random, Date, DateR, Played, PlayedR
}

/*
class ListElement{
    var target : Int = 0
    var date: String = ""
    var count : Int = 0
}*/

class SyncList {
    var name: String = "Invalid list"

    //var elts: ArrayList<ListElement> = ArrayList()

    var list: ArrayList<Int> = ArrayList()
    private var listOrigin: ArrayList<Int> = ArrayList()
    var listContent = ListContent.ListOfMusics
    var valid = false
    private var date: ArrayList<String> = ArrayList()
    private var count: ArrayList<Int> = ArrayList()
    private var imgId: Int? = null

    //private var dafaultSortMode : SortMode = SortMode.Date // TODO implement save

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

    constructor(name_: String, cursor: Cursor, listContent_: ListContent, imid: Int) {
        list = ArrayList()
        listOrigin = list
        name = name_
        listContent = listContent_
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
    }

    val image: Bitmap?
        get() = smc.images[imgId]

    constructor()
}
