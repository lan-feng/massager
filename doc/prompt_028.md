你是一个 Android Jetpack Compose & 多语言国际化 & 深色主题架构专家。

我的项目是【单 Activity + Jetpack Compose 架构】。
现在存在以下问题：
1️⃣ 运行时切换语言会导致 Activity recreate 和白屏闪烁
2️⃣ 切换深色/浅色模式也触发重建，用户体验很差
3️⃣ 希望做到：
   - ✅ 切换语言 0 闪屏、0 重建
   - ✅ 切换主题 0 闪屏、0 重建
   - ✅ 基于 StateFlow / DataStore 持久化
   - ✅ Compose 自动重组 UI
   - ✅ 适配 Material3 / Material2

请你在【不改变单 Activity 架构】的前提下，完成以下代码级改造输出：

--------------------------------------------------
【一】语言系统改造目标
--------------------------------------------------
✅ 要求：
- 使用 StateFlow + DataStore 存储当前语言
- 使用 CompositionLocal 动态注入 Context
- Text 使用 stringResource 自适配
- 不允许调用 recreate()
- 不允许 finish + startActivity

✅ 输出内容：
1. LanguageManager（StateFlow + DataStore）
2. LocalAppLocale CompositionLocal 定义
3. MainActivity 中语言状态绑定
4. Compose 层动态多语言示例（Text + Button）
5. 中文 / 英文切换示例

--------------------------------------------------
【二】主题系统改造目标（深色/浅色 + 可扩展自定义主题）
--------------------------------------------------
✅ 要求：
- 使用 StateFlow 管理 AppTheme（LIGHT / DARK）
- 使用 DataStore 持久化主题
- 不允许使用 AppCompatDelegate.setDefaultNightMode
- 主题变化必须 0 recreate、0 闪屏

✅ 输出内容：
1. ThemeManager（StateFlow + DataStore）
2. MyAppTheme(darkTheme: Boolean) 实现
3. MainActivity 中主题绑定
4. Compose 页面主题实时切换示例

--------------------------------------------------
【三】MainActivity 标准模板（最终结果）
--------------------------------------------------
✅ 要求：
- 只允许一个 setContent{}
- 使用 collectAsStateWithLifecycle
- 同时管理：
   - 当前语言
   - 当前主题
- 所有 UI 由 State 驱动
- 支持以后扩展多主题（如 AMOLED、Brand 定制主题）

--------------------------------------------------
【四】提供完整可直接运行的 Kotlin 代码文件：
--------------------------------------------------
✅ 每个文件都要单独输出：
- LanguageManager.kt
- ThemeManager.kt
- MyAppTheme.kt
- MainActivity.kt
- SettingsScreen.kt（包含语言 + 主题切换 UI）

--------------------------------------------------
【五】最终验收标准：
--------------------------------------------------
✅ 切换语言：
- 不闪屏
- 不重启 Activity
- 不跳转页面
- 所有 string 立即刷新

✅ 切换主题：
- 无闪烁
- 无白屏
- 无 recreate
- 所有控件立即变色

✅ 兼容：
- Android 8 - Android 15
- Material2 & Material3

--------------------------------------------------
请严格按模块分区输出完整 Kotlin 源码，不要省略。
