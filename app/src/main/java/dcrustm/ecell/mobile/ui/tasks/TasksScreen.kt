package dcrustm.ecell.mobile.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dcrustm.ecell.mobile.domain.model.Committee
import dcrustm.ecell.mobile.domain.model.CommitteeTask
import dcrustm.ecell.mobile.domain.model.ROLE
import dcrustm.ecell.mobile.domain.model.TaskPriority
import dcrustm.ecell.mobile.domain.model.TaskStatus
import dcrustm.ecell.mobile.ui.components.EmptyState
import dcrustm.ecell.mobile.ui.components.SubScreenTopBar
import dcrustm.ecell.mobile.ui.home.InitialsAvatar
import dcrustm.ecell.mobile.util.formatDate
import dcrustm.ecell.mobile.util.formatDueLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    onBack: () -> Unit,
    onCreateTask: () -> Unit,
    viewModel: TasksViewModel = hiltViewModel()
) {
    val tasks by viewModel.tasks.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val role = currentUser?.role
    val managesTasks = role == ROLE.COORDINATOR || role == ROLE.COMMITTEE_HEAD
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    val statuses = TaskStatus.entries
    val byStatus = statuses.associateWith { status ->
        tasks.filter { it.status == status }.sortedForBoard(status)
    }

    Scaffold(
        topBar = {
            SubScreenTopBar(
                title = "Tasks",
                subtitle = when (role) {
                    ROLE.COMMITTEE_HEAD -> currentUser?.committee?.displayName
                    ROLE.COORDINATOR -> "All committees"
                    else -> "Assigned to you"
                },
                onBack = onBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (managesTasks) {
                ExtendedFloatingActionButton(
                    text = { Text("Assign task") },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    onClick = onCreateTask,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            TabRow(selectedTabIndex = selectedTab) {
                statuses.forEachIndexed { index, status ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text("${status.label} (${byStatus[status]?.size ?: 0})") }
                    )
                }
            }

            if (managesTasks) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = !filter.mineOnly && filter.committee == null,
                            onClick = { viewModel.setFilter(TaskFilter()) },
                            label = { Text(if (role == ROLE.COORDINATOR) "Everyone" else "Whole team") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = filter.mineOnly,
                            onClick = { viewModel.setFilter(filter.copy(mineOnly = !filter.mineOnly)) },
                            label = { Text("Assigned to me") }
                        )
                    }
                    if (role == ROLE.COORDINATOR) {
                        items(Committee.entries.filter { it != Committee.NONE }) { committee ->
                            FilterChip(
                                selected = filter.committee == committee,
                                onClick = {
                                    viewModel.setFilter(
                                        filter.copy(committee = if (filter.committee == committee) null else committee)
                                    )
                                },
                                label = { Text(committee.shortName) }
                            )
                        }
                    }
                }
            }

            val visible = byStatus[statuses[selectedTab]].orEmpty()
            if (visible.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.TaskAlt,
                    title = when (statuses[selectedTab]) {
                        TaskStatus.TODO -> "Nothing to do"
                        TaskStatus.IN_PROGRESS -> "Nothing in progress"
                        TaskStatus.DONE -> "No finished tasks yet"
                    },
                    message = if (managesTasks) "Tap Assign task to hand out work to your team."
                    else "Tasks your committee head assigns to you show up here."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(visible, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            showAssignee = managesTasks,
                            canManage = viewModel.canManage(task),
                            canUpdateStatus = viewModel.canUpdateStatus(task),
                            onStatusChange = { viewModel.setStatus(task, it) },
                            onDelete = { viewModel.delete(task) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskCard(
    task: CommitteeTask,
    showAssignee: Boolean,
    canManage: Boolean,
    canUpdateStatus: Boolean,
    onStatusChange: (TaskStatus) -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val isDone = task.status == TaskStatus.DONE
    val overdue = task.isOverdue()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (overdue) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PriorityPill(task.priority)
                Spacer(Modifier.width(8.dp))
                if (isDone) {
                    Text(
                        "Done ${formatDate(task.completedAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (task.hasDueDate) {
                    Icon(
                        Icons.Outlined.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (overdue) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        formatDueLabel(task.dueAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (overdue) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (overdue) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
                Spacer(Modifier.weight(1f))
                if (canManage || canUpdateStatus) {
                    Box {
                        IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Task options", modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            if (canUpdateStatus) {
                                TaskStatus.entries.filter { it != task.status }.forEach { status ->
                                    DropdownMenuItem(
                                        text = { Text("Move to ${status.label}") },
                                        onClick = { menuExpanded = false; onStatusChange(status) }
                                    )
                                }
                            }
                            if (canManage) {
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    onClick = { menuExpanded = false; confirmDelete = true }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                task.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textDecoration = if (isDone) TextDecoration.LineThrough else null,
                color = if (isDone) MaterialTheme.colorScheme.onSurfaceVariant else Color.Unspecified,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (task.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    task.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                val person = if (showAssignee) task.assigneeName else task.assignedByName
                InitialsAvatar(
                    initials = person.split(" ").take(2)
                        .joinToString("") { it.firstOrNull()?.toString() ?: "" }.uppercase(),
                    size = 20
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (showAssignee) person else "From $person",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (canUpdateStatus) {
                    when (task.status) {
                        TaskStatus.TODO -> FilledTonalButton(
                            onClick = { onStatusChange(TaskStatus.IN_PROGRESS) },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                        ) { Text("Start", style = MaterialTheme.typography.labelMedium) }

                        TaskStatus.IN_PROGRESS -> FilledTonalButton(
                            onClick = { onStatusChange(TaskStatus.DONE) },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                        ) { Text("Mark done", style = MaterialTheme.typography.labelMedium) }

                        TaskStatus.DONE -> TextButton(
                            onClick = { onStatusChange(TaskStatus.TODO) }
                        ) { Text("Reopen", style = MaterialTheme.typography.labelMedium) }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete task?") },
            text = { Text("\"${task.title}\" will be removed from ${task.assigneeName}'s board.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun PriorityPill(priority: TaskPriority) {
    val (bg, fg) = when (priority) {
        TaskPriority.HIGH -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        TaskPriority.MEDIUM -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        TaskPriority.LOW -> MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(shape = RoundedCornerShape(4.dp), color = bg) {
        Text(
            text = priority.label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
