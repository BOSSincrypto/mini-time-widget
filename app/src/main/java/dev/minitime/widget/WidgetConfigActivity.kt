package dev.minitime.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextClock
import android.widget.TextView

class WidgetConfigActivity : Activity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var config: WidgetConfig

    private lateinit var previewContainer: FrameLayout
    private lateinit var spinnerZone1: Spinner
    private lateinit var spinnerZone2: Spinner
    private lateinit var spinnerFont: Spinner
    private lateinit var rowTextColor: LinearLayout
    private lateinit var rowBgColor: LinearLayout
    private lateinit var seekAlpha: SeekBar
    private lateinit var check24h: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        widgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        config = WidgetPrefs.load(this, widgetId)
        setContentView(R.layout.activity_config)

        previewContainer = findViewById(R.id.preview_container)
        spinnerZone1 = findViewById(R.id.spinner_zone1)
        spinnerZone2 = findViewById(R.id.spinner_zone2)
        spinnerFont = findViewById(R.id.spinner_font)
        rowTextColor = findViewById(R.id.row_text_color)
        rowBgColor = findViewById(R.id.row_bg_color)
        seekAlpha = findViewById(R.id.seek_alpha)
        check24h = findViewById(R.id.check_24h)

        setupZoneSpinner(spinnerZone1, config.zone1) { config = config.copy(zone1 = it) }
        setupZoneSpinner(spinnerZone2, config.zone2) { config = config.copy(zone2 = it) }
        setupFontSpinner()
        setupColorRow(rowTextColor, TEXT_COLORS, config.textColor) {
            config = config.copy(textColor = it)
        }
        setupColorRow(rowBgColor, BG_COLORS, config.bgColor) {
            config = config.copy(bgColor = it)
        }

        seekAlpha.progress = config.bgAlpha
        seekAlpha.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    config = config.copy(bgAlpha = progress)
                    refreshPreview()
                }
            }

            override fun onStartTrackingTouch(s: SeekBar) {}
            override fun onStopTrackingTouch(s: SeekBar) {}
        })

        check24h.isChecked = config.is24h
        check24h.setOnCheckedChangeListener { _: CompoundButton, checked: Boolean ->
            config = config.copy(is24h = checked)
            refreshPreview()
        }

        findViewById<Button>(R.id.btn_save).setOnClickListener { saveAndFinish() }

        refreshPreview()
    }

    private fun setupZoneSpinner(spinner: Spinner, selected: String, onSelect: (String) -> Unit) {
        val labels = ZONES.map { "${TimeWidgetProvider.cityName(it)}  ($it)" }
        spinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, labels
        )
        spinner.setSelection(ZONES.indexOf(selected).coerceAtLeast(0))
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                onSelect(ZONES[pos])
                refreshPreview()
            }

            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    private fun setupFontSpinner() {
        spinnerFont.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, FONT_NAMES
        )
        spinnerFont.setSelection(config.fontIndex)
        spinnerFont.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                config = config.copy(fontIndex = pos)
                refreshPreview()
            }

            override fun onNothingSelected(p: AdapterView<*>?) {}
        }
    }

    private fun setupColorRow(
        row: LinearLayout,
        colors: IntArray,
        selected: Int,
        onSelect: (Int) -> Unit
    ) {
        row.removeAllViews()
        val pad = (resources.displayMetrics.density * 3).toInt()
        for (color in colors) {
            val swatch = View(this)
            swatch.setBackgroundColor(color or (0xFF shl 24))
            val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
            lp.setMargins(pad, pad, pad, pad)
            swatch.layoutParams = lp
            swatch.alpha = if (color == selected) 1f else 0.5f
            swatch.setOnClickListener {
                onSelect(color)
                for (i in 0 until row.childCount) row.getChildAt(i).alpha = 0.5f
                swatch.alpha = 1f
                refreshPreview()
            }
            row.addView(swatch)
        }
    }

    private fun refreshPreview() {
        previewContainer.removeAllViews()
        val view = layoutInflater.inflate(
            TimeWidgetProvider.FONT_LAYOUTS[config.fontIndex], previewContainer, false
        )
        val format = if (config.is24h) "HH:mm" else "h:mm"
        val labelColor = config.textColor and 0x00FFFFFF or (0xB4 shl 24)
        for ((clockId, labelId, zone) in listOf(
            Triple(R.id.clock1, R.id.label1, config.zone1),
            Triple(R.id.clock2, R.id.label2, config.zone2)
        )) {
            view.findViewById<TextClock>(clockId).apply {
                timeZone = zone
                format12Hour = format
                format24Hour = format
                setTextColor(config.textColor)
            }
            view.findViewById<TextView>(labelId).apply {
                text = TimeWidgetProvider.cityName(zone)
                setTextColor(labelColor)
            }
        }
        view.findViewById<ImageView>(R.id.widget_bg).apply {
            setColorFilter(config.bgColor or (0xFF shl 24))
            imageAlpha = config.bgAlpha
        }
        previewContainer.addView(view)
    }

    private fun saveAndFinish() {
        WidgetPrefs.save(this, widgetId, config)
        AppWidgetManager.getInstance(this)
            .updateAppWidget(widgetId, TimeWidgetProvider.buildViews(this, widgetId))
        setResult(
            RESULT_OK,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        )
        finish()
    }

    companion object {
        val FONT_NAMES = listOf("Sans", "Light", "Condensed", "Serif", "Mono")

        val TEXT_COLORS = intArrayOf(
            Color.WHITE, 0xFF000000.toInt(), 0xFF9E9E9E.toInt(), 0xFFFFC107.toInt(),
            0xFF4DD0E1.toInt(), 0xFF81C784.toInt(), 0xFFE57373.toInt(), 0xFF64B5F6.toInt()
        )

        val BG_COLORS = intArrayOf(
            0xFF000000.toInt(), Color.WHITE, 0xFF263238.toInt(), 0xFF1A237E.toInt(),
            0xFF004D40.toInt(), 0xFF3E2723.toInt(), 0xFF880E4F.toInt(), 0xFF37474F.toInt()
        )

        val ZONES = listOf(
            "Pacific/Honolulu", "America/Anchorage", "America/Los_Angeles", "America/Denver",
            "America/Chicago", "America/New_York", "America/Toronto", "America/Mexico_City",
            "America/Bogota", "America/Lima", "America/Sao_Paulo", "America/Argentina/Buenos_Aires",
            "UTC", "Europe/London", "Europe/Lisbon", "Europe/Paris", "Europe/Berlin",
            "Europe/Madrid", "Europe/Rome", "Europe/Warsaw", "Europe/Kyiv", "Europe/Istanbul",
            "Europe/Moscow", "Asia/Yekaterinburg", "Asia/Novosibirsk", "Asia/Krasnoyarsk",
            "Asia/Irkutsk", "Asia/Yakutsk", "Asia/Vladivostok", "Asia/Dubai", "Asia/Tehran",
            "Asia/Karachi", "Asia/Kolkata", "Asia/Dhaka", "Asia/Bangkok", "Asia/Jakarta",
            "Asia/Shanghai", "Asia/Hong_Kong", "Asia/Singapore", "Asia/Taipei", "Asia/Seoul",
            "Asia/Tokyo", "Australia/Perth", "Australia/Sydney", "Australia/Melbourne",
            "Pacific/Auckland", "Africa/Cairo", "Africa/Lagos", "Africa/Johannesburg",
            "Africa/Nairobi"
        )
    }
}
