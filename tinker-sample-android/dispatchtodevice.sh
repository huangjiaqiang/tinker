#!/bin/bash

# 获取已连接设备列表
devices=$(adb devices | grep -w "device" | awk '{print $1}')

# 检查是否有设备连接
if [[ -z "$devices" ]]; then
  echo "未发现任何设备，请确保设备已连接并启用USB调试模式。"
  exit 1
fi

# 遍历设备列表并执行adb命令
for device in $devices; do
  echo "正在处理设备: $device"

  # 执行push命令
  adb -s "$device" push app/build/outputs/apk/tinkerPatch/debug/app-debug-patch_signed.apk /sdcard/Download/
  if [[ $? -eq 0 ]]; then
    echo "app-debug-patch_signed.apk 已成功推送到设备 $device"
  else
    echo "推送 app-debug-patch_signed.apk 到设备 $device 时发生错误"
  fi

  adb -s "$device" push app/build/outputs/apk/tinkerPatch/release/app-release-patch_signed.apk /sdcard/Download/
  if [[ $? -eq 0 ]]; then
    echo "app-release-patch_signed.apk 已成功推送到设备 $device"
  else
    echo "推送 app-release-patch_signed.apk 到设备 $device 时发生错误"
  fi

  echo "完成设备 $device 的操作"
done

echo "所有设备处理完成。"h app/build/outputs/apk/tinkerPatch/release/app-release-patch_signed.apk /sdcard/Download/