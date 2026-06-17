package com.planify.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.planify.app.data.Task
import com.planify.app.data.TaskStatus
import com.planify.app.ui.theme.StatusDone
import com.planify.app.ui.theme.StatusInProgress
import com.planify.app.ui.theme.StatusTodo

@Composable
fun BoardScreen(
    todoTasks: List<Task>,
    inProgressTasks: List<Task>,
    doneTasks: List<Task>,
    onAddTask: (TaskStatus) -> Unit,
    onEditTask: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onMoveTask: (Long, TaskStatus) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TaskColumn(
            title = "To Do",
            tasks = todoTasks,
            status = TaskStatus.TODO,
            color = StatusTodo,
            onAddTask = onAddTask,
            onEditTask = onEditTask,
            onDeleteTask = onDeleteTask,
            onMoveTask = onMoveTask,
            modifier = Modifier.weight(1f)
        )
        TaskColumn(
            title = "In Progress",
            tasks = inProgressTasks,
            status = TaskStatus.IN_PROGRESS,
            color = StatusInProgress,
            onAddTask = onAddTask,
            onEditTask = onEditTask,
            onDeleteTask = onDeleteTask,
            onMoveTask = onMoveTask,
            modifier = Modifier.weight(1f)
        )
        TaskColumn(
            title = "Done",
            tasks = doneTasks,
            status = TaskStatus.DONE,
            color = StatusDone,
            onAddTask = onAddTask,
            onEditTask = onEditTask,
            onDeleteTask = onDeleteTask,
            onMoveTask = onMoveTask,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TaskColumn(
    title: String,
    tasks: List<Task>,
    status: TaskStatus,
    color: androidx.compose.ui.graphics.Color,
    onAddTask: (TaskStatus) -> Unit,
    onEditTask: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onMoveTask: (Long, TaskStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                RoundedCornerShape(16.dp)
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = color
            )
            Text(
                text = "${tasks.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tasks, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    onEdit = { onEditTask(task) },
                    onDelete = { onDeleteTask(task) },
                    onMoveLeft = when (status) {
                        TaskStatus.IN_PROGRESS -> {{ onMoveTask(task.id, TaskStatus.TODO) }}
                        TaskStatus.DONE -> {{ onMoveTask(task.id, TaskStatus.IN_PROGRESS) }}
                        else -> null
                    },
                    onMoveRight = when (status) {
                        TaskStatus.TODO -> {{ onMoveTask(task.id, TaskStatus.IN_PROGRESS) }}
                        TaskStatus.IN_PROGRESS -> {{ onMoveTask(task.id, TaskStatus.DONE) }}
                        else -> null
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        FilledTonalButton(
            onClick = { onAddTask(status) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text("Add Task", modifier = Modifier.padding(start = 4.dp))
        }
    }
}

@Composable
private fun TaskCard(
    task: Task,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveLeft: (() -> Unit)?,
    onMoveRight: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (task.status == TaskStatus.DONE) TextDecoration.LineThrough else TextDecoration.None
            )

            if (task.description.isNotBlank()) {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                onMoveLeft?.let {
                    IconButton(onClick = it, modifier = Modifier.width(32.dp).height(32.dp)) {
                        Text("<", style = MaterialTheme.typography.labelLarge)
                    }
                }
                IconButton(onClick = onEdit, modifier = Modifier.width(32.dp).height(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.width(16.dp).height(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.width(32.dp).height(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.width(16.dp).height(16.dp))
                }
                onMoveRight?.let {
                    IconButton(onClick = it, modifier = Modifier.width(32.dp).height(32.dp)) {
                        Text(">", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
