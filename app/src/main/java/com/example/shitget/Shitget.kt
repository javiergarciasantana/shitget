package com.example.shitget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.util.TypedValue
import android.widget.RemoteViews
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class Shitget : AppWidgetProvider() {

    private val ACTION_PLUS = "com.example.shitget.ACTION_PLUS"
    private val ACTION_MINUS = "com.example.shitget.ACTION_MINUS"

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val action = intent.action
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID && (action == ACTION_PLUS || action == ACTION_MINUS)) {

            val prefs = context.getSharedPreferences("ShitgetPrefs", 0)
            val name = prefs.getString("widget_name_$appWidgetId", "Colu") ?: "Colu"
            val savedColor = prefs.getInt("widget_color_$appWidgetId", android.graphics.Color.BLACK)
            val savedFont = prefs.getString("widget_font_$appWidgetId", "sans-serif") ?: "sans-serif"
            val savedWeight = prefs.getInt("widget_weight_$appWidgetId", Typeface.NORMAL)

            val actionType = if (action == ACTION_PLUS) "plus" else "minus"

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val views = RemoteViews(context.packageName, R.layout.shitget)

            views.setTextViewText(R.id.appwidget_text, createStyledText("...", savedColor, savedFont, savedWeight))
            appWidgetManager.updateAppWidget(appWidgetId, views)

            thread {
                try {
                    val urlStr = "https://script.google.com/macros/s/AKfycbxkrheg1lCyjKL4PAmMVQndIpmQnqaepYBJc6Io79PXoBGyY-xPg9O2oRvrI3scVXtwlw/exec?name=$name&type=update&action=$actionType"
                    val connection = URL(urlStr).openConnection() as HttpURLConnection
                    val newValue = connection.inputStream.bufferedReader().use { it.readText() }

                    views.setTextViewText(R.id.appwidget_text, createStyledText(newValue, savedColor, savedFont, savedWeight))
                    appWidgetManager.updateAppWidget(appWidgetId, views)

                } catch (e: Exception) {
                    e.printStackTrace()
                    views.setTextViewText(R.id.appwidget_text, createStyledText("Err", savedColor, savedFont, savedWeight))
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            }
        }
    }
}

// --- FUNCIÓN DE ESTILO BLINDADA (Con Peso) ---
internal fun createStyledText(text: String, color: Int, fontFamily: String, textStyle: Int): SpannableString {
    val spannable = SpannableString(text)
    spannable.setSpan(ForegroundColorSpan(color), 0, spannable.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    spannable.setSpan(TypefaceSpan(fontFamily), 0, spannable.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    spannable.setSpan(StyleSpan(textStyle), 0, spannable.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    return spannable
}

internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    val prefs = context.getSharedPreferences("ShitgetPrefs", 0)
    val name = prefs.getString("widget_name_$appWidgetId", "Colu") ?: "Colu"

    // Leer TODAS las configuraciones estéticas
    val savedColor = prefs.getInt("widget_color_$appWidgetId", android.graphics.Color.BLACK)
    val savedFont = prefs.getString("widget_font_$appWidgetId", "sans-serif") ?: "sans-serif"
    val savedWeight = prefs.getInt("widget_weight_$appWidgetId", Typeface.NORMAL)
    val savedSizeSp = prefs.getInt("widget_size_$appWidgetId", 48).toFloat()

    val views = RemoteViews(context.packageName, R.layout.shitget)

    // 1. Aplicar Color, Estilo y Tamaño a los Botones
    views.setTextColor(R.id.button_plus, savedColor)
    views.setTextColor(R.id.button_minus, savedColor)

    val buttonSizeSp = (savedSizeSp - 8).coerceAtLeast(20f)
    views.setTextViewTextSize(R.id.button_plus, TypedValue.COMPLEX_UNIT_SP, buttonSizeSp)
    views.setTextViewTextSize(R.id.button_minus, TypedValue.COMPLEX_UNIT_SP, buttonSizeSp)

    // Aplicamos también el peso a los botones
    views.setTextViewText(R.id.button_plus, createStyledText("+", savedColor, savedFont, savedWeight))
    views.setTextViewText(R.id.button_minus, createStyledText("-", savedColor, savedFont, savedWeight))

    // 2. Aplicar Tamaño al Número Central
    views.setTextViewTextSize(R.id.appwidget_text, TypedValue.COMPLEX_UNIT_SP, savedSizeSp)

    // 3. Cargar Imagen de Fondo
    val bgFile = java.io.File(context.filesDir, "bg_$appWidgetId.jpg")
    if (bgFile.exists()) {
        val bitmap = android.graphics.BitmapFactory.decodeFile(bgFile.absolutePath)
        views.setImageViewBitmap(R.id.widget_bg_image, bitmap)
    } else {
        views.setImageViewResource(R.id.widget_bg_image, android.R.color.white)
    }

    // 4. Configurar Intents de los Botones
    val intentPlus = Intent(context, Shitget::class.java).apply {
        action = "com.example.shitget.ACTION_PLUS"
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    }
    val pendingPlus = PendingIntent.getBroadcast(context, appWidgetId * 2, intentPlus, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    views.setOnClickPendingIntent(R.id.button_plus, pendingPlus)

    val intentMinus = Intent(context, Shitget::class.java).apply {
        action = "com.example.shitget.ACTION_MINUS"
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    }
    val pendingMinus = PendingIntent.getBroadcast(context, (appWidgetId * 2) + 1, intentMinus, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    views.setOnClickPendingIntent(R.id.button_minus, pendingMinus)

    // 5. Estado inicial con estilo y peso
    views.setTextViewText(R.id.appwidget_text, createStyledText("...", savedColor, savedFont, savedWeight))
    appWidgetManager.updateAppWidget(appWidgetId, views)

    // 6. Leer el valor real de Google
    thread {
        try {
            val urlStr = "https://script.google.com/macros/s/AKfycbxkrheg1lCyjKL4PAmMVQndIpmQnqaepYBJc6Io79PXoBGyY-xPg9O2oRvrI3scVXtwlw/exec?name=$name&type=read"
            val connection = URL(urlStr).openConnection() as HttpURLConnection
            val currentValue = connection.inputStream.bufferedReader().use { it.readText() }

            views.setTextViewText(R.id.appwidget_text, createStyledText(currentValue, savedColor, savedFont, savedWeight))
            appWidgetManager.updateAppWidget(appWidgetId, views)
        } catch (e: Exception) {
            e.printStackTrace()
            views.setTextViewText(R.id.appwidget_text, createStyledText("Err", savedColor, savedFont, savedWeight))
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}