package fi.clocklings.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkShapes
import fi.clocklings.ui.theme.ledgeColor

/** A pressable colour drop: the colour itself is the button, ringed in white with a check when chosen. */
@Composable
fun SwatchTile(
    color: Color,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DepthSurface(
        onClick,
        color,
        ledgeColor(color),
        SkShapes.Pill,
        modifier.size(SkDimens.ChildTarget).semantics {
            contentDescription = label
            this.selected = selected
        },
        border = if (selected) BorderStroke(SkDimens.SwatchRing, SkColors.Surface) else null,
        role = Role.RadioButton,
    ) {
        // The check sits at the top corner, as on the other choice tiles.
        Box { if (selected) SelectionBadge(Modifier.align(Alignment.TopEnd)) }
    }
}
