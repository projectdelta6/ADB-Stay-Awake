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
        Log.d("ADBNotificationListener", "onCreate called")
        settingsHelperUtil = SettingsHelperUtil(applicationContext)
        //ensure the notification channel is created
        NotificationUtil.createNotificationChannel(this, notificationData)
    }

    override fun onDestroy() {
        Log.d("ADBNotificationListener", "onDestroy called")
        super.onDestroy()
    }

    override fun onListenerConnected() {
        Log.d(
            "ADBNotificationListener",
            "onListenerConnected - Notification listener service connected"
        )
        // Check if we have notification access permission
        if (!settingsHelperUtil.notificationPermissionGranted) {
            Log.w("ADBNotificationListener", "Notification access permission not granted!")
        } else {
            Log.d("ADBNotificationListener", "Notification access permission granted")
        }
        super.onListenerConnected()
    }

    override fun onListenerDisconnected() {
        Log.d(
            "ADBNotificationListener",
            "onListenerDisconnected - Notification listener service disconnected"
        )
        // Reset any cached state if needed
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        Log.d(
            "ADBNotificationListener",
            "Notification posted: ${sbn.packageName} - ${sbn.notification.extras.getString("android.title")}"
        )
        // Only process notifications if we have permission and are connected
        if (!settingsHelperUtil.notificationPermissionGranted) {
            Log.w(
                "ADBNotificationListener",
                "Ignoring notification - no notification access permission"
            )
            return
        }
        checkNotification(sbn) {
            Log.d("ADBNotificationListener", "ADB connection detected, turning stay awake ON")
            setAndSendBroadcast(true)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        Log.d(
            "ADBNotificationListener",
            "Notification removed: ${sbn.packageName} - ${sbn.notification.extras.getString("android.title")}"
        )
        // Only process notifications if we have permission and are connected
        if (!settingsHelperUtil.notificationPermissionGranted) {
            Log.w(
                "ADBNotificationListener",
                "Ignoring notification removal - no notification access permission"
            )
            return
        }
        checkNotification(sbn) {
            Log.d("ADBNotificationListener", "ADB disconnection detected, turning stay awake OFF")
            setAndSendBroadcast(false)
        }
    }

    private fun checkNotification(sbn: StatusBarNotification, onPositiveCheck: () -> Unit) {
        if (settingsHelperUtil.developerOptionsEnabled
            && (settingsHelperUtil.usbDebuggingEnabled || settingsHelperUtil.wirelessDebuggingEnabled)
            && sbn.packageName.equals("android", ignoreCase = true)
        ) {
            val title = sbn.notification.extras.getString("android.title") ?: return

            if (title.equals(
                    applicationContext.getString(R.string.adb_notification_title),
                    ignoreCase = true
                ) ||
                title.equals(
                    applicationContext.getString(R.string.adb_notification_title_huawei),
                    ignoreCase = true
                ) ||
                title.equals(
                    applicationContext.getString(R.string.adb_wifi_notification_title),
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

        // Check if auto-toggle is enabled before changing stay awake setting
        if (settingsHelperUtil.autoToggleStayAwake) {
            Log.d(
                "ADBNotificationListener",
                "Auto-toggle enabled, attempting to set stay awake to: $turnOn"
            )
            if (settingsHelperUtil.setStayAwake(turnOn)) {
                Log.d("ADBNotificationListener", "Successfully set stay awake to: $turnOn")
                //update the Activity UI if it is running...
                LocalBroadcastManager
                    .getInstance(applicationContext)
                    .sendBroadcast(Intent(INTENT_ACTION))
            } else {
                Log.e("ADBNotificationListener", "Failed to set stay awake to: $turnOn")
                //todo:?
                Log.e("Error", "settingsHelperUtil.setStayAwake($turnOn) returned false")
            }
        } else {
            Log.d("ADBNotificationListener", "Auto-toggle disabled, skipping stay awake change")
            // Still send broadcast to update UI state even if we don't change the setting
            LocalBroadcastManager
                .getInstance(applicationContext)
                .sendBroadcast(Intent(INTENT_ACTION))
        }
        
        NotificationUtil.updateStayAwakeNotification(this)
    }

    companion object {
        const val INTENT_ACTION = "com.duck.stayawakeadb.ADB_Activity"
        val intentFilter: IntentFilter
            get() = IntentFilter(INTENT_ACTION)
    }
}
