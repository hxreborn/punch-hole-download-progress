package eu.hxreborn.phdp.ui.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import eu.hxreborn.phdp.ui.theme.Tokens

@Composable
fun OverflowMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(Tokens.MenuCornerRadius),
        modifier = modifier,
        content = content,
    )
}

@Composable
fun OverflowMenuItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textColor: Color = Color.Unspecified,
    iconTint: Color = LocalContentColor.current,
) {
    DropdownMenuItem(
        text = { Text(text = text, color = textColor) },
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        leadingIcon = { Icon(icon, contentDescription = null, tint = iconTint) },
    )
}

@Composable
fun OverflowMenuToggle(
    text: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenuItem(
        text = { Text(text) },
        onClick = { onCheckedChange(!checked) },
        modifier = modifier,
        leadingIcon = { Icon(icon, contentDescription = null) },
        trailingIcon = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.scale(Tokens.MENU_SWITCH_SCALE),
            )
        },
    )
}
