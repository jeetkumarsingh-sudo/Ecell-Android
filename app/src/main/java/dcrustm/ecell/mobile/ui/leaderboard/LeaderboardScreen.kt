package dcrustm.ecell.mobile.ui.leaderboard

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.HowToVote
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dcrustm.ecell.mobile.domain.model.Achievement
import dcrustm.ecell.mobile.domain.model.AchievementMetric
import dcrustm.ecell.mobile.domain.model.Committee
import dcrustm.ecell.mobile.domain.model.LeaderboardEntry
import dcrustm.ecell.mobile.domain.model.MemberStats
import dcrustm.ecell.mobile.ui.components.MemberAvatar
import dcrustm.ecell.mobile.ui.components.SubScreenTopBar

private val Gold = Color(0xFFE6A700)
private val Silver = Color(0xFF9AA4AE)
private val Bronze = Color(0xFFB87333)

private fun medalColor(rank: Int): Color? = when (rank) {
    1 -> Gold
    2 -> Silver
    3 -> Bronze
    else -> null
}

private fun AchievementMetric.icon(): ImageVector = when (this) {
    AchievementMetric.MEETINGS -> Icons.Outlined.EventAvailable
    AchievementMetric.TASKS -> Icons.Outlined.TaskAlt
    AchievementMetric.VOTES -> Icons.Outlined.HowToVote
    AchievementMetric.POINTS -> Icons.Outlined.EmojiEvents
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    onBack: () -> Unit,
    onMemberClick: (String) -> Unit,
    viewModel: LeaderboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            SubScreenTopBar(
                title = "Leaderboard",
                subtitle = "Show up, ship tasks, have your say",
                onBack = onBack,
                actions = {
                    IconButton(onClick = viewModel::refresh, enabled = !state.isLoading) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Rankings") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("My badges") })
            }

            when {
                state.isLoading && state.allStats.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.error != null && state.allStats.isEmpty() -> {
                    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                state.error ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            TextButton(onClick = viewModel::refresh) { Text("Try again") }
                        }
                    }
                }
                selectedTab == 0 -> RankingsTab(
                    state = state,
                    onScopeChange = viewModel::setScope,
                    onMemberClick = onMemberClick
                )
                else -> BadgesTab(stats = state.myStats, me = state.me)
            }
        }
    }
}

@Composable
private fun RankingsTab(
    state: LeaderboardUiState,
    onScopeChange: (LeaderboardScope) -> Unit,
    onMemberClick: (String) -> Unit
) {
    val entries = state.entries
    val me = entries.firstOrNull { it.member.uid == state.myUid }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (state.myCommittee != Committee.NONE) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.scope == LeaderboardScope.EVERYONE,
                        onClick = { onScopeChange(LeaderboardScope.EVERYONE) },
                        label = { Text("Everyone") }
                    )
                    FilterChip(
                        selected = state.scope == LeaderboardScope.MY_COMMITTEE,
                        onClick = { onScopeChange(LeaderboardScope.MY_COMMITTEE) },
                        label = { Text(state.myCommittee.shortName) }
                    )
                }
            }
        }

        if (me != null) {
            item { MyRankCard(me = me, total = entries.size) }
        }

        items(entries, key = { it.member.uid }) { entry ->
            RankRow(
                entry = entry,
                isMe = entry.member.uid == state.myUid,
                onClick = { onMemberClick(entry.member.uid) }
            )
        }

        item {
            Text(
                "Meeting attended = ${MemberStats.POINTS_PER_MEETING} pts · " +
                    "Task done = ${MemberStats.POINTS_PER_TASK} pts · " +
                    "Poll vote = ${MemberStats.POINTS_PER_VOTE} pts",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun MyRankCard(me: LeaderboardEntry, total: Int) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Your rank",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    "#${me.rank} of $total",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "${me.stats.meetingsAttended} meetings · ${me.stats.tasksCompleted} tasks · ${me.stats.pollsVoted} votes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${me.stats.points}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "points",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RankRow(entry: LeaderboardEntry, isMe: Boolean, onClick: () -> Unit) {
    val medal = medalColor(entry.rank)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isMe) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(medal ?: Color.Transparent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${entry.rank}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (medal != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(10.dp))
            MemberAvatar(member = entry.member, size = 36)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (isMe) "${entry.member.name} (you)" else entry.member.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    entry.member.committee.shortName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "${entry.stats.points}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                " pts",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BadgesTab(stats: MemberStats, me: LeaderboardEntry?) {
    val unlocked = Achievement.entries.count { it.isUnlocked(stats) }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBox("Points", "${stats.points}", Modifier.weight(1f))
                StatBox("Meetings", "${stats.meetingsAttended}", Modifier.weight(1f))
                StatBox("Tasks", "${stats.tasksCompleted}", Modifier.weight(1f))
                StatBox("Votes", "${stats.pollsVoted}", Modifier.weight(1f))
            }
        }
        item {
            Text(
                "$unlocked of ${Achievement.entries.size} badges unlocked" +
                    (me?.let { " · rank #${it.rank}" } ?: ""),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
            )
        }
        items(Achievement.entries, key = { it.name }) { achievement ->
            AchievementRow(achievement = achievement, stats = stats)
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AchievementRow(achievement: Achievement, stats: MemberStats) {
    val unlocked = achievement.isUnlocked(stats)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (unlocked) MaterialTheme.colorScheme.tertiaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (unlocked) achievement.metric.icon() else Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = if (unlocked) MaterialTheme.colorScheme.onTertiaryContainer
                    else MaterialTheme.colorScheme.outline
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    achievement.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (unlocked) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    achievement.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!unlocked) {
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { achievement.progress(stats) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(
                if (unlocked) "Unlocked"
                else "${achievement.currentValue(stats).coerceAtMost(achievement.threshold)}/${achievement.threshold}",
                style = MaterialTheme.typography.labelMedium,
                color = if (unlocked) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
