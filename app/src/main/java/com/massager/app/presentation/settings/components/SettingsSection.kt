package com.massager.app.presentation.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.massager.app.presentation.theme.massagerExtendedColors

data class SettingsEntry(
    val title: String,
    val icon: ImageVector? = null,
    val trailingText: String? = null,
    val trailingContent: (@Composable () -> Unit)? = null,
    val enabled: Boolean = true,
    val onClick: (() -> Unit)? = null
)

@Composable
fun SettingsSectionCard(
    title: String?,
    items: List<SettingsEntry>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
    cornerRadius: Int = 26
) {
    if (items.isEmpty()) return

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.massagerExtendedColors.cardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.massagerExtendedColors.cardBackground)
                .fillMaxWidth()
                .padding(contentPadding)
        ) {
            title?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            items.forEachIndexed { index, item ->
                SettingsEntryRow(item)
                if (index != items.lastIndex) {
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                }
            }
        }
    }
}

@Composable
private fun SettingsEntryRow(item: SettingsEntry) {
    val interaction = remember(item.title) { MutableInteractionSource() }
    val hasNavigation = item.onClick != null || item.trailingContent != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = item.enabled && hasNavigation,
                interactionSource = interaction,
                indication = if (hasNavigation) {
                    rememberRipple(color = MaterialTheme.massagerExtendedColors.band.copy(alpha = 0.16f))
                } else null
            ) { if (item.enabled) item.onClick?.invoke() }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item.icon?.let {
            Icon(
                imageVector = it,
                contentDescription = item.title,
                tint = MaterialTheme.massagerExtendedColors.band,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        when {
            item.trailingContent != null -> item.trailingContent.invoke()
            item.trailingText != null -> Text(
                text = item.trailingText,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (hasNavigation) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
            )
        }
    }
}
