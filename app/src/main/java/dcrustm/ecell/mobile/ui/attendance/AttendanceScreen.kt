package dcrustm.ecell.mobile.ui.attendance

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import dcrustm.ecell.mobile.domain.model.CheckInResult
import dcrustm.ecell.mobile.domain.model.Committee
import dcrustm.ecell.mobile.domain.model.Meeting
import dcrustm.ecell.mobile.domain.model.ROLE
import dcrustm.ecell.mobile.ui.components.EmptyState
import dcrustm.ecell.mobile.ui.components.SectionLabel
import dcrustm.ecell.mobile.ui.components.SubScreenTopBar
import dcrustm.ecell.mobile.ui.home.CommitteeBadge
import dcrustm.ecell.mobile.util.formatDateTime
import dcrustm.ecell.mobile.util.formatTimeAgo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    onBack: () -> Unit,
    onOpenHostView: (String) -> Unit,
    viewModel: AttendanceViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isBusy by viewModel.isBusy.collectAsState()
    val canHost = currentUser?.role == ROLE.COORDINATOR || currentUser?.role == ROLE.COMMITTEE_HEAD
    val snackbarHostState = remember { SnackbarHostState() }

    var showStartDialog by remember { mutableStateOf(false) }
    var checkInTarget by remember { mutableStateOf<Meeting?>(null) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = { SubScreenTopBar(title = "Attendance", subtitle = "Check in to meetings", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (canHost) {
                ExtendedFloatingActionButton(
                    text = { Text("Start meeting") },
                    icon = { Icon(Icons.Filled.PlayArrow, contentDescription = null) },
                    onClick = { showStartDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 88.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { AttendanceStatsCard(state.stats) }

            if (state.live.isNotEmpty()) {
                item { SectionLabel("Live now", modifier = Modifier.padding(top = 6.dp)) }
                items(state.live, key = { it.meeting.id }) { row ->
                    LiveMeetingCard(
                        row = row,
                        onCheckIn = { checkInTarget = row.meeting },
                        onOpenHostView = { onOpenHostView(row.meeting.id) }
                    )
                }
            }

            if (state.past.isNotEmpty()) {
                item { SectionLabel("Past meetings", modifier = Modifier.padding(top = 6.dp)) }
                items(state.past, key = { it.meeting.id }) { row ->
                    PastMeetingRow(
                        row = row,
                        onClick = if (row.canManage) ({ onOpenHostView(row.meeting.id) }) else null
                    )
                }
            }

            if (state.live.isEmpty() && state.past.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.EventAvailable,
                        title = "No meetings yet",
                        message = if (canHost) "Tap Start meeting when your session begins — members check in with a 4-digit code."
                        else "When a meeting starts, it shows up here and you can check in with the host's code.",
                        modifier = Modifier.height(280.dp)
                    )
                }
            }
        }
    }

    if (showStartDialog) {
        StartMeetingDialog(
            isCoordinator = currentUser?.role == ROLE.COORDINATOR,
            ownCommittee = currentUser?.committee ?: Committee.NONE,
            isBusy = isBusy,
            onDismiss = { showStartDialog = false },
            onStart = { title, committee ->
                viewModel.startMeeting(title, committee) { id ->
                    showStartDialog = false
                    onOpenHostView(id)
                }
            }
        )
    }

    checkInTarget?.let { meeting ->
        CheckInDialog(
            meeting = meeting,
            isBusy = isBusy,
            onDismiss = { checkInTarget = null },
            onSubmit = { code, onResult -> viewModel.checkIn(meeting, code, onResult) },
            onCheckedIn = { checkInTarget = null }
        )
    }
}

@Composable
private fun AttendanceStatsCard(stats: AttendanceStats) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Your attendance",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    if (stats.total == 0) "—" else "${stats.percent}%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    if (stats.total == 0) "No meetings yet"
                    else "${stats.attended} of ${stats.total} meeting${if (stats.total != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { if (stats.total == 0) 0f else stats.attended.toFloat() / stats.total },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun LiveMeetingCard(row: MeetingRow, onCheckIn: () -> Unit, onOpenHostView: () -> Unit) {
    val meeting = row.meeting
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CommitteeBadge(label = meeting.committee?.shortName ?: "All", isOrgWide = meeting.isOrgWide)
                Spacer(Modifier.width(8.dp))
                Text(
                    "● Live · started ${formatTimeAgo(meeting.startedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(meeting.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                "Hosted by ${meeting.hostName}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                when {
                    row.attended -> {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "You're checked in",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    meeting.checkInOpen -> Button(onClick = onCheckIn, shape = RoundedCornerShape(10.dp)) {
                        Text("Check in")
                    }
                    else -> Text(
                        "Check-in is closed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.weight(1f))
                if (row.canManage) {
                    FilledTonalButton(onClick = onOpenHostView, shape = RoundedCornerShape(10.dp)) {
                        Text("Host view")
                    }
                }
            }
        }
    }
}

@Composable
private fun PastMeetingRow(row: MeetingRow, onClick: (() -> Unit)?) {
    val meeting = row.meeting
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (row.attended) Icons.Filled.CheckCircle else Icons.Outlined.Cancel,
                contentDescription = if (row.attended) "Attended" else "Missed",
                tint = if (row.attended) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    meeting.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${formatDateTime(meeting.startedAt)} · ${meeting.committee?.shortName ?: "All members"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                if (row.attended) "Present" else "Missed",
                style = MaterialTheme.typography.labelMedium,
                color = if (row.attended) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartMeetingDialog(
    isCoordinator: Boolean,
    ownCommittee: Committee,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onStart: (String, Committee?) -> Unit
) {
    var title by remember {
        mutableStateOf(if (isCoordinator) "General meeting" else "${ownCommittee.shortName} meeting")
    }
    var committee by remember { mutableStateOf<Committee?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        title = { Text("Start a meeting") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (isCoordinator) {
                    ExposedDropdownMenuBox(
                        expanded = menuExpanded,
                        onExpandedChange = { menuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = committee?.displayName ?: "All members (org-wide)",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Who should attend") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("All members (org-wide)") },
                                onClick = { committee = null; menuExpanded = false }
                            )
                            Committee.entries.filter { it != Committee.NONE }.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c.displayName) },
                                    onClick = { committee = c; menuExpanded = false }
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        "For ${ownCommittee.displayName}. A 4-digit check-in code is shown once it starts.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onStart(title, committee) },
                enabled = title.isNotBlank() && !isBusy
            ) { Text(if (isBusy) "Starting…" else "Start") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isBusy) { Text("Cancel") }
        }
    )
}

@Composable
private fun CheckInDialog(
    meeting: Meeting,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, (CheckInResult) -> Unit) -> Unit,
    onCheckedIn: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val currentError = error

    AlertDialog(
        onDismissRequest = { if (!isBusy) onDismiss() },
        title = { Text("Check in") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Enter the 4-digit code shown on ${meeting.hostName}'s screen.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { input ->
                        code = input.filter { it.isDigit() }.take(4)
                        error = null
                    },
                    singleLine = true,
                    isError = currentError != null,
                    supportingText = if (currentError != null) {
                        { Text(currentError) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    textStyle = MaterialTheme.typography.headlineSmall.copy(
                        textAlign = TextAlign.Center,
                        letterSpacing = 8.sp
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSubmit(code) { result ->
                        when (result) {
                            CheckInResult.Success, CheckInResult.AlreadyCheckedIn -> onCheckedIn()
                            CheckInResult.WrongCode -> error = "That code doesn't match. Check with the host."
                            CheckInResult.Closed -> error = "Check-in for this meeting is closed."
                            is CheckInResult.Failed -> error = "Couldn't check in — check your connection."
                        }
                    }
                },
                enabled = code.length == 4 && !isBusy
            ) {
                if (isBusy) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Check in")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isBusy) { Text("Cancel") }
        }
    )
}
