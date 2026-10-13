package com.ayomide.minigames.ui

import android.content.Context

/** Best scores, win counts and saved games, stored privately on the phone. */
class Stats(context: Context) {
    private val p = context.applicationContext.getSharedPreferences("stats", Context.MODE_PRIVATE)

    fun int(key: String, def: Int = 0) = p.getInt(key, def)
    fun setInt(key: String, v: Int) = p.edit().putInt(key, v).apply()
    fun inc(key: String) = setInt(key, int(key) + 1)
    fun long(key: String, def: Long = 0) = p.getLong(key, def)
    fun setLong(key: String, v: Long) = p.edit().putLong(key, v).apply()
    fun str(key: String): String? = p.getString(key, null)
    fun setStr(key: String, v: String?) = p.edit().putString(key, v).apply()

    /** Keeps the lower value (e.g. fastest time, fewest moves); 0 means unset. */
    fun setBestLow(key: String, v: Long): Boolean {
        val cur = long(key)
        if (cur == 0L || v < cur) {
            setLong(key, v); return true
        }
        return false
    }
}

fun mmss(seconds: Long): String = String.format(java.util.Locale.US, "%d:%02d", seconds / 60, seconds % 60)
