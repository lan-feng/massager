package com.massager.app.presentation.settings

// 文件说明：展示应用版本信息与相关链接的关于页面。
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.massager.app.BuildConfig
import com.massager.app.R
import com.massager.app.presentation.settings.components.SettingsEntry
import com.massager.app.presentation.settings.components.SettingsSectionCard
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
                title = { Text(text = stringResource(id = R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.massagerExtendedColors.surfaceSubtle,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
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
                        .background(MaterialTheme.massagerExtendedColors.cardBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_massager_logo),
                        contentDescription = logoContentDescription,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(52.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(
                        id = R.string.version_label,
                        BuildConfig.VERSION_NAME,
                        BuildConfig.VERSION_CODE
                    ),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.massagerExtendedColors.textMuted,
                        fontSize = 14.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            SettingsSectionCard(
                title = null,
                items = listOf(
                    SettingsEntry(
                        title = stringResource(R.string.check_update),
                        icon = Icons.Outlined.SystemUpdate,
                        onClick = { openOnPlayStore(context) }
                    ),
                    SettingsEntry(
                        title = stringResource(R.string.user_agreement),
                        icon = Icons.Outlined.Article,
                        onClick = onOpenUserAgreement
                    ),
                    SettingsEntry(
                        title = stringResource(R.string.privacy_policy),
                        icon = Icons.Outlined.Policy,
                        onClick = onOpenPrivacyPolicy
                    )
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 0.dp)
            )

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
