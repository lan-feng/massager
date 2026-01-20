package com.massager.app.presentation.settings

// 文件说明：头像资源的集中定义与工具方法。
import androidx.annotation.DrawableRes
import com.massager.app.core.avatar.DEFAULT_AVATAR_NAME
import com.massager.app.R

data class AvatarOption(
    val name: String,
    @DrawableRes val drawableRes: Int,
    val label: String
)

private data class AvatarAsset(
    val canonicalName: String,
    @DrawableRes val drawableRes: Int,
    val label: String,
    val aliases: Set<String> = emptySet()
)

private val avatarAssets = listOf(
    AvatarAsset(
        canonicalName = "baseballCap",
        drawableRes = R.drawable.ic_avatar_cap,
        label = "Baseball cap",
        aliases = setOf("ic_avatar_cap")
    ),
    AvatarAsset(
        canonicalName = "glasses",
        drawableRes = R.drawable.ic_avatar_glasses,
        label = "Glasses",
        aliases = setOf("ic_avatar_glasses")
    ),
    AvatarAsset(
        canonicalName = "hoodie",
        drawableRes = R.drawable.ic_avatar_hoodie,
        label = "Hoodie",
        aliases = setOf("ic_avatar_hoodie")
    ),
    AvatarAsset(
        canonicalName = "shortHair",
        drawableRes = R.drawable.ic_avatar_short_hair,
        label = "Short hair",
        aliases = setOf("ic_avatar_short_hair")
    )
)

val avatarOptions = avatarAssets.map {
    AvatarOption(name = it.canonicalName, drawableRes = it.drawableRes, label = it.label)
}

private val avatarNameLookup: Map<String, AvatarAsset> = avatarAssets
    .flatMap { asset ->
        (asset.aliases + asset.canonicalName).map { alias -> alias to asset }
    }
    .toMap()

@DrawableRes
fun resolveAvatarDrawable(name: String?): Int =
    avatarNameLookup[name]?.drawableRes
        ?: avatarNameLookup[DEFAULT_AVATAR_NAME]?.drawableRes
        ?: avatarAssets.first().drawableRes

fun isLocalAvatarName(name: String?): Boolean =
    avatarNameLookup.containsKey(name)

/**
 * Normalizes avatar names to the new canonical values while keeping backwards compatibility
 * with the old ic_avatar_* names.
 */
fun normalizeAvatarName(name: String?): String {
    if (name.isNullOrBlank()) return DEFAULT_AVATAR_NAME
    return avatarNameLookup[name]?.canonicalName ?: name
}
