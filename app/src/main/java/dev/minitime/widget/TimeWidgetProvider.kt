package dev.minitime.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class TimeWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) {
            appWidgetManager.updateAppWidget(id, buildViews(context, id))
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (id in appWidgetIds) WidgetPrefs.delete(context, id)
    }

    companion object {
        val FONT_LAYOUTS = intArrayOf(
            R.layout.widget_font_sans,
            R.layout.widget_font_light,
            R.layout.widget_font_condensed,
            R.layout.widget_font_serif,
            R.layout.widget_font_mono
        )

        fun buildViews(context: Context, widgetId: Int): RemoteViews {
            val c = WidgetPrefs.load(context, widgetId)
            val rv = RemoteViews(context.packageName, FONT_LAYOUTS[c.fontIndex])

            val format = if (c.is24h) "HH:mm" else "h:mm"
            val labelColor = c.textColor and 0x00FFFFFF or (0xB4 shl 24)

            for ((clockId, labelId, zone) in listOf(
                Triple(R.id.clock1, R.id.label1, c.zone1),
                Triple(R.id.clock2, R.id.label2, c.zone2)
            )) {
                rv.setString(clockId, "setTimeZone", zone)
                rv.setCharSequence(clockId, "setFormat12Hour", format)
                rv.setCharSequence(clockId, "setFormat24Hour", format)
                rv.setTextColor(clockId, c.textColor)
                rv.setTextViewText(labelId, cityName(zone))
                rv.setTextColor(labelId, labelColor)
            }

            rv.setInt(R.id.widget_bg, "setColorFilter", c.bgColor or (0xFF shl 24))
            rv.setInt(R.id.widget_bg, "setImageAlpha", c.bgAlpha)

            val intent = Intent(context, WidgetConfigActivity::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            rv.setOnClickPendingIntent(
                R.id.widget_root,
                PendingIntent.getActivity(
                    context, widgetId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            return rv
        }

        fun cityName(zoneId: String): String =
            zoneId.substringAfterLast('/').replace('_', ' ')
    }
}
