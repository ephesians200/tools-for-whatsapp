package com.prince.whatstoolbox
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/** Home-screen widget: one tap opens Direct Chat. */
class DirectChatWidget : AppWidgetProvider() {
    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) {
        ids.forEach { id ->
            val v = RemoteViews(c.packageName, R.layout.widget_direct_chat)
            val pi = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java).setAction(MainActivity.ACTION_OPEN_TOOL).putExtra("tool", "DirectChat")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            v.setOnClickPendingIntent(R.id.widget_root, pi); m.updateAppWidget(id, v)
        }
    }
}
