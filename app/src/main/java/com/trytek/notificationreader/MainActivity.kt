package com.trytek.notificationreader

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.tabs.TabLayoutMediator
import com.trytek.notificationreader.db.NotificationsDB
import com.trytek.notificationreader.db.getStatus
import com.trytek.notificationreader.db.saveStatus
import com.trytek.notificationreader.service.NotificationReceiver
import com.trytek.notificationreader.utils.getFormattedDateTime
import com.trytek.notificationreader.utils.setCustomAdapter
import com.trytek.notificationreader.utils.showConfirmDialog
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.item_notification.view.*
import kotlinx.android.synthetic.main.layout_recycler_view.view.*
import org.jetbrains.anko.startActivityForResult
import org.jetbrains.anko.toast


class MainActivity : AppCompatActivity() {

    var enableStatus = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)
        setAdapter()

        addButton.setOnClickListener { pickApp() }

        enableStatus = getStatus()
        enableSwitch.isChecked = enableStatus

        enableSwitch.setOnCheckedChangeListener { compoundButton, isChecked ->

            if(!hasNotificationPermission()){
                enableStatus = false
                enableSwitch.isChecked = false
                toast("Notification Access Permission Required")
                startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                return@setOnCheckedChangeListener
            }


            if(!isChecked){
                enableSwitch.isChecked = isChecked
                enableStatus = isChecked
//                showConfirmDialog("Are you sure you want to stop reading notifications?"){
//                    enableSwitch.isChecked = false
//                    enableStatus = false
                    stopService()
//                }
            }
            else
            {
                enableSwitch.isChecked = true
                enableStatus = true
                startService()
            }
            saveStatus(enableStatus)

        }

        registerReceiver(receiver, IntentFilter(NotificationReceiver.ACTION_RECEIVE_NOTIFICATION))
    }

    override fun onResume() {
        super.onResume()

        if(hasNotificationPermission() && enableStatus){
            startService()
        }
    }


    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(receiver)
    }

    private fun setAdapter(refreshPackage:String? = null){

        val apps = NotificationsDB(this).getAllApps()

        viewPager.isVisible = apps.isNotEmpty()
        tabLayout.isVisible = viewPager.isVisible

        if(refreshPackage != null){
            kotlin.runCatching {
                val pos = apps.indexOfFirst { it[NotificationsDB.COL_APP_PACKAGE] == refreshPackage }
                viewPager.adapter?.notifyItemChanged(pos);
                Log.d("MainActivity", "setAdapter: page refreshed : $refreshPackage")
            }
        }

        viewPager.setCustomAdapter(apps, R.layout.layout_recycler_view){ itemView, position, item ->
            val appPackageName = NotificationsDB(this).getAllPackageName()[position]

            itemView.setRecyclerAdapter(appPackageName)

        }
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            val app = apps[position]
            val appPackageName = app[NotificationsDB.COL_APP_PACKAGE].toString()

            tab.view.setOnLongClickListener {
                val popupMenu = PopupMenu(this, it)
                popupMenu.menu.add("Remove")
                popupMenu.setOnMenuItemClickListener {
                    NotificationsDB(this).removeApp(appPackageName)
                    setAdapter()
                    return@setOnMenuItemClickListener false
                }
                popupMenu.show()
                return@setOnLongClickListener false
            }

            tab.text = app[NotificationsDB.COL_TITLE].toString()
            try {
                tab.icon = packageManager.getApplicationIcon(appPackageName)
            }
            catch (e:java.lang.Exception){
                e.printStackTrace()
                tab.setIcon(R.mipmap.ic_launcher_round)
            }
        }.attach()
    }


    private fun View.setRecyclerAdapter(packageName:String){

        val notifications = NotificationsDB(context).getAllNotifications(packageName)
        recyclerView.setCustomAdapter(notifications, R.layout.item_notification){
            itemView, position, item ->

            itemView.itemTitle.text = item[NotificationsDB.COL_TITLE].toString()
            itemView.itemSubTitle.text = item[NotificationsDB.COL_BODY].toString()
            itemView.itemTime.text = getFormattedDateTime(item[NotificationsDB.COL_ADDED_AT].toString().toLong())
        }

    }

    private fun hasNotificationPermission(): Boolean {
        return try {
            Settings.Secure.getString(
                contentResolver,
                "enabled_notification_listeners"
            ).contains(packageName)
        } catch (ex: Exception) {
            ex.printStackTrace()
            false
        }
    }

    private fun startService() {
        val intent = Intent(this, NotificationReceiver::class.java)
//        intent.action = NotificationReceiver.ACTION_START_FOREGROUND_SERVICE
        startService(intent)
        Log.d("MainActivity", "startService: ")

        saveStatus(true)
    }

    private fun stopService() {
        val intent = Intent(this, NotificationReceiver::class.java)
//        intent.action = NotificationReceiver.ACTION_STOP_FOREGROUND_SERVICE
        startService(intent)
        Log.d("MainActivity", "stopService: ")
        saveStatus(false)
    }

    private fun pickApp(){

       startActivityForResult<ChooseAppActivity>(1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        setAdapter()
    }

    val receiver = object : BroadcastReceiver(){
        override fun onReceive(p0: Context?, p1: Intent?) {
            p1?:return
            val appPackage = p1.getStringExtra("package")

            Log.d("MainActivity", "onReceive: $appPackage")
            setAdapter(appPackage)

        }
    }

}