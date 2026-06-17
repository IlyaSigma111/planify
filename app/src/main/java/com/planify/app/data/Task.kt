package com.planify.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TaskStatus {
    TODO, IN_PROGRESS, DONE
}

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val status: TaskStatus = TaskStatus.TODO,
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val posX: Float = 0f,
    val posY: Float = 0f
)
