package eu.hxreborn.phdp.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import eu.hxreborn.phdp.ui.theme.Tokens

@Composable
fun ExpressiveCheckbox(
    checked: Boolean,
    modifier: Modifier = Modifier,
) {
    val checkboxShape = RoundedCornerShape(Tokens.CheckboxCornerRadius)

    val backgroundColor =
        animateColorAsState(
            if (checked) MaterialTheme.colorScheme.primary else Color.Transparent,
            label = "bgColor",
        )
    val borderColor =
        animateColorAsState(
            if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            label = "borderColor",
        )

    Surface(
        modifier = modifier.size(Tokens.CheckboxSize),
        shape = checkboxShape,
        color = backgroundColor.value,
        border = BorderStroke(Tokens.CheckboxBorderWidth, borderColor.value),
    ) {
        Box(contentAlignment = Alignment.Center) {
            AnimatedVisibility(
                visible = checked,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(Tokens.CheckboxIconSize),
                )
            }
        }
    }
}
