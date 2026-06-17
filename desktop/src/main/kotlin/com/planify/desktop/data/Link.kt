package com.planify.desktop.data

enum class LinkNodeType { NOTE, TASK }

data class Link(
    val id: Long = 0,
    val sourceId: Long,
    val sourceType: LinkNodeType,
    val targetId: Long,
    val targetType: LinkNodeType
)
