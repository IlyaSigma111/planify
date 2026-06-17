package com.planify.desktop.data

enum class TaskStatus { TODO, IN_PROGRESS, DONE }

data class Task(
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val status: TaskStatus = TaskStatus.TODO,
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val posX: Float = 0f,
    val posY: Float = 0f
)
