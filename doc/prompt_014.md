## Role
你是一名资深 Android BLE SDK 架构师，负责为一系列使用 JL6328 芯片 的产品（例如 EMS 按摩仪、智能体温计、加热理疗仪等）设计一套可扩展 BLE GATT 通信框架。
SDK 必须支持多种设备类型，每种设备使用不同的自定义上位协议帧结构，应用程序在扫描广播后自动识别设备类型并加载对应协议。

## 核心目标
兼容文档EMSv2设备描述的 EMS 设备（产品ID=1）；
框架需支持多设备协议注册与动态匹配；
提供高内聚、模块化、生产可用的 Kotlin 代码；
满足 BLE 工程实践要求（MTU 协商、分片、安全回调、错误恢复等）；
所有接口均具备完善注释（KDoc）与示例。

## 通信协议规范（EMS v2 设备）
1️⃣ 帧格式（上位协议透传于 GATT 特征值）

引用：v2 文档【data0~data(n+4) 段落】
| 字段              | 长度 | 含义                          |
| `data0,1`       | 2  | 固定帧头 `'y','h'` (0x79, 0x68) |
| `data2,3`       | 2  | 数据长度（data0~datan+2 总字节数）    |
| `data4~n`       | N  | 实际数据内容                      |
| `data(n+1,n+2)` | 2  | CRC16（对 data0~datan 计算）     |
| `data(n+3,n+4)` | 2  | `\r\n` 结尾符（不参与校验）           |
限制：
单帧最大 128 字节；
CRC16：初值 0x0000，多项式 0x1021；
波特率 115200（串口透传模拟环境）。
2️⃣ 数据区结构（data4~n）
引用：v2 文档数据流定义
字节	含义
Byte0	数据流方向：1=APP→MCU，2=MCU→APP
Byte1	数据流 ID（详见下表）
Byte2~N	具体功能数据
3️⃣ 数据流 ID（EMS产品）
| ID | 功能    | 数据内容                                                                |
| 0  | 心跳包   | 运行状态、电池档、模式、档位、位置、剩余时间（5s周期）                                        |
| 1  | 模式设置  | 0–7：massage/knead/scraping/pressure/acupoint/cupping/activate/shape |
| 2  | 档位    | 0–19                                                                |
| 3  | 按摩位置  | 0–5                                                                 |
| 4  | 电池档位  | 0–4                                                                 |
| 5  | 蓝牙状态  | 0/1（未连接/已连接）                                                        |
| 6  | 充电状态  | 0/1..4                                                              |
| 7  | 蜂鸣器开关 | 0=关闭，1=开启                                                       |
| 8  | 定时    | 0–255 分钟                                                            |
4️⃣ 广播结构
引用：v2 文档“蓝牙广播数据定义”
广播字节格式：
'y','h' + productId + fwVersion
其中 productId 表示产品类型（EMS=1，其他产品另定义），fwVersion 表示固件版本。

🧠 框架设计目标
支持多设备协议动态加载：
当扫描到广播 'yh'+productId+fwVersion 时：
自动识别 productId；
从注册中心 ProtocolRegistry 中查找对应协议；
加载匹配的 ProtocolAdapter；
GATT 层据此进行 encode/decode。

## GATT 连接流程
扫描并过滤 'yh' 广播；
自动匹配协议；
建立 GATT 连接；
请求 MTU(247)，回退兼容；
写入/通知使用对应协议适配器；
接收特征值 → decode → 分发至 Flow；
提供状态流与心跳超时检测。

### CRC16
文档 eg：
unsigned short crc16(unsigned char *data, unsigned int len)
{
unsigned short wCRCin = 0x0000;
unsigned short wCPoly = 0x1021;
unsigned char wChar = 0;

    while (len--) {
        wChar = *(data++);
        wCRCin ^= (wChar << 8);
 
        for (int i = 0; i < 8; i++) {
            if (wCRCin & 0x8000) {
                wCRCin = (wCRCin << 1) ^ wCPoly;
            } else {
                wCRCin = wCRCin << 1;
            }
        }
    }
    return (wCRCin);
}

🎯 生成目标
产出一套：
Kotlin + BLE GATT 通信框架；
支持多设备协议动态匹配与注册；
符合 JL6328 EMS v2 协议标准；
具备分片、CRC校验、错误恢复、日志、测试；
具备模块化架构、强扩展性与完整文档。
## Implementation Notes
- Added ProtocolRegistry with Hilt multibindings to resolve BLE protocol adapters by productId.
- Implemented EmsV2ProtocolAdapter supporting frame encode/decode, CRC16-CCITT validation, and high level commands (ReadStatus, SetMode, TogglePower).
- Exposed HyAdvertisement parser to extract productId/firmware from 'yh' broadcasts and wired the data into scan results.
- MassagerBluetoothService now negotiates MTU based on the active protocol, routes notifications into a shared flow of decoded ProtocolMessages, and offers sendProtocolCommand helpers.
- Updated DeviceTypeConfig to prioritise EMS v2 product ids when resolving backend device types.


```
79 68  固定帧头
10 00  数据长度
02     MCU→APP 方向
00     命令 (STATUS)
00     运行状态 (0=停止/1=运行)
02     电池档位 (0~4)
04     模式值 (0~7)
00     强度 (0~19)
04     按摩位置 (zone)
00     时间(分钟)
06 D9  CRC16 (0x1021)
0D 0A  结尾符
```
注：STATUS 帧已不再包含蜂鸣器状态，需要通过 BUZZER(CMD=7) 指令单独查询。
