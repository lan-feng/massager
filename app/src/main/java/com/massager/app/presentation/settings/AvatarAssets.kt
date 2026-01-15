package com.massager.app.presentation.settings

// 文件说明：头像资源的集中定义与工具方法。
import androidx.annotation.DrawableRes
import com.massager.app.core.avatar.DEFAULT_AVATAR_NAME
import com.massager.app.R

data class AvatarOption(
    val name: String,
    @DrawableRes val drawableRes: Int
)

val avatarOptions = listOf(
    AvatarOption(name = "ic_avatar_cap", drawableRes = R.drawable.ic_avatar_cap),
    AvatarOption(name = "ic_avatar_glasses", drawableRes = R.drawable.ic_avatar_glasses),
    AvatarOption(name = "ic_avatar_hoodie", drawableRes = R.drawable.ic_avatar_hoodie),
    AvatarOption(name = "ic_avatar_short_hair", drawableRes = R.drawable.ic_avatar_short_hair)
)

@DrawableRes
fun resolveAvatarDrawable(name: String?): Int =
    avatarOptions.firstOrNull { it.name == name }?.drawableRes
        ?: avatarOptions.first { it.name == DEFAULT_AVATAR_NAME }.drawableRes

fun isLocalAvatarName(name: String?): Boolean =
    avatarOptions.any { it.name == name }
