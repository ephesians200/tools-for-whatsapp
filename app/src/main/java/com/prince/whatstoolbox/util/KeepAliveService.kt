package com.prince.whatstoolbox.util
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.prince.whatstoolbox.R

/** Foreground "keep-alive" while a long video/audio job runs, so Android doesn't kill the process when the user switches apps. */
class KeepAliveService : Service() {
    override fun onBind(i: Intent?): IBinder? = null
    override fun onStartCommand(i: Intent?, flags: Int, startId: Int): Int {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH, getString(R.string.keepalive_channel), NotificationManager.IMPORTANCE_LOW))
        val open = packageManager.getLaunchIntentForPackage(packageName)?.let { PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT) }
        val b = Notification.Builder(this, CH).setSmallIcon(R.drawable.ic_notification).setContentTitle(getString(R.string.app_name))
            .setContentText(i?.getStringExtra("t") ?: "Working…").setOngoing(true).setOnlyAlertOnce(true)
            .setProgress(0, 0, true).setCategory(Notification.CATEGORY_PROGRESS).setContentIntent(open)
        if (Build.VERSION.SDK_INT >= 31) b.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)   // otherwise Android 12+ hides it for ~10 s
        val n = b.build()
        runCatching { ServiceCompat.startForeground(this, 42, n, if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0) }
        return START_NOT_STICKY
    }
    companion object {
        private const val CH = "keepalive"
        fun start(c: Context, text: String) { runCatching { ContextCompat.startForegroundService(c, Intent(c, KeepAliveService::class.java).putExtra("t", text)) } }
        fun stop(c: Context) { runCatching { c.stopService(Intent(c, KeepAliveService::class.java)) } }
    }
}
