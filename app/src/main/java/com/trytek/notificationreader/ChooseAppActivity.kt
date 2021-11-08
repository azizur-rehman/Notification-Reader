package com.trytek.notificationreader

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.trytek.notificationreader.db.NotificationsDB
import com.trytek.notificationreader.utils.convertToJsonString
import com.trytek.notificationreader.utils.getAllInstalledApps
import com.trytek.notificationreader.utils.setCustomAdapter
import kotlinx.android.synthetic.main.item_app.view.*
import kotlinx.android.synthetic.main.layout_recycler_view.*

class ChooseAppActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_recycler_view)

        title = "Choose App"
        setAdapter()
    }

    private fun setAdapter(){

        val db = NotificationsDB(this)
        val apps = getAllInstalledApps().sortedBy { it.activityInfo.loadLabel(packageManager).toString() }


        recyclerView.setCustomAdapter(apps, R.layout.item_app){
            itemView, position, item ->

            with(itemView){
                itemTitle.text = item.activityInfo.loadLabel(packageManager)
                itemIcon.setImageDrawable(item.activityInfo.loadIcon(packageManager))

                val addedApps = db.getAllPackageName()

                itemSwitchApp.isChecked = item.activityInfo.packageName in addedApps

                itemSwitchApp.setOnCheckedChangeListener { compoundButton, b ->
                    if(b) {
                        db.addApp(itemTitle.text.toString(), item.activityInfo.packageName.toString())
                    }else{
                        db.removeApp(item.activityInfo.packageName)
                    }
                }
            }
        }


    }
}