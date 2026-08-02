package com.buge.codeintegrator

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File
import java.io.FileWriter
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var edtSourcePath: EditText
    private lateinit var btnIntegrate: Button
    private lateinit var btnSelectSource: Button
    private lateinit var btnHelp: Button
    private lateinit var btnFileTypes: Button
    private lateinit var btnSettings: Button
    private lateinit var cbExcludeBuild: CheckBox
    private lateinit var tvStatus: TextView
    private lateinit var lvFileRanking: ListView
    private lateinit var rankingAdapter: ArrayAdapter<String>
    private lateinit var fileSizeList: MutableList<String>

    companion object {
        private const val REQUEST_CODE_MANAGE_STORAGE = 101
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
        setContentView(R.layout.activity_main)
        
        initViews()
        setupClickListeners()
        checkAndRequestPermissions()
        
        // Enable exclude build folder by default
        cbExcludeBuild.isChecked = true
    }

    override fun onResume() {
        super.onResume()
        refreshUI()
    }

    private fun refreshUI() {
        tvStatus.text = getString(R.string.tv_status)
    }

    private fun initViews() {
        edtSourcePath = findViewById(R.id.edtSourcePath)
        btnIntegrate = findViewById(R.id.btnIntegrate)
        btnSelectSource = findViewById(R.id.btnSelectSource)
        btnHelp = findViewById(R.id.btnHelp)
        btnFileTypes = findViewById(R.id.btnFileTypes)
        btnSettings = findViewById(R.id.btnSettings)
        cbExcludeBuild = findViewById(R.id.cbExcludeBuild)
        tvStatus = findViewById(R.id.tvStatus)
        lvFileRanking = findViewById(R.id.lvFileRanking)
        
        // Set text using resources
        btnSelectSource.text = getString(R.string.btn_select_source)
        btnIntegrate.text = getString(R.string.btn_integrate)
        btnFileTypes.text = getString(R.string.btn_file_types)
        btnHelp.text = getString(R.string.btn_help)
        btnSettings.text = getString(R.string.settings)
        cbExcludeBuild.text = getString(R.string.cb_exclude_build)
        tvStatus.text = getString(R.string.tv_status)
        
        fileSizeList = mutableListOf()
        rankingAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, fileSizeList)
        lvFileRanking.adapter = rankingAdapter
    }

    private fun setupClickListeners() {
        btnSelectSource.setOnClickListener {
            if (edtSourcePath.text.toString().trim().isEmpty()) {
                edtSourcePath.setText("/storage/emulated/0")
                Toast.makeText(this, R.string.example_path_set, Toast.LENGTH_SHORT).show()
            } else {
                edtSourcePath.setText("")
                Toast.makeText(this, R.string.path_cleared, Toast.LENGTH_SHORT).show()
            }
        }

        btnIntegrate.setOnClickListener {
            if (checkPermissions()) {
                integrateSourceFiles()
            } else {
                requestPermissions()
            }
        }

        btnHelp.setOnClickListener {
            showHelpDialog()
        }

        btnFileTypes.setOnClickListener {
            startActivity(Intent(this, FileTypesActivity::class.java))
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun showHelpDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.help_title)
            .setMessage(R.string.help_message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                requestManageStoragePermission()
            }
        } else if (!checkReadPermission()) {
            requestReadPermission()
        }
    }

    private fun checkPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            checkReadPermission()
        }
    }

    private fun checkReadPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requestManageStoragePermission()
        } else {
            requestReadPermission()
        }
    }

    private fun requestReadPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            100
        )
    }

    private fun requestManageStoragePermission() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.storage_permission_title)
            .setMessage(R.string.storage_permission_message)
            .setPositiveButton(R.string.grant) { _, _ ->
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = android.net.Uri.parse("package:$packageName")
                startActivityForResult(intent, REQUEST_CODE_MANAGE_STORAGE)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.permission_granted, Toast.LENGTH_SHORT).show()
                tvStatus.text = getString(R.string.tv_status)
            } else {
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show()
                tvStatus.text = getString(R.string.permission_denied)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_MANAGE_STORAGE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    Toast.makeText(this, R.string.permission_granted, Toast.LENGTH_SHORT).show()
                    tvStatus.text = getString(R.string.tv_status)
                } else {
                    Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show()
                    tvStatus.text = getString(R.string.permission_denied)
                }
            }
        }
    }

    private fun integrateSourceFiles() {
        val sourcePath = edtSourcePath.text.toString().trim()
        if (sourcePath.isEmpty()) {
            Toast.makeText(this, R.string.enter_path, Toast.LENGTH_SHORT).show()
            return
        }

        val sourceDir = File(sourcePath)
        if (!sourceDir.exists()) {
            Toast.makeText(this, R.string.source_path_invalid, Toast.LENGTH_SHORT).show()
            return
        }
        
        if (!sourceDir.isDirectory) {
            Toast.makeText(this, R.string.invalid_path, Toast.LENGTH_SHORT).show()
            return
        }

        tvStatus.text = getString(R.string.integrating)
        
        Thread {
            try {
                val outputFile = File(sourceDir, "SourceCodeIntegration.txt")
                val content = StringBuilder()
                
                content.append("================================================================================\n")
                content.append("【Project Tree Structure】\n")
                content.append("================================================================================\n")
                content.append("📁 ${sourceDir.name}\n")
                buildProjectTree(sourceDir, content, "", 0)
                
                val fileInfoList = mutableListOf<Triple<String, String, Long>>()
                collectFiles(sourceDir, sourceDir, fileInfoList)
                
                fileInfoList.sortByDescending { it.third }
                
                for ((fullPath, relativePath, size) in fileInfoList) {
                    content.append("\n")
                    content.append("================================================================================\n")
                    content.append("【File Information】\n")
                    content.append("================================================================================\n")
                    content.append("Complete Path: $fullPath\n")
                    content.append("Project Path: $relativePath\n")
                    content.append("File Name: ${File(fullPath).name}\n")
                    content.append("File Size: $size bytes\n")
                    content.append("================================================================================\n")
                    content.append("File Content:\n")
                    
                    try {
                        val fileContent = File(fullPath).readText()
                        content.append(fileContent)
                        if (!fileContent.endsWith("\n")) {
                            content.append("\n")
                        }
                    } catch (e: Exception) {
                        content.append("--- Error reading file: ${e.message} ---\n")
                    }
                }
                
                content.append("\n")
                content.append("================================================================================\n")
                content.append("Integration Complete! Total ${fileInfoList.size} files processed.\n")
                content.append("================================================================================\n")
                
                FileWriter(outputFile).use { writer ->
                    writer.write(content.toString())
                }
                
                runOnUiThread {
                    tvStatus.text = getString(R.string.integration_complete, fileInfoList.size)
                    Toast.makeText(
                        this,
                        getString(R.string.integration_success, outputFile.absolutePath, fileInfoList.size),
                        Toast.LENGTH_LONG
                    ).show()
                    
                    fileSizeList.clear()
                    fileSizeList.add("=== Top 20 Largest Files ===")
                    for (i in 0 until minOf(20, fileInfoList.size)) {
                        val (_, relativePath, size) = fileInfoList[i]
                        fileSizeList.add("${i + 1}. ${File(relativePath).name} (${formatSize(size)})")
                    }
                    rankingAdapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    tvStatus.text = getString(R.string.integration_error)
                    Toast.makeText(this, R.string.integration_error, Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }
    
    private fun buildProjectTree(dir: File, content: StringBuilder, prefix: String, depth: Int) {
        if (depth > 10) return
        
        val files = dir.listFiles() ?: return
        files.sortBy { it.name }
        
        for ((index, file) in files.withIndex()) {
            val isLast = index == files.size - 1
            val connector = if (isLast) "└── " else "├── "
            
            if (cbExcludeBuild.isChecked && file.name.equals("build", ignoreCase = true)) {
                continue
            }
            
            if (cbExcludeBuild.isChecked && file.name.equals(".gradle", ignoreCase = true)) {
                continue
            }
            
            content.append(prefix).append(connector)
            if (file.isDirectory) {
                content.append("📁 ${file.name}\n")
                val extension = if (isLast) "    " else "│   "
                buildProjectTree(file, content, prefix + extension, depth + 1)
            } else {
                content.append("📄 ${file.name}\n")
            }
        }
    }
    
    private fun collectFiles(baseDir: File, currentDir: File, fileInfoList: MutableList<Triple<String, String, Long>>) {
        val files = currentDir.listFiles() ?: return
        
        for (file in files) {
            if (file.name.startsWith(".")) continue
            
            if (cbExcludeBuild.isChecked && file.name.equals("build", ignoreCase = true)) {
                continue
            }
            
            if (cbExcludeBuild.isChecked && file.name.equals(".gradle", ignoreCase = true)) {
                continue
            }
            
            if (file.isDirectory) {
                collectFiles(baseDir, file, fileInfoList)
            } else if (isSupportedFileType(file)) {
                val relativePath = file.absolutePath.replace(baseDir.absolutePath, "")
                    .removePrefix("/")
                val fullPath = file.absolutePath
                fileInfoList.add(Triple(fullPath, if (relativePath.isEmpty()) file.name else relativePath, file.length()))
            }
        }
    }
    
    private fun isSupportedFileType(file: File): Boolean {
        val fileName = file.name
        val extension = if (fileName.contains(".")) {
            fileName.substringAfterLast(".").lowercase()
        } else {
            ""
        }
        
        val sharedPreferences = getSharedPreferences("FileTypesPrefs", MODE_PRIVATE)
        val unsupportedTypes = getUnsupportedFileTypesFromPrefs(sharedPreferences)
        
        for (unsupported in unsupportedTypes) {
            val unsupportedExt = if (unsupported.startsWith(".")) unsupported.substring(1) else unsupported
            if (extension == unsupportedExt.lowercase()) {
                return false
            }
        }
        
        val supportedExtensions = setOf(
            "txt", "java", "kt", "xml", "json", "gradle", "properties", "pro",
            "md", "cpp", "c", "h", "js", "html", "css", "php", "py", "rb",
            "pl", "sh", "bat", "cmd", "gitignore", "gitattributes", "kts",
            "gradle.kts", "toml"
        )
        
        return extension in supportedExtensions
    }
    
    private fun getUnsupportedFileTypesFromPrefs(sharedPreferences: android.content.SharedPreferences): Array<String> {
        val savedTypes = sharedPreferences.getString("excluded_types", "")
        return if (savedTypes.isNullOrEmpty()) {
            arrayOf(
                ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp", ".svg",
                ".mp3", ".mp4", ".avi", ".mkv", ".mov", ".wmv", ".flv", ".wav", ".m4a",
                ".zip", ".rar", ".7z", ".tar", ".gz", ".bz2", ".iso",
                ".exe", ".dll", ".so", ".apk", ".dex", ".class",
                ".psd", ".ai", ".sketch",
                ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx"
            )
        } else {
            savedTypes.split("|").toTypedArray()
        }
    }
    
    private fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
            bytes >= 1024 -> "${bytes / 1024} KB"
            else -> "$bytes B"
        }
    }
}