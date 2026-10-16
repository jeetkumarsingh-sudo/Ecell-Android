package dcrustm.ecell.mobile.ui.resources

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dcrustm.ecell.mobile.domain.model.Committee
import dcrustm.ecell.mobile.domain.model.ROLE
import dcrustm.ecell.mobile.domain.model.ResourceType
import dcrustm.ecell.mobile.domain.model.SharedResource
import dcrustm.ecell.mobile.ui.components.EmptyState
import dcrustm.ecell.mobile.ui.components.SubScreenTopBar
import dcrustm.ecell.mobile.util.formatTimeAgo

fun ResourceType.icon(): ImageVector = when (this) {
    ResourceType.DOC -> Icons.Outlined.Description
    ResourceType.SLIDES -> Icons.Outlined.Slideshow
    ResourceType.SHEET -> Icons.Outlined.TableChart
    ResourceType.PDF -> Icons.Outlined.PictureAsPdf
    ResourceType.VIDEO -> Icons.Outlined.VideoLibrary
    ResourceType.DESIGN -> Icons.Outlined.Palette
    ResourceType.LINK -> Icons.Outlined.Link
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourcesScreen(
    onBack: () -> Unit,
    onAddResource: () -> Unit,
    viewModel: ResourcesViewModel = hiltViewModel()
) {
    val resources by viewModel.resources.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val uriHandler = LocalUriHandler.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    val committeeChoices: List<Committee> = when {
        currentUser?.role == ROLE.COORDINATOR -> Committee.entries.filter { it != Committee.NONE }
        currentUser?.committee != null && currentUser?.committee != Committee.NONE -> listOfNotNull(currentUser?.committee)
        else -> emptyList()
    }

    Scaffold(
        topBar = { SubScreenTopBar(title = "Resources", subtitle = "Docs, decks and links the team shares", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Share") },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                onClick = onAddResource,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            OutlinedTextField(
                value = filter.query,
                onValueChange = { viewModel.setFilter(filter.copy(query = it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search resources…") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                item {
                    FilterChip(
                        selected = filter.audience == ResourceAudience.Everything && filter.type == null,
                        onClick = { viewModel.setFilter(ResourceFilter(query = filter.query)) },
                        label = { Text("All") }
                    )
                }
                item {
                    FilterChip(
                        selected = filter.audience == ResourceAudience.OrgWide,
                        onClick = {
                            viewModel.setFilter(
                                filter.copy(
                                    audience = if (filter.audience == ResourceAudience.OrgWide) ResourceAudience.Everything
                                    else ResourceAudience.OrgWide
                                )
                            )
                        },
                        label = { Text("Org-wide") }
                    )
                }
                items(committeeChoices) { committee ->
                    val audience = ResourceAudience.OneCommittee(committee)
                    FilterChip(
                        selected = filter.audience == audience,
                        onClick = {
                            viewModel.setFilter(
                                filter.copy(
                                    audience = if (filter.audience == audience) ResourceAudience.Everything else audience
                                )
                            )
                        },
                        label = { Text(committee.shortName) }
                    )
                }
                items(ResourceType.entries) { type ->
                    FilterChip(
                        selected = filter.type == type,
                        onClick = { viewModel.setFilter(filter.copy(type = if (filter.type == type) null else type)) },
                        label = { Text(type.label) },
                        leadingIcon = { Icon(type.icon(), contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }

            if (resources.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.FolderOpen,
                    title = if (filter == ResourceFilter()) "Nothing shared yet" else "No matches",
                    message = if (filter == ResourceFilter()) "Tap Share to add the first doc, deck or link for the team."
                    else "Try a different search or filter."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(resources, key = { it.id }) { resource ->
                        ResourceRow(
                            resource = resource,
                            canDelete = viewModel.canDelete(resource),
                            onOpen = {
                                runCatching { uriHandler.openUri(resource.url) }
                                    .onFailure { viewModel.reportCannotOpen() }
                            },
                            onDelete = { viewModel.delete(resource) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResourceRow(
    resource: SharedResource,
    canDelete: Boolean,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onOpen)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    resource.type.icon(),
                    contentDescription = resource.type.label,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    resource.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    resource.host,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (resource.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        resource.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "${resource.committee?.shortName ?: "Everyone"} · ${resource.addedByName} · ${formatTimeAgo(resource.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (canDelete) {
                Box {
                    IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More", modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Remove") },
                            onClick = { menuExpanded = false; confirmDelete = true }
                        )
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Remove resource?") },
            text = { Text("\"${resource.title}\" will be removed from the library for everyone. The link itself isn't affected.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }
}
