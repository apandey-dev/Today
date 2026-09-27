package com.minimal.today.model

import java.util.UUID

data class TodoItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val notes: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val isDone: Boolean = false,
    val date: String // format: yyyy-MM-dd
) {
    val formattedTime: String?
        get() = when {
            !startTime.isNullOrBlank() && !endTime.isNullOrBlank() -> "$startTime - $endTime"
            !startTime.isNullOrBlank() -> startTime
            !endTime.isNullOrBlank() -> endTime
            else -> null
        }
}
