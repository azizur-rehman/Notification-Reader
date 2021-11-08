package com.trytek.notificationreader.service

import android.app.*
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.trytek.notificationreader.MainActivity
import com.trytek.notificationreader.R
import com.trytek.notificationreader.db.NotificationsDB
import com.trytek.notificationreader.utils.convertToJsonString

class NotificationReceiver : NotificationListenerService() {
    private fun sendDebugNotification(msg: String, notId: Int = 50) {
        //Intent it = new Intent(this, MainActivity.class);
        //UIHelper.sendNotification(AppCache.NOTIFICATION_COMMON_CHANNEL_ID, "Debug", msg, "debug", notId, R.drawable.icon, it, getApplicationContext());
    }

    var attachmentSymbols = arrayOf("\uD83D\uDCF7", "\uD83C\uDFA5", "\uD83D\uDCC4")
    private fun isGroup(bundle: Bundle): Boolean {
        val isGroupConversation = bundle["android.isGroupConversation"]
        val conversationTitle = bundle.getString("android.conversationTitle")
        var isGroup = false
        if (isGroupConversation != null) {
            isGroup = isGroupConversation as Boolean && conversationTitle != null //Group Params
        }
        return isGroup
    }

    var currentPackage:String? = null
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        Log.d("NotificationReceiver", "onNotificationPosted: from ${sbn.packageName}")
        if (sbn != null && sbn.packageName.compareTo(packageName) == 0) {
            //Ignore the self notification
            return
        }
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) {
            //Ignore the duplicate notification
            return
        }

        val packages = NotificationsDB(this).getAllPackageName()
        currentPackage = packages.find { it == sbn.packageName }

        currentPackage ?: return

        val data = sbn.notification.extras
        Log.d("NotificationReceiver", "onNotificationPosted: package = $currentPackage")
//        Log.d("NotificationReceiver", "onNotificationPosted: data = "+data.convertToJsonString())

        val title = data.getString("android.title")
        val body = data.getString("android.text")
        NotificationsDB(this).saveNotification(title, body, data.convertToJsonString(), System.currentTimeMillis().toString(), currentPackage)
        sendBroadcast(Intent(ACTION_RECEIVE_NOTIFICATION).apply { putExtra("package", currentPackage) })
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (intent != null) {
            val action = intent.action
            if (action != null) when (action) {
                ACTION_START_FOREGROUND_SERVICE -> startForegroundService()
                ACTION_STOP_FOREGROUND_SERVICE -> stopForegroundService()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel("notification_reader", "Foreground Service")
        } else {
            // Create notification builder.
            val builder = NotificationCompat.Builder(this)
            builder.setWhen(System.currentTimeMillis())
            builder.setSmallIcon(R.mipmap.ic_launcher)
            builder.setContentTitle(getString(R.string.app_name))
            builder.setContentText("Notification Reader is active..!")
            builder.priority = NotificationManager.IMPORTANCE_MIN
            builder.setCategory(Notification.CATEGORY_SERVICE)
            // Build the notification.
            val notification = builder.build()
            // Start foreground service.
            startForeground(1, notification)
            notificationCompatBuilder = builder
        }
    }

    var notificationCompatBuilder: NotificationCompat.Builder? = null
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String) {
        val resultIntent = Intent(this, MainActivity::class.java)
        // Create the TaskStackBuilder and add the intent, which inflates the back stack
        val stackBuilder = TaskStackBuilder.create(this)
        stackBuilder.addNextIntentWithParentStack(resultIntent)
        val resultPendingIntent =
            stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
        chan.lightColor = Color.GREEN
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val manager = (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
        manager.createNotificationChannel(chan)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
        val notification = notificationBuilder.setOngoing(true)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Notification Reader is active..!")
            .setPriority(NotificationManager.IMPORTANCE_MIN)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setContentIntent(resultPendingIntent) //intent
            .build()
        notificationCompatBuilder = notificationBuilder
        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.notify(1, notificationBuilder.build())
        startForeground(1, notification)
    }

    private fun stopForegroundService() {
        // Stop foreground service and remove the notification.
        stopForeground(true)
        // Stop the foreground service.
        stopSelf()
    }//        LocalStorage lst = new LocalStorage(getApplicationContext());

    //        return lst.getStatus();
    /*
       * Get Status of Toggle
       */
    private val isOn: Boolean
        private get() =//        LocalStorage lst = new LocalStorage(getApplicationContext());
//        return lst.getStatus();
            true

    companion object {
        const val ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE"
        const val ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE"

        const val ACTION_RECEIVE_NOTIFICATION = "ACTION_RECEIVE_NOTIFICATION"
    }
}