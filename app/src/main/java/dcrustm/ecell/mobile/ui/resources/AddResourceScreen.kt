package dcrustm.ecell.mobile.ui.resources

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dcrustm.ecell.mobile.domain.model.Committee
import dcrustm.ecell.mobile.domain.model.ROLE
import dcrustm.ecell.mobile.domain.model.ResourceType
import dcrustm.ecell.mobile.ui.components.SubScreenTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddResourceScreen(
    onBack: () -> Unit,
    viewModel: AddResourceViewModel = hiltViewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val error by viewModel.error.collectAsState()
    val isCoordinator = currentUser?.role == ROLE.COORDINATOR
    val ownCommittee = currentUser?.committee?.takeIf { it != Committee.NONE }

    var url by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(ResourceType.LINK) }
    var typePickedByHand by remember { mutableStateOf(false) }
    var committee by remember { mutableStateOf<Committee?>(null) }
    var audienceMenuExpanded by remember { mutableStateOf(false) }

    val urlValid = ResourceType.looksLikeUrl(url)
    val canSave = urlValid && title.isNotBlank() && !isSaving
    val fieldColors = OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
    )

    // Who can see it: org-wide or one committee. Non-coordinators only get their own committee.
    val audienceChoices: List<Committee?> = buildList<Committee?> {
        add(null)
        if (isCoordinator) addAll(Committee.entries.filter { it != Committee.NONE })
        else if (ownCommittee != null) add(ownCommittee)
    }

    Scaffold(
        topBar = { SubScreenTopBar(title = "Share a resource", onBack = onBack) }
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
                value = url,
                onValueChange = { input ->
                    url = input
                    if (!typePickedByHand) type = ResourceType.detect(input)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Link") },
                placeholder = { Text("https://docs.google.com/…") },
                singleLine = true,
                isError = url.isNotBlank() && !urlValid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Title") },
                placeholder = { Text("e.g. Sponsorship deck 2026") },
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
                    .height(110.dp),
                label = { Text("What is it? (optional)") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors
            )

            Text(
                "Type",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ResourceType.entries.forEach { t ->
                    FilterChip(
                        selected = type == t,
                        onClick = { type = t; typePickedByHand = true },
                        label = { Text(t.label) },
                        leadingIcon = { Icon(t.icon(), contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }

            ExposedDropdownMenuBox(
                expanded = audienceMenuExpanded,
                onExpandedChange = { audienceMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = committee?.displayName ?: "Everyone",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Share with") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = audienceMenuExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors
                )
                ExposedDropdownMenu(
                    expanded = audienceMenuExpanded,
                    onDismissRequest = { audienceMenuExpanded = false }
                ) {
                    audienceChoices.forEach { choice ->
                        DropdownMenuItem(
                            text = { Text(choice?.displayName ?: "Everyone", style = MaterialTheme.typography.bodyMedium) },
                            onClick = { committee = choice; audienceMenuExpanded = false },
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        )
                    }
                }
            }

            error?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = { viewModel.save(url, title, description, type, committee, onDone = onBack) },
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
                    Text("Share")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
