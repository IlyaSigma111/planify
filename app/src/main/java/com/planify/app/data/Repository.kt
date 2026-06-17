package com.planify.app.data

import kotlinx.coroutines.flow.Flow

class Repository(
    private val noteDao: NoteDao,
    private val taskDao: TaskDao,
    private val linkDao: LinkDao
) {
    // Notes
    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()

    fun searchNotes(query: String): Flow<List<Note>> = noteDao.searchNotes(query)

    suspend fun getNoteById(id: Long): Note? = noteDao.getNoteById(id)

    suspend fun createNote(title: String, content: String = ""): Long {
        val id = noteDao.insert(Note(title = title, content = content, updatedAt = System.currentTimeMillis()))
        processWikilinks(id, content)
        return id
    }

    suspend fun updateNote(id: Long, title: String, content: String) {
        noteDao.update(Note(id = id, title = title, content = content, updatedAt = System.currentTimeMillis()))
        processWikilinks(id, content)
    }

    suspend fun deleteNote(note: Note) {
        linkDao.deleteAllLinksFor(note.id, LinkNodeType.NOTE)
        noteDao.delete(note)
    }

    suspend fun updateNotePosition(id: Long, x: Float, y: Float) {
        noteDao.updatePosition(id, x, y)
    }

    // Tasks
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()

    fun getTasksByStatus(status: TaskStatus): Flow<List<Task>> = taskDao.getTasksByStatus(status)

    fun searchTasks(query: String): Flow<List<Task>> = taskDao.searchTasks(query)

    suspend fun getTaskById(id: Long): Task? = taskDao.getTaskById(id)

    suspend fun createTask(title: String, description: String = "", status: TaskStatus = TaskStatus.TODO): Long {
        val orderIndex = taskDao.getNextOrderIndex(status)
        return taskDao.insert(Task(title = title, description = description, status = status, orderIndex = orderIndex))
    }

    suspend fun updateTask(task: Task) = taskDao.update(task)

    suspend fun deleteTask(task: Task) {
        linkDao.deleteAllLinksFor(task.id, LinkNodeType.TASK)
        taskDao.delete(task)
    }

    suspend fun moveTask(taskId: Long, toStatus: TaskStatus) {
        val orderIndex = taskDao.getNextOrderIndex(toStatus)
        taskDao.moveTask(taskId, toStatus, orderIndex)
    }

    suspend fun updateTaskPosition(id: Long, x: Float, y: Float) {
        taskDao.updatePosition(id, x, y)
    }

    // Links
    val allLinks: Flow<List<Link>> = linkDao.getAllLinks()

    suspend fun createLink(sourceId: Long, sourceType: LinkNodeType, targetId: Long, targetType: LinkNodeType) {
        linkDao.insert(Link(sourceId = sourceId, sourceType = sourceType, targetId = targetId, targetType = targetType))
    }

    suspend fun deleteLink(link: Link) = linkDao.delete(link)

    // Wikilink parsing: [[Title]] -> create link to note with that title
    private suspend fun processWikilinks(noteId: Long, content: String) {
        val pattern = Regex("\\[\\[([^\\]]+)\\]\\]")
        val matches = pattern.findAll(content)
        linkDao.deleteAllLinksFor(noteId, LinkNodeType.NOTE)

        for (match in matches) {
            val targetTitle = match.groupValues[1].trim()
            val existingNote = noteDao.findByTitle(targetTitle)
            if (existingNote != null) {
                linkDao.insert(
                    Link(
                        sourceId = noteId,
                        sourceType = LinkNodeType.NOTE,
                        targetId = existingNote.id,
                        targetType = LinkNodeType.NOTE
                    )
                )
            }
        }
    }

    // Graph data
    data class GraphNode(
        val id: Long,
        val type: LinkNodeType,
        val title: String,
        val posX: Float,
        val posY: Float
    )
}
