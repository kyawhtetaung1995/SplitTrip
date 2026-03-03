package com.splittrip.data.database
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()
    @TypeConverter fun fromStringMap(v: Map<String, Double>?): String = gson.toJson(v ?: emptyMap<String, Double>())
    @TypeConverter fun toStringMap(v: String): Map<String, Double> {
        val type = object : TypeToken<Map<String, Double>>() {}.type
        return gson.fromJson(v, type) ?: emptyMap()
    }
    @TypeConverter fun fromStringList(v: List<String>?): String = gson.toJson(v ?: emptyList<String>())
    @TypeConverter fun toStringList(v: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(v, type) ?: emptyList()
    }
}
