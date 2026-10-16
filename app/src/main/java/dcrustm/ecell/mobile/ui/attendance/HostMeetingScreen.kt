package dcrustm.ecell.mobile.ui.attendance

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import dcrustm.ecell.mobile.domain.model.AttendanceRecord
import dcrustm.ecell.mobile.domain.model.Meeting
import dcrustm.ecell.mobile.domain.model.Member
import dcrustm.ecell.mobile.ui.components.SectionLabel
import dcrustm.ecell.mobile.ui.components.SubScreenTopBar
import dcrustm.ecell.mobile.ui.home.InitialsAvatar
import dcrustm.ecell.mobile.util.formatClockTime
import dcrustm.ecell.mobile.util.formatDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostMeetingScreen(
    onBack: () -> Unit,
    viewModel: HostMeetingViewModel = hiltViewModel()
) {
    val meeting by viewModel.meeting.collectAsState()
    val code by viewModel.code.collectAsState()
    val attendees by viewModel.attendees.collectAsState()
    val absentees by viewModel.absentees.collectAsState()
    val canManage by viewModel.canManage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showMarkPresent by remember { mutableStateOf(false) }
    var confirmEnd by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    val m = meeting
    Scaffold(
        topBar = {
            SubScreenTopBar(
                title = m?.title ?: "Meeting",
                subtitle = m?.let { (it.committee?.displayName ?: "All members") + if (it.isLive) " · Live" else " · Ended" },
                onBack = onBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (m == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (m.isLive && canManage) {
                item {
                    CheckInCodeCard(
                        meeting = m,
                        code = code,
                        onNewCode = viewModel::regenerateCode,
                        onCheckInOpenChange = viewModel::setCheckInOpen
                    )
                }
            } else if (!m.isLive) {
                item {
                    Text(
                        "Ended ${formatDateTime(m.endedAt)} · hosted by ${m.hostName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(label = "Present", value = attendees.size.toString(), modifier = Modifier.weight(1f))
                    StatTile(
                        label = if (m.isLive) "Not here yet" else "Missed",
                        value = absentees.size.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (canManage) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { showMarkPresent = true },
                            enabled = absentees.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Mark present")
                        }
                        if (m.isLive) {
                            Button(
                                onClick = { confirmEnd = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                )
                            ) { Text("End meeting") }
                        }
                    }
                }
            }

            item { SectionLabel("Checked in (${attendees.size})", modifier = Modifier.padding(top = 6.dp)) }
            items(attendees, key = { it.uid }) { record ->
                AttendeeRow(
                    record = record,
                    canRemove = canManage && record.uid != m.hostUid,
                    onRemove = { viewModel.remove(record) }
                )
            }
        }
    }

    if (showMarkPresent) {
        MarkPresentDialog(
            absentees = absentees,
            onMark = viewModel::markPresent,
            onDismiss = { showMarkPresent = false }
        )
    }

    if (confirmEnd) {
        AlertDialog(
            onDismissRequest = { confirmEnd = false },
            title = { Text("End meeting?") },
            text = { Text("Check-in closes and the meeting moves to everyone's history. You can still mark people present afterwards.") },
            confirmButton = {
                TextButton(onClick = { confirmEnd = false; viewModel.endMeeting() }) { Text("End meeting") }
            },
            dismissButton = {
                TextButton(onClick = { confirmEnd = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CheckInCodeCard(
    meeting: Meeting,
    code: String?,
    onNewCode: () -> Unit,
    onCheckInOpenChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Check-in code",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (meeting.checkInOpen) code ?: "····" else "Closed",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = if (meeting.checkInOpen) 10.sp else 0.sp
                ),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Members open Hub → Attendance and enter this code",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onNewCode, enabled = meeting.checkInOpen) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("New code")
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "Check-in open",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.width(8.dp))
                Switch(checked = meeting.checkInOpen, onCheckedChange = onCheckInOpenChange)
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AttendeeRow(record: AttendanceRecord, canRemove: Boolean, onRemove: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InitialsAvatar(
                initials = record.name.split(" ").take(2)
                    .joinToString("") { it.firstOrNull()?.toString() ?: "" }.uppercase(),
                size = 34
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    record.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${record.committee.shortName} · ${formatClockTime(record.checkedInAt)}" +
                        if (record.markedByHost) " · added by host" else " · code",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (canRemove) {
                IconButton(onClick = onRemove) {
                    Icon(Icons.Outlined.Close, contentDescription = "Remove ${record.name}")
                }
            }
        }
    }
}

@Composable
private fun MarkPresentDialog(
    absentees: List<Member>,
    onMark: (Member) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mark present") },
        text = {
            if (absentees.isEmpty()) {
                Text("Everyone expected is already checked in.")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 380.dp)) {
                    items(absentees, key = { it.uid }) { member ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onMark(member) }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            InitialsAvatar(initials = member.initials, size = 30)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(member.name, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    member.committee.shortName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.Filled.PersonAdd,
                                contentDescription = "Mark ${member.name} present",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}
