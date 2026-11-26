package com.prince.whatstoolbox.util
import android.app.*
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class ReminderWorker(c: Context, p: WorkerParameters) : CoroutineWorker(c, p) {
    override suspend fun doWork(): Result {
        val n = inputData.getString("n").orEmpty(); val note = inputData.getString("t").orEmpty(); val ctx = applicationContext
        val nm = ctx.getSystemService(NotificationManager::class.java); nm.createNotificationChannel(NotificationChannel("rem", "Follow-ups", NotificationManager.IMPORTANCE_DEFAULT))
        val pi = PendingIntent.getActivity(ctx, n.hashCode(), Intent(Intent.ACTION_VIEW, Wa.link(n)), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        nm.notify(n.hashCode(), Notification.Builder(ctx, "rem").setSmallIcon(com.prince.whatstoolbox.R.drawable.ic_notification).setContentTitle("Follow up: +$n").setContentText(note).setContentIntent(pi).setAutoCancel(true).build())
        return Result.success()
    }
}
