package com.trytek.notificationreader.db

import android.content.Context


fun Context.saveStatus(enable:Boolean){
    getSharedPreferences("preferences", Context.MODE_PRIVATE)
        .edit().putBoolean("enable", enable).apply()
}

fun Context.getStatus() =
    getSharedPreferences("preferences", Context.MODE_PRIVATE) .getBoolean("enable", false)
