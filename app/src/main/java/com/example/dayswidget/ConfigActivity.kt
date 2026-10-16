package com.example.dayswidget

import android.app.Activity
import android.app.DatePickerDialog
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import java.time.LocalDate

class ConfigActivity : Activity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        widgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val prefs = getSharedPreferences(DaysWidget.PREFS, Context.MODE_PRIVATE)
        val initial = prefs.getString("date_$widgetId", null)
            ?.let { LocalDate.parse(it) } ?: LocalDate.now()

        val dialog = DatePickerDialog(
            this,
            { _, y, m, d ->
                val date = LocalDate.of(y, m + 1, d)
                prefs.edit().putString("date_$widgetId", date.toString()).apply()

                DaysWidget.updateWidget(this, AppWidgetManager.getInstance(this), widgetId)
                DaysWidget.scheduleMidnightUpdate(this)

                setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
                finish()
            },
            initial.year, initial.monthValue - 1, initial.dayOfMonth
        )
        dialog.setOnCancelListener { finish() }
        dialog.show()
    }
}
