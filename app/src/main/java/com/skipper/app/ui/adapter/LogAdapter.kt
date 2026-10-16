package com.skipper.app.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.skipper.app.databinding.ItemLogBinding
import com.skipper.app.model.TriggerEvent
import com.skipper.app.model.TriggerStatus

class LogAdapter(
    private var logs: List<TriggerEvent> = emptyList()
) : RecyclerView.Adapter<LogAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemLogBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLogBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val log = logs[position]
        with(holder.binding) {
            tvLogTime.text = log.formattedTime
            tvLogBadge.text = log.appName
            tvLogMessage.text = "${log.actionDescription}: ${log.details}"

            when (log.status) {
                TriggerStatus.SUCCESS -> {
                    tvLogBadge.setTextColor(Color.parseColor("#81C784"))
                    tvLogMessage.setTextColor(Color.parseColor("#E0E0E0"))
                }
                TriggerStatus.BLOCKED_BY_SAFEGUARD -> {
                    tvLogBadge.setTextColor(Color.parseColor("#FFB74D"))
                    tvLogMessage.setTextColor(Color.parseColor("#B0B0B0"))
                }
                TriggerStatus.ERROR -> {
                    tvLogBadge.setTextColor(Color.parseColor("#E57373"))
                    tvLogMessage.setTextColor(Color.parseColor("#FFCDD2"))
                }
                TriggerStatus.INFO -> {
                    tvLogBadge.setTextColor(Color.parseColor("#BB86FC"))
                    tvLogMessage.setTextColor(Color.parseColor("#E0E0E0"))
                }
            }
        }
    }

    override fun getItemCount(): Int = logs.size

    fun updateLogs(newLogs: List<TriggerEvent>) {
        logs = newLogs
        notifyDataSetChanged()
    }
}
