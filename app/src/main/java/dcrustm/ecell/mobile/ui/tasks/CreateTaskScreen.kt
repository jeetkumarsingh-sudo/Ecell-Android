package dcrustm.ecell.mobile.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dcrustm.ecell.mobile.domain.model.Member
import dcrustm.ecell.mobile.domain.model.TaskPriority
import dcrustm.ecell.mobile.ui.components.SubScreenTopBar
import dcrustm.ecell.mobile.util.formatDate
import dcrustm.ecell.mobile.util.localEndOfDayFromUtcDate
import dcrustm.ecell.mobile.util.todayAsUtcDateMillis

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskScreen(
    onBack: () -> Unit,
    viewModel: CreateTaskViewModel = hiltViewModel()
) {
    val members by viewModel.assignableMembers.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val error by viewModel.error.collectAsState()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var assignee by remember { mutableStateOf<Member?>(null) }
    var assigneeMenuExpanded by remember { mutableStateOf(false) }
    var priority by remember { mutableStateOf(TaskPriority.MEDIUM) }
    var dueAt by remember { mutableLongStateOf(0L) }
    var showDatePicker by remember { mutableStateOf(false) }

    val canSave = title.isNotBlank() && assignee != null && !isSaving
    val fieldColors = OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
    )

    Scaffold(
        topBar = { SubScreenTopBar(title = "Assign task", onBack = onBack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Task") },
                placeholder = { Text("e.g. Design the hackathon poster") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                label = { Text("Details (optional)") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors
            )

            ExposedDropdownMenuBox(
                expanded = assigneeMenuExpanded,
                onExpandedChange = { assigneeMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = assignee?.let { "${it.name} · ${it.committee.shortName}" } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Assign to") },
                    placeholder = { Text(if (members.isEmpty()) "No members available" else "Pick a member") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = assigneeMenuExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors
                )
                ExposedDropdownMenu(
                    expanded = assigneeMenuExpanded,
                    onDismissRequest = { assigneeMenuExpanded = false }
                ) {
                    members.forEach { member ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(member.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "${member.roleLabel} · ${member.committee.shortName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = { assignee = member; assigneeMenuExpanded = false },
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        )
                    }
                }
            }

            Text(
                "Priority",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                TaskPriority.entries.forEachIndexed { index, p ->
                    SegmentedButton(
                        selected = priority == p,
                        onClick = { priority = p },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = TaskPriority.entries.size)
                    ) {
                        Text(p.label)
                    }
                }
            }

            Text(
                "Due date",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (dueAt == 0L) "No due date" else "Due ${formatDate(dueAt)}")
                }
                if (dueAt != 0L) {
                    IconButton(onClick = { dueAt = 0L }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Clear due date")
                    }
                }
            }

            error?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    val person = assignee ?: return@Button
                    viewModel.create(title, description, person, priority, dueAt, onDone = onBack)
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text("Assign task")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDatePicker) {
        val today = remember { todayAsUtcDateMillis() }
        val datePickerState = rememberDatePickerState(
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= today
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { dueAt = localEndOfDayFromUtcDate(it) }
                        showDatePicker = false
                    },
                    enabled = datePickerState.selectedDateMillis != null
                ) { Text("Set") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
