package com.duck.stayawakeadb.data.base

abstract class BaseNotificationData {
    /**
     * Notification Standard notification get methods:
     */
    abstract val contentTitle: String
    abstract val contentText: String
    abstract val priority: Int
    /**
     * Channel values (O and above) get methods:
     */
    abstract val channelId: String
    abstract val channelName: CharSequence
    abstract val channelDescription: String
    abstract val channelImportance: Int
    abstract val isChannelEnableVibrate: Boolean
    abstract val channelLockscreenVisibility: Int
}