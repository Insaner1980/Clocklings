package fi.clocklings.domain

enum class CompanionKind { HUMAN, CAT, DOG, ROBOT, DRAGON, DINOSAUR, NONE }

enum class CompanionColor { ORIGINAL, BLUE, GREEN, PURPLE }

data class CompanionStyle(
    val color: CompanionColor = CompanionColor.ORIGINAL,
)

enum class ClockTheme { CLASSIC, RAINBOW, SPACE, FOREST, OCEAN, GARDEN, WORKSHOP, CUSTOM }

enum class ClockPalette { BLUE, GREEN, PURPLE, CORAL }

enum class ClockFrame { ROUND, SCALLOPED, BLOCKS }

enum class NumeralStyle { ROUNDED, PLAIN }

enum class HandStyle { ROUND, POINTER }

/** Only readable combinations are offered; all faces retain the same time geometry. */
data class CustomClock(
    val palette: ClockPalette = ClockPalette.BLUE,
    val frame: ClockFrame = ClockFrame.ROUND,
    val numerals: NumeralStyle = NumeralStyle.ROUNDED,
    val hands: HandStyle = HandStyle.ROUND,
)

data class ClockAppearance(
    val theme: ClockTheme = ClockTheme.RAINBOW,
    val custom: CustomClock = CustomClock(),
) {
    val numerals: NumeralStyle get() = if (theme == ClockTheme.CUSTOM) custom.numerals else NumeralStyle.ROUNDED
    val hands: HandStyle get() = if (theme == ClockTheme.CUSTOM) custom.hands else HandStyle.ROUND
}

data class Personalization(
    val companion: CompanionKind = CompanionKind.HUMAN,
    val styles: Map<CompanionKind, CompanionStyle> = emptyMap(),
    val clock: ClockAppearance = ClockAppearance(),
) {
    fun styleFor(kind: CompanionKind): CompanionStyle = styles[kind] ?: CompanionStyle()

    fun withStyle(
        kind: CompanionKind,
        style: CompanionStyle,
    ): Personalization = copy(styles = if (style == CompanionStyle()) styles - kind else styles + (kind to style))
}
