package com.duck.stayawakeadb.service

import android.app.IntentService
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.duck.stayawakeadb.util.NotificationUtil
import com.duck.stayawakeadb.util.SettingsHelperUtil

class NotificationActionService: IntentService("StayAwakeNotificationActionService") {

    @Deprecated("Deprecated in Java")
    override fun onHandleIntent(intent: Intent?) {
        if(intent != null) {
            val settingsHelperUtil: SettingsHelperUtil = SettingsHelperUtil(this)
            if(ACTION_TOGGLE_STAY_AWAKE == intent.action) {
                if(settingsHelperUtil.setStayAwake(!settingsHelperUtil.stayAwakeEnabled)) {
                    //update the Activity UI if it is running...
                    LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(Intent(
                        ADBNotificationListener.INTENT_ACTION
                    ))
                }
                NotificationUtil.updateStayAwakeNotification(this)
            } else if(ACTION_DISABLE_NOTIFICATION == intent.action) {
                settingsHelperUtil.showNotification = false
                //update the Activity UI if it is running...
                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(Intent(
                    ADBNotificationListener.INTENT_ACTION
                ))
            }
        }
    }

    companion object {
        const val ACTION_DISABLE_NOTIFICATION: String = "com.duck.stayawakeadb.action.disable.notification"
        const val ACTION_TOGGLE_STAY_AWAKE: String = "com.duck.stayawakeadb.action.toggle.stay.awake"
    }
}