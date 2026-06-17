package com.planify.desktop.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

data class StoreData(
    val notes: MutableList<Note> = mutableListOf(),
    val tasks: MutableList<Task> = mutableListOf(),
    val links: MutableList<Link> = mutableListOf(),
    private var nextNoteId: Long = 1,
    private var nextTaskId: Long = 1,
    private var nextLinkId: Long = 1
) {
    fun nextNote(): Long = nextNoteId++
    fun nextTask(): Long = nextTaskId++
    fun nextLink(): Long = nextLinkId++
}

class FileStore(private val dataDir: String) {
    private val gson = Gson()
    private val file: File
    private var data = StoreData()

    init {
        val dir = File(dataDir, "planify")
        dir.mkdirs()
        file = File(dir, "data.json")
        load()
    }

    private fun load() {
        if (file.exists()) {
            try {
                val type = object : TypeToken<StoreData>() {}.type
                data = gson.fromJson(file.readText(), type) ?: StoreData()
            } catch (_: Exception) {
                data = StoreData()
            }
        }
    }

    private fun save() {
        file.writeText(gson.toJson(data))
    }

    fun getAllNotes(): List<Note> = data.notes.toList()
    fun getAllTasks(): List<Task> = data.tasks.toList()
    fun getAllLinks(): List<Link> = data.links.toList()

    fun getNoteById(id: Long): Note? = data.notes.find { it.id == id }
    fun getTaskById(id: Long): Task? = data.tasks.find { it.id == id }
    fun findByTitle(title: String): Note? = data.notes.find { it.title == title }

    fun searchNotes(query: String): List<Note> {
        val q = query.lowercase()
        return data.notes.filter { it.title.lowercase().contains(q) || it.content.lowercase().contains(q) }
    }

    fun searchTasks(query: String): List<Task> {
        val q = query.lowercase()
        return data.tasks.filter { it.title.lowercase().contains(q) || it.description.lowercase().contains(q) }
    }

    fun insertNote(note: Note): Note {
        val id = if (note.id == 0L) data.nextNote() else note.id
        val n = note.copy(id = id)
        data.notes.removeAll { it.id == id }
        data.notes.add(n)
        save()
        return n
    }

    fun insertTask(task: Task): Task {
        val id = if (task.id == 0L) data.nextTask() else task.id
        val t = task.copy(id = id)
        data.tasks.removeAll { it.id == id }
        data.tasks.add(t)
        save()
        return t
    }

    fun insertLink(link: Link): Link {
        val id = if (link.id == 0L) data.nextLink() else link.id
        val l = link.copy(id = id)
        data.links.add(l)
        save()
        return l
    }

    fun deleteNote(id: Long) {
        data.notes.removeAll { it.id == id }
        data.links.removeAll { (it.sourceId == id && it.sourceType == LinkNodeType.NOTE) || (it.targetId == id && it.targetType == LinkNodeType.NOTE) }
        save()
    }

    fun deleteTask(id: Long) {
        data.tasks.removeAll { it.id == id }
        data.links.removeAll { (it.sourceId == id && it.sourceType == LinkNodeType.TASK) || (it.targetId == id && it.targetType == LinkNodeType.TASK) }
        save()
    }

    fun deleteLink(link: Link) {
        data.links.removeAll { it.id == link.id }
        save()
    }

    fun updateNotePosition(id: Long, x: Float, y: Float) {
        data.notes.replaceAll { if (it.id == id) it.copy(posX = x, posY = y) else it }
        save()
    }

    fun updateTaskPosition(id: Long, x: Float, y: Float) {
        data.tasks.replaceAll { if (it.id == id) it.copy(posX = x, posY = y) else it }
        save()
    }

    fun getNextTaskOrderIndex(status: TaskStatus): Int {
        return (data.tasks.filter { it.status == status }.maxOfOrNull { it.orderIndex } ?: -1) + 1
    }

    fun moveTask(taskId: Long, toStatus: TaskStatus) {
        val idx = getNextTaskOrderIndex(toStatus)
        data.tasks.replaceAll { if (it.id == taskId) it.copy(status = toStatus, orderIndex = idx) else it }
        save()
    }
}
