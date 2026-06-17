package com.planify.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.planify.app.data.AppDatabase
import com.planify.app.data.Link
import com.planify.app.data.LinkNodeType
import com.planify.app.data.Note
import com.planify.app.data.Repository
import com.planify.app.data.Task
import com.planify.app.data.TaskStatus
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class Screen {
    GRAPH, BOARD, NOTE_EDITOR, SEARCH, SETTINGS
}

enum class AppLanguage(val code: String) {
    ENGLISH("en"), RUSSIAN("ru")
}

data class AppState(
    val currentScreen: Screen = Screen.GRAPH,
    val language: AppLanguage = AppLanguage.ENGLISH,
    // Graph
    val graphNodes: List<Repository.GraphNode> = emptyList(),
    val graphLinks: List<Link> = emptyList(),
    // Notes
    val notes: List<Note> = emptyList(),
    val openNote: Note? = null,
    val noteTitle: String = "",
    val noteContent: String = "",
    val isPreview: Boolean = true,
    // Tasks
    val todoTasks: List<Task> = emptyList(),
    val inProgressTasks: List<Task> = emptyList(),
    val doneTasks: List<Task> = emptyList(),
    // Dialogs
    val showNewNoteDialog: Boolean = false,
    val showNewTaskDialog: Boolean = false,
    val newItemTitle: String = "",
    val newItemDescription: String = "",
    val selectedTaskStatus: TaskStatus = TaskStatus.TODO,
    val editingTask: Task? = null,
    val showEditTaskDialog: Boolean = false,
    // Search
    val searchQuery: String = "",
    val searchResults: List<Any> = emptyList(),
    // Connection mode: drag from one node to another
    val connectionSource: Repository.GraphNode? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: Repository

    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()

    init {
        val db = AppDatabase.getInstance(application)
        repository = Repository(db.noteDao(), db.taskDao(), db.linkDao())

        val prefs = application.getSharedPreferences("planify_prefs", android.content.Context.MODE_PRIVATE)
        val savedLang = prefs.getString("language", "en")
        val initialLang = if (savedLang == "ru") AppLanguage.RUSSIAN else AppLanguage.ENGLISH
        _state.update { it.copy(language = initialLang) }

        viewModelScope.launch {
            combine(
                repository.allNotes,
                repository.allTasks,
                repository.allLinks
            ) { notes, tasks, links ->
                val nodes = notes.map { Repository.GraphNode(it.id, LinkNodeType.NOTE, it.title, it.posX, it.posY) } +
                        tasks.map { Repository.GraphNode(it.id, LinkNodeType.TASK, it.title, it.posX, it.posY) }
                Triple(nodes, links, notes to tasks)
            }.collect { (nodes, links, notesAndTasks) ->
                val (notes, tasks) = notesAndTasks
                _state.update { s ->
                    val query = s.searchQuery.lowercase()
                    s.copy(
                        graphNodes = nodes,
                        graphLinks = links,
                        notes = notes,
                        todoTasks = tasks.filter { it.status == TaskStatus.TODO },
                        inProgressTasks = tasks.filter { it.status == TaskStatus.IN_PROGRESS },
                        doneTasks = tasks.filter { it.status == TaskStatus.DONE }
                    )
                }
            }
        }
    }

    fun navigateTo(screen: Screen) {
        _state.update { it.copy(currentScreen = screen) }
    }

    fun openSettings() {
        _state.update { it.copy(currentScreen = Screen.SETTINGS) }
    }

    fun setLanguage(language: AppLanguage) {
        _state.update { it.copy(language = language) }
        val prefs = getApplication<android.app.Application>()
            .getSharedPreferences("planify_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("language", language.code).apply()
    }

    // ---- Notes ----
    fun createNote(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val id = repository.createNote(title.trim())
            val note = repository.getNoteById(id)
            _state.update {
                it.copy(
                    openNote = note,
                    currentScreen = Screen.NOTE_EDITOR,
                    noteTitle = note?.title ?: "",
                    noteContent = note?.content ?: "",
                    isPreview = false
                )
            }
        }
    }

    fun openNote(note: Note) {
        _state.update {
            it.copy(
                openNote = note,
                currentScreen = Screen.NOTE_EDITOR,
                noteTitle = note.title,
                noteContent = note.content,
                isPreview = true
            )
        }
    }

    fun updateNoteTitle(title: String) {
        _state.update { it.copy(noteTitle = title) }
    }

    fun updateNoteContent(content: String) {
        _state.update { it.copy(noteContent = content) }
    }

    fun togglePreview() {
        _state.update { it.copy(isPreview = !it.isPreview) }
    }

    fun saveNote() {
        val note = _state.value.openNote ?: return
        val title = _state.value.noteTitle.trim()
        val content = _state.value.noteContent.trim()
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.updateNote(note.id, title, content)
            _state.update {
                it.copy(
                    openNote = it.openNote?.copy(title = title, content = content),
                    isPreview = true
                )
            }
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
            _state.update {
                it.copy(
                    openNote = null,
                    currentScreen = Screen.GRAPH
                )
            }
        }
    }

    fun closeNote() {
        _state.update {
            it.copy(
                openNote = null,
                currentScreen = Screen.GRAPH,
                isPreview = true
            )
        }
    }

    // ---- Tasks ----
    fun createTaskFromDialog(title: String, description: String, status: TaskStatus) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.createTask(title.trim(), description.trim(), status)
        }
    }

    fun showNewTaskDialog(status: TaskStatus = TaskStatus.TODO) {
        _state.update {
            it.copy(
                showNewTaskDialog = true,
                selectedTaskStatus = status,
                newItemTitle = "",
                newItemDescription = ""
            )
        }
    }

    fun createTask() {
        val title = _state.value.newItemTitle.trim()
        val desc = _state.value.newItemDescription.trim()
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.createTask(title, desc, _state.value.selectedTaskStatus)
            _state.update { it.copy(showNewTaskDialog = false) }
        }
    }

    fun openTaskEdit(task: Task) {
        _state.update {
            it.copy(
                showEditTaskDialog = true,
                editingTask = task,
                newItemTitle = task.title,
                newItemDescription = task.description
            )
        }
    }

    fun updateTask() {
        val task = _state.value.editingTask ?: return
        val title = _state.value.newItemTitle.trim()
        val desc = _state.value.newItemDescription.trim()
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.updateTask(task.copy(title = title, description = desc))
            _state.update { it.copy(showEditTaskDialog = false, editingTask = null) }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch { repository.deleteTask(task) }
    }

    fun moveTask(taskId: Long, toStatus: TaskStatus) {
        viewModelScope.launch { repository.moveTask(taskId, toStatus) }
    }

    // ---- Graph ----
    fun updateNodePosition(id: Long, type: LinkNodeType, x: Float, y: Float) {
        viewModelScope.launch {
            when (type) {
                LinkNodeType.NOTE -> repository.updateNotePosition(id, x, y)
                LinkNodeType.TASK -> repository.updateTaskPosition(id, x, y)
            }
        }
    }

    fun startConnection(fromNode: Repository.GraphNode) {
        _state.update { it.copy(connectionSource = fromNode) }
    }

    fun completeConnection(toNode: Repository.GraphNode) {
        val source = _state.value.connectionSource ?: return
        if (source.id == toNode.id && source.type == toNode.type) {
            _state.update { it.copy(connectionSource = null) }
            return
        }
        viewModelScope.launch {
            repository.createLink(source.id, source.type, toNode.id, toNode.type)
            _state.update { it.copy(connectionSource = null) }
        }
    }

    fun cancelConnection() {
        _state.update { it.copy(connectionSource = null) }
    }

    // ---- Search ----
    fun updateSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
        if (query.isBlank()) {
            _state.update { it.copy(searchResults = emptyList()) }
            return
        }
        viewModelScope.launch {
            val noteResults = kotlinx.coroutines.flow.firstOrNull(repository.searchNotes(query)) ?: emptyList()
            val taskResults = kotlinx.coroutines.flow.firstOrNull(repository.searchTasks(query)) ?: emptyList()
            _state.update { it.copy(searchResults = noteResults + taskResults) }
        }
    }
}
