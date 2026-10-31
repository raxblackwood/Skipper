package com.skipper.app.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.skipper.app.databinding.ItemAppSectionBinding
import com.skipper.app.model.AppSection

class AppSectionAdapter(
    private var items: List<AppSection>,
    private val onToggle: (AppSection, Boolean) -> Unit,
    private val onConfigure: (AppSection) -> Unit
) : RecyclerView.Adapter<AppSectionAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemAppSectionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppSectionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        with(holder.binding) {
            tvAppName.text = item.name
            
            // Avoid triggering listener when updating checked state
            switchEnabled.setOnCheckedChangeListener(null)
            switchEnabled.isChecked = item.isEnabled
            switchEnabled.setOnCheckedChangeListener { _, isChecked ->
                item.isEnabled = isChecked
                onToggle(item, isChecked)
            }

            root.setOnClickListener {
                onConfigure(item)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<AppSection>) {
        items = newItems
        notifyDataSetChanged()
    }
}