package com.planify.desktop.viewmodel

import com.planify.desktop.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

enum class Screen { GRAPH, BOARD, NOTE_EDITOR, SEARCH, SETTINGS }
enum class AppLanguage(val code: String) { ENGLISH("en"), RUSSIAN("ru") }

data class GraphNode(val id: Long, val type: LinkNodeType, val title: String, val posX: Float, val posY: Float)

data class AppState(
    val currentScreen: Screen = Screen.GRAPH,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val graphNodes: List<GraphNode> = emptyList(),
    val graphLinks: List<Link> = emptyList(),
    val notes: List<Note> = emptyList(),
    val openNote: Note? = null,
    val noteTitle: String = "",
    val noteContent: String = "",
    val isPreview: Boolean = true,
    val todoTasks: List<Task> = emptyList(),
    val inProgressTasks: List<Task> = emptyList(),
    val doneTasks: List<Task> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<Any> = emptyList(),
    val connectionSource: GraphNode? = null
)

class MainViewModel(private val store: FileStore) {
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state

    init { refresh() }

    private fun refresh() {
        val notes = store.getAllNotes()
        val tasks = store.getAllTasks()
        val links = store.getAllLinks()
        val nodes = notes.map { GraphNode(it.id, LinkNodeType.NOTE, it.title, it.posX, it.posY) } +
                tasks.map { GraphNode(it.id, LinkNodeType.TASK, it.title, it.posX, it.posY) }
        _state.update { s ->
            val q = s.searchQuery.lowercase()
            s.copy(
                graphNodes = nodes, graphLinks = links, notes = notes,
                todoTasks = tasks.filter { it.status == TaskStatus.TODO },
                inProgressTasks = tasks.filter { it.status == TaskStatus.IN_PROGRESS },
                doneTasks = tasks.filter { it.status == TaskStatus.DONE },
                searchResults = if (q.isBlank()) emptyList()
                else store.searchNotes(q) + store.searchTasks(q)
            )
        }
    }

    fun navigateTo(screen: Screen) { _state.update { it.copy(currentScreen = screen) } }

    fun setLanguage(lang: AppLanguage) { _state.update { it.copy(language = lang) } }

    // Notes
    fun createNote(title: String) {
        val note = store.insertNote(Note(title = title))
        _state.update { it.copy(openNote = note, currentScreen = Screen.NOTE_EDITOR, noteTitle = note.title, noteContent = note.content, isPreview = false) }
        refresh()
    }

    fun openNote(note: Note) {
        _state.update { it.copy(openNote = note, currentScreen = Screen.NOTE_EDITOR, noteTitle = note.title, noteContent = note.content, isPreview = true) }
    }

    fun updateNoteTitle(t: String) { _state.update { it.copy(noteTitle = t) } }
    fun updateNoteContent(c: String) { _state.update { it.copy(noteContent = c) } }
    fun togglePreview() { _state.update { it.copy(isPreview = !it.isPreview) } }

    fun saveNote() {
        val n = _state.value.openNote ?: return
        val title = _state.value.noteTitle.trim()
        val content = _state.value.noteContent.trim()
        if (title.isBlank()) return
        store.insertNote(n.copy(title = title, content = content, updatedAt = System.currentTimeMillis()))
        processWikilinks(n.id, content)
        _state.update { it.copy(openNote = it.openNote?.copy(title = title, content = content), isPreview = true) }
        refresh()
    }

    fun deleteNote(note: Note) {
        store.deleteNote(note.id)
        _state.update { it.copy(openNote = null, currentScreen = Screen.GRAPH) }
        refresh()
    }

    fun closeNote() { _state.update { it.copy(openNote = null, currentScreen = Screen.GRAPH, isPreview = true) } }

    // Tasks
    fun createTask(title: String, desc: String = "", status: TaskStatus = TaskStatus.TODO) {
        store.insertTask(Task(title = title, description = desc, status = status, orderIndex = store.getNextTaskOrderIndex(status)))
        refresh()
    }

    fun moveTask(id: Long, to: TaskStatus) { store.moveTask(id, to); refresh() }
    fun deleteTask(task: Task) { store.deleteTask(task.id); refresh() }
    fun updateTask(task: Task) { store.insertTask(task); refresh() }

    // Graph
    fun updateNodePosition(id: Long, type: LinkNodeType, x: Float, y: Float) {
        when (type) { LinkNodeType.NOTE -> store.updateNotePosition(id, x, y); LinkNodeType.TASK -> store.updateTaskPosition(id, x, y) }
        refresh()
    }

    fun startConnection(from: GraphNode) { _state.update { it.copy(connectionSource = from) } }

    fun completeConnection(to: GraphNode) {
        val src = _state.value.connectionSource ?: return
        if (src.id == to.id && src.type == to.type) { _state.update { it.copy(connectionSource = null) }; return }
        store.insertLink(Link(sourceId = src.id, sourceType = src.type, targetId = to.id, targetType = to.type))
        _state.update { it.copy(connectionSource = null) }
        refresh()
    }

    fun cancelConnection() { _state.update { it.copy(connectionSource = null) } }

    // Search
    fun updateSearchQuery(q: String) {
        _state.update { it.copy(searchQuery = q) }
        if (q.isBlank()) { _state.update { it.copy(searchResults = emptyList()) }; return }
        _state.update { it.copy(searchResults = store.searchNotes(q) + store.searchTasks(q)) }
    }

    private fun processWikilinks(noteId: Long, content: String) {
        val pattern = Regex("\\[\\[([^\\]]+)\\]\\]")
        for (m in pattern.findAll(content)) {
            val target = store.findByTitle(m.groupValues[1].trim())
            if (target != null) store.insertLink(Link(sourceId = noteId, sourceType = LinkNodeType.NOTE, targetId = target.id, targetType = LinkNodeType.NOTE))
        }
    }
}
