package com.duck.stayawakeadb.service

import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.duck.stayawakeadb.constant.Constants.notificationData
import com.duck.stayawakeadb.util.SettingsHelperUtil
import com.duck.stayawakeadb.util.NotificationUtil

/**
 * Created by Bradley Duck on 2019/02/18.
 */
class ADBNotificationListener : android.service.notification.NotificationListenerService() {

    private lateinit var settingsHelperUtil: SettingsHelperUtil

    override fun onCreate() {
        super.onCreate()
        settingsHelperUtil = SettingsHelperUtil(applicationContext)
        //ensure the notification channel is created
        NotificationUtil.createNotificationChannel(this, notificationData)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (settingsHelperUtil.developerOptionsEnabled && settingsHelperUtil.usbDebuggingEnabled) {
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
        if (settingsHelperUtil.developerOptionsEnabled) {
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
        //save the ADB connection state
        SettingsHelperUtil.ADBConnectionState = turnOn
        if (settingsHelperUtil.setStayAwake(turnOn)) {
            //update the Activity UI if it is running...
            LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(Intent(
                INTENT_ACTION
            ))
        } else {
            //todo:?
            Log.e("Error", "settingsHelperUtil.setStayAwake($turnOn) returned false")
        }
        NotificationUtil.updateStayAwakeNotification(this)
    }

    companion object {
        const val INTENT_ACTION = "com.duck.stayawakeadb.ADB_Activity"
        val intentFilter: IntentFilter
            get() = IntentFilter(INTENT_ACTION)
    }
}
