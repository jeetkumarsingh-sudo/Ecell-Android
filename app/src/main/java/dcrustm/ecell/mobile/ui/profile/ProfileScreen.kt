package dcrustm.ecell.mobile.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dcrustm.ecell.mobile.domain.model.ProfileUpdate
import dcrustm.ecell.mobile.ui.components.MemberAvatar
import dcrustm.ecell.mobile.ui.directory.RolePill

private val yearChoices = listOf(1 to "1st", 2 to "2nd", 3 to "3rd", 4 to "4th", 5 to "5th")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    onDismiss: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val member by viewModel.currentUser.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // The form starts from the saved profile once it has loaded, then holds the member's edits.
    var draft by remember { mutableStateOf<ProfileUpdate?>(null) }
    var skillInput by remember { mutableStateOf("") }

    LaunchedEffect(member?.uid) {
        val m = member
        if (draft == null && m != null) draft = ProfileUpdate.from(m)
    }
    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    val saved = member?.let { ProfileUpdate.from(it) }
    val hasChanges = draft != null && saved != null && draft?.cleaned() != saved

    val fieldColors = OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
    )

    fun addSkills(raw: String) {
        val current = draft ?: return
        val added = raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        if (added.isNotEmpty()) {
            draft = current.copy(
                skills = (current.skills + added).distinctBy { it.lowercase() }.take(ProfileUpdate.MAX_SKILLS)
            )
        }
        skillInput = ""
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                // Scaffold's padding already covers the status and navigation bars.
                .padding(scaffoldPadding)
                .padding(16.dp)
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close Profile")
            }

            val m = member
            val d = draft
            if (m == null || d == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 56.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    MemberAvatar(member = m, size = 88)
                    Spacer(Modifier.height(12.dp))
                    Text(m.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    RolePill(m.role)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${m.committee.displayName} · ${m.yearLabel}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(24.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Email", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(2.dp))
                            Text(m.email.ifBlank { "—" }, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    FormLabel("About you")

                    OutlinedTextField(
                        value = d.branch,
                        onValueChange = { draft = d.copy(branch = it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Branch") },
                        placeholder = { Text("e.g. CSE, ECE, MBA") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors
                    )

                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Year",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        yearChoices.forEachIndexed { index, (year, label) ->
                            SegmentedButton(
                                selected = d.year == year,
                                onClick = { draft = d.copy(year = year) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = yearChoices.size),
                                icon = {}
                            ) { Text(label) }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = d.phone,
                        onValueChange = { draft = d.copy(phone = it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Phone") },
                        placeholder = { Text("Visible to your committee head and coordinator") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = d.bio,
                        onValueChange = { draft = d.copy(bio = it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        label = { Text("Bio") },
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors
                    )

                    Spacer(Modifier.height(20.dp))
                    FormLabel("Skills")
                    Text(
                        "Help teammates find you — e.g. Figma, Video editing, Public speaking.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))

                    if (d.skills.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            d.skills.forEach { skill ->
                                InputChip(
                                    selected = false,
                                    onClick = { draft = d.copy(skills = d.skills - skill) },
                                    label = { Text(skill) },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Filled.Close,
                                            contentDescription = "Remove $skill",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }

                    if (d.skills.size < ProfileUpdate.MAX_SKILLS) {
                        OutlinedTextField(
                            value = skillInput,
                            onValueChange = { value ->
                                if (',' in value) addSkills(value) else skillInput = value
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Add a skill") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { addSkills(skillInput) }),
                            trailingIcon = {
                                IconButton(
                                    onClick = { addSkills(skillInput) },
                                    enabled = skillInput.isNotBlank()
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "Add skill")
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = fieldColors
                        )
                    }

                    Spacer(Modifier.height(20.dp))
                    FormLabel("Links")

                    OutlinedTextField(
                        value = d.linkedinUrl,
                        onValueChange = { draft = d.copy(linkedinUrl = it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("LinkedIn") },
                        leadingIcon = { Icon(Icons.Outlined.Work, contentDescription = null) },
                        placeholder = { Text("linkedin.com/in/…") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = d.githubUrl,
                        onValueChange = { draft = d.copy(githubUrl = it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("GitHub") },
                        leadingIcon = { Icon(Icons.Outlined.Code, contentDescription = null) },
                        placeholder = { Text("github.com/…") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = d.portfolioUrl,
                        onValueChange = { draft = d.copy(portfolioUrl = it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Portfolio / website") },
                        leadingIcon = { Icon(Icons.Outlined.Language, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors
                    )

                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick = { viewModel.updateProfile(d) },
                        enabled = hasChanges && !isSaving,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isSaving) "Saving…" else "Save changes")
                    }

                    Spacer(Modifier.height(32.dp))

                    OutlinedButton(
                        onClick = { viewModel.signOut(); onDismiss() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Sign out")
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun FormLabel(text: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}
