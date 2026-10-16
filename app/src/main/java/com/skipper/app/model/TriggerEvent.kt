package com.skipper.app.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TriggerStatus {
    SUCCESS,
    BLOCKED_BY_SAFEGUARD,
    INFO,
    ERROR
}

data class TriggerEvent(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val appName: String,
    val actionDescription: String,
    val details: String,
    val status: TriggerStatus = TriggerStatus.INFO
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
}
