package fi.clocklings.domain

/** The shared English past/to style selected for Clocklings. */
object EnglishTime {
    private val small =
        listOf(
            "zero",
            "one",
            "two",
            "three",
            "four",
            "five",
            "six",
            "seven",
            "eight",
            "nine",
            "ten",
            "eleven",
            "twelve",
            "thirteen",
            "fourteen",
            "fifteen",
            "sixteen",
            "seventeen",
            "eighteen",
            "nineteen",
        )
    private val tens = listOf("", "", "twenty", "thirty", "forty", "fifty")

    private fun number(value: Int): String =
        if (value < small.size) {
            small[value]
        } else {
            tens[value / DECIMAL_BASE] + if (value % DECIMAL_BASE == 0) "" else "-${small[value % DECIMAL_BASE]}"
        }

    fun timeWords(total: Int): String {
        val time = ClockTime.mod(total, ClockTime.HALF_DAY)
        val hour = time / ClockTime.MINUTES_PER_HOUR
        val minute = time % ClockTime.MINUTES_PER_HOUR
        val current = number(if (hour == 0) ClockTime.HOURS_PER_HALF_DAY else hour)
        val next = number(hour + 1)
        return when (minute) {
            0 -> {
                "$current o'clock"
            }

            MINUTES_PER_QUARTER_HOUR -> {
                "quarter past $current"
            }

            ClockTime.MINUTES_PER_HALF_HOUR -> {
                "half past $current"
            }

            ClockTime.MINUTES_PER_HOUR - MINUTES_PER_QUARTER_HOUR -> {
                "quarter to $next"
            }

            else -> {
                if (minute < ClockTime.MINUTES_PER_HALF_HOUR) {
                    "${minutePhrase(minute)} past $current"
                } else {
                    "${minutePhrase(ClockTime.MINUTES_PER_HOUR - minute)} to $next"
                }
            }
        }
    }

    private fun minutePhrase(minute: Int): String =
        when {
            minute == 1 -> "one minute"
            minute % ClockTime.MINUTES_PER_NUMBER == 0 -> number(minute)
            else -> "${number(minute)} minutes"
        }

    fun digital(
        total: Int,
        h24: Boolean = false,
    ): String {
        val time = ClockTime.mod(total, ClockTime.DAY)
        val hour = time / ClockTime.MINUTES_PER_HOUR
        val minute = (time % ClockTime.MINUTES_PER_HOUR).toString().padStart(2, '0')
        val displayHour =
            if (h24) {
                hour.toString().padStart(2, '0')
            } else {
                (hour % ClockTime.HOURS_PER_HALF_DAY)
                    .let {
                        if (it == 0) ClockTime.HOURS_PER_HALF_DAY else it
                    }.toString()
            }
        return "$displayHour:$minute"
    }

    fun numericWords(
        total: Int,
        h24: Boolean = false,
    ): String {
        val time = ClockTime.mod(total, ClockTime.DAY)
        val hour = time / ClockTime.MINUTES_PER_HOUR
        val minute = time % ClockTime.MINUTES_PER_HOUR
        if (!h24 && minute == 0) return timeWords(time)
        val displayHour =
            if (h24) {
                hour
            } else {
                (hour % ClockTime.HOURS_PER_HALF_DAY).let {
                    if (it == 0) ClockTime.HOURS_PER_HALF_DAY else it
                }
            }
        val minutes =
            when {
                minute == 0 -> "zero zero"
                minute < DECIMAL_BASE -> "oh ${number(minute)}"
                else -> number(minute)
            }
        return "${number(displayHour)} $minutes"
    }

    fun dayPart(total: Int): DayPart {
        val time = ClockTime.mod(total, ClockTime.DAY)
        val hour = time / ClockTime.MINUTES_PER_HOUR
        val sun = hour in SUNRISE_HOUR until EVENING_FROM
        return when {
            time == 0 -> DayPart("midnight", "at midnight", sun)
            time == ClockTime.HALF_DAY -> DayPart("noon", "at noon", sun)
            hour < MORNING_FROM || hour >= NIGHT_FROM -> DayPart("night", "at night", sun)
            hour < ClockTime.HOURS_PER_HALF_DAY -> DayPart("morning", "in the morning", sun)
            hour < EVENING_FROM -> DayPart("afternoon", "in the afternoon", sun)
            else -> DayPart("evening", "in the evening", sun)
        }
    }

    fun fullWords(
        total: Int,
        h24: Boolean,
    ): String {
        if (!h24) return timeWords(total)
        val time = ClockTime.mod(total, ClockTime.DAY)
        val part = dayPart(time)
        return if (time == 0 || time == ClockTime.HALF_DAY) part.name else "${timeWords(time)} ${part.adverb}"
    }

    fun greeting(total: Int): Greeting =
        when (ClockTime.mod(total, ClockTime.DAY) / ClockTime.MINUTES_PER_HOUR) {
            in MORNING_FROM until DAY_GREETING_FROM -> Greeting.MORNING
            in DAY_GREETING_FROM until EVENING_FROM -> Greeting.DAY
            in EVENING_FROM until NIGHT_FROM -> Greeting.EVENING
            else -> Greeting.NIGHT
        }
}

private const val DECIMAL_BASE = 10
private const val MINUTES_PER_QUARTER_HOUR = 15
private const val SUNRISE_HOUR = 6
private const val MORNING_FROM = 5
private const val DAY_GREETING_FROM = 10
private const val EVENING_FROM = 18
private const val NIGHT_FROM = 22
