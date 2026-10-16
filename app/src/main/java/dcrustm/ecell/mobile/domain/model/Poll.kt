package dcrustm.ecell.mobile.domain.model

data class Poll(
    val id: String = "",
    val question: String = "",
    val options: List<String> = emptyList(),
    val createdByUid: String = "",
    val createdByName: String = "",
    /** null = every member can vote; otherwise only that committee (and the coordinator). */
    val committee: Committee? = null,
    val createdAt: Long = System.currentTimeMillis(),
    /** 0 = stays open until someone closes it manually. */
    val closesAt: Long = 0L,
    val isClosed: Boolean = false
) {
    val isOrgWide: Boolean get() = committee == null

    fun isOpen(now: Long = System.currentTimeMillis()): Boolean =
        !isClosed && (closesAt == 0L || now < closesAt)

    companion object {
        const val MIN_OPTIONS = 2
        const val MAX_OPTIONS = 6
    }
}

/** Stored at pollVotes/{pollId}_{voterUid}, so a member can only ever hold one vote per poll. */
data class PollVote(
    val pollId: String = "",
    val voterUid: String = "",
    val optionIndex: Int = 0,
    val votedAt: Long = System.currentTimeMillis()
)

data class PollWithResults(
    val poll: Poll,
    val votes: List<PollVote>,
    val myVote: Int?
) {
    val totalVotes: Int get() = votes.size

    fun countFor(optionIndex: Int): Int = votes.count { it.optionIndex == optionIndex }

    fun fractionFor(optionIndex: Int): Float =
        if (totalVotes == 0) 0f else countFor(optionIndex).toFloat() / totalVotes

    val leadingOption: Int? get() {
        if (totalVotes == 0) return null
        return poll.options.indices.maxByOrNull { countFor(it) }
    }
}
