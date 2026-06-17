package com.planify.desktop.data

data class Note(
    val id: Long = 0,
    val title: String,
    val content: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val posX: Float = 0f,
    val posY: Float = 0f
)
