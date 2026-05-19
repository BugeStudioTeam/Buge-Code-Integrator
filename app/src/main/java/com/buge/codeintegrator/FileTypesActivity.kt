package com.buge.codeintegrator

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

class FileTypesActivity : AppCompatActivity() {

    private lateinit var rvFileTypes: RecyclerView
    private lateinit var etFileType: EditText
    private lateinit var btnAdd: Button
    private lateinit var btnReset: Button
    private lateinit var fileTypesList: MutableList<String>
    private lateinit var adapter: FileTypeAdapter
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_types)
        
        rvFileTypes = findViewById(R.id.rvFileTypes)
        etFileType = findViewById(R.id.edtNewFileType)
        btnAdd = findViewById(R.id.btnAddFileType)
        btnReset = findViewById(R.id.btnResetToDefault)
        val btnBack: Button = findViewById(R.id.btnBack)
        
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
                    Toast.makeText(this, "Added: $formatted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Already exists", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please enter a file type", Toast.LENGTH_SHORT).show()
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
            .setTitle("Remove file type")
            .setMessage("Are you sure you want to remove \"$item\"?")
            .setPositiveButton("Remove") { _, _ ->
                fileTypesList.removeAt(position)
                adapter.notifyItemRemoved(position)
                saveFileTypes()
                Toast.makeText(this, "Removed: $item", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showResetConfirmDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Reset to default")
            .setMessage("This will restore all default file types. Continue?")
            .setPositiveButton("Reset") { _, _ ->
                fileTypesList.clear()
                fileTypesList.addAll(getDefaultFileTypes())
                adapter.notifyDataSetChanged()
                saveFileTypes()
                Toast.makeText(this, "Reset to default", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
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
    
    private fun getUnsupportedFileTypesFromPrefs(sharedPreferences: SharedPreferences): Array<String> {
        val savedTypes = sharedPreferences.getString("excluded_types", "")
        return if (savedTypes.isNullOrEmpty()) {
            getDefaultFileTypes().toTypedArray()
        } else {
            savedTypes.split("|").toTypedArray()
        }
    }
}