package com.duck.stayawakeadb.service

import android.app.IntentService
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.duck.stayawakeadb.util.NotificationUtil
import com.duck.stayawakeadb.util.SettingsHelperUtil

class NotificationActionService: IntentService("StayAwakeNotificationActionService") {

    override fun onHandleIntent(intent: Intent?) {
        if(intent != null) {
            if(ACTION_TOGGLE_STAY_AWAKE == intent.action) {
                val settingsHelperUtil: SettingsHelperUtil = SettingsHelperUtil(this)
                if(settingsHelperUtil.setStayAwake(!settingsHelperUtil.stayAwakeEnabled)) {
                    //update the Activity UI if it is running...
                    LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(Intent(
                        ADBNotificationListener.INTENT_ACTION
                    ))
                }
                Log.v("testing","Updating notification from action Service")
                NotificationUtil.updateStayAwakeNotification(this)
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE_STAY_AWAKE: String = "com.duck.stayawakeadb.action.toggle.stay.awake"
    }
}