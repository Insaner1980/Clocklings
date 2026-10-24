package fi.clocklings.ui.companion

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import fi.clocklings.ui.components.Eyebrow
import fi.clocklings.ui.components.Pill
import fi.clocklings.ui.components.PillKind
import fi.clocklings.ui.components.ScreenScaffold
import fi.clocklings.ui.components.SkIcon
import fi.clocklings.ui.components.SkIcons
import fi.clocklings.ui.components.TopBar
import fi.clocklings.ui.components.WordSafeText
import fi.clocklings.ui.components.cardShadow
import fi.clocklings.ui.itemName
import fi.clocklings.ui.slotName
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkShapes
import fi.clocklings.ui.theme.SkType

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
    ScreenScaffold(modifier) {
        TopBar(stringResource(R.string.doll_title), stringResource(R.string.back), onBack)
        WardrobeStage(
            kind,
            outfit,
            color,
            progress.nameFor(kind),
            onNameChange = { app.setName(kind, it.trim()) },
        )
        Text(
            if (Wardrobe.lockedCount(kind, total) > 0) {
                pluralStringResource(R.plurals.doll_info, total, total)
            } else {
                stringResource(R.string.doll_all_open)
            },
            style = SkType.Body.copy(fontSize = SkType.FeedbackHint.fontSize),
        )
        if (kind != CompanionKind.HUMAN) {
            SectionTitle(stringResource(R.string.companion_color))
            ItemGrid(CompanionColor.entries, Modifier.selectableGroup(), wideColumns = COLOR_COLUMNS) { option, cell ->
                ColorTile(
                    kind,
                    outfit,
                    option,
                    option == color,
                    onClick = {
                        app.setColor(kind, option)
                        app.play(Sound.POP)
                    },
                    modifier = cell,
                )
            }
        }
        for (slot in kind.slots) {
            SectionTitle(stringResource(slotName(slot)))
            if (slot.optional) {
                Text(
                    stringResource(R.string.doll_optional_hint),
                    style = SkType.Small.copy(color = SkColors.InkSoft),
                )
            }
            ItemGrid(Wardrobe.itemsFor(kind, slot)) { item, cell ->
                if (item.star <= total) {
                    ItemTile(item, outfit, color, cell, on = outfit.isOn(item), onClick = {
                        app.toggleItem(kind, item)
                        app.play(Sound.POP)
                    })
                } else {
                    LockedTile(item, cell)
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
    onNameChange: (String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .cardShadow()
            .background(Brush.verticalGradient(listOf(SkColors.DollStageTop, SkColors.DollStageBottom)), SkShapes.Card)
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CompanionView(
            kind,
            outfit,
            Modifier.width(SkDimens.DollBigMax).aspectRatio(companionAspectRatio(kind)),
            color = color,
        )
        Eyebrow(stringResource(R.string.doll_name_label))
        NameField(kind, name, onNameChange)
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
            Box(
                Modifier
                    .background(SkColors.Surface, SkShapes.Field)
                    .border(SkDimens.Border, SkColors.Line, SkShapes.Field)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (text.isEmpty()) {
                    Text(
                        stringResource(R.string.doll_name_placeholder),
                        style = SkType.NameField.copy(color = SkColors.InkSoft, textAlign = TextAlign.Center),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                inner()
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
private fun <T> ItemGrid(
    options: List<T>,
    modifier: Modifier = Modifier,
    wideColumns: Int = GRID_COLUMNS,
    tile: @Composable (T, Modifier) -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val narrow = maxWidth < NarrowGridWidth || LocalDensity.current.fontScale > SkDimens.MAX_ROW_FONT_SCALE
        val columns = if (narrow) NARROW_GRID_COLUMNS else wideColumns
        Column(verticalArrangement = Arrangement.spacedBy(SkDimens.GapM)) {
            for (row in options.chunked(columns)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(SkDimens.GapM),
                    modifier = Modifier.height(IntrinsicSize.Max),
                ) {
                    row.forEach { tile(it, Modifier.weight(1f).fillMaxHeight()) }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** A selectable or display-only tile: highlighted background and border when [on]. */
private fun Modifier.tile(
    on: Boolean,
    onClick: (() -> Unit)?,
    role: Role,
): Modifier =
    semantics { if (onClick != null) selected = on }
        .background(if (on) SkColors.ItemOnBg else SkColors.Surface, SkShapes.Item)
        .border(SkDimens.Border, if (on) SkColors.ItemOnBorder else SkColors.Line, SkShapes.Item)
        .then(if (onClick != null) Modifier.clickable(role = role, onClick = onClick) else Modifier)
        .padding(TilePadding)

/** An item thumbnail: the companion wearing this item, cropped to the item's slot. */
@Composable
fun ItemTile(
    item: Item,
    outfit: Outfit,
    color: CompanionColor,
    modifier: Modifier = Modifier,
    on: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier.tile(on, onClick, Role.Button),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CompanionView(
            item.companion,
            outfit.with(item.slot, item.id),
            Modifier.size(SkDimens.ItemArt),
            color = color,
            animated = false,
            cropSlot = item.slot,
        )
        WordSafeText(stringResource(itemName(item.id)), SkType.ItemName)
        if (on) Pill(stringResource(R.string.doll_item_on))
    }
}

/** The whole companion in [color], wearing its current outfit. Colors are always free. */
@Composable
private fun ColorTile(
    kind: CompanionKind,
    outfit: Outfit,
    color: CompanionColor,
    on: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.testTag("color_${color.name}").tile(on, onClick, Role.RadioButton),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CompanionView(
            kind,
            outfit,
            Modifier.height(SkDimens.ItemArt).aspectRatio(companionAspectRatio(kind)),
            color = color,
            animated = false,
        )
        WordSafeText(stringResource(companionColorName(color)), SkType.ItemName)
        if (on) Pill(stringResource(R.string.appearance_selected))
    }
}

@Composable
private fun LockedTile(
    item: Item,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.doll_item_locked, item.star)
    Column(
        modifier
            .background(SkColors.ItemLockedBg, SkShapes.Item)
            .drawBehind {
                val w = SkDimens.Border.toPx()
                drawRoundRect(
                    SkColors.Line,
                    topLeft = Offset(w / 2, w / 2),
                    size = Size(size.width - w, size.height - w),
                    cornerRadius = CornerRadius(20.dp.toPx()),
                    style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 5.dp.toPx()))),
                )
            }.semantics(mergeDescendants = true) { contentDescription = description }
            .padding(TilePadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(SkDimens.ItemArt), contentAlignment = Alignment.Center) { SkIcon(SkIcons.Lock, 34.dp) }
        WordSafeText(stringResource(itemName(item.id)), SkType.ItemName.copy(color = SkColors.InkSoft))
        Pill(item.star.toString(), kind = PillKind.MUTED, icon = SkIcons.StarOn)
    }
}

private val TilePadding = PaddingValues(start = 8.dp, end = 8.dp, top = 10.dp, bottom = 12.dp)
private val NarrowGridWidth = 360.dp

private const val MAX_NAME = 16
private const val GRID_COLUMNS = 3
private const val NARROW_GRID_COLUMNS = 2
private const val COLOR_COLUMNS = 4
