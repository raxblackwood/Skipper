package com.skipper.app.model

data class AppSection(
    val id: String,
    val name: String,
    val targetPackages: List<String>,
    val description: String,
    var isEnabled: Boolean,
    val summaryRule: String,
    val iconResName: String = "ic_chat"
)
