package com.duck.stayawakeadb.service

import android.content.Intent
import android.content.IntentFilter
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.duck.stayawakeadb.R
import com.duck.stayawakeadb.constant.Constants.notificationData
import com.duck.stayawakeadb.util.NotificationUtil
import com.duck.stayawakeadb.util.SettingsHelperUtil

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
        checkNotification(sbn) { setAndSendBroadcast(true) }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        checkNotification(sbn) { setAndSendBroadcast(false) }
    }

    private fun checkNotification(sbn: StatusBarNotification, onPositiveCheck: () -> Unit) {
        if (settingsHelperUtil.developerOptionsEnabled
            && settingsHelperUtil.usbDebuggingEnabled
            && sbn.packageName.equals("android", ignoreCase = true)
        ) {
            val title = sbn.notification.extras.getString("android.title") ?: return

            if (title.equals(
                    applicationContext.getString(R.string.adb_notification_title),
                    ignoreCase = true
                )
            ) {
                onPositiveCheck()
            }
        }
    }

    private fun setAndSendBroadcast(turnOn: Boolean) {
        //save the ADB connection state
        SettingsHelperUtil.ADBConnectionState = turnOn
        if (settingsHelperUtil.setStayAwake(turnOn)) {
            //update the Activity UI if it is running...
            LocalBroadcastManager
                .getInstance(applicationContext)
                .sendBroadcast(Intent(INTENT_ACTION))
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
