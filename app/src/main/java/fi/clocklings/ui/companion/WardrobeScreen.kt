package fi.clocklings.ui.companion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fi.clocklings.R
import fi.clocklings.audio.Sound
import fi.clocklings.domain.CompanionColor
import fi.clocklings.domain.CompanionKind
import fi.clocklings.domain.Item
import fi.clocklings.domain.Outfit
import fi.clocklings.domain.Progress
import fi.clocklings.domain.Wardrobe
import fi.clocklings.domain.slots
import fi.clocklings.ui.LocalAppViewModel
import fi.clocklings.ui.appearance.companionColorName
import fi.clocklings.ui.components.DepthSurface
import fi.clocklings.ui.components.FlatPanel
import fi.clocklings.ui.components.Pill
import fi.clocklings.ui.components.PillKind
import fi.clocklings.ui.components.ScreenScaffold
import fi.clocklings.ui.components.SelectionBadge
import fi.clocklings.ui.components.SkIcon
import fi.clocklings.ui.components.SkIcons
import fi.clocklings.ui.components.SwatchTile
import fi.clocklings.ui.components.TopBar
import fi.clocklings.ui.components.WordSafeText
import fi.clocklings.ui.itemName
import fi.clocklings.ui.slotName
import fi.clocklings.ui.theme.PersonalizationColors
import fi.clocklings.ui.theme.Section
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkShapes
import fi.clocklings.ui.theme.SkType
import fi.clocklings.ui.theme.ledgeColor

/**
 * The selected companion's wardrobe: the big companion and its name, then (for animals) its color,
 * then its items slot by slot. Unlock texts count only this companion's items.
 */
@Composable
fun WardrobeScreen(
    kind: CompanionKind,
    progress: Progress,
    color: CompanionColor,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val app = LocalAppViewModel.current
    val total = progress.totalStars
    val outfit = progress.outfitFor(kind)
    ScreenScaffold(
        modifier,
        section = Section.COMPANION,
        topBar = { TopBar(stringResource(R.string.doll_title), onBack) },
    ) {
        val starInfo =
            if (Wardrobe.lockedCount(kind, total) > 0) {
                pluralStringResource(R.plurals.doll_info, total, total)
            } else {
                stringResource(R.string.doll_all_open)
            }
        WardrobeStage(
            kind,
            outfit,
            color,
            progress.nameFor(kind),
            stars = stringResource(R.string.home_star_total, total, Wardrobe.MAX_STARS),
            starInfo = starInfo,
            onNameChange = { app.setName(kind, it.trim()) },
        )
        if (Wardrobe.lockedCount(kind, total) > 0) {
            Text(stringResource(R.string.doll_stars_explained), style = SkType.SmallSoft)
        }
        if (kind != CompanionKind.HUMAN) {
            SectionTitle(stringResource(R.string.companion_color))
            CompanionColors(kind, color)
        }
        val firstOptional = kind.slots.firstOrNull { it.optional }
        for (slot in kind.slots) {
            SectionTitle(stringResource(slotName(slot)))
            // Said once: every optional slot works the same way.
            if (slot == firstOptional) {
                Text(stringResource(R.string.doll_optional_hint), style = SkType.SmallSoft)
            }
            ItemGrid(Wardrobe.itemsFor(kind, slot)) { item, cell, nameStyle ->
                if (item.star <= total) {
                    ItemTile(item, outfit, color, cell, nameStyle = nameStyle, on = outfit.isOn(item), onClick = {
                        app.toggleItem(kind, item)
                        app.play(Sound.POP)
                    })
                } else {
                    LockedTile(item, outfit, color, total, cell, nameStyle)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = SkType.SectionTitle, modifier = Modifier.semantics { heading() })
}

@Composable
private fun WardrobeStage(
    kind: CompanionKind,
    outfit: Outfit,
    color: CompanionColor,
    name: String,
    stars: String,
    starInfo: String,
    onNameChange: (String) -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(SkColors.DollStageTop, SkColors.DollStageBottom)), SkShapes.Card)
            .padding(SkDimens.Gap),
    ) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SkDimens.GapM),
        ) {
            CompanionView(
                kind,
                outfit,
                Modifier.width(SkDimens.DollBigMax).aspectRatio(companionAspectRatio(kind)),
                color = color,
            )
            NameField(kind, name, onNameChange)
        }
        Pill(
            stars,
            Modifier.align(Alignment.TopEnd).semantics { contentDescription = starInfo },
            icon = SkIcons.StarOn,
            large = true,
        )
    }
}

/** Name field: saved while typing, at most 16 characters. Each companion keeps its own name. */
@Composable
private fun NameField(
    kind: CompanionKind,
    saved: String,
    onChange: (String) -> Unit,
) {
    var text by remember(kind) { mutableStateOf(saved) }
    val label = stringResource(R.string.doll_name_label)
    BasicTextField(
        value = text,
        onValueChange = {
            text = it.take(MAX_NAME)
            onChange(text)
        },
        singleLine = true,
        textStyle = SkType.NameField.copy(textAlign = TextAlign.Center),
        cursorBrush = SolidColor(SkColors.Hour),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        modifier = Modifier.widthIn(max = 280.dp).fillMaxWidth().semantics { contentDescription = label },
        decorationBox = { inner ->
            Row(
                Modifier
                    .background(SkColors.Surface, SkShapes.Field)
                    .border(SkDimens.BorderThin, SkColors.Line, SkShapes.Field)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SkDimens.GapS),
            ) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (text.isEmpty()) {
                        Text(
                            stringResource(R.string.doll_name_placeholder),
                            style = SkType.NameField.copy(color = SkColors.InkSoft, textAlign = TextAlign.Center),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    inner()
                }
                // Shows that the name can be typed in; the field itself carries the label.
                SkIcon(SkIcons.Pencil, SkDimens.ActionIcon, Modifier.clearAndSetSemantics { })
            }
        },
    )
}

/**
 * [wideColumns] columns (three for items, one row of four for colors); two on narrow screens and
 * with large text, so long names such as "Firefighter helmet" never split mid-word. Empty places
 * in the last row stay empty.
 */
@Composable
private fun ItemGrid(
    options: List<Item>,
    modifier: Modifier = Modifier,
    wideColumns: Int = GRID_COLUMNS,
    tile: @Composable (Item, Modifier, TextStyle) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val narrow = maxWidth < NarrowGridWidth || LocalDensity.current.fontScale > SkDimens.MAX_ROW_FONT_SCALE
        val density = LocalDensity.current
        val measurer = rememberTextMeasurer()
        val names = options.map { stringResource(itemName(it.id)) }
        val words = names.flatMap { it.split(Regex("(?<=-)|\\s+")) }

        fun longestWordWidth(fontSize: Float) =
            words.maxOfOrNull {
                measurer.measure(it, SkType.ItemName.copy(fontSize = fontSize.sp)).size.width
            } ?: 0
        var columns = if (narrow) NARROW_GRID_COLUMNS else wideColumns

        fun textWidth(count: Int) =
            with(density) {
                (maxWidth.roundToPx() - SkDimens.GapM.roundToPx() * (count - 1)) / count -
                    SkDimens.GapS.roundToPx() * 2
            }
        val minimumSize = SkType.ItemNameMinSize.value
        // At large font scales a long word may need one column even at the minimum size.
        if (longestWordWidth(minimumSize) > textWidth(columns)) columns = 1
        var fontSize = SkType.ItemName.fontSize.value
        while (fontSize > minimumSize && longestWordWidth(fontSize) > textWidth(columns)) {
            fontSize = (fontSize - SkType.ItemNameStep.value).coerceAtLeast(minimumSize)
        }
        val nameStyle = SkType.ItemName.copy(fontSize = fontSize.sp)
        Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapM)) {
            for (row in options.chunked(columns)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM),
                    modifier = Modifier.height(IntrinsicSize.Max),
                ) {
                    row.forEach { tile(it, Modifier.weight(1f).fillMaxHeight(), nameStyle) }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** Item previews are raised only when they can be selected. */
@Composable
fun ItemTile(
    item: Item,
    outfit: Outfit,
    color: CompanionColor,
    modifier: Modifier = Modifier,
    on: Boolean = false,
    nameStyle: TextStyle? = null,
    onClick: (() -> Unit)? = null,
) {
    if (onClick == null) {
        FlatPanel(modifier, padding = SkDimens.GapS) { ItemTileContent(item, outfit, color, on, nameStyle) }
    } else {
        val fill = if (on) SkColors.ItemOnBg else SkColors.Surface
        DepthSurface(
            onClick,
            fill,
            ledgeColor(fill),
            SkShapes.Item,
            modifier.semantics { selected = on },
            border = if (on) BorderStroke(SkDimens.Border, SkColors.ItemOnBorder) else null,
        ) {
            Box(Modifier.padding(TilePadding)) { ItemTileContent(item, outfit, color, on, nameStyle) }
        }
    }
}

@Composable
private fun ItemTileContent(
    item: Item,
    outfit: Outfit,
    color: CompanionColor,
    on: Boolean,
    nameStyle: TextStyle?,
) {
    Box {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(SkDimens.GapS),
        ) {
            ItemArtWindow(item, outfit, color)
            val name = stringResource(itemName(item.id))
            if (nameStyle == null) {
                WordSafeText(name, SkType.ItemName)
            } else {
                WordSafeText(name, nameStyle, minScale = 1f)
            }
        }
        if (on) SelectionBadge(Modifier.align(Alignment.TopEnd))
    }
}

/**
 * A locked item shows a faded, colourless preview of itself, so the child can see what the next stars
 * will bring, with a lock and progress toward its star threshold.
 */
@Composable
private fun LockedTile(
    item: Item,
    outfit: Outfit,
    color: CompanionColor,
    total: Int,
    modifier: Modifier = Modifier,
    nameStyle: TextStyle = SkType.ItemName,
) {
    val description = stringResource(R.string.doll_item_locked, item.star, total)
    Column(
        modifier
            .background(SkColors.ItemLockedBg, SkShapes.Item)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(TilePadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box {
            ItemArtWindow(item, outfit, color, Modifier.graphicsLayer { alpha = LOCKED_ALPHA }.grayscale())
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(SkDimens.SelectionBadge)
                    .background(SkColors.Surface, SkShapes.Pill),
                contentAlignment = Alignment.Center,
            ) { SkIcon(SkIcons.Lock, SkDimens.StarSmall) }
        }
        WordSafeText(stringResource(itemName(item.id)), nameStyle.copy(color = SkColors.InkSoft), minScale = 1f)
        Pill(stringResource(R.string.home_star_total, total, item.star), kind = PillKind.MUTED, icon = SkIcons.StarOn)
    }
}

private const val LOCKED_ALPHA = 0.55f

private val TilePadding = PaddingValues(start = 8.dp, end = 8.dp, top = 10.dp, bottom = 12.dp)
private val NarrowGridWidth = 360.dp

private const val MAX_NAME = 16
private const val GRID_COLUMNS = 3
private const val NARROW_GRID_COLUMNS = 2

@Composable
private fun CompanionColors(
    kind: CompanionKind,
    color: CompanionColor,
) {
    val app = LocalAppViewModel.current
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(SkDimens.GapS),
        verticalArrangement = Arrangement.spacedBy(SkDimens.GapS),
    ) {
        CompanionColor.entries.forEach { option ->
            SwatchTile(
                PersonalizationColors.companion(kind, option),
                stringResource(companionColorName(option)),
                option == color,
                {
                    app.setColor(kind, option)
                    app.play(Sound.POP)
                },
                Modifier.testTag("color_${option.name}"),
            )
        }
    }
}
