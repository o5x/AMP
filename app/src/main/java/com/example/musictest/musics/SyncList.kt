package com.example.musictest.musics

import android.database.Cursor
import android.graphics.Bitmap
import com.example.musictest.activities.syncMusicController
import kotlin.collections.ArrayList

class SyncList {
    var name: String = "Invalid list"
    var list: ArrayList<Int> = ArrayList()
    var listType = ListContent.ListOfMusics
    var valid = false
    var date : ArrayList<String> = ArrayList()
    var img_id : Int? = null

    constructor(name_: String, listContent_: ListContent, list_ : ArrayList<Int>)
    {
        list = list_
        name = name_
        listType = listContent_

        valid = true
    }

    constructor(name_: String, listContent_: ListContent, imid : Int) {
        list = ArrayList()
        name = name_
        listType = listContent_
        img_id = imid
        valid = true
    }

    constructor(name_: String, cursor: Cursor, listContent_: ListContent, imid : Int) {
        list = ArrayList()
        name = name_
        listType = listContent_
        valid = true
        img_id = imid
        for (i in 0 until cursor.count) {
            list.add(cursor.getInt(0))
            date.add(cursor.getString(1))
            cursor.moveToNext()
        }
    }

    val image: Bitmap?
        get() = syncMusicController.images[img_id]

    constructor()
}
