package com.minimal.today.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.minimal.today.R
import com.minimal.today.data.TodoRepository

class TodayWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val repository = TodoRepository(context.applicationContext)
        val todos = repository.getTodayTodos()

        val prefs = context.getSharedPreferences("today_prefs", Context.MODE_PRIVATE)
        val isDark = prefs.getBoolean("dark_theme", false)

        val bitmap = WidgetHelper.renderSquareWidgetBitmap(context, todos, isDark)
        val pendingIntent = WidgetHelper.getLaunchPendingIntent(context)

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_today_layout).apply {
                setImageViewBitmap(R.id.widget_today_image, bitmap)
                setOnClickPendingIntent(R.id.widget_today_root, pendingIntent)
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
