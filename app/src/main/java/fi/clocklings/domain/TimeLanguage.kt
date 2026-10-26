package fi.clocklings.domain

enum class Greeting { MORNING, DAY, EVENING, NIGHT }

/** A contextual day label, its phrase form and the teaching sun indicator. */
data class DayPart(
    val name: String,
    val adverb: String,
    val isSun: Boolean,
)

interface TimeLanguage {
    /** An analog reading with no inferred morning, evening, noon or midnight. */
    fun timeWords(total: Int): String

    fun digital(
        total: Int,
        h24: Boolean = false,
    ): String

    fun numericWords(
        total: Int,
        h24: Boolean = false,
    ): String

    fun dayPart(total: Int): DayPart

    fun fullWords(
        total: Int,
        h24: Boolean,
    ): String {
        if (!h24) return timeWords(total)
        val time = ClockTime.mod(total, ClockTime.DAY)
        val part = dayPart(time)
        return if (time == 0 || time == ClockTime.HALF_DAY) part.name else "${timeWords(time)} ${part.adverb}"
    }

    fun greeting(total: Int): Greeting
}

fun timeLanguage(language: AppLanguage): TimeLanguage =
    when (language) {
        AppLanguage.ENGLISH -> EnglishTime
        AppLanguage.FINNISH -> FinnishTime
        AppLanguage.SWEDISH -> SwedishTime
    }
