package com.buge.codeintegrator

import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.*

class SettingsActivity : AppCompatActivity() {

    private lateinit var themeToggleGroup: MaterialButtonToggleGroup
    private lateinit var languageSpinner: Spinner
    private lateinit var sharedPrefs: android.content.SharedPreferences
    private var isRestarting = false

    companion object {
        private const val PREFS_NAME = "SettingsPrefs"
        private const val KEY_THEME = "theme_mode"
        private const val KEY_LANGUAGE = "language_code"
        
        const val LANG_SYSTEM = "system"
        const val LANG_EN = "en"
        const val LANG_ZH = "zh"
        const val LANG_DE = "de"
        const val LANG_RU = "ru"
        const val LANG_AR = "ar"
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
        setContentView(R.layout.activity_settings)

        sharedPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        setupToolbar()
        setupThemeToggle()
        setupLanguageSpinner()
        loadSettings()
    }

    override fun onResume() {
        super.onResume()
        isRestarting = false
    }

    private fun setupToolbar() {
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.title = getString(R.string.settings_title)
    }

    private fun setupThemeToggle() {
        themeToggleGroup = findViewById(R.id.themeToggleGroup)
        
        // Set button texts
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnThemeLight).text = getString(R.string.theme_light)
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnThemeDark).text = getString(R.string.theme_dark)
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnThemeSystem).text = getString(R.string.theme_system)
        
        themeToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked && !isRestarting) {
                val mode = when (checkedId) {
                    R.id.btnThemeLight -> AppCompatDelegate.MODE_NIGHT_NO
                    R.id.btnThemeDark -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                applyTheme(mode)
            }
        }
    }

    private fun applyTheme(mode: Int) {
        val currentMode = sharedPrefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        if (currentMode == mode) return
        
        sharedPrefs.edit().putInt(KEY_THEME, mode).apply()
        AppCompatDelegate.setDefaultNightMode(mode)
        
        isRestarting = true
        finish()
        overridePendingTransition(0, 0)
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    private fun setupLanguageSpinner() {
        languageSpinner = findViewById(R.id.languageSpinner)
        
        val languages = arrayOf(
            getString(R.string.lang_system_default),
            "English",
            "中文",
            "Deutsch",
            "Русский",
            "العربية"
        )
        
        val languageCodes = arrayOf(
            LANG_SYSTEM,
            LANG_EN,
            LANG_ZH,
            LANG_DE,
            LANG_RU,
            LANG_AR
        )
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        languageSpinner.adapter = adapter
        
        val savedLanguage = sharedPrefs.getString(KEY_LANGUAGE, LANG_SYSTEM) ?: LANG_SYSTEM
        val position = languageCodes.indexOf(savedLanguage)
        if (position >= 0) {
            languageSpinner.setSelection(position)
        }
        
        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedCode = languageCodes[position]
                val currentLang = sharedPrefs.getString(KEY_LANGUAGE, LANG_SYSTEM) ?: LANG_SYSTEM
                if (selectedCode != currentLang && !isRestarting) {
                    showLanguageChangeDialog(selectedCode)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun showLanguageChangeDialog(languageCode: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.language_change_title)
            .setMessage(R.string.language_change_message)
            .setPositiveButton(R.string.restart) { _, _ ->
                applyLanguage(languageCode)
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                val savedLanguage = sharedPrefs.getString(KEY_LANGUAGE, LANG_SYSTEM) ?: LANG_SYSTEM
                val codes = arrayOf(LANG_SYSTEM, LANG_EN, LANG_ZH, LANG_DE, LANG_RU, LANG_AR)
                val pos = codes.indexOf(savedLanguage)
                if (pos >= 0) {
                    languageSpinner.setSelection(pos)
                }
            }
            .show()
    }

    private fun applyLanguage(languageCode: String) {
        val currentLang = sharedPrefs.getString(KEY_LANGUAGE, LANG_SYSTEM) ?: LANG_SYSTEM
        if (currentLang == languageCode) return
        
        sharedPrefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
        
        isRestarting = true
        
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun loadSettings() {
        val themeMode = sharedPrefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        when (themeMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> themeToggleGroup.check(R.id.btnThemeLight)
            AppCompatDelegate.MODE_NIGHT_YES -> themeToggleGroup.check(R.id.btnThemeDark)
            else -> themeToggleGroup.check(R.id.btnThemeSystem)
        }
    }
}