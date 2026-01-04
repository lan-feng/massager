package com.massager.app.presentation.device

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.style.TextAlign
import com.massager.app.R
import com.massager.app.presentation.theme.massagerExtendedColors
import java.util.Locale

@Composable
fun TimerDashboard(
    isRunning: Boolean,
    remainingSeconds: Int,
    timerMinutes: Int,
    brand: Color,
    brandSoft: Color, // kept for API parity; not used currently
    enabled: Boolean,
    onSelectTimer: (Int) -> Unit,
    onToggleSession: () -> Unit
) {
    val baseMinutes = timerMinutes.coerceIn(0, 60)
    val remainingMinutes = if (remainingSeconds > 0) (remainingSeconds + 59) / 60 else baseMinutes
    val displayMinutes = (if (isRunning) remainingMinutes else baseMinutes).coerceIn(0, 60)
    val dashboardBackground = controlPanelBackground()
    val displaySeconds = if (isRunning) {
        remainingSeconds.coerceAtLeast(0)
    } else {
        remainingSeconds.coerceAtLeast(0).takeIf { it > 0 } ?: baseMinutes * 60
    }
    val hours = displaySeconds / 3600
    val minutes = (displaySeconds % 3600) / 60
    val seconds = displaySeconds % 60
    val timeText = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)

    val increaseColor = Color(0xFF7AC99A)
    val decreaseColor = Color(0xFFE4BF87)
    val stopColor = MaterialTheme.massagerExtendedColors.danger
    val increaseGradient = remember { Brush.verticalGradient(listOf(increaseColor.copy(alpha = 0.28f), increaseColor.copy(alpha = 0.08f))) }
    val decreaseGradient = remember { Brush.verticalGradient(listOf(decreaseColor.copy(alpha = 0.28f), decreaseColor.copy(alpha = 0.08f))) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(dashboardBackground)
            .padding(horizontal = 18.dp, vertical = 18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.dp, brand, RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(
                                enabled = enabled,
                                onClick = { onSelectTimer(30) }
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Timer,
                            contentDescription = null,
                            tint = brand,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "30 min",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = brand
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                            .background(brand)
                    )

                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontSize = 36.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = brand,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionButton(
                    modifier = Modifier.weight(2f),
                    icon = Icons.Filled.Stop,
                    label = stringResource(id = R.string.device_stop),
                    containerColor = stopColor,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    enabled = enabled,
                    orientation = ActionButtonOrientation.Horizontal,
                    onClick = onToggleSession
                )
                ActionButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Remove,
                    label = "5 min",
                    containerBrush = decreaseGradient,
                    containerColor = decreaseColor.copy(alpha = 0.15f),
                    contentColor = decreaseColor,
                    enabled = enabled,
                    orientation = ActionButtonOrientation.Vertical,
                    onClick = { onSelectTimer((displayMinutes - 5).coerceIn(0, 60)) }
                )
                ActionButton(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Add,
                    label = "5 min",
                    containerBrush = increaseGradient,
                    containerColor = increaseColor.copy(alpha = 0.15f),
                    contentColor = increaseColor,
                    enabled = enabled,
                    orientation = ActionButtonOrientation.Vertical,
                    onClick = { onSelectTimer((displayMinutes + 5).coerceIn(0, 60)) }
                )
            }
        }
    }
}

private enum class ActionButtonOrientation {
    Horizontal,
    Vertical
}

@Composable
private fun ActionButton(
    modifier: Modifier,
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    containerBrush: Brush? = null,
    orientation: ActionButtonOrientation = ActionButtonOrientation.Horizontal
) {
    val backgroundModifier = if (containerBrush == null) {
        Modifier.background(containerColor, RoundedCornerShape(16.dp))
    } else {
        Modifier
            .background(containerColor, RoundedCornerShape(16.dp))
            .background(containerBrush, RoundedCornerShape(16.dp))
    }
    val iconSize = if (orientation == ActionButtonOrientation.Horizontal) 28.dp else 22.dp
    val spacing = if (orientation == ActionButtonOrientation.Horizontal) 6.dp else 1.dp
    val contentPadding = if (orientation == ActionButtonOrientation.Horizontal) {
        PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    } else {
        PaddingValues(horizontal = 12.dp, vertical = 4.dp)
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .then(backgroundModifier)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        if (orientation == ActionButtonOrientation.Horizontal) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(iconSize)
                )

                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = contentColor
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(iconSize)
                )

                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = contentColor
                )
            }
        }
    }
}
