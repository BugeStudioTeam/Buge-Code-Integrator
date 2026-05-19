package com.buge.codeintegrator

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FileTypeAdapter(
    private val list: MutableList<String>,
    private val onDeleteClick: (Int) -> Unit
) : RecyclerView.Adapter<FileTypeAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val container: FrameLayout = v.findViewById(R.id.item_container)
        val title: TextView = v.findViewById(R.id.tv_title)
        val desc: TextView = v.findViewById(R.id.tv_desc)
        val delete: ImageView = v.findViewById(R.id.item_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_file_type, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.title.text = item
        
        val bgRes = when {
            list.size == 1 -> R.drawable.bg_list_item_single
            position == 0 -> R.drawable.bg_list_item_top
            position == list.size - 1 -> R.drawable.bg_list_item_bottom
            else -> R.drawable.bg_list_item_middle
        }
        holder.container.setBackgroundResource(bgRes)
        
        holder.delete.setOnClickListener {
            onDeleteClick(position)
        }
    }
    
    override fun getItemCount() = list.size
}