package com.planify.desktop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.planify.desktop.data.FileStore
import com.planify.desktop.data.LinkNodeType
import com.planify.desktop.data.Note
import com.planify.desktop.data.Task
import com.planify.desktop.data.TaskStatus
import com.planify.desktop.ui.theme.Amber
import com.planify.desktop.ui.theme.Cyan
import com.planify.desktop.ui.theme.PlanifyTheme
import com.planify.desktop.ui.theme.Primary
import com.planify.desktop.ui.theme.StatusDone
import com.planify.desktop.ui.theme.StatusProgress
import com.planify.desktop.ui.theme.StatusTodo
import com.planify.desktop.viewmodel.*

fun main() {
    val dataDir = System.getProperty("user.home") ?: "."
    val store = FileStore(dataDir)
    val viewModel = MainViewModel(store)

    application {
        val state by viewModel.state.collectAsState()

        Window(
            onCloseRequest = ::exitApplication,
            title = "Planify",
            state = rememberWindowState(size = DpSize(1200.dp, 800.dp))
        ) {
            PlanifyTheme {
                AppContent(viewModel, state)
            }
        }
    }
}

@Composable
fun AppContent(viewModel: MainViewModel, state: AppState) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail {
                Spacer(Modifier.height(8.dp))
                Text(
                    "P",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                NavigationRailItem(
                    selected = state.currentScreen == Screen.GRAPH,
                    onClick = { viewModel.navigateTo(Screen.GRAPH) },
                    icon = { Icon(Icons.Default.Hub, contentDescription = null) },
                    label = { Text("Graph", style = MaterialTheme.typography.labelSmall) }
                )
                NavigationRailItem(
                    selected = state.currentScreen == Screen.BOARD,
                    onClick = { viewModel.navigateTo(Screen.BOARD) },
                    icon = { Icon(Icons.Default.ViewKanban, contentDescription = null) },
                    label = { Text("Board", style = MaterialTheme.typography.labelSmall) }
                )
                NavigationRailItem(
                    selected = state.currentScreen == Screen.SEARCH,
                    onClick = { viewModel.navigateTo(Screen.SEARCH) },
                    icon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = { Text("Search", style = MaterialTheme.typography.labelSmall) }
                )
                Spacer(Modifier.weight(1f))
                NavigationRailItem(
                    selected = state.currentScreen == Screen.SETTINGS,
                    onClick = { viewModel.navigateTo(Screen.SETTINGS) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Settings", style = MaterialTheme.typography.labelSmall) }
                )
            }

            when (state.currentScreen) {
                Screen.GRAPH -> GraphView(state, viewModel)
                Screen.BOARD -> BoardView(state, viewModel)
                Screen.SEARCH -> SearchView(state, viewModel)
                Screen.NOTE_EDITOR -> NoteEditorView(state, viewModel)
                Screen.SETTINGS -> SettingsView(state, viewModel)
            }
        }
    }
}

// ===== GRAPH VIEW =====
@Composable
fun GraphView(state: AppState, vm: MainViewModel) {
    val nodeSize = 28f
    val nodes = remember(state.graphNodes) {
        state.graphNodes.map { n ->
            GraphNodeRender(
                id = n.id, type = n.type, title = n.title,
                x = if (n.posX != 0f) n.posX else (Math.random() * 700).toFloat() + 100,
                y = if (n.posY != 0f) n.posY else (Math.random() * 400).toFloat() + 100
            )
        }.toMutableList()
    }
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(nodes) {
                    detectTapGestures { offset ->
                        val hit = nodes.find { n ->
                            val dx = offset.x - n.x; val dy = offset.y - n.y
                            Math.sqrt((dx * dx + dy * dy).toDouble()) < nodeSize
                        }
                        if (hit != null) {
                            when (hit.type) {
                                LinkNodeType.NOTE -> {
                                    val note = state.notes.find { it.id == hit.id }
                                    if (note != null) vm.openNote(note)
                                }
                                LinkNodeType.TASK -> {}
                            }
                        }
                    }
                }
                .pointerInput(nodes) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val hit = nodes.find { n ->
                                val dx = change.position.x - n.x; val dy = change.position.y - n.y
                                Math.sqrt((dx * dx + dy * dy).toDouble()) < nodeSize
                            }
                            if (hit != null) {
                                hit.x += dragAmount.x
                                hit.y += dragAmount.y
                                vm.updateNodePosition(hit.id, hit.type, hit.x, hit.y)
                            }
                        }
                    )
                }
        ) {
            val linkColor = Primary.copy(alpha = 0.25f)
            for (link in state.graphLinks) {
                val s = nodes.find { it.id == link.sourceId && it.type == link.sourceType }
                val t = nodes.find { it.id == link.targetId && it.type == link.targetType }
                if (s != null && t != null) {
                    drawLine(
                        color = linkColor, strokeWidth = 1.5f,
                        start = Offset(s.x, s.y), end = Offset(t.x, t.y)
                    )
                }
            }
            for (n in nodes) {
                val c = if (n.type == LinkNodeType.NOTE) Primary else Amber
                val r = if (n.type == LinkNodeType.NOTE) 26f else 20f
                drawCircle(color = c.copy(alpha = 0.15f), radius = r + 8f, center = Offset(n.x, n.y))
                drawCircle(color = c, radius = r, center = Offset(n.x, n.y), style = Stroke(width = 2f))
                drawCircle(color = c.copy(alpha = 0.12f), radius = r, center = Offset(n.x, n.y))
            }
        }
    }
}

private data class GraphNodeRender(
    var id: Long, var type: LinkNodeType, var title: String,
    var x: Float, var y: Float
)

// ===== BOARD VIEW =====
@Composable
fun BoardView(state: AppState, vm: MainViewModel) {
    var showNewTaskDialog by remember { mutableStateOf(false) }
    var taskCol by remember { mutableStateOf(TaskStatus.TODO) }
    var taskTitle by remember { mutableStateOf("") }
    var taskDesc by remember { mutableStateOf("") }

    if (showNewTaskDialog) {
        AlertDialog(
            onDismissRequest = { showNewTaskDialog = false },
            title = { Text("New Task") },
            text = {
                Column {
                    OutlinedTextField(value = taskTitle, onValueChange = { taskTitle = it },
                        placeholder = { Text("Task title") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = taskDesc, onValueChange = { taskDesc = it },
                        placeholder = { Text("Description (optional)") },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), maxLines = 3)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (taskTitle.isNotBlank()) {
                        vm.createTask(taskTitle.trim(), taskDesc.trim(), taskCol)
                        taskTitle = ""; taskDesc = ""; showNewTaskDialog = false
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { taskTitle = ""; taskDesc = ""; showNewTaskDialog = false }) { Text("Cancel") }
            }
        )
    }

    Row(Modifier.fillMaxSize().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
            Triple("To Do", state.todoTasks, StatusTodo),
            Triple("In Progress", state.inProgressTasks, StatusProgress),
            Triple("Done", state.doneTasks, StatusDone)
        ).forEach { (title, items, color) ->
            val col = when (title) {
                "To Do" -> TaskStatus.TODO
                "In Progress" -> TaskStatus.IN_PROGRESS
                else -> TaskStatus.DONE
            }
            Card(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.fillMaxSize().padding(8.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(color, RoundedCornerShape(4.dp)))
                        Spacer(Modifier.width(6.dp))
                        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Text("${items.size}", color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(Modifier.height(6.dp))
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(items) { task ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(Modifier.padding(8.dp)) {
                                    Text(task.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    if (task.description.isNotBlank()) {
                                        Text(task.description, style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End) {
                                        if (col == TaskStatus.IN_PROGRESS || col == TaskStatus.DONE) {
                                            IconButton(onClick = {
                                                vm.moveTask(task.id, TaskStatus.entries[TaskStatus.entries.indexOf(col) - 1])
                                            }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Left",
                                                    modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        if (col == TaskStatus.TODO || col == TaskStatus.IN_PROGRESS) {
                                            IconButton(onClick = {
                                                vm.moveTask(task.id, TaskStatus.entries[TaskStatus.entries.indexOf(col) + 1])
                                            }, modifier = Modifier.size(24.dp)) {
                                                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Right",
                                                    modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        IconButton(onClick = { vm.deleteTask(task) }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete",
                                                modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { taskCol = col; taskTitle = ""; taskDesc = ""; showNewTaskDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Primary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

// ===== SEARCH VIEW =====
@Composable
fun SearchView(state: AppState, vm: MainViewModel) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { vm.updateSearchQuery(it) },
            placeholder = { Text("Search notes and tasks...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
        )
        Spacer(Modifier.height(12.dp))
        if (state.searchQuery.isBlank()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Type to search", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else if (state.searchResults.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No results found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(state.searchResults) { item ->
                    when (item) {
                        is Note -> SearchResultItem(
                            title = item.title, subtitle = item.content.take(60),
                            label = "Note", labelColor = Primary,
                            onClick = { vm.openNote(item) }
                        )
                        is Task -> SearchResultItem(
                            title = item.title, subtitle = item.description,
                            label = "Task", labelColor = Amber,
                            onClick = { vm.navigateTo(Screen.BOARD) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    title: String, subtitle: String, label: String,
    labelColor: Color, onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(28.dp).background(labelColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(label.first().toString(), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = labelColor)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                if (subtitle.isNotBlank()) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(4.dp)) {
                Text(label, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ===== NOTE EDITOR =====
@Composable
fun NoteEditorView(state: AppState, vm: MainViewModel) {
    val scrollState = rememberScrollState()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = state.noteTitle,
                onValueChange = { vm.updateNoteTitle(it) },
                placeholder = { Text("Note title") },
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(0.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent
                )
            )
            Spacer(Modifier.width(8.dp))
            if (state.isPreview) {
                IconButton(onClick = { vm.togglePreview() }) {
                    Icon(Icons.Default.Article, contentDescription = "Edit", tint = Primary)
                }
            } else {
                IconButton(onClick = {
                    vm.saveNote()
                }) {
                    Icon(Icons.Default.Check, contentDescription = "Save", tint = StatusDone)
                }
            }
            IconButton(onClick = {
                val note = state.openNote ?: return@IconButton
                vm.deleteNote(note)
            }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        if (state.isPreview) {
            Column(
                Modifier.fillMaxSize().verticalScroll(scrollState).padding(4.dp)
            ) {
                renderMarkdown(state.noteContent)
            }
        } else {
            OutlinedTextField(
                value = state.noteContent,
                onValueChange = { vm.updateNoteContent(it) },
                placeholder = { Text("Write in markdown... Use [[Title]] for links") },
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(0.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedContainerColor = Color.Transparent
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
            )
        }
    }
}

@Composable
private fun renderMarkdown(md: String) {
    if (md.isBlank()) {
        Text("Empty note", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val lines = md.split("\n")
    var inCode = false
    var codeContent = ""
    for (line in lines) {
        when {
            line.startsWith("```") -> {
                if (inCode) {
                    Text(codeContent.trimEnd(), fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp, color = Amber,
                        modifier = Modifier.fillMaxWidth().background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(6.dp)
                        ).padding(12.dp))
                    codeContent = ""
                    inCode = false
                } else {
                    inCode = true
                }
            }
            inCode -> codeContent += line + "\n"
            line.startsWith("### ") -> Text(line.drop(4), style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 2.dp))
            line.startsWith("## ") -> Text(line.drop(3), style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
            line.startsWith("# ") -> Text(line.drop(2), style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
            line.startsWith("- ") || line.startsWith("* ") -> Text("  •  ${line.drop(2)}",
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
            line.startsWith("> ") -> Text(line.drop(2), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().background(
                    Primary.copy(alpha = 0.04f)
                ).padding(horizontal = 12.dp, vertical = 6.dp))
            line.isBlank() -> Spacer(Modifier.height(4.dp))
            else -> {
                val rendered = line
                    .replace(Regex("\\*\\*(.+?)\\*\\*")) { "<b>${it.groupValues[1]}</b>" }
                    .replace(Regex("`([^`]+)`")) { "<code>${it.groupValues[1]}</code>" }
                if (rendered.contains("<b>") || rendered.contains("<code>")) {
                    val parts = rendered.split(Regex("(?=<)|(?<=>)"), 0)
                    Row {
                        for (part in parts) {
                            when {
                                part.startsWith("<b>") -> Text(part.removePrefix("<b>").removeSuffix("</b>"),
                                    fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                part.startsWith("<code>") -> Text(part.removePrefix("<code>").removeSuffix("</code>"),
                                    fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = Cyan,
                                    modifier = Modifier.background(Primary.copy(alpha = 0.1f)))
                                else -> Text(part, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                } else {
                    Text(line, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

// ===== SETTINGS =====
@Composable
fun SettingsView(state: AppState, vm: MainViewModel) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(32.dp))
        Text("Language", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth().clickable { vm.setLanguage(AppLanguage.ENGLISH) },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (state.language == AppLanguage.ENGLISH)
                    Primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("English", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.weight(1f))
                if (state.language == AppLanguage.ENGLISH) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Primary)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth().clickable { vm.setLanguage(AppLanguage.RUSSIAN) },
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (state.language == AppLanguage.RUSSIAN)
                    Primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Русский", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.weight(1f))
                if (state.language == AppLanguage.RUSSIAN) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Primary)
                }
            }
        }
    }
}
