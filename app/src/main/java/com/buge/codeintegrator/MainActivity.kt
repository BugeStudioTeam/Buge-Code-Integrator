package com.buge.codeintegrator

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
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
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var edtSourcePath: EditText
    private lateinit var btnIntegrate: Button
    private lateinit var btnSelectSource: Button
    private lateinit var btnHelp: Button
    private lateinit var btnFileTypes: Button
    private lateinit var cbExcludeBuild: CheckBox
    private lateinit var tvStatus: TextView
    private lateinit var lvFileRanking: ListView
    private lateinit var rankingAdapter: ArrayAdapter<String>
    private lateinit var fileSizeList: MutableList<String>

    companion object {
        private const val REQUEST_CODE_MANAGE_STORAGE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        initViews()
        setupClickListeners()
        checkAndRequestPermissions()
        
        // 默认勾选排除 build 文件夹
        cbExcludeBuild.isChecked = true
    }

    private fun initViews() {
        edtSourcePath = findViewById(R.id.edtSourcePath)
        btnIntegrate = findViewById(R.id.btnIntegrate)
        btnSelectSource = findViewById(R.id.btnSelectSource)
        btnHelp = findViewById(R.id.btnHelp)
        btnFileTypes = findViewById(R.id.btnFileTypes)
        cbExcludeBuild = findViewById(R.id.cbExcludeBuild)
        tvStatus = findViewById(R.id.tvStatus)
        lvFileRanking = findViewById(R.id.lvFileRanking)
        
        // 设置灰色提示文字
        edtSourcePath.hint = "e.g., /storage/emulated/0/MyProject"
        
        fileSizeList = mutableListOf()
        rankingAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, fileSizeList)
        lvFileRanking.adapter = rankingAdapter
        
        tvStatus.text = "Ready to integrate files"
    }

    private fun setupClickListeners() {
        btnSelectSource.setOnClickListener {
            if (edtSourcePath.text.toString().trim().isEmpty()) {
                edtSourcePath.setText("/storage/emulated/0")
                Toast.makeText(this, "Example path set", Toast.LENGTH_SHORT).show()
            } else {
                edtSourcePath.setText("")
                Toast.makeText(this, "Path cleared", Toast.LENGTH_SHORT).show()
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
    }

    private fun showHelpDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Usage Instructions")
            .setMessage("Usage Instructions:\n\n1. Enter source code folder path\n2. You can paste the path directly\n3. Click 'Example Path' to see path format\n4. Check 'Exclude build folder' to skip cache files (enabled by default)\n5. Click 'Filter File Types' to manage excluded file formats\n6. Click 'Start Integration' to begin\n7. Output file will be created as 'SourceCodeIntegration.txt' in the selected folder\n\nOutput Contents:\n• Project tree structure\n• Complete content of all text files\n• File size ranking\n\nDefault Supported File Types:\n• Code files: .java, .kt, .xml, .gradle, etc.\n• Config files: .properties, .pro, .gitignore, etc.\n• Script files: .sh, .bat, .cmd, etc.\n• Text files: .txt, .md, etc.\n\nDefault Filtered File Types:\n• Images, audio, video files\n• Archives\n• Binary executables\n• Documents, etc.\n\nFile Ranking:\n• Shows largest text files in source folder\n• Helps identify large config files")
            .setPositiveButton("Got it", null)
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
            .setTitle("Storage Permission Required")
            .setMessage("This app needs access to manage all files to read your source code folders and save output files. Please grant permission in the next screen.")
            .setPositiveButton("Grant") { _, _ ->
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = android.net.Uri.parse("package:$packageName")
                startActivityForResult(intent, REQUEST_CODE_MANAGE_STORAGE)
            }
            .setNegativeButton("Cancel", null)
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
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
                tvStatus.text = "Ready to integrate files"
            } else {
                Toast.makeText(this, "Permission required to access files", Toast.LENGTH_LONG).show()
                tvStatus.text = "Permission denied"
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_MANAGE_STORAGE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    Toast.makeText(this, "Storage permission granted", Toast.LENGTH_SHORT).show()
                    tvStatus.text = "Ready to integrate files"
                } else {
                    Toast.makeText(this, "Storage permission denied", Toast.LENGTH_LONG).show()
                    tvStatus.text = "Permission denied"
                }
            }
        }
    }

    private fun integrateSourceFiles() {
        val sourcePath = edtSourcePath.text.toString().trim()
        if (sourcePath.isEmpty()) {
            Toast.makeText(this, "Please enter a source path", Toast.LENGTH_SHORT).show()
            return
        }

        val sourceDir = File(sourcePath)
        if (!sourceDir.exists()) {
            Toast.makeText(this, "Path does not exist", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (!sourceDir.isDirectory) {
            Toast.makeText(this, "Not a directory", Toast.LENGTH_SHORT).show()
            return
        }

        tvStatus.text = "Integrating files..."
        
        Thread {
            try {
                // 输出到用户指定的目录
                val outputFile = File(sourceDir, "SourceCodeIntegration.txt")
                val content = StringBuilder()
                
                // 写入项目树形结构
                content.append("================================================================================\n")
                content.append("【Project Tree Structure】\n")
                content.append("================================================================================\n")
                content.append("📁 ${sourceDir.name}\n")
                buildProjectTree(sourceDir, content, "", 0)
                
                // 收集文件信息
                val fileInfoList = mutableListOf<Triple<String, String, Long>>()
                collectFiles(sourceDir, sourceDir, fileInfoList)
                
                // 按大小排序
                fileInfoList.sortByDescending { it.third }
                
                // 写入每个文件的内容
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
                
                // 写入文件
                FileWriter(outputFile).use { writer ->
                    writer.write(content.toString())
                }
                
                // 更新UI
                runOnUiThread {
                    tvStatus.text = "Integration complete! Processed ${fileInfoList.size} files"
                    Toast.makeText(this, "Integration successful!\nOutput file: ${outputFile.absolutePath}\nProcessed files: ${fileInfoList.size}", Toast.LENGTH_LONG).show()
                    
                    // 更新文件排名
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
                    tvStatus.text = "Error during integration"
                    Toast.makeText(this, "Integration error: ${e.message}", Toast.LENGTH_LONG).show()
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
            
            // 跳过build文件夹（默认勾选，但尊重用户选择）
            if (cbExcludeBuild.isChecked && file.name.equals("build", ignoreCase = true)) {
                continue
            }
            
            // 跳过 .gradle 文件夹
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
            
            // 跳过build文件夹（默认勾选，但尊重用户选择）
            if (cbExcludeBuild.isChecked && file.name.equals("build", ignoreCase = true)) {
                continue
            }
            
            // 跳过 .gradle 文件夹
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
        
        // 获取排除的文件类型
        val sharedPreferences = getSharedPreferences("FileTypesPrefs", MODE_PRIVATE)
        val unsupportedTypes = getUnsupportedFileTypesFromPrefs(sharedPreferences)
        
        // 检查是否在排除列表中
        for (unsupported in unsupportedTypes) {
            val unsupportedExt = if (unsupported.startsWith(".")) unsupported.substring(1) else unsupported
            if (extension == unsupportedExt.lowercase()) {
                return false
            }
        }
        
        // 支持的文件扩展名
        val supportedExtensions = setOf(
            "txt", "java", "kt", "xml", "json", "gradle", "properties", "pro",
            "md", "cpp", "c", "h", "js", "html", "css", "php", "py", "rb",
            "pl", "sh", "bat", "cmd", "gitignore", "gitattributes", "kts",
            "gradle.kts", "toml"
        )
        
        return extension in supportedExtensions
    }
    
    private fun getUnsupportedFileTypesFromPrefs(sharedPreferences: SharedPreferences): Array<String> {
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