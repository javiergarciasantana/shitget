package com.example.shitget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class Shitget : AppWidgetProvider() {

    // Nombres de nuestras acciones personalizadas
    private val ACTION_PLUS = "com.example.shitget.ACTION_PLUS"
    private val ACTION_MINUS = "com.example.shitget.ACTION_MINUS"

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    // AQUÍ ATRAPAMOS LOS CLICS DE LOS BOTONES
    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val action = intent.action
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)

        if (action == ACTION_PLUS || action == ACTION_MINUS) {
            val prefs = context.getSharedPreferences("ShitgetPrefs", 0)
            val name = prefs.getString("widget_name_$appWidgetId", "Colu") ?: "Colu"

            val actionType = if (action == ACTION_PLUS) "plus" else "minus"

            // Hacer la petición en segundo plano
            thread {
                try {
                    val urlStr = "https://script.google.com/macros/s/AKfycbxkrheg1lCyjKL4PAmMVQndIpmQnqaepYBJc6Io79PXoBGyY-xPg9O2oRvrI3scVXtwlw/exec?name=$name&type=update&action=$actionType"
                    val url = URL(urlStr)
                    val connection = url.openConnection() as HttpURLConnection
                    val newValue = connection.inputStream.bufferedReader().use { it.readText() }

                    // Actualizar el número en el widget
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val views = RemoteViews(context.packageName, R.layout.shitget)
                    views.setTextViewText(R.id.appwidget_text, newValue) // R.id.appwidget_text es el ID de tu número central
                    appWidgetManager.updateAppWidget(appWidgetId, views)

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}

internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    val prefs = context.getSharedPreferences("ShitgetPrefs", 0)
    val name = prefs.getString("widget_name_$appWidgetId", "Colu") ?: "Colu"

    val views = RemoteViews(context.packageName, R.layout.shitget)

    // --- APLICAR ESTILOS GUARDADOS ---
    val savedColor = prefs.getInt("widget_color_$appWidgetId", android.graphics.Color.BLACK)
    val savedFont = prefs.getString("widget_font_$appWidgetId", "sans-serif")

    views.setTextColor(R.id.button_plus, savedColor)
    views.setTextColor(R.id.button_minus, savedColor)

    // Para cambiar la fuente del número junto con el color
    val spannableString = android.text.SpannableString("...") // Luego cuando hagas la petición, le pasas el número real aquí
    spannableString.setSpan(android.text.style.TypefaceSpan(savedFont!!), 0, spannableString.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    spannableString.setSpan(android.text.style.ForegroundColorSpan(savedColor), 0, spannableString.length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    views.setTextViewText(R.id.appwidget_text, spannableString)

    // Cargar imagen de fondo si existe
    val bgFile = java.io.File(context.filesDir, "bg_$appWidgetId.jpg")
    if (bgFile.exists()) {
        val bitmap = android.graphics.BitmapFactory.decodeFile(bgFile.absolutePath)
        views.setImageViewBitmap(R.id.widget_bg_image, bitmap)
    } else {
        views.setImageViewResource(R.id.widget_bg_image, android.R.color.white)
    }
    // 1. Estado inicial de carga
    views.setTextViewText(R.id.appwidget_text, "...")

    // 2. Configurar Botones
    val intentPlus = Intent(context, Shitget::class.java).apply {
        action = "com.example.shitget.ACTION_PLUS"
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    }
    val pendingPlus = PendingIntent.getBroadcast(context, appWidgetId + 1, intentPlus, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    views.setOnClickPendingIntent(R.id.button_plus, pendingPlus)

    val intentMinus = Intent(context, Shitget::class.java).apply {
        action = "com.example.shitget.ACTION_MINUS"
        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    }
    val pendingMinus = PendingIntent.getBroadcast(context, appWidgetId + 2, intentMinus, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    views.setOnClickPendingIntent(R.id.button_minus, pendingMinus)

    // Mostramos los botones y el "..." instantáneamente
    appWidgetManager.updateAppWidget(appWidgetId, views)

    // 3. Buscar el número real en Google (type=read)
    thread {
        try {
            // OJO: Pon aquí tu URL real
            val urlStr = "https://script.google.com/macros/s/AKfycbxkrheg1lCyjKL4PAmMVQndIpmQnqaepYBJc6Io79PXoBGyY-xPg9O2oRvrI3scVXtwlw/exec?name=$name&type=read"
            val url = URL(urlStr)
            val connection = url.openConnection() as HttpURLConnection
            val currentValue = connection.inputStream.bufferedReader().use { it.readText() }

            // Actualizamos solo el texto con el número final
            views.setTextViewText(R.id.appwidget_text, currentValue)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        } catch (e: Exception) {
            e.printStackTrace()
            views.setTextViewText(R.id.appwidget_text, "Err")
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}