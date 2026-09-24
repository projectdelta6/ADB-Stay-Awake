package com.duck.stayawakeadb.service

import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
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
    private val handler = Handler(Looper.getMainLooper())

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
        // Being connected means access is granted; the system can bind us before it persists the
        // grant, so don't gate on notificationPermissionGranted here
        restoreConnectionState(attempt = 1)
        super.onListenerConnected()
    }

    /**
     * The per-type connection flags live in memory, so rebuild them from the ADB notifications
     * that are already showing (e.g. after the process was killed while ADB stayed connected).
     *
     * On the first bind of a fresh process the system can call [onListenerConnected] before our
     * listener is fully registered, and [getActiveNotifications] comes back empty (not even the
     * charging notification). An entirely empty list is therefore retried a few times.
     */
    private fun restoreConnectionState(attempt: Int) {
        val active = try {
            activeNotifications ?: emptyArray()
        } catch (e: SecurityException) {
            Log.w("ADBNotificationListener", "Unable to read active notifications", e)
            return
        }
        if (active.isEmpty() && attempt < RESTORE_MAX_ATTEMPTS) {
            Log.d("ADBNotificationListener", "No active notifications yet (attempt $attempt), retrying")
            handler.postDelayed({ restoreConnectionState(attempt + 1) }, RESTORE_RETRY_DELAY_MS)
            return
        }
        val types = active.mapNotNull { adbConnectionType(it) }.toSet()
        SettingsHelperUtil.usbAdbConnected = AdbConnectionType.USB in types
        SettingsHelperUtil.wirelessAdbConnected = AdbConnectionType.WIRELESS in types
        SettingsHelperUtil.ADBConnectionState = types.isNotEmpty()
        Log.d("ADBNotificationListener", "Restored ADB connection state: $types")
    }

    override fun onListenerDisconnected() {
        Log.d(
            "ADBNotificationListener",
            "onListenerDisconnected - Notification listener service disconnected"
        )
        handler.removeCallbacksAndMessages(null)
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        Log.d(
            "ADBNotificationListener",
            "Notification posted: ${sbn.packageName} - ${sbn.notification.extras.getString("android.title")}"
        )
        val type = adbConnectionType(sbn) ?: return
        Log.d("ADBNotificationListener", "ADB $type connection detected, turning stay awake ON")
        setConnected(type, true)
        setAndSendBroadcast(true)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        Log.d(
            "ADBNotificationListener",
            "Notification removed: ${sbn.packageName} - ${sbn.notification.extras.getString("android.title")}"
        )
        val type = adbConnectionType(sbn) ?: return
        setConnected(type, false)
        if (SettingsHelperUtil.usbAdbConnected || SettingsHelperUtil.wirelessAdbConnected) {
            // The other ADB transport is still connected, so leave stay awake as it is
            Log.d("ADBNotificationListener", "ADB $type disconnected, another ADB connection remains")
            sendUiUpdateBroadcast()
            NotificationUtil.updateStayAwakeNotification(this)
        } else {
            Log.d("ADBNotificationListener", "ADB $type disconnected, turning stay awake OFF")
            setAndSendBroadcast(false)
        }
    }

    /**
     * Returns the ADB connection type if [sbn] is the system's ADB-connected notification, else null.
     */
    private fun adbConnectionType(sbn: StatusBarNotification): AdbConnectionType? {
        if (!sbn.packageName.equals("android", ignoreCase = true)) return null
        // Android 17+ reports debugging as disabled to apps, so the notification is the only signal there
        if (!settingsHelperUtil.debugStateHidden
            && !(settingsHelperUtil.developerOptionsEnabled
                    && (settingsHelperUtil.usbDebuggingEnabled || settingsHelperUtil.wirelessDebuggingEnabled))
        ) {
            return null
        }
        val title = sbn.notification.extras.getString("android.title") ?: return null
        return when {
            title.equals(applicationContext.getString(R.string.adb_notification_title), ignoreCase = true) ||
                    title.equals(
                        applicationContext.getString(R.string.adb_notification_title_huawei),
                        ignoreCase = true,
                    ) -> AdbConnectionType.USB

            title.equals(
                applicationContext.getString(R.string.adb_wifi_notification_title),
                ignoreCase = true,
            ) -> AdbConnectionType.WIRELESS

            else -> null
        }
    }

    private fun setConnected(type: AdbConnectionType, connected: Boolean) {
        when (type) {
            AdbConnectionType.USB -> SettingsHelperUtil.usbAdbConnected = connected
            AdbConnectionType.WIRELESS -> SettingsHelperUtil.wirelessAdbConnected = connected
        }
    }

    private fun sendUiUpdateBroadcast() {
        LocalBroadcastManager
            .getInstance(applicationContext)
            .sendBroadcast(Intent(INTENT_ACTION))
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
            } else if (settingsHelperUtil.stayAwakeEnabled == turnOn) {
                Log.d("ADBNotificationListener", "Stay awake already set to: $turnOn")
            } else {
                Log.e("ADBNotificationListener", "Failed to set stay awake to: $turnOn")
            }
        } else {
            Log.d("ADBNotificationListener", "Auto-toggle disabled, skipping stay awake change")
        }
        //update the Activity UI if it is running...
        sendUiUpdateBroadcast()
        
        NotificationUtil.updateStayAwakeNotification(this)
    }

    private enum class AdbConnectionType {
        USB,
        WIRELESS,
    }

    companion object {
        const val INTENT_ACTION = "com.duck.stayawakeadb.ADB_Activity"
        private const val RESTORE_MAX_ATTEMPTS = 5
        private const val RESTORE_RETRY_DELAY_MS = 1_000L
        val intentFilter: IntentFilter
            get() = IntentFilter(INTENT_ACTION)
    }
}
