package dcrustm.ecell.mobile.domain.model

/** The parts of their own profile a member can edit (never role, committee or approval). */
data class ProfileUpdate(
    val phone: String,
    val bio: String,
    val year: Int,
    val branch: String,
    val skills: List<String>,
    val linkedinUrl: String,
    val githubUrl: String,
    val portfolioUrl: String
) {
    /** What actually gets saved: trimmed, de-duplicated skills, links with https:// added. */
    fun cleaned(): ProfileUpdate = copy(
        phone = phone.trim(),
        bio = bio.trim(),
        branch = branch.trim(),
        skills = skills.map { it.trim() }.filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .take(MAX_SKILLS),
        linkedinUrl = cleanLink(linkedinUrl),
        githubUrl = cleanLink(githubUrl),
        portfolioUrl = cleanLink(portfolioUrl)
    )

    private fun cleanLink(input: String): String =
        if (input.isBlank()) "" else ResourceType.normalizeUrl(input)

    companion object {
        const val MAX_SKILLS = 12

        fun from(member: Member) = ProfileUpdate(
            phone = member.phone,
            bio = member.bio,
            year = member.year,
            branch = member.branch,
            skills = member.skills,
            linkedinUrl = member.linkedinUrl,
            githubUrl = member.githubUrl,
            portfolioUrl = member.portfolioUrl
        )
    }
}
