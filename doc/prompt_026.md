你是一个 Android 资深 Kotlin 工程师。

请基于以下数据结构，为“按摩模式选择（Mode Selection）”和“身体部位选择（Body Part）”模块生成完整的 Jetpack Compose UI 代码：

数据模型如下：

data class MassagerOption(
    val id: String,
    val name: String,
    val iconRes: Int
)

模式数据：

val modeList = listOf(
    MassagerOption("MASSAGE", "Massage", R.drawable.ic_self_improvement),
    MassagerOption("KNEAD", "Knead", R.drawable.ic_accessibility),
    MassagerOption("SCRAPING", "Scraping", R.drawable.ic_gesture),
    MassagerOption("PRESSURE", "Pressure", R.drawable.ic_compress),
    MassagerOption("ACUPOINT", "Acupoint", R.drawable.ic_scatter_plot),
    MassagerOption("CUPPING", "Cupping", R.drawable.ic_invert_colors),
    MassagerOption("ACTIVATE", "Activate", R.drawable.ic_local_fire_department),
    MassagerOption("SHAPE", "Shape", R.drawable.ic_woman)
)

部位数据：

val bodyPartList = listOf(
    MassagerOption("SHOULDER", "Shoulder", R.drawable.ic_deck),
    MassagerOption("WAIST", "Waist", R.drawable.ic_airline_seat_recline_normal),
    MassagerOption("LEGS", "Legs", R.drawable.ic_hiking),
    MassagerOption("ARMS", "Arms", R.drawable.ic_sports_gymnastics),
    MassagerOption("JOINT", "Joint", R.drawable.ic_settings_accessibility),
    MassagerOption("BODY", "Body", R.drawable.ic_person)
)

功能要求：
1. 使用 LazyVerticalGrid 实现 4 列网格布局
2. 支持选中状态高亮（背景色 + icon 变色）
3. 点击回调返回选中的 id
4. 分别封装为：
   - ModeSelectionGrid()
   - BodyPartSelectionGrid()

请输出完整可运行的 Compose 代码。
