package dcrustm.ecell.mobile.ui.polls

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dcrustm.ecell.mobile.domain.model.Committee
import dcrustm.ecell.mobile.domain.model.Poll
import dcrustm.ecell.mobile.domain.model.ROLE
import dcrustm.ecell.mobile.ui.components.SubScreenTopBar

private val durationChoices: List<Pair<String, Long>> = listOf(
    "No deadline" to 0L,
    "1 hour" to 60L * 60 * 1000,
    "1 day" to 24L * 60 * 60 * 1000,
    "3 days" to 3L * 24 * 60 * 60 * 1000,
    "1 week" to 7L * 24 * 60 * 60 * 1000
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePollScreen(
    onBack: () -> Unit,
    viewModel: CreatePollViewModel = hiltViewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isPosting by viewModel.isPosting.collectAsState()
    val error by viewModel.error.collectAsState()
    val isCoordinator = currentUser?.role == ROLE.COORDINATOR

    var question by remember { mutableStateOf("") }
    val options = remember { mutableStateListOf("", "") }
    var selectedCommittee by remember { mutableStateOf<Committee?>(null) }
    var committeeMenuExpanded by remember { mutableStateOf(false) }
    var durationMillis by remember { mutableLongStateOf(24L * 60 * 60 * 1000) }

    val filledOptions = options.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    val canPost = question.isNotBlank() && filledOptions.size >= Poll.MIN_OPTIONS && !isPosting

    val fieldColors = OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
    )

    Scaffold(
        topBar = { SubScreenTopBar(title = "New poll", onBack = onBack) }
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
                value = question,
                onValueChange = { question = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Question") },
                placeholder = { Text("e.g. Which date works for the hackathon?") },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors
            )

            Text(
                "Options",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            options.forEachIndexed { index, value ->
                OutlinedTextField(
                    value = value,
                    onValueChange = { options[index] = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Option ${index + 1}") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    trailingIcon = if (options.size > Poll.MIN_OPTIONS) {
                        {
                            IconButton(onClick = { options.removeAt(index) }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Remove option")
                            }
                        }
                    } else null,
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors
                )
            }

            if (options.size < Poll.MAX_OPTIONS) {
                TextButton(onClick = { options.add("") }) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Add option")
                }
            }

            if (isCoordinator) {
                ExposedDropdownMenuBox(
                    expanded = committeeMenuExpanded,
                    onExpandedChange = { committeeMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCommittee?.displayName ?: "All members (org-wide)",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Who can vote") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = committeeMenuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors
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
            } else {
                Text(
                    "Only ${currentUser?.committee?.displayName ?: "your committee"} members can see and vote on this poll.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                "Voting closes",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                durationChoices.forEach { (label, millis) ->
                    FilterChip(
                        selected = durationMillis == millis,
                        onClick = { durationMillis = millis },
                        label = { Text(label) }
                    )
                }
            }

            error?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    viewModel.create(question, options.toList(), selectedCommittee, durationMillis, onDone = onBack)
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
                    Text("Create poll")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
