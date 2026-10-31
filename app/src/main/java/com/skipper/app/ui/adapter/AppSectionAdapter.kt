package com.skipper.app.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
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
            tvAppSubtitle.text = if (item.summaryRule.isNotBlank()) item.summaryRule else "Tap to configure rules & triggers"
            
            // Avoid triggering listener when updating checked state
            switchEnabled.setOnCheckedChangeListener(null)
            switchEnabled.isChecked = item.isEnabled
            switchEnabled.setOnCheckedChangeListener { _, isChecked ->
                item.isEnabled = isChecked
                onToggle(item, isChecked)
            }

            // Tapping anywhere on the card opens configuration
            root.setOnClickListener {
                onConfigure(item)
            }

            // Tapping the three-dot menu shows options
            btnMenu.setOnClickListener { view ->
                val popup = PopupMenu(view.context, view)
                popup.menu.add(0, 1, 0, "Configure Rules & Triggers")
                popup.menu.add(0, 2, 1, if (item.isEnabled) "Turn Off" else "Turn On")
                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        1 -> onConfigure(item)
                        2 -> {
                            val newState = !item.isEnabled
                            item.isEnabled = newState
                            switchEnabled.isChecked = newState
                            onToggle(item, newState)
                        }
                    }
                    true
                }
                popup.show()
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<AppSection>) {
        items = newItems
        notifyDataSetChanged()
    }
}
