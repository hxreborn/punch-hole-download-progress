package eu.hxreborn.phdp.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppTypeChip(
    isSystem: Boolean,
    modifier: Modifier = Modifier,
) {
    val containerColor =
        if (isSystem) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer
        }
    val contentColor =
        if (isSystem) {
            MaterialTheme.colorScheme.onTertiaryContainer
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        }

    Box(
        modifier =
            modifier
                .background(containerColor, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isSystem) Icons.Outlined.Android else Icons.Outlined.Person,
                contentDescription = null,
                modifier = Modifier.size(10.dp),
                tint = contentColor,
            )
            Text(
                text = if (isSystem) "SYSTEM" else "USER",
                style =
                    MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 10.sp,
                    ),
                color = contentColor,
                modifier = Modifier.padding(start = 3.dp),
            )
        }
    }
}
