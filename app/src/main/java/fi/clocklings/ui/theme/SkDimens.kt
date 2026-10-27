package fi.clocklings.ui.theme

import androidx.compose.ui.unit.dp

/** Välit ja koot. Kosketusalueet ovat vähintään 48 dp. */
object SkDimens {
    const val MAX_ROW_FONT_SCALE = 1.3f

    val Gutter = 16.dp
    val GapS = 8.dp
    val GapM = 12.dp

    /** Näkymän osien väli: tiivis, jotta kello ja kolme vastausta mahtuvat 360×640 dp:n näytölle. */
    val GapScreen = 10.dp
    val GapOptions = 10.dp
    val Gap = 16.dp
    val GapL = 18.dp

    val PanelPadding = 16.dp
    val BorderThin = 2.dp
    val Border = 3.dp

    /** Napin "3D-varjo": painamaton ja painettu korkeus. */
    val ButtonDepth = 5.dp
    val ButtonDepthPressed = 1.dp
    val BigButtonDepth = 7.dp
    val OptionDepth = 4.dp

    val IconButton = 48.dp
    val ActionIcon = 24.dp
    val ChildTarget = 64.dp
    val PathNode = 96.dp
    val PathLead = 20.dp
    val PathGap = 20.dp
    val PathStroke = 10.dp

    /** Near-zero dashes with round caps draw the trail as a row of pebbles. */
    val PathDash = 0.5.dp
    val PathDashGap = 18.dp
    val SelectionBadge = 24.dp
    val TipCompanion = 72.dp
    val TipCompanionCompact = 48.dp
    val BubbleTail = 10.dp
    val LegendSwatchShort = 22.dp
    val LegendSwatchLong = 34.dp
    val LegendSwatchHeight = 8.dp
    val StageMinHeight = 200.dp
    val StageMinHeightCompact = 320.dp
    val LogoMinSize = 14.dp
    val PillIconLarge = 20.dp
    val CompanionThumbnail = 64.dp
    val MenuTileHeight = 92.dp
    val Divider = 1.dp
    val SettingsGroupPadding = 16.dp
    val SettingsLineGap = 2.dp
    val SettingsCardInset = 4.dp
    val SettingsRowHeight = 56.dp
    val GridBreakpoint = 320.dp
    val SwatchRing = 4.dp
    val SegmentInset = 4.dp
    val SegmentLift = 2.dp
    val ItemWindow = 76.dp
    val ZoomWindowHeight = 84.dp
    val ZoomClock = 230.dp
    val ZoomClockShift = (-22).dp
    val ThumbnailMin = 44.dp
    const val CLOCK_HEIGHT_SHARE = 0.42f
    const val PLAY_CLOCK_HEIGHT_SHARE = 0.52f
    val HomeStageMaxHeight = 300.dp
    val BubbleHeadOffset = 4.dp
    val ScrollFade = 16.dp
    const val CLOCK_INNER_SHADOW_WIDTH = 4f
    const val CLOCK_GLASS_INSET = 6f
    const val CLOCK_GLASS_WIDTH = 3f
    const val CLOCK_GLASS_START = 208f
    const val CLOCK_GLASS_SWEEP = 44f
    const val CLOCK_HAND_SHADOW_OFFSET = 3f
    const val CLOCK_HAND_SHADOW_SPREAD = 3f

    val ProgressDot = 16.dp
    val Star = 28.dp
    val StarSmall = 18.dp
    val StarResult = 72.dp
    val MenuIcon = 44.dp
    val LargeActionIcon = 32.dp

    val MiniClockLevel = 76.dp
    val DollResult = 110.dp
    val DollBigMax = 220.dp
    val CompanionPreview = 245.dp
    val ItemMinWidth = 104.dp
    val TimerHeight = 16.dp
}
