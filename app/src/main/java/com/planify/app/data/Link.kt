package com.planify.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "links")
data class Link(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceId: Long,
    val sourceType: LinkNodeType,
    val targetId: Long,
    val targetType: LinkNodeType
)

enum class LinkNodeType {
    NOTE, TASK
}
