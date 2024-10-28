package io.linksquared.handlers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.GsonBuilder
import io.linksquared.R
import io.linksquared.model.DebugLogger
import io.linksquared.model.LogLevel
import java.time.Instant
import kotlin.random.Random

class MessagingService: FirebaseMessagingService() {

    override fun onCreate() {
        super.onCreate()

    }

    override fun onMessageReceived(message: RemoteMessage) {
        DebugLogger.instance.log(LogLevel.INFO, "Push notification handled by linksquared FirebaseMessagingService service.")
        if (handleLinksquaredNotification(message)) {
            DebugLogger.instance.log(LogLevel.INFO, "Push notification if from linksquared -> handled.")
        } else {
            DebugLogger.instance.log(LogLevel.INFO, "Push notification if NOT from linksquared -> ignored.")
        }
    }

    override fun onDestroy() {
        super.onDestroy()

    }

}

fun FirebaseMessagingService.handleLinksquaredNotification(message: RemoteMessage): Boolean {
    val data = message.data
    if (data["linksquared"] == null) {
        return false
    }

    DebugLogger.instance.log(LogLevel.INFO, "Received push notification: ${message.notification} data: ${message.data} ")

    // Retrieve the drawable name from meta-data
    val applicationInfo = packageManager.getApplicationInfo(
        packageName,
        PackageManager.GET_META_DATA
    )
    val iconName = applicationInfo.metaData?.getString("io.linksquared.NotificationIconSmall")
    // Get the drawable resource ID
    val iconResId = iconName?.let { resources.getIdentifier(it, "drawable", packageName) }

    handleLinksquaredNotification(message.notification?.title, message.notification?.body, iconResId ?: R.drawable.ic_linksquared_notification_default_small)

    return true
}

private fun FirebaseMessagingService.handleLinksquaredNotification(title: String?, body: String?, smallIcon: Int) {
    val channelId = "LinksquaredChannel"

    // Build the notification
    val notificationBuilder = NotificationCompat.Builder(this, channelId)
        .setSmallIcon(smallIcon)
        .setContentTitle(title)
        .setContentText(body)
        .setAutoCancel(true)

    // Send the notification
    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val channel = NotificationChannel(channelId, "Linksquared Channel", NotificationManager.IMPORTANCE_HIGH)
    channel.description = "Channel for Linksquared messages"
    channel.enableLights(true)
    channel.lightColor = getColor(R.color.linksquared_notification_icon_tint)
    channel.enableVibration(true)

    notificationManager.createNotificationChannel(channel)

    var notificationId = Random.nextInt(1000, Int.MAX_VALUE)
    notificationManager.notify(notificationId, notificationBuilder.build())
}