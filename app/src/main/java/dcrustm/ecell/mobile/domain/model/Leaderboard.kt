package dcrustm.ecell.mobile.domain.model

data class MemberStats(
    val meetingsAttended: Int = 0,
    val tasksCompleted: Int = 0,
    val pollsVoted: Int = 0
) {
    val points: Int
        get() = meetingsAttended * POINTS_PER_MEETING +
            tasksCompleted * POINTS_PER_TASK +
            pollsVoted * POINTS_PER_VOTE

    companion object {
        const val POINTS_PER_MEETING = 10
        const val POINTS_PER_TASK = 20
        const val POINTS_PER_VOTE = 2
    }
}

data class LeaderboardEntry(
    val member: Member,
    val stats: MemberStats,
    /** Members with equal points share a rank (1, 2, 2, 4…). */
    val rank: Int
)

/** Ranks by points (ties share a rank), then alphabetically within a tie. */
fun rankMembers(stats: List<Pair<Member, MemberStats>>): List<LeaderboardEntry> {
    val sorted = stats.sortedWith(
        compareByDescending<Pair<Member, MemberStats>> { it.second.points }.thenBy { it.first.name }
    )
    var lastPoints = -1
    var lastRank = 0
    return sorted.mapIndexed { index, (member, s) ->
        val rank = if (s.points == lastPoints) lastRank else index + 1
        lastPoints = s.points
        lastRank = rank
        LeaderboardEntry(member, s, rank)
    }
}

enum class AchievementMetric { MEETINGS, TASKS, VOTES, POINTS }

enum class Achievement(
    val title: String,
    val description: String,
    val metric: AchievementMetric,
    val threshold: Int
) {
    FIRST_CHECK_IN("First check-in", "Attend your first meeting", AchievementMetric.MEETINGS, 1),
    REGULAR("Regular", "Attend 10 meetings", AchievementMetric.MEETINGS, 10),
    PILLAR("Pillar", "Attend 25 meetings", AchievementMetric.MEETINGS, 25),
    GETTING_STARTED("Getting things done", "Finish your first task", AchievementMetric.TASKS, 1),
    FINISHER("Finisher", "Finish 10 tasks", AchievementMetric.TASKS, 10),
    POWERHOUSE("Powerhouse", "Finish 25 tasks", AchievementMetric.TASKS, 25),
    VOICE("Voice", "Vote in 5 polls", AchievementMetric.VOTES, 5),
    OPINION_LEADER("Opinion leader", "Vote in 20 polls", AchievementMetric.VOTES, 20),
    CENTURY("Century", "Earn 100 points", AchievementMetric.POINTS, 100),
    LEGEND("Legend", "Earn 500 points", AchievementMetric.POINTS, 500);

    fun currentValue(stats: MemberStats): Int = when (metric) {
        AchievementMetric.MEETINGS -> stats.meetingsAttended
        AchievementMetric.TASKS -> stats.tasksCompleted
        AchievementMetric.VOTES -> stats.pollsVoted
        AchievementMetric.POINTS -> stats.points
    }

    fun isUnlocked(stats: MemberStats): Boolean = currentValue(stats) >= threshold

    fun progress(stats: MemberStats): Float = (currentValue(stats).toFloat() / threshold).coerceIn(0f, 1f)
}
