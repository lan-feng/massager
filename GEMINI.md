# 项目背景
这是一个基于 Kotlin 的 Android IoT 应用，用于连接和控制按摩/EMS 设备。应用采用 MVVM + Jetpack Compose 架构，覆盖账号体系、设备绑定与控制、蓝牙扫描/会话、用户资料与设置等，并集成 Firebase Analytics/Crashlytics 做线上诊断。

# 项目结构与架构规范
- 单模块 `:app`，包名 `com.massager.app`。
- 分层目录：
  - `data/`：本地 Room、远程 Retrofit、蓝牙会话与协议解析。
  - `domain/`：业务模型与 UseCase 聚合。
  - `presentation/`：Compose UI、导航与 ViewModel。
  - `di/`：Hilt 模块与依赖注入配置。
- Compose UI 入口：`presentation/MassagerApp.kt`，主 Activity：`presentation/MainActivity.kt`。
- Hilt Application：`MassagerApplication.kt`，初始化语言偏好与 Firebase。

# 业务逻辑梳理
- 账号与会话：
  - `data/repository/AuthRepository.kt` 负责登录/注册/重置/登出，并写入 `SessionManager`。
  - Google 登录通过 Firebase ID Token 与后端交换。
- 设备与测量：
  - `data/repository/MassagerRepository.kt` 处理设备列表、绑定/解绑、重命名、组合信息、测量数据刷新。
  - Guest 模式走本地数据，不请求后端；登录模式依赖 API 与 Room 同步。
- 蓝牙与协议：
  - `data/bluetooth/MassagerBluetoothService.kt` 管理多设备 GATT 会话，状态流按地址分发。
  - `data/bluetooth/scan/BleScanCoordinator.kt` 负责扫描与缓存。
  - `data/bluetooth/protocol/*` 负责协议适配、CRC、广告解析与注册。
- UI 状态：
  - ViewModel 暴露 `StateFlow`，Compose 使用 `collectAsStateWithLifecycle` 订阅。
  - `presentation/navigation/MassagerNavigation.kt` 统一路由与跨页状态切换。

# 技术栈
- UI：Jetpack Compose + Material3，Navigation Compose。
- 依赖注入：Hilt（`@HiltViewModel`/`@HiltAndroidApp`）。
- 网络：Retrofit + OkHttp + Kotlinx Serialization。
- 本地：Room + DataStore/SharedPreferences。
- 设备：Android BLE API + 自定义协议适配。
- 分析：Firebase Analytics/Crashlytics（通过 BuildConfig 开关）。

# 构建与环境配置
- `app/build.gradle.kts` 使用 flavor `dev/qa` 配置 `BASE_URL`，`buildTypes` 配置 `APP_ID` 和 Crashlytics 开关。
- `NetworkModule.kt` 使用 `BuildConfig.BASE_URL` 构建 Retrofit。
- `DatabaseModule.kt` 使用 `fallbackToDestructiveMigration()`，无迁移将清库。

# 编码规范与约定
- UseCase 只调用 Repository；ViewModel 只调用 UseCase。
- 禁止 UI 直接调用 Retrofit/Room。
- 网络/数据库操作须在 IO dispatcher；UI 不做阻塞调用。
- 避免硬编码字符串，统一走 `res/values*`。
- BLE 权限在 Manifest 声明；Android 12+ 使用 `BLUETOOTH_SCAN/CONNECT`。

# 质量与兼容性扫描重点
- BLE：
  - 扫描/连接流程的权限检查、状态机正确性、GATT 回调线程安全。
  - MTU/Service discovery 超时与重试逻辑是否可重入。
- 网络：
  - API 返回 `success` 失败路径是否完整覆盖；错误信息是否可追踪。
  - BaseUrl 与 flavor/buildType 组合是否一致，避免遗漏。
- 数据库：
  - Room 实体/DAO 与业务逻辑一致性，关注 `fallbackToDestructiveMigration` 的数据风险。
- 并发与状态：
  - Flow/StateFlow 的取消与生命周期绑定是否正确。
  - 连接状态更新是否会被并发覆盖或丢失。
- 登录与会话：
  - Guest 模式与登录模式的分支一致性。
  - Token/用户 ID 写入后是否被遗漏读取。
- UI/导航：
  - 导航状态切换是否受认证状态影响；错误提示是否被清理。

# 重要文件索引
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/com/massager/app/MassagerApplication.kt`
- `app/src/main/java/com/massager/app/di/NetworkModule.kt`
- `app/src/main/java/com/massager/app/di/DatabaseModule.kt`
- `app/src/main/java/com/massager/app/data/repository/AuthRepository.kt`
- `app/src/main/java/com/massager/app/data/repository/MassagerRepository.kt`
- `app/src/main/java/com/massager/app/data/bluetooth/MassagerBluetoothService.kt`
- `app/src/main/java/com/massager/app/presentation/navigation/MassagerNavigation.kt`
