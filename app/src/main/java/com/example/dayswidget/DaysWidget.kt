package com.example.dayswidget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Calendar
import kotlin.math.abs

class DaysWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateWidget(context, manager, it) }
        scheduleMidnightUpdate(context)
    }

    override fun onDeleted(context: Context, ids: IntArray) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        ids.forEach { prefs.remove("date_$it") }
        prefs.apply()
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_MIDNIGHT) updateAll(context)
    }

    companion object {
        const val PREFS = "days_widget"
        const val ACTION_MIDNIGHT = "com.example.dayswidget.MIDNIGHT_UPDATE"

        fun daysLabel(n: Long): String {
            val a = abs(n)
            val m100 = a % 100
            val m10 = a % 10
            return if (m100 in 11..14) "дней" else when (m10) {
                1L -> "день"
                2L, 3L, 4L -> "дня"
                else -> "дней"
            }
        }

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, DaysWidget::class.java))
            ids.forEach { updateWidget(context, manager, it) }
            scheduleMidnightUpdate(context)
        }

        fun updateWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val dateStr = prefs.getString("date_$id", null)
            val views = RemoteViews(context.packageName, R.layout.widget_days)

            if (dateStr != null) {
                val days = ChronoUnit.DAYS.between(
                    LocalDate.parse(dateStr), LocalDate.now(ZoneId.systemDefault())
                )
                views.setTextViewText(R.id.tvDays, days.toString())
                views.setTextViewText(R.id.tvLabel, daysLabel(days))
            } else {
                views.setTextViewText(R.id.tvDays, "?")
                views.setTextViewText(R.id.tvLabel, "нажмите")
            }

            // Нажатие на виджет открывает выбор даты
            val cfg = Intent(context, ConfigActivity::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                data = android.net.Uri.parse("widget://$id")
            }
            val pi = PendingIntent.getActivity(
                context, id, cfg,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.root, pi)

            manager.updateAppWidget(id, views)
        }

        fun scheduleMidnightUpdate(context: Context) {
            val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, DaysWidget::class.java).apply { action = ACTION_MIDNIGHT }
            val pi = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 5)
            }
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) DaysWidget.updateAll(context)
    }
}
