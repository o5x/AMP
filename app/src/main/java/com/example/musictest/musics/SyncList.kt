package com.example.musictest.musics

import android.database.Cursor

class SyncList {
    var name: String = "Invalid list"
    var list: ArrayList<Int> = ArrayList()
    var listType = ListType.None
    var valid = false

    constructor(name_: String, listType_: ListType) {
        list = ArrayList()
        name = name_
        listType = listType_
        valid = true
    }

    constructor(name_: String, cursor: Cursor, listType_: ListType) {
        list = ArrayList()
        name = name_
        listType = listType_
        valid = true
        for (i in 0 until cursor.count) {
            list.add(cursor.getInt(0))
            cursor.moveToNext()
        }
    }

    constructor()
}
