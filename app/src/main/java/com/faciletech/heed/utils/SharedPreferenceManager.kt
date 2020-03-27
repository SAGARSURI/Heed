package com.faciletech.heed.utils

import android.content.Context
import android.content.SharedPreferences

const val PREFS_NAME = "heed"
class SharedPreferenceManager(val context: Context) {

    private val sharedPref: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(KEY_NAME: String, text: String) {
        sharedPref.edit().putString(KEY_NAME, text).apply()
    }

    fun save(KEY_NAME: String, value: Int) {
        sharedPref.edit().putInt(KEY_NAME, value).apply()
    }

    fun save(KEY_NAME: String, status: Boolean) {
        val editor: SharedPreferences.Editor = sharedPref.edit().putBoolean(KEY_NAME, status)
        editor.apply()
    }

    fun getValueString(KEY_NAME: String): String? {
        return sharedPref.getString(KEY_NAME, null)
    }

    fun getValueInt(KEY_NAME: String): Int {
        return sharedPref.getInt(KEY_NAME, 0)
    }

    fun getValueDouble(KEY_NAME: String): Double? {
        return sharedPref.getString(KEY_NAME, null)?.toDouble()
    }

    fun getValueBoolean(KEY_NAME: String, defaultValue: Boolean): Boolean {
        return sharedPref.getBoolean(KEY_NAME, defaultValue)
    }

    fun clearSharedPreference() {
       sharedPref.edit().clear().apply()
    }

    fun removeValue(KEY_NAME: String) {
        sharedPref.edit().remove(KEY_NAME).apply()
    }
}