# Android Kotlin 多设备并行：BLE/业务改造提示

> 将 BLE 连接与 UI 适配为“多设备并行连接与心跳/电量分流”。按阶段推进，避免一次性大改导致回归风险。

## 目标
- 切换卡片不再断开其他设备。
- 每台设备独立维护连接/心跳/电量；UI 卡片按 address 展示。
- 指令与回显都按目标设备路由，避免串设备。

## 阶段 1：服务层多连接架构（MassagerBluetoothService）
- **Session 结构**：`data class DeviceSession(val gatt: BluetoothGatt, val adapter: ProtocolAdapter, val address: String, …)`；使用 `mutableMap<String, DeviceSession>` 持有会话。
- **连接管理**：`connect(address)` 创建/覆盖对应 session；`disconnect(address?: String)` 支持单断/全断；不再用全局 gatt。
- **状态流**：`connectionState` 改为 per-device，如 `StateFlow<Map<String, BleConnectionState>>`，每条记录带 address。
- **协议流**：`protocolMessages` 携带 address，例如 `Flow<DeviceProtocolMessage(address, payload)>`。
- **Gatt 回调**：按 `gatt.device.address` 路由到对应 session；`onConnectionStateChange/onServicesDiscovered/onCharacteristicChanged` 都带上 address。
- **心跳/电量**：协议解析后构造 `Telemetry(address=..., battery=..., …)`，广播到 per-device 流（如 `MutableStateFlow<DeviceTelemetry>` keyed by address）。

## 阶段 2：ViewModel 分设备状态（DeviceControlViewModel）
- **UI 状态**：`DeviceControlUiState` 增加 per-device map：`deviceStatuses: Map<String, DeviceStatus>`，含连接、`isRunning`、`battery`、`lastTelemetry` 等；`selectedDeviceSerial` 表示当前主控设备。
- **订阅流**：连接流仅更新对应设备状态；Telemetry 按 address 更新 `deviceStatuses[address].battery` 等。已选设备沿用现有心跳/模式/定时逻辑，未选设备只更新电量。
- **卡片显示**：选中卡片使用 `selectedDeviceSerial` 数据；未选卡片显示电量或离线提示。
- **切换**：`selectComboDevice` 只改 `selectedDeviceSerial`，若未连接再 `connect(address)`；不再断开其他设备。

## 阶段 3：命令路由
- 所有命令接口（start/stop、level、mode、timer …）增加目标 address 参数，默认 `selectedDeviceSerial`。
- 服务层写指令时，按 address 找对应 session 的 gatt/characteristic。
- 确认/回显（如 SessionStarted/Stopped）携带 address，只更新该设备状态。

## 阶段 4：UI/交互
- 未选卡片：只展示电量/离线重连；不展示名称（已处理）。
- 计时/模式/强度等 UI 继续绑定 `selectedDeviceSerial`。
- `DeviceSwitcher` 仅负责切换与加设备入口。

## 验证清单
- 单设备/多设备连接、切换无断连。
- 未选卡片电量展示与重连按钮正常。
- 选中卡片的心跳/模式/定时/强度指令只作用于对应设备。
- 异常场景：掉线、重连、服务发现失败、电量上报异常。
