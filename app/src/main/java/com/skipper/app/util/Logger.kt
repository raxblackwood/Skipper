package com.skipper.app.util

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.skipper.app.model.TriggerEvent
import com.skipper.app.model.TriggerStatus
import java.util.concurrent.CopyOnWriteArrayList

object Logger {
    private const val TAG = "Skipper"
    private const val MAX_LOGS = 100

    private val logList = CopyOnWriteArrayList<TriggerEvent>()
    private val _logsLiveData = MutableLiveData<List<TriggerEvent>>(emptyList())
    val logsLiveData: LiveData<List<TriggerEvent>> = _logsLiveData

    fun log(appName: String, action: String, details: String, status: TriggerStatus = TriggerStatus.INFO) {
        val event = TriggerEvent(
            appName = appName,
            actionDescription = action,
            details = details,
            status = status
        )
        when (status) {
            TriggerStatus.SUCCESS -> Log.i(TAG, "[$appName] $action - $details")
            TriggerStatus.BLOCKED_BY_SAFEGUARD -> Log.d(TAG, "[$appName SAFEGUARD] $action - $details")
            TriggerStatus.INFO -> Log.i(TAG, "[$appName] $action - $details")
            TriggerStatus.ERROR -> Log.e(TAG, "[$appName ERROR] $action - $details")
        }

        logList.add(0, event)
        if (logList.size > MAX_LOGS) {
            logList.removeAt(logList.lastIndex)
        }
        _logsLiveData.postValue(logList.toList())
    }

    fun clear() {
        logList.clear()
        _logsLiveData.postValue(emptyList())
    }
}
