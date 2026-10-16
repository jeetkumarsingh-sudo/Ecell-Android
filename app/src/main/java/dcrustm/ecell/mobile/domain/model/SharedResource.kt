package dcrustm.ecell.mobile.domain.model

/** A link the team shares: docs, decks, sheets, videos, design files… */
data class SharedResource(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val url: String = "",
    val type: ResourceType = ResourceType.LINK,
    /** null = shared with everyone; otherwise one committee's library. */
    val committee: Committee? = null,
    val addedByUid: String = "",
    val addedByName: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val isOrgWide: Boolean get() = committee == null

    /** "docs.google.com", "figma.com" — shown under the title. */
    val host: String get() = url
        .substringAfter("://")
        .substringBefore('/')
        .removePrefix("www.")
}

enum class ResourceType(val label: String) {
    DOC("Doc"),
    SLIDES("Slides"),
    SHEET("Sheet"),
    PDF("PDF"),
    VIDEO("Video"),
    DESIGN("Design"),
    LINK("Link");

    companion object {
        /** Best guess from the URL; the person adding it can still change it. */
        fun detect(url: String): ResourceType {
            val u = url.lowercase()
            return when {
                "docs.google.com/document" in u || u.endsWith(".doc") || u.endsWith(".docx") -> DOC
                "docs.google.com/presentation" in u || u.endsWith(".ppt") || u.endsWith(".pptx") -> SLIDES
                "docs.google.com/spreadsheets" in u || u.endsWith(".xls") || u.endsWith(".xlsx") ||
                    u.endsWith(".csv") -> SHEET
                u.endsWith(".pdf") || ".pdf?" in u -> PDF
                "youtube.com" in u || "youtu.be" in u || "vimeo.com" in u || "loom.com" in u -> VIDEO
                "figma.com" in u || "canva.com" in u || "behance.net" in u || "dribbble.com" in u -> DESIGN
                else -> LINK
            }
        }

        /** Adds https:// when someone pastes "example.com/thing". */
        fun normalizeUrl(input: String): String {
            val trimmed = input.trim()
            if (trimmed.isEmpty()) return trimmed
            return if (trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true)
            ) trimmed else "https://$trimmed"
        }

        fun looksLikeUrl(input: String): Boolean {
            val normalized = normalizeUrl(input)
            val host = normalized.substringAfter("://").substringBefore('/')
            return host.contains('.') && !host.contains(' ') && host.length >= 4
        }
    }
}
