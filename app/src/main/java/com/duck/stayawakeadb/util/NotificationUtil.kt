/*
 * Copyright (C) 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.duck.stayawakeadb.util

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.duck.stayawakeadb.R
import com.duck.stayawakeadb.activity.MainActivity
import com.duck.stayawakeadb.constant.Constants.notificationData
import com.duck.stayawakeadb.data.base.BaseNotificationData
import com.duck.stayawakeadb.service.NotificationActionService

/**
 * Simplifies common [Notification] tasks.
 */
object NotificationUtil {

    const val NOTIFICATION_ID: Int = 8086

    fun createNotificationChannel(
        context: Context,
        notificationData: BaseNotificationData
    ) {

        // NotificationChannels are required for Notifications on O (API 26) and above.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // The user-visible name of the channel.
            val channelName = notificationData.channelName
            // The user-visible description of the channel.
            val channelDescription = notificationData.channelDescription
            val channelImportance = notificationData.channelImportance
            val channelEnableVibrate = notificationData.isChannelEnableVibrate
            val channelLockscreenVisibility = notificationData.channelLockscreenVisibility

            // Initializes NotificationChannel.
            val notificationChannel =
                NotificationChannel(notificationData.channelId, channelName, channelImportance)
            notificationChannel.description = channelDescription
            notificationChannel.enableVibration(channelEnableVibrate)
            notificationChannel.lockscreenVisibility = channelLockscreenVisibility

            // Adds NotificationChannel to system. Attempting to create an existing notification
            // channel with its original values performs no operation, so it's safe to perform the
            // below sequence.
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    @SuppressLint("MissingPermission")
    fun updateStayAwakeNotification(context: Context) {
        val settingsHelperUtil: SettingsHelperUtil =
            SettingsHelperUtil(context.applicationContext)
        if(settingsHelperUtil.showNotification && SettingsHelperUtil.ADBConnectionState) {

            val actionText: String =
                if (settingsHelperUtil.stayAwakeEnabled) {
                    notificationData.setData(
                        contentTitle = context.getString(R.string.awake_on_notification_title),
                        contentText = context.getString(R.string.awake_on_notification_text),
                        mBigContentTitle = context.getString(R.string.awake_on_notification_title),
                        mBigText = context.getString(R.string.awake_on_notification_big_text),
                        mSummaryText = context.getString(R.string.awake_on_notification_summary)
                    )
                    context.getString(R.string.awake_on_notification_action_text)
                } else {
                    notificationData.setData(
                        contentTitle = context.getString(R.string.awake_off_notification_title),
                        contentText = context.getString(R.string.awake_off_notification_text),
                        mBigContentTitle = context.getString(R.string.awake_off_notification_title),
                        mBigText = context.getString(R.string.awake_off_notification_big_text),
                        mSummaryText = context.getString(R.string.awake_off_notification_summary)
                    )
                    context.getString(R.string.awake_off_notification_action_text)
                }

            val mainIntent: Intent = Intent(context, MainActivity::class.java)
            mainIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            val notifyPendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.getActivity(
                    context,
                    0,
                    mainIntent,
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            } else {
                PendingIntent.getActivity(
                    context,
                    0,
                    mainIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            }

            val toggleIntent: Intent = Intent(context, NotificationActionService::class.java)
                .setAction(NotificationActionService.ACTION_TOGGLE_STAY_AWAKE)
            val togglePendingIntent: PendingIntent =
                PendingIntent.getService(context, 0, toggleIntent, PendingIntent.FLAG_IMMUTABLE)
            val toggleAction: NotificationCompat.Action = NotificationCompat.Action.Builder(
                R.drawable.ic_adb,
                actionText,
                togglePendingIntent
            ).build()

            val disableNotificationIntent: Intent =
                Intent(context, NotificationActionService::class.java)
                    .setAction(NotificationActionService.ACTION_DISABLE_NOTIFICATION)
            val disableNotificationPendingIntent: PendingIntent =
                PendingIntent.getService(
                    context,
                    0,
                    disableNotificationIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            val dissableNotificationAction: NotificationCompat.Action =
                NotificationCompat.Action.Builder(
                    R.drawable.ic_cancel_black,
                    "Don't show this notification",
                    disableNotificationPendingIntent
                ).build()


            val notificationCompatBuilder: NotificationCompat.Builder = NotificationCompat.Builder(
                context, notificationData.channelId
            )

            val notification =
                notificationCompatBuilder
                    .setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText(notificationData.mBigText)
                            .setBigContentTitle(notificationData.mBigContentTitle)
                            .setSummaryText(notificationData.mSummaryText)
                    )
                    .setContentTitle(notificationData.mBigContentTitle)
                    .setContentText(notificationData.contentText)
                    .setSmallIcon(R.drawable.ic_adb)
                    .setLargeIcon(
                        BitmapFactory.decodeResource(
                            context.resources,
                            R.mipmap.ic_launcher
                        )
                    )
                    .setContentIntent(notifyPendingIntent)
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setColor(
                        ContextCompat.getColor(
                            context.applicationContext,
                            R.color.colorPrimary
                        )
                    )
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setPriority(notificationData.priority)
                    .setVisibility(notificationData.channelLockscreenVisibility)
                    .addAction(toggleAction)
                    .addAction(dissableNotificationAction)
                    .setSilent(true)
                    .build()

            notification.flags = notification.flags or Notification.FLAG_NO_CLEAR or Notification.FLAG_ONGOING_EVENT

            NotificationManagerCompat.from(context.applicationContext)
                .notify(NOTIFICATION_ID, notification)
        } else {
            NotificationManagerCompat.from(context.applicationContext).cancel(NOTIFICATION_ID)
        }
    }
}