package com.example.musictest.services

import android.content.Context
import com.example.musictest.Music
import org.json.JSONObject
import java.io.*

var FILE_NAME : String = "musics.json"

class MemoryController {

    lateinit var json : JSONObject
    lateinit var context: Context

    fun init(c: Context)
    {
        context = c
    }

    fun restore()
    {
        val file: File = File(context.getFilesDir(), FILE_NAME)
        val fileReader = FileReader(file)
        val bufferedReader = BufferedReader(fileReader)
        val stringBuilder = StringBuilder()
        var line: String = bufferedReader.readLine()
        while (line != null) {
            stringBuilder.append(line).append("\n")
            line = bufferedReader.readLine()
        }
        bufferedReader.close()
        // This responce will have Json Format String
        // This responce will have Json Format String
        val responce = stringBuilder.toString()

        json = JSONObject(responce)
    }

    fun addMusic(music : Music)
    {
        json.getJSONArray("musics").put("")
    }

    fun save()
    {
        // Convert JsonObject to String Format
        val userString: String = json.toString()
        val file = File(context.getFilesDir(), FILE_NAME)
        val fileWriter = FileWriter(file)
        val bufferedWriter = BufferedWriter(fileWriter)
        bufferedWriter.write(userString)
        bufferedWriter.close()
    }
}