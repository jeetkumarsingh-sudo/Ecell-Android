package dcrustm.ecell.mobile.ui.polls

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.HowToVote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dcrustm.ecell.mobile.domain.model.PollWithResults
import dcrustm.ecell.mobile.domain.model.ROLE
import dcrustm.ecell.mobile.ui.components.EmptyState
import dcrustm.ecell.mobile.ui.components.SectionLabel
import dcrustm.ecell.mobile.ui.components.SubScreenTopBar
import dcrustm.ecell.mobile.ui.home.CommitteeBadge
import dcrustm.ecell.mobile.util.formatTimeAgo
import dcrustm.ecell.mobile.util.formatTimeLeft
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PollsScreen(
    onBack: () -> Unit,
    onCreatePoll: () -> Unit,
    viewModel: PollsViewModel = hiltViewModel()
) {
    val polls by viewModel.polls.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val canCreate = currentUser?.role == ROLE.COORDINATOR || currentUser?.role == ROLE.COMMITTEE_HEAD
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = { SubScreenTopBar(title = "Polls", subtitle = "Vote and see live results", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (canCreate) {
                ExtendedFloatingActionButton(
                    text = { Text("New poll") },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    onClick = onCreatePoll,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { innerPadding ->
        if (polls.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.HowToVote,
                title = "No polls yet",
                message = if (canCreate) "Tap New poll to ask the team something."
                else "Polls from the coordinator and your committee head show up here.",
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            val open = polls.filter { it.poll.isOpen() }
            val closed = polls.filter { !it.poll.isOpen() }
            LazyColumn(
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 88.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (open.isNotEmpty()) {
                    item { SectionLabel("Open") }
                    items(open, key = { it.poll.id }) { item ->
                        PollCard(
                            item = item,
                            canManage = viewModel.canManage(item.poll),
                            onVote = { index -> viewModel.vote(item.poll, index) },
                            onClose = { viewModel.close(item.poll) },
                            onDelete = { viewModel.delete(item.poll) }
                        )
                    }
                }
                if (closed.isNotEmpty()) {
                    item { SectionLabel("Closed", modifier = Modifier.padding(top = 8.dp)) }
                    items(closed, key = { it.poll.id }) { item ->
                        PollCard(
                            item = item,
                            canManage = viewModel.canManage(item.poll),
                            onVote = {},
                            onClose = {},
                            onDelete = { viewModel.delete(item.poll) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PollCard(
    item: PollWithResults,
    canManage: Boolean,
    onVote: (Int) -> Unit,
    onClose: () -> Unit,
    onDelete: () -> Unit
) {
    val poll = item.poll
    val isOpen = poll.isOpen()
    val showResults = item.myVote != null || !isOpen
    var menuExpanded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val statusLabel = when {
        !isOpen -> "Closed"
        poll.closesAt == 0L -> "Open · posted ${formatTimeAgo(poll.createdAt)}"
        else -> "Open · ${formatTimeLeft(poll.closesAt)}"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CommitteeBadge(
                    label = poll.committee?.shortName ?: "All",
                    isOrgWide = poll.isOrgWide
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isOpen) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                if (canManage) {
                    Box {
                        IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                Icons.Filled.MoreVert,
                                contentDescription = "Poll options",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            if (isOpen) {
                                DropdownMenuItem(
                                    text = { Text("Close poll") },
                                    onClick = { menuExpanded = false; onClose() }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                onClick = { menuExpanded = false; confirmDelete = true }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = poll.question,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(12.dp))

            poll.options.forEachIndexed { index, option ->
                PollOptionRow(
                    text = option,
                    selected = item.myVote == index,
                    showResults = showResults,
                    fraction = item.fractionFor(index),
                    isWinner = !isOpen && item.leadingOption == index,
                    enabled = isOpen,
                    onClick = { if (item.myVote != index) onVote(index) }
                )
                if (index != poll.options.lastIndex) Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(12.dp))

            val hint = when {
                !isOpen -> ""
                item.myVote == null -> " · tap an option to vote"
                else -> " · tap another option to change"
            }
            Text(
                text = "${item.totalVotes} vote${if (item.totalVotes != 1) "s" else ""} · by ${poll.createdByName}$hint",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete poll?") },
            text = { Text("The poll and all of its votes will be removed for everyone.") },
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
private fun PollOptionRow(
    text: String,
    selected: Boolean,
    showResults: Boolean,
    fraction: Float,
    isWinner: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    val animatedFraction by animateFloatAsState(
        targetValue = if (showResults) fraction else 0f,
        label = "pollOptionFraction"
    )
    val fillColor = if (selected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceContainerHighest

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                shape = shape
            )
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Box(modifier = Modifier.matchParentSize()) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedFraction)
                    .background(fillColor)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "Your vote",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isWinner || selected) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            if (showResults) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${(fraction * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
