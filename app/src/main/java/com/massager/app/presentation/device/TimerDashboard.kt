package com.massager.app.presentation.device

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import com.massager.app.R
import com.massager.app.presentation.theme.massagerExtendedColors

@OptIn(ExperimentalMaterial3Api::class)
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
    val progress = (displayMinutes.toFloat() / 60f).coerceIn(0f, 1f)
    val dashboardBackground = controlPanelBackground()

    var showSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val increaseColor = Color(0xFF7AC99A)
    val decreaseColor = Color(0xFFE4BF87)
    val increaseGradient = remember {
        Brush.linearGradient(
            listOf(
                increaseColor.copy(alpha = 0.32f),
                increaseColor.copy(alpha = 0.12f)
            )
        )
    }
    val decreaseGradient = remember {
        Brush.linearGradient(
            listOf(
                decreaseColor.copy(alpha = 0.30f),
                decreaseColor.copy(alpha = 0.12f)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(dashboardBackground)
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(50.dp, Alignment.CenterHorizontally)
        ) {
            Box(
                modifier = Modifier
                    .size(140.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(140.dp)) {
                    drawArc(
                        color = brand.copy(alpha = 0.15f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = brand,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .offset(y = 12.dp)
                            .clickable(
                                enabled = enabled,
                                onClick = { showSheet = true }
                            )
                    ) {
                        Text(
                            text = "${displayMinutes} min",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(1.dp))
                        Icon(
                            imageVector = Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    TextButton(
                        enabled = enabled,
                        onClick = onToggleSession,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (isRunning) MaterialTheme.massagerExtendedColors.danger else brand
                        ),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.offset(y = 4.dp)
                    ) {
                        Text(
                            text = if (isRunning) stringResource(id = R.string.device_stop) else stringResource(id = R.string.timer_start),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                QuickAdjustButton(
                    icon = Icons.Filled.Add,
                    animationLabel = "plus",
                    caption = "5 min",
                    foreground = increaseColor,
                    gradient = increaseGradient,
                    borderColor = increaseColor,
                    enabled = enabled,
                    isRunning = isRunning,
                    onClick = { onSelectTimer((displayMinutes + 5).coerceIn(0, 60)) }
                )
                QuickAdjustButton(
                    icon = Icons.Filled.Remove,
                    animationLabel = "minus",
                    caption = "5 min",
                    foreground = decreaseColor,
                    gradient = decreaseGradient,
                    borderColor = decreaseColor,
                    enabled = enabled,
                    isRunning = isRunning,
                    onClick = { onSelectTimer((displayMinutes - 5).coerceIn(0, 60)) }
                )
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState
        ) {
            val options = listOf(5, 10, 15, 20, 25, 30, 40, 50, 60)
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(options) { value ->
                    val isSelected = value == displayMinutes
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        tonalElevation = if (isSelected) 1.dp else 0.dp,
                        color = if (isSelected) brand else MaterialTheme.massagerExtendedColors.surfaceBright,
                        modifier = Modifier
                            .height(64.dp)
                            .clickable {
                                onSelectTimer(value)
                                showSheet = false
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = "$value min",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAdjustButton(
    icon: ImageVector,
    animationLabel: String,
    caption: String,
    foreground: Color,
    gradient: Brush,
    borderColor: Color,
    enabled: Boolean,
    isRunning: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pulseTransition = rememberInfiniteTransition(label = "${animationLabel}_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                1f at 0
                1.03f at 600
                1f at 1200
            }
        ),
        label = "${animationLabel}_pulse_scale"
    )
    val targetScale = when {
        isPressed -> 0.92f
        isRunning && enabled -> pulseScale
        else -> 1f
    }
    val scale by animateFloatAsState(targetValue = targetScale, label = "${animationLabel}_scale_anim")

    Box(
        modifier = Modifier
            .width(80.dp)
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.12f))
            .background(gradient, RoundedCornerShape(14.dp))
            .border(width = 1.5.dp, color = borderColor, shape = RoundedCornerShape(14.dp))
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = caption,
                tint = foreground,
                modifier = Modifier.size(30.dp)
            )

            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = foreground
            )
        }
    }
}
