你是一名资深移动端工程师。

请在不修改现有业务接口定义、不破坏现有网络层结构的前提下，
为 App 增加“后端接口国际化语言传递能力”。

【背景】
- App 已支持语言切换（非系统语言，App 内切换）
- 后端是 Spring Boot，支持通过 HTTP Header 识别语言
- 目标是：App 当前语言变化后，所有 API 请求都自动携带语言信息
- 不允许因语言切换导致 App 重启或页面闪烁

【技术约束】
- Android 使用 OkHttp / Retrofit（如是 iOS 使用 Alamofire）
- 项目采用单 Activity + Jetpack Compose（或等价架构）
- 网络请求统一经过拦截器

【实现要求】
1. 在网络请求拦截器中统一注入 HTTP Header：
   - Header Key: Accept-Language
   - Header Value 示例：
     - zh-CN
     - en-US
     - ja-JP

2. Header 的语言值来源于：
   - App 内当前语言状态（非系统语言）
   - 使用 LanguageManager / LocaleManager / SettingsRepository 等统一管理

3. 支持语言动态切换：
   - 切换语言后，新请求立即生效
   - 已在进行中的请求不强制取消

4. 不允许：
   - 在每个 API 手动传语言参数
   - 在 ViewModel / UseCase 层感知语言逻辑

【输出内容】
- 语言管理器（LanguageManager / LocaleManager）示例代码
- OkHttp / Alamofire 拦截器示例代码
- 语言切换后触发刷新网络 Header 的实现方式
- 简要说明关键设计点（不超过 5 条）

请给出可直接用于生产项目的代码实现。
