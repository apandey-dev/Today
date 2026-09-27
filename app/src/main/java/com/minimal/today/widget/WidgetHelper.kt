package com.minimal.today.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.minimal.today.MainActivity
import com.minimal.today.R
import com.minimal.today.model.TodoItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WidgetHelper {

    private fun getMaliFont(context: Context, isMedium: Boolean): Typeface {
        return try {
            val fontRes = if (isMedium) R.font.mali_medium else R.font.mali_regular
            ResourcesCompat.getFont(context, fontRes) ?: Typeface.DEFAULT
        } catch (_: Exception) {
            Typeface.DEFAULT
        }
    }

    /**
     * Renders 1:1 square Canvas Bitmap for the Today widget:
     * - "today"
     * - date below (e.g. "sat, 26 sep")
     * - "done  <count>"
     * - "undone  <count>"
     */
    fun renderSquareWidgetBitmap(context: Context, todos: List<TodoItem>, isDark: Boolean): Bitmap {
        val size = 360
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val total = todos.size
        val doneCount = todos.count { it.isDone }
        val undoneCount = total - doneCount

        val fontMedium = getMaliFont(context, true)
        val fontRegular = getMaliFont(context, false)

        val bgCardColor = if (isDark) Color.parseColor("#141414") else Color.parseColor("#FFFFFF")
        val textPrimary = if (isDark) Color.parseColor("#F5F5F5") else Color.parseColor("#121212")
        val textSecondary = if (isDark) Color.parseColor("#888888") else Color.parseColor("#777777")
        val dividerColor = if (isDark) Color.parseColor("#282828") else Color.parseColor("#ECECEC")
        val greenAccent = Color.parseColor("#22C55E")

        // 1. Draw rounded square card
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgCardColor
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = dividerColor
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
        }
        val cardRect = RectF(2f, 2f, (size - 2).toFloat(), (size - 2).toFloat())
        canvas.drawRoundRect(cardRect, 44f, 44f, bgPaint)
        canvas.drawRoundRect(cardRect, 44f, 44f, borderPaint)

        // 2. Header: "today"
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textPrimary
            typeface = fontMedium
            textSize = 36f
        }
        canvas.drawText("today", 34f, 60f, headerPaint)

        // Date below: e.g. "sat, 26 sep"
        val dateStr = try {
            SimpleDateFormat("EEE, d MMM", Locale.ENGLISH).format(Date()).lowercase()
        } catch (_: Exception) {
            ""
        }
        val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textSecondary
            typeface = fontRegular
            textSize = 22f
        }
        canvas.drawText(dateStr, 34f, 94f, datePaint)

        // Subtle Divider
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = dividerColor
            strokeWidth = 2f
        }
        canvas.drawLine(34f, 122f, (size - 34).toFloat(), 122f, linePaint)

        // 3. Done Row
        val doneLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = greenAccent
            typeface = fontMedium
            textSize = 28f
        }
        canvas.drawText("done", 34f, 190f, doneLabelPaint)

        val doneNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = greenAccent
            typeface = fontMedium
            textSize = 28f
        }
        val doneStr = doneCount.toString()
        val doneNumW = doneNumPaint.measureText(doneStr)
        canvas.drawText(doneStr, (size - 34 - doneNumW), 190f, doneNumPaint)

        // Subtle row separator
        val sepPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (isDark) Color.parseColor("#222222") else Color.parseColor("#F4F4F4")
            strokeWidth = 1.5f
        }
        canvas.drawLine(34f, 222f, (size - 34).toFloat(), 222f, sepPaint)

        // 4. Undone Row
        val undoneLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textSecondary
            typeface = fontMedium
            textSize = 28f
        }
        canvas.drawText("undone", 34f, 280f, undoneLabelPaint)

        val undoneNumPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textPrimary
            typeface = fontMedium
            textSize = 28f
        }
        val undoneStr = undoneCount.toString()
        val undoneNumW = undoneNumPaint.measureText(undoneStr)
        canvas.drawText(undoneStr, (size - 34 - undoneNumW), 280f, undoneNumPaint)

        return bitmap
    }

    /**
     * Broadcasts update to all active widgets
     */
    fun updateAllWidgets(context: Context) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widgetComponent = ComponentName(context, TodayWidgetProvider::class.java)
            val widgetIds = appWidgetManager.getAppWidgetIds(widgetComponent)
            if (widgetIds.isNotEmpty()) {
                val intent = Intent(context, TodayWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
                }
                context.sendBroadcast(intent)
            }
        } catch (_: Exception) {}
    }

    fun getLaunchPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
