package com.planify.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.planify.app.data.TaskStatus
import com.planify.app.ui.components.GraphView
import com.planify.app.ui.screens.BoardScreen
import com.planify.app.ui.screens.NoteEditorScreen
import com.planify.app.ui.screens.SearchScreen
import com.planify.app.ui.theme.PlanifyTheme
import com.planify.app.viewmodel.MainViewModel
import com.planify.app.viewmodel.Screen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PlanifyTheme {
                PlanifyApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanifyApp(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    var showNewNoteDialog by remember { mutableStateOf(false) }
    var newNoteTitle by remember { mutableStateOf("") }

    var showNewTaskDialog by remember { mutableStateOf(false) }
    var newTaskTitle by remember { mutableStateOf("") }
    var newTaskDesc by remember { mutableStateOf("") }
    var newTaskStatus by remember { mutableStateOf(TaskStatus.TODO) }

    if (showNewNoteDialog) {
        AlertDialog(
            onDismissRequest = { showNewNoteDialog = false },
            title = { Text("New Note") },
            text = {
                OutlinedTextField(
                    value = newNoteTitle,
                    onValueChange = { newNoteTitle = it },
                    placeholder = { Text("Note title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newNoteTitle.isNotBlank()) {
                        viewModel.createNote(newNoteTitle.trim())
                        newNoteTitle = ""
                        showNewNoteDialog = false
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = {
                    newNoteTitle = ""
                    showNewNoteDialog = false
                }) { Text("Cancel") }
            }
        )
    }

    if (showNewTaskDialog) {
        AlertDialog(
            onDismissRequest = { showNewTaskDialog = false },
            title = { Text("New Task") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTaskTitle,
                        onValueChange = { newTaskTitle = it },
                        placeholder = { Text("Task title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newTaskDesc,
                        onValueChange = { newTaskDesc = it },
                        placeholder = { Text("Description (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newTaskTitle.isNotBlank()) {
                        viewModel.createTaskFromDialog(newTaskTitle.trim(), newTaskDesc.trim(), newTaskStatus)
                        newTaskTitle = ""
                        newTaskDesc = ""
                        showNewTaskDialog = false
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = {
                    newTaskTitle = ""
                    newTaskDesc = ""
                    showNewTaskDialog = false
                }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = when (state.currentScreen) {
                            Screen.GRAPH -> "Planify"
                            Screen.BOARD -> "Board"
                            Screen.NOTE_EDITOR -> "Note"
                            Screen.SEARCH -> "Search"
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (state.currentScreen != Screen.NOTE_EDITOR) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = state.currentScreen == Screen.GRAPH,
                        onClick = { viewModel.navigateTo(Screen.GRAPH) },
                        icon = { Icon(Icons.Default.Hub, contentDescription = null) },
                        label = { Text("Graph") },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                    )
                    NavigationBarItem(
                        selected = state.currentScreen == Screen.BOARD,
                        onClick = { viewModel.navigateTo(Screen.BOARD) },
                        icon = { Icon(Icons.Default.ViewKanban, contentDescription = null) },
                        label = { Text("Board") },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                    )
                    NavigationBarItem(
                        selected = state.currentScreen == Screen.SEARCH,
                        onClick = { viewModel.navigateTo(Screen.SEARCH) },
                        icon = { Icon(Icons.Default.Search, contentDescription = null) },
                        label = { Text("Search") },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            when (state.currentScreen) {
                Screen.GRAPH -> {
                    FloatingActionButton(
                        onClick = { showNewNoteDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.Article, contentDescription = "New note")
                    }
                }
                Screen.BOARD -> {
                    FloatingActionButton(
                        onClick = {
                            newTaskStatus = TaskStatus.TODO
                            showNewTaskDialog = true
                        },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.TaskAlt, contentDescription = "New task")
                    }
                }
                else -> {}
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (state.currentScreen) {
                Screen.GRAPH -> {
                    GraphView(
                        nodes = state.graphNodes,
                        links = state.graphLinks,
                        connectionSource = state.connectionSource,
                        onNodeDrag = { id, type, x, y ->
                            viewModel.updateNodePosition(id, type, x, y)
                        },
                        onNodeTap = { node ->
                            when (node.type) {
                                com.planify.app.data.LinkNodeType.NOTE -> {
                                    val note = state.notes.find { it.id == node.id }
                                    if (note != null) viewModel.openNote(note)
                                }
                                com.planify.app.data.LinkNodeType.TASK -> {
                                    viewModel.openTaskEdit(
                                        state.todoTasks.find { it.id == node.id }
                                            ?: state.inProgressTasks.find { it.id == node.id }
                                            ?: state.doneTasks.find { it.id == node.id }
                                    )
                                }
                            }
                        },
                        onNodeDoubleTap = { node ->
                            when (node.type) {
                                com.planify.app.data.LinkNodeType.NOTE -> {
                                    val note = state.notes.find { it.id == node.id }
                                    if (note != null) viewModel.openNote(note)
                                }
                                com.planify.app.data.LinkNodeType.TASK -> {}
                            }
                        },
                        onConnectionEnd = { targetNode ->
                            viewModel.completeConnection(targetNode)
                        }
                    )
                }
                Screen.BOARD -> {
                    BoardScreen(
                        todoTasks = state.todoTasks,
                        inProgressTasks = state.inProgressTasks,
                        doneTasks = state.doneTasks,
                        onAddTask = { status ->
                            newTaskStatus = status
                            showNewTaskDialog = true
                        },
                        onEditTask = { viewModel.openTaskEdit(it) },
                        onDeleteTask = { viewModel.deleteTask(it) },
                        onMoveTask = { id, status -> viewModel.moveTask(id, status) },
                        onBack = { viewModel.navigateTo(Screen.GRAPH) }
                    )
                }
                Screen.SEARCH -> {
                    SearchScreen(
                        query = state.searchQuery,
                        results = state.searchResults,
                        onQueryChange = { viewModel.updateSearchQuery(it) },
                        onNoteClick = { viewModel.openNote(it) },
                        onTaskClick = { viewModel.openTaskEdit(it) },
                        onBack = { viewModel.navigateTo(Screen.GRAPH) }
                    )
                }
                Screen.NOTE_EDITOR -> {
                    NoteEditorScreen(
                        note = state.openNote,
                        title = state.noteTitle,
                        content = state.noteContent,
                        isPreview = state.isPreview,
                        onTitleChange = { viewModel.updateNoteTitle(it) },
                        onContentChange = { viewModel.updateNoteContent(it) },
                        onTogglePreview = { viewModel.togglePreview() },
                        onSave = { viewModel.saveNote() },
                        onDelete = { viewModel.deleteNote(it) },
                        onBack = { viewModel.closeNote() }
                    )
                }
            }
        }
    }
}
