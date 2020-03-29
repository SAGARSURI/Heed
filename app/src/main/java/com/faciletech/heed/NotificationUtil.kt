package com.faciletech.heed

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.faciletech.heed.ui.main.MainActivity
import com.faciletech.heed.utils.KeyConstants


class NotificationUtil private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: NotificationUtil? = null

        fun getInstance(context: Context): NotificationUtil {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NotificationUtil(context)
                    .also { INSTANCE = it }
            }
        }
    }

    private val mNotificationManager: NotificationManager =
        context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    private val DEFAULT_CHANNEL_ID = "default_channel"
    private val DEFAULT_CHANNEL_NAME = "Default"

    init {
        createNotificationChannel(mNotificationManager)
    }

    fun buildNotification(title: String, content: String, tipType: Int) {
        val intent = Intent(context, MainActivity::class.java)
        intent.putExtra(KeyConstants.TIP_TYPE, tipType)
        val pendingIntent =
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(context, DEFAULT_CHANNEL_ID)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentTitle(title)
            .setContentText(content)
            .setAutoCancel(true)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setContentIntent(pendingIntent)
            .build()

        mNotificationManager.notify(1, notification)
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(DEFAULT_CHANNEL_ID) == null) {
                notificationManager.createNotificationChannel(
                    NotificationChannel(
                        DEFAULT_CHANNEL_ID,
                        DEFAULT_CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_DEFAULT
                    )
                )
            }
        }
    }
}