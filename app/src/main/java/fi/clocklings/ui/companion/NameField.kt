package fi.clocklings.ui.companion

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import fi.clocklings.R
import fi.clocklings.ui.components.SkIcon
import fi.clocklings.ui.components.SkIcons
import fi.clocklings.ui.theme.SkColors
import fi.clocklings.ui.theme.SkDimens
import fi.clocklings.ui.theme.SkShapes
import fi.clocklings.ui.theme.SkType

/** Name field: saved while typing, at most 16 characters. Each companion keeps its own name. */
@Composable
internal fun NameField(
    text: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.doll_name_label)
    BasicTextField(
        value = text,
        onValueChange = {
            val end =
                if (it.length > MAX_NAME && it[MAX_NAME - 1].isHighSurrogate() && it[MAX_NAME].isLowSurrogate()) {
                    MAX_NAME - 1
                } else {
                    MAX_NAME
                }
            onChange(it.take(end))
        },
        singleLine = true,
        textStyle = SkType.NameField.copy(textAlign = TextAlign.Center),
        cursorBrush = SolidColor(SkColors.Hour),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
        modifier =
            modifier
                .widthIn(max = SkDimens.NameFieldMaxWidth)
                .fillMaxWidth()
                .semantics { contentDescription = label },
        decorationBox = { inner ->
            Row(
                Modifier
                    .background(SkColors.Surface, SkShapes.Field)
                    .border(SkDimens.BorderThin, SkColors.Line, SkShapes.Field)
                    .padding(horizontal = SkDimens.FieldPaddingHorizontal, vertical = SkDimens.FieldPaddingVertical),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(SkDimens.GapS),
            ) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (text.isEmpty()) {
                        Text(
                            stringResource(R.string.doll_name_placeholder),
                            style = SkType.NameFieldPlaceholder.copy(textAlign = TextAlign.Center),
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

private const val MAX_NAME = 16
