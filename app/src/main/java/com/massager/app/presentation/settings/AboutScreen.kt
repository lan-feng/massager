package com.massager.app.presentation.settings

// 文件说明：展示应用版本信息与相关链接的关于页面。
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.massager.app.R
import com.massager.app.presentation.theme.massagerExtendedColors

@kotlin.OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    onOpenUserAgreement: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit
) {
    val context = LocalContext.current
    var isLogoVisible by remember { mutableStateOf(false) }
    val logoAlpha by animateFloatAsState(
        targetValue = if (isLogoVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "logoAlpha"
    )

    LaunchedEffect(Unit) {
        isLogoVisible = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.about_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.massagerExtendedColors.textPrimary
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.massagerExtendedColors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.massagerExtendedColors.surfaceBright,
                    navigationIconContentColor = MaterialTheme.massagerExtendedColors.textPrimary,
                    titleContentColor = MaterialTheme.massagerExtendedColors.textPrimary
                )
            )
        },
        containerColor = MaterialTheme.massagerExtendedColors.surfaceSubtle
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.massagerExtendedColors.surfaceSubtle)
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 80.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val logoContentDescription = stringResource(R.string.about_logo_description)
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .graphicsLayer { alpha = logoAlpha }
                        .shadow(elevation = 12.dp, shape = CircleShape, clip = false)
                        .clip(CircleShape)
                        .semantics { contentDescription = logoContentDescription }
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.massagerExtendedColors.danger,
                                    MaterialTheme.massagerExtendedColors.danger.copy(alpha = 0.8f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "M",
                        color = MaterialTheme.massagerExtendedColors.textOnAccent,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.version_label),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.massagerExtendedColors.textMuted,
                        fontSize = 14.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 2.dp,
                color = MaterialTheme.massagerExtendedColors.surfaceBright
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    InfoListItem(
                        title = stringResource(R.string.check_update),
                        leadingIcon = Icons.Outlined.SystemUpdate,
                        onClick = { openOnPlayStore(context) },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.massagerExtendedColors.iconMuted
                            )
                        }
                    )
                    Divider(color = MaterialTheme.massagerExtendedColors.divider, thickness = 1.dp)
                    InfoListItem(
                        title = stringResource(R.string.user_agreement),
                        leadingIcon = Icons.Outlined.Article,
                        onClick = onOpenUserAgreement
                    )
                    Divider(color = MaterialTheme.massagerExtendedColors.divider, thickness = 1.dp)
                    InfoListItem(
                        title = stringResource(R.string.privacy_policy),
                        leadingIcon = Icons.Outlined.Policy,
                        onClick = onOpenPrivacyPolicy
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                text = stringResource(R.string.copyright),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.massagerExtendedColors.textMuted,
                    fontSize = 12.sp
                ),
                lineHeight = 16.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun InfoListItem(
    title: String,
    leadingIcon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: @Composable () -> Unit = {
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.massagerExtendedColors.iconMuted
        )
    },
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(),
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = MaterialTheme.massagerExtendedColors.danger
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    color = MaterialTheme.massagerExtendedColors.textPrimary
                )
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        trailingContent()
    }
}

private fun openOnPlayStore(context: android.content.Context) {
    val packageName = context.packageName
    val marketIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("market://details?id=$packageName")
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val webIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(marketIntent)
    } catch (exception: ActivityNotFoundException) {
        context.startActivity(webIntent)
    }
}
