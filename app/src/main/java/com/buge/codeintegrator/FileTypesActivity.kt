package com.buge.codeintegrator

import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.*

class FileTypesActivity : AppCompatActivity() {

    private lateinit var rvFileTypes: RecyclerView
    private lateinit var etFileType: EditText
    private lateinit var btnAdd: Button
    private lateinit var btnReset: Button
    private lateinit var fileTypesList: MutableList<String>
    private lateinit var adapter: FileTypeAdapter
    private lateinit var sharedPreferences: android.content.SharedPreferences

    companion object {
        private const val PREFS_NAME = "SettingsPrefs"
        private const val KEY_LANGUAGE = "language_code"
        private var currentLocale: Locale? = null
    }

    override fun attachBaseContext(newBase: android.content.Context) {
        val locale = getSavedLocale(newBase)
        val config = Configuration(newBase.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        val context = newBase.createConfigurationContext(config)
        currentLocale = locale
        super.attachBaseContext(context)
    }

    override fun getResources(): Resources {
        val resources = super.getResources()
        val locale = currentLocale ?: getSavedLocale(this)
        val config = Configuration(resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        resources.updateConfiguration(config, resources.displayMetrics)
        return resources
    }

    private fun getSavedLocale(context: android.content.Context): Locale {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
            val languageCode = prefs.getString(KEY_LANGUAGE, "system") ?: "system"
            
            if (languageCode != "system") {
                return when (languageCode) {
                    "zh" -> Locale("zh")
                    "de" -> Locale("de")
                    "ru" -> Locale("ru")
                    "ar" -> Locale("ar")
                    else -> Locale("en")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Locale.getDefault()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_types)
        
        rvFileTypes = findViewById(R.id.rvFileTypes)
        etFileType = findViewById(R.id.edtNewFileType)
        btnAdd = findViewById(R.id.btnAddFileType)
        btnReset = findViewById(R.id.btnResetToDefault)
        val btnBack: Button = findViewById(R.id.btnBack)
        
        // Set text using resources
        btnAdd.text = getString(R.string.add_file_type)
        btnReset.text = getString(R.string.reset_to_default)
        btnBack.text = getString(R.string.back)
        etFileType.hint = getString(R.string.enter_file_type)
        
        sharedPreferences = getSharedPreferences("FileTypesPrefs", MODE_PRIVATE)
        
        fileTypesList = getUnsupportedFileTypesFromPrefs(sharedPreferences).toMutableList()
        
        rvFileTypes.layoutManager = LinearLayoutManager(this)
        adapter = FileTypeAdapter(fileTypesList) { position ->
            showDeleteConfirmDialog(position)
        }
        rvFileTypes.adapter = adapter
        
        btnAdd.setOnClickListener {
            val type = etFileType.text.toString().trim()
            if (type.isNotEmpty()) {
                val formatted = if (type.startsWith(".")) type else ".$type"
                if (!fileTypesList.contains(formatted)) {
                    fileTypesList.add(formatted)
                    adapter.notifyItemInserted(fileTypesList.size - 1)
                    saveFileTypes()
                    etFileType.setText("")
                    Toast.makeText(this, getString(R.string.file_type_added, formatted), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, R.string.file_type_exists, Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, R.string.enter_file_type, Toast.LENGTH_SHORT).show()
            }
        }
        
        btnReset.setOnClickListener {
            showResetConfirmDialog()
        }
        
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun showDeleteConfirmDialog(position: Int) {
        val item = fileTypesList[position]
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.remove_file_type)
            .setMessage(getString(R.string.remove_confirm_message, item))
            .setPositiveButton(R.string.remove) { _, _ ->
                fileTypesList.removeAt(position)
                adapter.notifyItemRemoved(position)
                saveFileTypes()
                Toast.makeText(this, getString(R.string.file_type_removed, item), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun showResetConfirmDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.reset_confirm_title)
            .setMessage(R.string.reset_confirm_message)
            .setPositiveButton(R.string.reset) { _, _ ->
                fileTypesList.clear()
                fileTypesList.addAll(getDefaultFileTypes())
                adapter.notifyDataSetChanged()
                saveFileTypes()
                Toast.makeText(this, R.string.reset_complete, Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    private fun saveFileTypes() {
        sharedPreferences.edit().putString("excluded_types", fileTypesList.joinToString("|")).apply()
    }
    
    private fun getDefaultFileTypes(): List<String> {
        return listOf(
            ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".svg",
            ".mp3", ".mp4", ".avi", ".mkv", ".mov", ".wmv", ".flv", ".wav", ".m4a",
            ".zip", ".rar", ".7z", ".tar", ".gz", ".bz2", ".iso",
            ".exe", ".dll", ".so", ".apk", ".dex", ".class",
            ".psd", ".ai", ".sketch",
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx"
        )
    }
    
    private fun getUnsupportedFileTypesFromPrefs(sharedPreferences: android.content.SharedPreferences): Array<String> {
        val savedTypes = sharedPreferences.getString("excluded_types", "")
        return if (savedTypes.isNullOrEmpty()) {
            getDefaultFileTypes().toTypedArray()
        } else {
            savedTypes.split("|").toTypedArray()
        }
    }
}