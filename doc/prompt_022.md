# 设备组合优化

# 1.设备扫描组件优化
首页->设备扫描页面 和 设备控制页面->设备扫描页面 共用一个设备扫描组件；当具体业务有差异。
首页->设备扫描页面：扫描设备时需要排除已经在首页设备列表的设备，点击绑定数据保存成功后回到首页。
设备控制页面->设备扫描页面：扫描设备时需要排除当前控制页面的主设备和已经在combo_info列表里面的设备，点击时不是触发绑定操作而是触发组合操作，追加combo_info列表调用/device/v1/updateComboInfo接口更新主设备的combo_info，然后回到备控制页面。
combo_info记录扫描页面选中设备的,device_serial,device_type,firmware_version,unique_id，name_alias等信息
注意：deviceType是通过device_types.json映射的typeId

# 2.首页优化
1.-首页>设备扫描页面时，需要传入当前列表信息用于扫描排除，同时传入进入标识方便设备扫描页面回到首页。
2.首页 重命名成后 应该回到首页取消选中状态。

# 3.设备控制页面优化

1.设备绑定EMS后进入设备控制页面，DeviceDisplaySection显示的是设备名称，这时是正确的；但是在设备控制页面点击“+”进行组合扫描返回时，显示明显修改成了BLE_EMS，不管是否有成功进行组合都修改了，但是数据本身还是原来的BLE_EMS fw:2 #279ABB71
2.设备绑定EMS后进入设备控制页面, DeviceDisplaySection刷新图标消失，占位的背景还在；同时，DeviceDisplaySection显示的设备名称超出显示范围没有进行控制，没有进行"..."处理。
3.通过首页设备列表进入设备控制页面，获取当前设备信息解析combo_info；如果combo_info为空，只需要显示当前设备的DeviceDisplaySection及相关控制。默认选中当前主设备；且要刷新选中设备DeviceDisplaySection的连接、电池、小喇叭信息。
优化BatteryStatusRow显示，不在显示battery文字，显示电池的图标和数值。
4.在设备控制页面进入设备控制页面绑定成功返回设备控制页面，需要在设备控制页面显示新增的设备在DeviceDisplaySection区域。主设备显示在最左边，然后依次显示combo_info列表里面的设备，跳转过来默认选中刚组合的设备。

