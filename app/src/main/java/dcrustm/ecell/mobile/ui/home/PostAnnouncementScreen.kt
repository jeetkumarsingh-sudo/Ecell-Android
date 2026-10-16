package dcrustm.ecell.mobile.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dcrustm.ecell.mobile.domain.model.Committee

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostAnnouncementScreen(
    onBack: () -> Unit,
    viewModel: PostAnnouncementViewModel = hiltViewModel()
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var isPinned by remember { mutableStateOf(false) }
    var selectedCommittee by remember { mutableStateOf<Committee?>(null) }
    var committeeMenuExpanded by remember { mutableStateOf(false) }
    val isPosting by viewModel.isPosting.collectAsState()

    val canPost = title.isNotBlank() && body.length >= 10 && !isPosting

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New announcement", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Title") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                label = { Text("Message") },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )

            ExposedDropdownMenuBox(
                expanded = committeeMenuExpanded,
                onExpandedChange = { committeeMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedCommittee?.displayName ?: "All members (org-wide)",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Audience") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = committeeMenuExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )
                ExposedDropdownMenu(
                    expanded = committeeMenuExpanded,
                    onDismissRequest = { committeeMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("All members (org-wide)") },
                        onClick = { selectedCommittee = null; committeeMenuExpanded = false },
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    )
                    Committee.entries.filter { it != Committee.NONE }.forEach { c ->
                        DropdownMenuItem(
                            text = { Text(c.displayName, style = MaterialTheme.typography.bodyMedium) },
                            onClick = { selectedCommittee = c; committeeMenuExpanded = false },
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Pin to top", style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium)
                    Text("Stays at the top of the feed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = isPinned, onCheckedChange = { isPinned = it })
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = {
                    viewModel.post(title, body, selectedCommittee, isPinned) { onBack() }
                },
                enabled = canPost,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isPosting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text("Post announcement")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
