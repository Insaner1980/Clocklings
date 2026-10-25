package fi.clocklings.domain

enum class AppLanguage(
    val tag: String,
) {
    ENGLISH("en"),
    FINNISH("fi"),
    SWEDISH("sv"),
    ;

    companion object {
        fun fromTag(tag: String?): AppLanguage? {
            val primary = tag?.substringBefore('-')?.lowercase()
            return entries.firstOrNull { it.tag == primary }
        }
    }
}

fun resolveLanguage(tags: List<String>): AppLanguage =
    tags.firstNotNullOfOrNull(AppLanguage::fromTag) ?: AppLanguage.ENGLISH
