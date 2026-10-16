package dcrustm.ecell.mobile.ui.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.HowToVote
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dcrustm.ecell.mobile.domain.model.Committee
import dcrustm.ecell.mobile.domain.model.Member
import dcrustm.ecell.mobile.navigation.HubRoutes
import dcrustm.ecell.mobile.ui.components.SectionLabel

private data class HubFeature(
    val route: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)

private val hubFeatures = listOf(
    HubFeature(HubRoutes.POLLS, "Polls", "Vote on team decisions", Icons.Outlined.HowToVote),
    HubFeature(HubRoutes.TASKS, "Tasks", "Your committee's to-do board", Icons.Outlined.TaskAlt),
    HubFeature(HubRoutes.ATTENDANCE, "Attendance", "Check in to meetings", Icons.Outlined.EventAvailable),
    HubFeature(HubRoutes.RESOURCES, "Resources", "Docs, decks and links", Icons.Outlined.FolderOpen),
    HubFeature(HubRoutes.LEADERBOARD, "Leaderboard", "Points, ranks and badges", Icons.Outlined.EmojiEvents)
)

private data class HubAction(val label: String, val route: String, val icon: ImageVector)

private fun HubSummary.badgeFor(route: String): Int = when (route) {
    HubRoutes.POLLS -> pollsAwaitingVote
    HubRoutes.TASKS -> myOpenTasks
    HubRoutes.ATTENDANCE -> liveMeetingsToJoin.size
    else -> 0
}

private fun HubSummary.actions(): List<HubAction> = buildList {
    liveMeetingsToJoin.forEach { title ->
        add(HubAction("$title is live — check in", HubRoutes.ATTENDANCE, Icons.Outlined.EventAvailable))
    }
    if (overdueTasks > 0) {
        add(
            HubAction(
                "$overdueTasks overdue task${if (overdueTasks != 1) "s" else ""}",
                HubRoutes.TASKS,
                Icons.Outlined.TaskAlt
            )
        )
    } else if (myOpenTasks > 0) {
        add(
            HubAction(
                "$myOpenTasks open task${if (myOpenTasks != 1) "s" else ""} assigned to you",
                HubRoutes.TASKS,
                Icons.Outlined.TaskAlt
            )
        )
    }
    if (pollsAwaitingVote > 0) {
        add(
            HubAction(
                "$pollsAwaitingVote poll${if (pollsAwaitingVote != 1) "s" else ""} waiting for your vote",
                HubRoutes.POLLS,
                Icons.Outlined.HowToVote
            )
        )
    }
}

@Composable
fun HubScreen(
    onOpen: (String) -> Unit,
    viewModel: HubViewModel = hiltViewModel()
) {
    val user by viewModel.currentUser.collectAsState()
    val summary by viewModel.summary.collectAsState()

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            HubHeader(user = user, actions = summary.actions(), onOpen = onOpen)
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionLabel("Tools", modifier = Modifier.padding(top = 4.dp))
        }
        items(hubFeatures, key = { it.route }) { feature ->
            FeatureTile(
                feature = feature,
                badge = summary.badgeFor(feature.route),
                onClick = { onOpen(feature.route) }
            )
        }
    }
}

@Composable
private fun HubHeader(user: Member?, actions: List<HubAction>, onOpen: (String) -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            val firstName = user?.name?.trim()?.split(" ")?.firstOrNull()?.takeIf { it.isNotEmpty() }
            Text(
                text = if (firstName != null) "Hi, $firstName" else "Hi there",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (user != null) {
                val committee = user.committee.takeIf { it != Committee.NONE }?.displayName
                Text(
                    text = listOfNotNull(user.roleLabel, committee).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                )
            }

            Spacer(Modifier.height(14.dp))

            if (actions.isEmpty()) {
                Text(
                    "You're all caught up.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    actions.forEach { action ->
                        HubActionRow(action = action, onClick = { onOpen(action.route) })
                    }
                }
            }
        }
    }
}

@Composable
private fun HubActionRow(action: HubAction, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                action.icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(10.dp))
            Text(
                action.label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FeatureTile(feature: HubFeature, badge: Int, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Box {
            Column(modifier = Modifier.padding(14.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        feature.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    feature.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    feature.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (badge > 0) {
                Badge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    Text(if (badge > 99) "99+" else badge.toString())
                }
            }
        }
    }
}
