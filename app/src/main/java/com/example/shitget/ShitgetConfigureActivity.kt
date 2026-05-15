package com.example.shitget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
    private lateinit var spinnerWeight: Spinner
    private lateinit var seekSize: SeekBar
    private lateinit var seekRed: SeekBar
    private lateinit var seekGreen: SeekBar
    private lateinit var seekBlue: SeekBar

    private lateinit var previewBg: ImageView
    private lateinit var previewNumber: TextView
    private lateinit var previewMinus: TextView
    private lateinit var previewPlus: TextView

    private var selectedImageUri: Uri? = null
    private val scriptUrl = "https://script.google.com/macros/s/AKfycbxkrheg1lCyjKL4PAmMVQndIpmQnqaepYBJc6Io79PXoBGyY-xPg9O2oRvrI3scVXtwlw/exec"

    private val fontNames = arrayOf("Default", "Sans-Serif", "Serif", "Monospace", "Condensed", "Light", "Thin", "Medium", "Black", "Casual", "Cursive", "Serif-Monospace")
    private val fontFamilies = arrayOf("sans-serif", "sans-serif", "serif", "monospace", "sans-serif-condensed", "sans-serif-light", "sans-serif-thin", "sans-serif-medium", "sans-serif-black", "casual", "cursive", "serif-monospace")

    private val weightNames = arrayOf("Normal", "Negrita (Bold)", "Cursiva (Italic)", "Negrita Cursiva")
    private val weightStyles = arrayOf(Typeface.NORMAL, Typeface.BOLD, Typeface.ITALIC, Typeface.BOLD_ITALIC)

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            previewBg.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) { window.setDecorFitsSystemWindows(false) }
        setContentView(R.layout.activity_configure)
        setResult(Activity.RESULT_CANCELED)

        val rootLayout = findViewById<View>(R.id.configRootScroll)
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(view.paddingLeft, insets.top, view.paddingRight, insets.bottom)
            windowInsets
        }

        appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }

        spinnerNames = findViewById(R.id.spinnerNames)
        spinnerFont = findViewById(R.id.spinnerFont)
        spinnerWeight = findViewById(R.id.spinnerWeight)
        seekSize = findViewById(R.id.seekSize)
        seekRed = findViewById(R.id.seekRed)
        seekGreen = findViewById(R.id.seekGreen)
        seekBlue = findViewById(R.id.seekBlue)
        previewBg = findViewById(R.id.preview_bg)
        previewNumber = findViewById(R.id.preview_number)
        previewMinus = findViewById(R.id.preview_minus)
        previewPlus = findViewById(R.id.preview_plus)

        setupControls()
        fetchNamesFromGoogle()
        updatePreviewStyles()

        findViewById<Button>(R.id.btnPickImage).setOnClickListener { pickImageLauncher.launch("image/*") }
        findViewById<Button>(R.id.btnSave).setOnClickListener { saveWidgetConfig() }
    }

    private fun setupControls() {
        // Fuentes
        spinnerFont.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, fontNames)
        spinnerFont.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) { updatePreviewStyles() }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        // Estilos/Pesos
        spinnerWeight.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, weightNames)
        spinnerWeight.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) { updatePreviewStyles() }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        // Listener global para Seekbars (Color y Tamaño)
        val changeListener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) { updatePreviewStyles() }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }
        seekSize.setOnSeekBarChangeListener(changeListener)
        seekRed.setOnSeekBarChangeListener(changeListener)
        seekGreen.setOnSeekBarChangeListener(changeListener)
        seekBlue.setOnSeekBarChangeListener(changeListener)
    }

    private fun updatePreviewStyles() {
        // Aplicar Color
        val color = Color.rgb(seekRed.progress, seekGreen.progress, seekBlue.progress)
        previewNumber.setTextColor(color)
        previewMinus.setTextColor(color)
        previewPlus.setTextColor(color)

        // Aplicar Fuente y Peso
        val fontFamily = fontFamilies[spinnerFont.selectedItemPosition]
        val textStyle = weightStyles[spinnerWeight.selectedItemPosition]
        val typeface = Typeface.create(fontFamily, textStyle)

        previewNumber.typeface = typeface
        previewMinus.typeface = typeface
        previewPlus.typeface = typeface

        // Aplicar Tamaño (Offset de +20 para que el mínimo sea 20sp, y max 100sp)
        val sizeSp = (seekSize.progress + 20).toFloat()
        previewNumber.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)

        // Hacemos que los botones + y - sean un poco más pequeños que el número central
        val buttonsSp = (sizeSp - 8).coerceAtLeast(20f)
        previewMinus.setTextSize(TypedValue.COMPLEX_UNIT_SP, buttonsSp)
        previewPlus.setTextSize(TypedValue.COMPLEX_UNIT_SP, buttonsSp)
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
                    runOnUiThread { spinnerNames.adapter = ArrayAdapter(this@ShitgetConfigureActivity, android.R.layout.simple_spinner_dropdown_item, namesList) }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun saveWidgetConfig() {
        if (spinnerNames.selectedItem == null) {
            Toast.makeText(this, "Espera a cargar nombres", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = getSharedPreferences("ShitgetPrefs", 0).edit()
        prefs.putString("widget_name_$appWidgetId", spinnerNames.selectedItem.toString())
        prefs.putInt("widget_color_$appWidgetId", Color.rgb(seekRed.progress, seekGreen.progress, seekBlue.progress))
        prefs.putString("widget_font_$appWidgetId", fontFamilies[spinnerFont.selectedItemPosition])
        prefs.putInt("widget_weight_$appWidgetId", weightStyles[spinnerWeight.selectedItemPosition])
        prefs.putInt("widget_size_$appWidgetId", seekSize.progress + 20) // Guardamos el tamaño final en SP

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