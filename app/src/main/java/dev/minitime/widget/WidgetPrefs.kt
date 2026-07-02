package dev.minitime.widget

import android.content.Context
import android.graphics.Color

data class WidgetConfig(
    val zone1: String,
    val zone2: String,
    val textColor: Int,
    val bgColor: Int,
    val bgAlpha: Int,
    val fontIndex: Int,
    val is24h: Boolean
)

object WidgetPrefs {
    private const val FILE = "widgets"

    val DEFAULT = WidgetConfig(
        zone1 = "Europe/Moscow",
        zone2 = "America/New_York",
        textColor = Color.WHITE,
        bgColor = Color.BLACK,
        bgAlpha = 160,
        fontIndex = 0,
        is24h = true
    )

    fun save(context: Context, widgetId: Int, c: WidgetConfig) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
            .putString("$widgetId:z1", c.zone1)
            .putString("$widgetId:z2", c.zone2)
            .putInt("$widgetId:tc", c.textColor)
            .putInt("$widgetId:bc", c.bgColor)
            .putInt("$widgetId:ba", c.bgAlpha)
            .putInt("$widgetId:f", c.fontIndex)
            .putBoolean("$widgetId:h24", c.is24h)
            .apply()
    }

    fun load(context: Context, widgetId: Int): WidgetConfig {
        val p = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        return WidgetConfig(
            zone1 = p.getString("$widgetId:z1", DEFAULT.zone1)!!,
            zone2 = p.getString("$widgetId:z2", DEFAULT.zone2)!!,
            textColor = p.getInt("$widgetId:tc", DEFAULT.textColor),
            bgColor = p.getInt("$widgetId:bc", DEFAULT.bgColor),
            bgAlpha = p.getInt("$widgetId:ba", DEFAULT.bgAlpha),
            fontIndex = p.getInt("$widgetId:f", DEFAULT.fontIndex),
            is24h = p.getBoolean("$widgetId:h24", DEFAULT.is24h)
        )
    }

    fun delete(context: Context, widgetId: Int) {
        val p = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val e = p.edit()
        for (key in p.all.keys) {
            if (key.startsWith("$widgetId:")) e.remove(key)
        }
        e.apply()
    }
}
