
## 总体目标
构建一个基于 Kotlin 的智能体温计管理应用。采用 MVVM 架构，分离数据层、领域层、展示层。UI 使用 Jetpack Compose，依赖注入使用 Hilt，网络使用 Retrofit/OkHttp，本地持久化使用 Room。包含基础的蓝牙功能骨架，以及多个健康功能的占位界面。
XXX:替换为项目的名称massager
## 模块与 Gradle
- 单模块：`:app`
- 根 `build.gradle.kts` 仅声明以下插件：
  - `com.android.application` 8.2.0
  - `org.jetbrains.kotlin.android`、`kotlin-kapt`、`kotlinx-serialization` 都是 1.9.22
  - `com.google.dagger.hilt.android` 2.48

### `settings.gradle.kts`
- `pluginManagement` 和 `dependencyResolutionManagement` 仓库包含：`google()`、`mavenCentral()`、`gradlePluginPortal()`
- `rootProject.name = "XXX"`
- `include(":app")`

### `gradle.properties`
- JVM 参数 `-Xmx2048m`
- `android.useAndroidX=true`
- `kotlin.code.style=official`
- `android.nonTransitiveRClass=true`

### `app/build.gradle.kts`
- 插件：Android Application、Kotlin Android、`kotlin-kapt`、`kotlinx-serialization`、`dagger.hilt.android.plugin`
- Android 配置：
  - `namespace = "com.XXX.app"`
  - `compileSdk = 34`，`minSdk = 24`，`targetSdk = 34`
  - `defaultConfig`：`applicationId`，`versionCode = 1`，`versionName = "1.0"`，启动 Compose 矢量图支持，`testInstrumentationRunner` 为 `androidx.test.runner.AndroidJUnitRunner`
  - `buildTypes`：
    - `debug`：`buildConfigField` BASE_URL `"http://192.168.2.110:9100/iot/api/"`，APP_ID `"test-app"`
    - `release`：`BASE_URL "https://api.yourproductionurl.com/"`，APP_ID 示例，`isMinifyEnabled=false`，ProGuard 使用 `proguard-android-optimize.txt` + `proguard-rules.pro`
  - Java/Kotlin 兼容版本均为 1.8
  - `buildFeatures { compose = true; buildConfig = true }`
  - `composeOptions.kotlinCompilerExtensionVersion = "1.5.8"`
  - Packaging 排除 `/META-INF/{AL2.0,LGPL2.1}`

- 依赖：
  - AndroidX core、lifecycle、activity-compose、splashscreen、Material3 + Compose BOM/Material Icons
  - Navigation Compose
  - Lifecycle ViewModel/runtime compose
  - Hilt (`hilt-android`、编译器、`hilt-navigation-compose`)
  - Room (`room-runtime`、`room-ktx`、编译器)
  - 网络：Retrofit 2.9.0，Gson 转换器，OkHttp 4.12.0 + logging
  - Kotlinx Serialization JSON 1.6.2 + Retrofit converter
  - 协程：Android 版 1.7.3
  - 蓝牙：`androidx.bluetooth:bluetooth:1.0.0-alpha02`
  - 第三方登录：Google Play Services Auth 20.7.0、Facebook SDK 16.2.0
  - 测试：JUnit 4.13.2，AndroidX junit/espresso，Compose UI 测试库（`debugImplementation` 用于工具和测试清单）

## ProGuard (`app/proguard-rules.pro`)
- 保留 Hilt、javax.inject、Room 数据库/实体、Retrofit 类、Kotlinx serialization 元数据，并忽略 Room paging 警告。

## 模块结构 (`app/src/main/java/com/xyj/XXX`)
```
XXXApplication.kt  // @HiltAndroidApp，应用入口

data/
  bluetooth/
    BluetoothManager.kt          // 封装 BluetoothAdapter 的状态检查与已配对设备扫描
    XXXBluetoothService.kt (功能骨架)
  local/
    dao/ (UserDao、DeviceDao、ThingDao、RecordDao、MeasurementDao、XXXDeviceDao 等)
    entity/ (UserEntity、DeviceEntity、ThingEntity、RecordEntity、MeasurementEntity、XXXDeviceEntity 等)
    Converters.kt                 // Room 类型转换
    SessionManager.kt             // SharedPreferences 会话存储
    XXXDatabase.kt        // Room 数据库与 DAO 绑定
  remote/
    dto/ (Api、AuthDto、DeviceDto、User DTO、HttpResult 包装、各种请求响应模型)
    AuthApiService.kt（如存在） + XXXApiService.kt 定义 REST 接口
    AuthInterceptor.kt            // 从 SessionManager 读取 token 并附加
  repository/
    AuthRepository.kt             // 登录/注册相关逻辑
    XXXRepository.kt      // 统一数据源，整合 Retrofit + Room，并包含恢复设备示例数据

di/
  DatabaseModule.kt               // 提供 Room 数据库及 DAO
  NetworkModule.kt                // 提供 SessionManager、AuthInterceptor、OkHttp、Retrofit、API Service

domain/
  model/ (AuthResult 密封类、DeviceMetadata、RecoveryXXXOption、TemperatureRecord 等)
  usecase/
    auth/ (LoginUseCase、RegisterUseCase)
    device/、measurement/、settings/ 等 UseCase 封装仓库调用

presentation/
  MainActivity.kt                 // `@AndroidEntryPoint`，设置 Compose 内容+Scaffold+XXXNavigation
  navigation/
    Screen 密封类路由
    XXXNavigation.kt      // NavHost 根据 AuthResult 选择起始目的地，包含 Login、Register、Home、DeviceScan、Settings 等
  auth/ (LoginScreen、RegisterScreen、AuthViewModel 负责状态流与 SessionManager)
  home/ (HomeDashboardScreen，展示快捷操作与卡片)
  XXX/ (DeviceScanScreen 调用蓝牙管理器等)
  settings/ (SettingsScreen、账号子页面、占位功能)
  recovery/ (RecoverySelectionRoute 等)
  common/ (UiText 抽象、加载/占位组件)
  theme/ (Color.kt、Typography.kt、Theme.kt，配置 Material3 样式)

## Android 清单 (`app/src/main/AndroidManifest.xml`)
- 权限：INTERNET、ACCESS_NETWORK_STATE、BLUETOOTH、BLUETOOTH_ADMIN、BLUETOOTH_CONNECT、BLUETOOTH_SCAN（neverForLocation）、ACCESS_FINE_LOCATION、ACCESS_COARSE_LOCATION。
- Application 属性：`android:name=".XXXApplication"`，备份规则，主题 `@style/Theme.Material3.DayNight.NoActionBar`，自定义 `network_security_config`。
- 主启动 Activity `.presentation.MainActivity`。

## 资源 (`app/src/main/res`)
- `values/`：颜色、字符串、主题（Material3）。`values-zh/` 提供中文翻译。
- `xml/`：`backup_rules.xml`、`data_extraction_rules.xml`、`network_security_config.xml`（允许运行期访问开发主机）。
- 主要依赖 Compose，无复杂 drawable。

## 其他文件
- `doc/swagger.yaml`：Retrofit 接口对应的 swagger 描述，AI不允许修改doc下文件。
- 根目录 `.vscode/`：Gradle 构建的 tasks/settings。
- `README.md`：中文项目说明，涵盖技术栈、功能、权限需求等。
- `gradle/wrapper/`：标准 Gradle wrapper；包含 `gradlew` 和 `gradlew.bat`。
- `app/build` 下以及根目录 `node_modules` 是生成产物；重新生成骨架时可忽略，除非另有要求。

## 行为预期
- 登录流程通过 `SessionManager` 持久化 token 和账号信息。
- 仓库层在成功的网络返回后写入 Room（用户、设备、实体、测量等表）。
- UseCase 暴露 `Flow`（特别是认证流程），更新 `AuthResult`。
  - `AuthResult.LoginSuccess` 触发导航跳转 Home，其余状态保持登录页面。
- Compose 界面通过 `collectAsStateWithLifecycle` 订阅状态；未实现的功能采用 `FeaturePlaceholderScreen` 统一占位。

## 构建与运行
- 使用 Gradle Wrapper (`./gradlew build`, `./gradlew installDebug`)。
- Compose 编译扩展 1.5.8；Kotlin 版本 1.9.22。
- Hilt 与 Room 使用 KAPT。
- Debug 环境 BASE_URL 指向局域网 API；Release 配置生产占位地址。

## 交付期望
- 占位功能（如蓝牙扫描、恢复流程）保持示例实现。
- DI 图能够编译（Application 标注 `@HiltAndroidApp`，模块安装到 `SingletonComponent`）。
- 保留相同的 Gradle 配置、依赖、Manifest 声明、资源与文档骨架。
