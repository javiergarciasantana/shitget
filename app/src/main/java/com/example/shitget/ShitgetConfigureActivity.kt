package com.example.shitget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
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

    // Los nombres oficiales de las familias de fuentes en Android
    private val fontNames = arrayOf("Normal", "Serif", "Monospace", "Sans-Serif Light", "Sans-Serif Condensed", "Cursive", "Casual")
    private val fontFamilies = arrayOf("sans-serif", "serif", "monospace", "sans-serif-light", "sans-serif-condensed", "cursive", "casual")

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            previewBg.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configure)
        setResult(Activity.RESULT_CANCELED)

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

        findViewById<Button>(R.id.btnPickImage).setOnClickListener { pickImageLauncher.launch("image/*") }
        findViewById<Button>(R.id.btnSave).setOnClickListener { saveWidgetConfig() }
    }

    private fun setupControls() {
        // --- FUENTES ---
        spinnerFont.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, fontNames)
        spinnerFont.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, position: Int, p3: Long) {
                val typeface = Typeface.create(fontFamilies[position], Typeface.NORMAL)
                previewNumber.typeface = typeface
                previewMinus.typeface = typeface
                previewPlus.typeface = typeface
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        // --- RGB SEEKBARS ---
        val colorChangeListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) { updatePreviewColor() }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }
        seekRed.setOnSeekBarChangeListener(colorChangeListener)
        seekGreen.setOnSeekBarChangeListener(colorChangeListener)
        seekBlue.setOnSeekBarChangeListener(colorChangeListener)
    }

    private fun updatePreviewColor() {
        val color = Color.rgb(seekRed.progress, seekGreen.progress, seekBlue.progress)
        previewNumber.setTextColor(color)
        previewMinus.setTextColor(color)
        previewPlus.setTextColor(color)
    }

    private fun fetchNamesFromGoogle() {
        thread {
            try {
                val connection = URL(scriptUrl).openConnection() as HttpURLConnection
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(response)
                val namesList = (0 until jsonArray.length()).map { jsonArray.getString(it) }

                runOnUiThread {
                    spinnerNames.adapter = ArrayAdapter(this@ShitgetConfigureActivity, android.R.layout.simple_spinner_dropdown_item, namesList)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun saveWidgetConfig() {
        // VALIDACIÓN: Campo obligatorio
        if (spinnerNames.selectedItem == null) {
            Toast.makeText(this, "Por favor, espera a que carguen los nombres", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedName = spinnerNames.selectedItem.toString()
        val selectedColor = Color.rgb(seekRed.progress, seekGreen.progress, seekBlue.progress)
        val selectedFontFamily = fontFamilies[spinnerFont.selectedItemPosition]

        val prefs = getSharedPreferences("ShitgetPrefs", 0).edit()
        prefs.putString("widget_name_$appWidgetId", selectedName)
        prefs.putInt("widget_color_$appWidgetId", selectedColor)
        prefs.putString("widget_font_$appWidgetId", selectedFontFamily)

        // Guardar foto
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

        val appWidgetManager = AppWidgetManager.getInstance(this)
        updateAppWidget(this, appWidgetManager, appWidgetId)

        val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(Activity.RESULT_OK, resultValue)
        finish()
    }
}