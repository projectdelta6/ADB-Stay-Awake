package com.duck.stayawakeadb

import android.content.Intent
import android.content.IntentFilter
import android.service.notification.StatusBarNotification
import androidx.localbroadcastmanager.content.LocalBroadcastManager

/**
 * Created by Bradley Duck on 2019/02/18.
 */
class ADBNotificationListener : android.service.notification.NotificationListenerService() {

    private lateinit var helperUtil: HelperUtil

    override fun onCreate() {
        super.onCreate()
        helperUtil = HelperUtil(applicationContext)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (helperUtil.developerOptionsEnabled && helperUtil.USBDebuggingEnabled) {
            if (sbn.packageName.equals("android", ignoreCase = true)) {
                val notification = sbn.notification
                val title = notification.extras.getString("android.title")
                if (title != null && title.equals("USB debugging connected", ignoreCase = true)) {
                    setAndSendBroadcast(true)
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (helperUtil.developerOptionsEnabled && helperUtil.USBDebuggingEnabled) {
            if (sbn.packageName.equals("android", ignoreCase = true)) {
                val notification = sbn.notification
                val title = notification.extras.getString("android.title")
                if (title != null && title.equals("USB debugging connected", ignoreCase = true)) {
                    setAndSendBroadcast(false)
                }
            }
        }
    }

    private fun setAndSendBroadcast(turnOn: Boolean) {
        helperUtil.setStayAwake(turnOn)
        //refresh the Activity UI if it is running...
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(Intent(INTENT_ACTION))
    }

    companion object {
        const val INTENT_ACTION = "com.duck.stayawakeadb.ADB_Activity"
        val intentFilter: IntentFilter
            get() = IntentFilter(INTENT_ACTION)
    }
}
