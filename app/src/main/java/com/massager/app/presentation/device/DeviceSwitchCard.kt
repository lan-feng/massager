package com.massager.app.presentation.device

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.massager.app.R
import com.massager.app.presentation.theme.massagerExtendedColors

enum class ConnectionState {
    IDLE,
    CONNECTING,
    READY,
    DISCONNECTED
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DeviceSwitchCard(
    name: String,
    subtitle: String,
    isSelected: Boolean,
    connectionState: ConnectionState,
    batteryPercent: Int?,
    buzzerOn: Boolean,
    isInteractive: Boolean,
    onReconnect: () -> Unit,
    onBuzzerToggle: (Boolean) -> Unit,
    onCardTap: () -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(20.dp)
    val brand = MaterialTheme.massagerExtendedColors.band
    val outline = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val error = MaterialTheme.colorScheme.error
    val neutral = controlPanelBackground()

    val background = when (connectionState) {
        ConnectionState.DISCONNECTED -> error.copy(alpha = 0.1f).compositeOver(neutral)
        else -> neutral
    }
    val borderStroke = when (connectionState) {
        ConnectionState.DISCONNECTED -> BorderStroke(2.dp, brand)
        else -> BorderStroke(1.dp, brand)
    }
    val shadowElevation = when (connectionState) {
        ConnectionState.DISCONNECTED -> 12.dp
        else -> 4.dp
    }
    val shadowColor = when (connectionState) {
        ConnectionState.DISCONNECTED -> error.copy(alpha = 0.4f)
        else -> Color.Transparent
    }
    val contentHeight = 96.dp

    // Tapping a card should only switch selection; reconnect lives on the dedicated icon.
    val clickable = true
    val onClick = onCardTap
    val scale = if (connectionState == ConnectionState.DISCONNECTED) {
        val transition = rememberInfiniteTransition(label = "disconnect_pulse")
        val animated by transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.02f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 1500,
                    easing = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "disconnect_scale"
        )
        animated
    } else {
        1f
    }

    val cardModifier = modifier
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .shadow(
            elevation = shadowElevation,
            shape = shape,
            spotColor = shadowColor,
            ambientColor = shadowColor,
            clip = false
        )
        .clip(shape)
        .then(
            if (clickable) {
                Modifier.combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongPress
                )
            } else {
                Modifier
            }
        )

    val readyBackground = if (connectionState == ConnectionState.READY) {
        Modifier
    } else {
        Modifier.background(color = Color.Transparent)
    }

    Card(
        modifier = cardModifier,
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = background),
        border = borderStroke
    ) {
        Box(
            modifier = readyBackground
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .height(contentHeight)
        ) {
            if (isSelected) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.5f)
                            .padding(top = 6.dp, end = 8.dp)
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.massagerExtendedColors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier.weight(0.5f),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        StatusBadge(
                            state = connectionState,
                            brand = brand,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    when (connectionState) {
                                        ConnectionState.READY, ConnectionState.CONNECTING -> brand.copy(alpha = 0.12f)
                                        ConnectionState.DISCONNECTED -> error.copy(alpha = 0.14f)
                                        ConnectionState.IDLE -> Color.Transparent
                                    }
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            onReconnect = onReconnect
                        )
                    }
                }

            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    batteryPercent?.let {
                        BatteryIndicator5Level(batteryPercent = it)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    StatusBadge(
                        state = connectionState,
                        brand = brand,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                when (connectionState) {
                                    ConnectionState.READY, ConnectionState.CONNECTING -> brand.copy(alpha = 0.12f)
                                    ConnectionState.DISCONNECTED -> error.copy(alpha = 0.14f)
                                    ConnectionState.IDLE -> Color.Transparent
                                }
                            )
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                        onReconnect = onReconnect
                    )
                }
            }


            // 当设备就绪时，显示电量和开关
            if (isSelected && connectionState == ConnectionState.READY && isInteractive && batteryPercent != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomStart),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 左侧电量指示器保持不变
                    BatteryIndicator5Level(batteryPercent = batteryPercent)

                    // 右侧的图标和开关组合
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.height(24.dp)
                    ) {
                        Icon(
                            imageVector = if (buzzerOn) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp), // 固定图标尺寸为 24dp
                            tint = if (buzzerOn) brand else MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Switch(
                            checked = buzzerOn,
                            onCheckedChange = { onBuzzerToggle(it) },
                            enabled = true,
                            modifier = Modifier
                                .scale(0.8f)
                                .size(width = 44.dp, height = 24.dp),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = brand,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = brand.copy(alpha = 0.35f),
                                uncheckedBorderColor = brand.copy(alpha = 0.4f),
                                checkedBorderColor = brand.copy(alpha = 0.7f)
                            )
                        )
                    }
                }
            }



            androidx.compose.animation.AnimatedVisibility(
                visible = isSelected && connectionState == ConnectionState.DISCONNECTED,
                modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(id = R.string.device_tap_to_reconnect),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = error
                    )
                }
            }

            if (connectionState == ConnectionState.IDLE) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.White.copy(alpha = 0.35f))
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(
    state: ConnectionState,
    brand: Color,
    modifier: Modifier = Modifier,
    onReconnect: (() -> Unit)? = null
) {
    val success = brand
    val danger = MaterialTheme.colorScheme.error
    val baseModifier = if (state == ConnectionState.DISCONNECTED && onReconnect != null) {
        modifier.clickable { onReconnect() }
    } else {
        modifier
    }
    when (state) {
        ConnectionState.READY -> {
            Row(
                modifier = baseModifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.WifiTethering,
                    contentDescription = null,
                    tint = success,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = androidx.compose.ui.res.stringResource(id = R.string.device_status_connected),
                    style = MaterialTheme.typography.labelMedium,
                    color = success,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false
                )
            }
        }

        ConnectionState.CONNECTING -> {
            Row(
                modifier = baseModifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    text = androidx.compose.ui.res.stringResource(id = R.string.device_status_waiting),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false
                )
            }
        }

        ConnectionState.DISCONNECTED -> {
            Row(
                modifier = baseModifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    tint = danger,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = androidx.compose.ui.res.stringResource(id = R.string.device_status_disconnected),
                    style = MaterialTheme.typography.labelMedium,
                    color = danger,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false
                )
            }
        }

        ConnectionState.IDLE -> Unit
    }
}


@Composable
fun BatteryIndicator5Level(
    batteryPercent: Int,
    modifier: Modifier = Modifier
) {
    val level = when {
        batteryPercent < 0 -> -1
        batteryPercent >= 80 -> 4
        batteryPercent >= 60 -> 3
        batteryPercent >= 40 -> 2
        batteryPercent >= 20 -> 1
        else -> 0
    }
    val fillColor = when (level) {
        -1 -> MaterialTheme.massagerExtendedColors.iconMuted
        0 -> MaterialTheme.massagerExtendedColors.danger
        1 -> Color(0xFFF6A609)
        else -> MaterialTheme.massagerExtendedColors.success
    }
    val outlineColor = MaterialTheme.massagerExtendedColors.outline
    val mutedFill = MaterialTheme.massagerExtendedColors.iconMuted.copy(alpha = 0.25f)
    Canvas(
        modifier = modifier
            .width(34.dp)
            .height(16.dp)
    ) {
        val strokeWidth = 1.8.dp.toPx()
        val headWidth = 5.dp.toPx()
        val bodyLeft = strokeWidth
        val bodyTop = strokeWidth
        val bodyWidth = size.width - headWidth - strokeWidth * 2
        val bodyHeight = size.height - strokeWidth * 2
        val corner = 3.dp.toPx()

        // Outline
        drawRoundRect(
            color = fillColor,
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = strokeWidth)
        )
        // Battery head
        val headHeight = bodyHeight / 2f
        drawRoundRect(
            color = fillColor,
            topLeft = Offset(bodyLeft + bodyWidth, (size.height - headHeight) / 2f),
            size = Size(headWidth, headHeight),
            cornerRadius = CornerRadius(corner / 2f, corner / 2f),
            style = Fill
        )

        if (level >= 0) {
            val segments = 5
            val fillWidth = (bodyWidth - strokeWidth * 2) * (level + 1) / segments
            drawRoundRect(
                color = fillColor,
                topLeft = Offset(bodyLeft + strokeWidth, bodyTop + strokeWidth),
                size = Size(fillWidth, bodyHeight - strokeWidth * 2),
                cornerRadius = CornerRadius(corner / 1.5f, corner / 1.5f),
                style = Fill
            )
        } else {
            drawRoundRect(
                color = mutedFill,
                topLeft = Offset(bodyLeft + strokeWidth, bodyTop + strokeWidth),
                size = Size(bodyWidth - strokeWidth * 2, bodyHeight - strokeWidth * 2),
                cornerRadius = CornerRadius(corner / 1.5f, corner / 1.5f),
                style = Fill
            )
        }
    }
}
