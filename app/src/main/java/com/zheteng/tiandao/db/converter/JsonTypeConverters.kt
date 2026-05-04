package com.zheteng.tiandao.db.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class JsonTypeConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromIntList(value: List<Int>?): String = gson.toJson(value)

    @TypeConverter
    fun toIntList(value: String): List<Int> {
        val listType = object : TypeToken<List<Int>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }
}