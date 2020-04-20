package com.duck.stayawakeadb.data

import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.duck.stayawakeadb.data.base.BaseNotificationData

class StayAwakeNotificationData : BaseNotificationData() {

    /**
     * Notification Standard notification get methods:
     */
    override lateinit var contentTitle: String
    override lateinit var contentText: String
    lateinit var mBigContentTitle: String
    lateinit var mBigText: String
    lateinit var mSummaryText: String

    override val priority: Int = NotificationCompat.PRIORITY_LOW

    /**
     * Channel values (O and above) get methods:
     */
    override val channelId: String = "adb_stay_awake_channel_1"
    override val channelName: CharSequence = "Stay Awake ADB notification channel"
    override val channelDescription: String =
        "Notification channel for the Stay Awake ADB setting notifications"
    override val channelImportance: Int = NotificationManager.IMPORTANCE_DEFAULT
    override val isChannelEnableVibrate: Boolean = false
    override val channelLockscreenVisibility: Int = NotificationCompat.VISIBILITY_PUBLIC

    fun setData(
        contentTitle: String,
        contentText: String,
        mBigContentTitle: String,
        mBigText: String,
        mSummaryText: String
    ) {
        this.contentTitle = contentTitle
        this.contentText = contentText
        this.mBigContentTitle = mBigContentTitle
        this.mBigText = mBigText
        this.mSummaryText = mSummaryText
    }
}