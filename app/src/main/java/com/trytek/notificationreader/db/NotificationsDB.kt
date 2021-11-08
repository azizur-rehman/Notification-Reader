package com.trytek.notificationreader.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import java.lang.Exception

class NotificationsDB(context: Context?) {
    private val _openHelper: SQLiteOpenHelper

    companion object {
        const val COL_NOTIFICATION_ID = "notification_id"
        const val COL_TITLE = "title"
        const val COL_BODY = "body"
        const val COL_ID = "_id"
        const val COL_NOTIFICATION_RAW = "raw_data"
        const val COL_APP_PACKAGE = "app_package"
        const val COL_ADDED_AT = "addedAt"
 
        const val tableNotifications = "Notifications"
        const val tableApps = "Apps"
    }


    internal inner class SimpleSQLiteOpenHelper(context: Context?) :
        SQLiteOpenHelper(context, "notifications.db", null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("create table $tableNotifications (_id integer primary key autoincrement, " +
                    "$COL_TITLE text, $COL_BODY text, " +
                    "$COL_NOTIFICATION_RAW text, $COL_ADDED_AT long, $COL_NOTIFICATION_ID text , $COL_APP_PACKAGE text)")
            
            db.execSQL("create table $tableApps (_id integer primary key autoincrement, " +
                    "$COL_ADDED_AT long, $COL_TITLE text, $COL_APP_PACKAGE text)")
          
        }

        override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
            db?.execSQL("drop table if exists $tableApps")
            db?.execSQL("drop table if exists $tableNotifications")
        }
    }

    private val notificationCursor: Cursor?

        get() {
            val db: SQLiteDatabase = _openHelper.readableDatabase ?: return null
            return db.rawQuery("select * from $tableNotifications ORDER BY $COL_ADDED_AT DESC", null)
        }

    private val appCursor: Cursor?

        get() {
            try {
                val db: SQLiteDatabase = _openHelper.readableDatabase ?: return null
                return db.rawQuery("select * from $tableApps ORDER BY $COL_ADDED_AT DESC", null)
            }
            catch (e:Exception){ return null}

        }
   

    fun getAllNotifications(packageName: String) : List<Map<String, Any?>>{

        val list = mutableListOf<Map<String, Any?>>()

        val db: SQLiteDatabase = _openHelper.readableDatabase
        val query = "select * from $tableNotifications WHERE $COL_APP_PACKAGE ='${packageName}'  ORDER BY $COL_ADDED_AT DESC"
        val result = db.rawQuery(query, null)

        Log.d("NotificationsDB", "getAllNotifications: query = $query")
        val cur = result ?: return  emptyList()

        while (cur.moveToNext()) {
            val row = mutableMapOf<String,Any?>()
            row[COL_ID] = cur.getInt(cur.getColumnIndexOrThrow(COL_ID))
            row[COL_TITLE] = cur.getStringOrNull(cur.getColumnIndex(COL_TITLE))
            row[COL_BODY] = cur.getStringOrNull(cur.getColumnIndex(COL_BODY))
            row[COL_NOTIFICATION_RAW] = cur.getStringOrNull(cur.getColumnIndex(COL_NOTIFICATION_RAW))
            row["$COL_ADDED_AT"] = cur.getLongOrNull(cur.getColumnIndex("$COL_ADDED_AT"))
            row[COL_NOTIFICATION_ID] = cur.getStringOrNull(cur.getColumnIndex(COL_NOTIFICATION_ID))

            list.add(row)
        }

         return list
    }

    fun getAllApps(): List<Map<String, Any?>>{

        var list = mutableListOf<Map<String, Any?>>()

        val cur = (appCursor ?: return  emptyList())

        while (cur.moveToNext()) {
            val row = mutableMapOf<String,Any?>()
            row[COL_ID] = cur.getInt(cur.getColumnIndexOrThrow(COL_ID))
            row[COL_TITLE] = cur.getStringOrNull(cur.getColumnIndex(COL_TITLE))
            row["$COL_ADDED_AT"] = cur.getLongOrNull(cur.getColumnIndex("$COL_ADDED_AT"))
            row[COL_APP_PACKAGE] = cur.getStringOrNull(cur.getColumnIndex(COL_APP_PACKAGE))

            list.add(row)
        }

        return list.distinctBy { it[COL_APP_PACKAGE] }
    }

    fun getAllPackageName() = getAllApps().map { it[COL_APP_PACKAGE].toString() }
    


    operator fun get(tableName:String, notificationID: String, type: String): ContentValues? {
        val db: SQLiteDatabase = _openHelper.readableDatabase ?: return null
        val row = ContentValues()
        val cur: Cursor =
            db.rawQuery(
                "select * from $tableName where $COL_NOTIFICATION_ID = ? and ${COL_NOTIFICATION_RAW}=? ORDER BY $COL_ADDED_AT DESC",
                arrayOf(notificationID, type)
            )
        if (cur.moveToNext()) {
            row.put(COL_TITLE, cur.getStringOrNull(cur.getColumnIndex(COL_TITLE)))
            row.put(COL_BODY, cur.getStringOrNull(cur.getColumnIndex(COL_BODY)))
            row.put(COL_NOTIFICATION_RAW, cur.getStringOrNull(cur.getColumnIndex(COL_NOTIFICATION_RAW)))
            row.put("$COL_ADDED_AT", cur.getLongOrNull(cur.getColumnIndex("$COL_ADDED_AT")))
            row.put(COL_NOTIFICATION_ID, cur.getLongOrNull(cur.getColumnIndex(COL_NOTIFICATION_ID)))
            row.put(COL_APP_PACKAGE , cur.getStringOrNull(cur.getColumnIndex(COL_APP_PACKAGE)))
        }
        cur.close()
        db.close()
        return row
    }

    fun saveNotification(
        title: String?,
        body: String?,
        rawData: String?,
        notificationID: String?,
        packageName: String?,
        timeInMillis: Long = System.currentTimeMillis()
    ) {
        val db: SQLiteDatabase = _openHelper.writableDatabase ?: return
        try {
            val row = ContentValues()
            row.put(COL_TITLE, title)
            row.put(COL_BODY, body)
            row.put(COL_NOTIFICATION_RAW, rawData)
            row.put("$COL_ADDED_AT", timeInMillis)
            row.put(COL_NOTIFICATION_ID, notificationID)
            row.put(COL_APP_PACKAGE, packageName)
            db.insert(tableNotifications, null, row)
           
            Log.d("NotificationDB", "---- Notifcaiton SAVED(txn ID - $notificationID, type = $rawData) ----")
            db.close()
        }
        catch (e:Exception){
            Log.e("NotificationDB", "saveNotification: ${e.message}", e)
            db.close()
        }
    }


    fun addApp(
        title: String?,
        packageName: String, 
        timeInMillis: Long = System.currentTimeMillis()
    ) {
        val db: SQLiteDatabase = _openHelper.writableDatabase ?: return
        try {
            val row = ContentValues()
            row.put(COL_TITLE, title)
            row.put("$COL_ADDED_AT", timeInMillis)
            row.put(COL_APP_PACKAGE, packageName)

            db.insert(tableApps, null, row)

            Log.d("NotificationDB", "---- Added App (title - $title, package = $packageName) ----")
            db.close()
        }
        catch (e:Exception){
            Log.e("NotificationDB", "addApp: ${e.message}", e)
            db.close()
        }
    }


    fun removeApp(packageName: String) {
        kotlin.runCatching {
            val db: SQLiteDatabase = _openHelper.writableDatabase ?: return
            db.delete(tableApps, "$COL_APP_PACKAGE = ?", arrayOf(packageName))
            Log.d("NotificationDB", "---- Removed App (package = $packageName) ----")
            db.close()
        }
    }


    fun deleteNotification(notificationID: String) {
        kotlin.runCatching {
            val db: SQLiteDatabase = _openHelper.writableDatabase ?: return
            db.delete(tableNotifications, "$COL_NOTIFICATION_ID = ?", arrayOf(notificationID))
            db.close()
        }
    }

    fun deleteItem(id: Int) {
        kotlin.runCatching {
            val db: SQLiteDatabase = _openHelper.writableDatabase ?: return
            db.delete(tableNotifications, "$COL_ID = ?", arrayOf(id.toString()))
            db.close()
        }
    }


    fun deleteAllNotifications() {
        val db: SQLiteDatabase = _openHelper.writableDatabase ?: return
        db.delete(tableNotifications, null, null)
        db.close()
    }
    

        fun updateLogbody(notificationID: String?, body: String?, type: String) {
            kotlin.runCatching {
                val db: SQLiteDatabase = _openHelper.writableDatabase ?: return
                val row = ContentValues()
                row.put(COL_BODY, body)
                row.put(COL_NOTIFICATION_RAW, type)
                db.update(
                    tableNotifications,
                    row,
                    "$COL_NOTIFICATION_ID = ? and payment_type = ?",
                    arrayOf(notificationID, type)
                )
                
                db.close()
                Log.d("NotificationDB", "updateLogbody: $notificationID updated")
            }
    }

//    fun update(id: Long, title: String?, priority: Int) {
//        val db: SQLiteDatabase = _openHelper.writableDatabase ?: return
//        val row = ContentValues()
//        row.put("title", title)
//        row.put("priority", priority)
//        db.update("$tableName", row, "_id = ?", arrayOf(id.toString()))
//        db.close()
//    }

    /**
     * Construct a new database helper object
     * @param context The current context for the application or activity
     */
    init {
        _openHelper = SimpleSQLiteOpenHelper(context)
    }
}