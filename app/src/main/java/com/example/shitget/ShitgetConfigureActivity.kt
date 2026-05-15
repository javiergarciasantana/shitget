package com.example.shitget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class ShitgetConfigureActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    private lateinit var spinnerNames: Spinner
    private lateinit var spinnerFont: Spinner
    private lateinit var seekRed: SeekBar
    private lateinit var seekGreen: SeekBar
    private lateinit var seekBlue: SeekBar
    private lateinit var previewBg: ImageView
    private lateinit var previewNumber: TextView
    private lateinit var previewMinus: TextView
    private lateinit var previewPlus: TextView

    private var selectedImageUri: Uri? = null
    private val scriptUrl = "https://script.google.com/macros/s/AKfycbxkrheg1lCyjKL4PAmMVQndIpmQnqaepYBJc6Io79PXoBGyY-xPg9O2oRvrI3scVXtwlw/exec"

    // --- SOLUCIÓN PUNTO 4: MÁS FUENTES DEL SISTEMA ---
    private val fontNames = arrayOf(
        "Default", "Sans-Serif", "Serif", "Monospace",
        "Condensed", "Light", "Thin", "Medium", "Black",
        "Casual", "Cursive", "Serif-Monospace"
    )
    private val fontFamilies = arrayOf(
        "sans-serif", "sans-serif", "serif", "monospace",
        "sans-serif-condensed", "sans-serif-light", "sans-serif-thin", "sans-serif-medium", "sans-serif-black",
        "casual", "cursive", "serif-monospace"
    )

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            previewBg.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Habilitar diseño de borde a borde (Edge-to-Edge) para gestionar la cámara
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }

        setContentView(R.layout.activity_configure)
        setResult(Activity.RESULT_CANCELED)

        // --- SOLUCIÓN PUNTO 1: PADDING SUPERIOR PARA CÁMARA ---
        val rootLayout = findViewById<View>(R.id.configRootScroll)
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            // Aplicamos padding superior e inferior según las barras del sistema y recortes
            view.setPadding(view.paddingLeft, insets.top, view.paddingRight, insets.bottom)
            windowInsets
        }

        appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }

        // Vincular UI
        spinnerNames = findViewById(R.id.spinnerNames)
        spinnerFont = findViewById(R.id.spinnerFont)
        seekRed = findViewById(R.id.seekRed)
        seekGreen = findViewById(R.id.seekGreen)
        seekBlue = findViewById(R.id.seekBlue)
        previewBg = findViewById(R.id.preview_bg)
        previewNumber = findViewById(R.id.preview_number)
        previewMinus = findViewById(R.id.preview_minus)
        previewPlus = findViewById(R.id.preview_plus)

        setupControls()
        fetchNamesFromGoogle()

        // --- SOLUCIÓN PUNTO 3: INICIALIZAR COLOR DE PREVIEW ---
        updatePreviewColor()

        findViewById<Button>(R.id.btnPickImage).setOnClickListener { pickImageLauncher.launch("image/*") }
        findViewById<Button>(R.id.btnSave).setOnClickListener { saveWidgetConfig() }
    }

    private fun setupControls() {
        // --- FUENTES ---
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, fontNames)
        spinnerFont.adapter = adapter
        spinnerFont.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                // Creamos la fuente basada en el nombre de familia
                val typeface = Typeface.create(fontFamilies[position], Typeface.NORMAL)
                previewNumber.typeface = typeface
                previewMinus.typeface = typeface
                previewPlus.typeface = typeface
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        // --- RGB SEEKBARS ---
        val colorChangeListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // --- SOLUCIÓN PUNTO 3: ACTUALIZACIÓN EN TIEMPO REAL ---
                updatePreviewColor()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }
        seekRed.setOnSeekBarChangeListener(colorChangeListener)
        seekGreen.setOnSeekBarChangeListener(colorChangeListener)
        seekBlue.setOnSeekBarChangeListener(colorChangeListener)
    }

    private fun updatePreviewColor() {
        // Obtenemos color de los seekbars y lo aplicamos a los 3 textos de preview
        val color = Color.rgb(seekRed.progress, seekGreen.progress, seekBlue.progress)
        previewNumber.setTextColor(color)
        previewMinus.setTextColor(color)
        previewPlus.setTextColor(color)
    }

    private fun fetchNamesFromGoogle() {
        thread {
            try {
                val connection = URL(scriptUrl).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                val response = connection.inputStream.bufferedReader().use { it.readText() }

                if (response.startsWith("[")) {
                    val jsonArray = JSONArray(response)
                    val namesList = (0 until jsonArray.length()).map { jsonArray.getString(it) }

                    runOnUiThread {
                        val adapter = ArrayAdapter(this@ShitgetConfigureActivity, android.R.layout.simple_spinner_dropdown_item, namesList)
                        spinnerNames.adapter = adapter
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveWidgetConfig() {
        // VALIDACIÓN: Campo obligatorio
        if (spinnerNames.selectedItem == null) {
            Toast.makeText(this, "Por favor, selecciona un nombre", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedName = spinnerNames.selectedItem.toString()
        // Guardamos el color como un Integer directo
        val selectedColorInt = Color.rgb(seekRed.progress, seekGreen.progress, seekBlue.progress)
        val selectedFontFamily = fontFamilies[spinnerFont.selectedItemPosition]

        // Guardar preferencias blindadas (sin forzar temas)
        val prefs = getSharedPreferences("ShitgetPrefs", 0).edit()
        prefs.putString("widget_name_$appWidgetId", selectedName)
        prefs.putInt("widget_color_$appWidgetId", selectedColorInt)
        prefs.putString("widget_font_$appWidgetId", selectedFontFamily)

        // Guardar foto de fondo
        selectedImageUri?.let { uri ->
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val file = File(filesDir, "bg_$appWidgetId.jpg")
                val outputStream = FileOutputStream(file)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
            } catch (e: Exception) { e.printStackTrace() }
        }

        prefs.apply()

        // Forzar actualización del widget real
        val appWidgetManager = AppWidgetManager.getInstance(this)
        updateAppWidget(this, appWidgetManager, appWidgetId)

        val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(Activity.RESULT_OK, resultValue)
        finish()
    }
}