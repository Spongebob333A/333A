# 药时安心

药时安心是一个面向老年人日常用药提醒的 Android 课程项目。当前版本在同一台设备上提供“老人端”和“家属端”两种操作视图，方便演示完整交互；药品和服药记录保存在本机。

## 当前可体验功能

- 启动时选择老人端或家属端；老人端突出今日服药和简明反馈，家属端负责药品方案、OCR、库存和联系人设置。
- 老人可标记“已服用”“稍后提醒”或“跳过”；确认服用时按单次剂量扣减库存，稍后提醒与跳过不扣库存。
- 家属可添加、编辑、暂停、恢复和归档药品，设置单位、服药时间、疗程和库存提醒线。
- 拍摄说明书后使用本机 ML Kit OCR 预填候选信息；保存前由家属核对。
- 保存药品前显示本机风险预检与二次确认。当前只核对重名和备注提示，不具备药品相互作用数据库，不能判断临床风险。
- 服药未确认和库存不足时保留本机提醒；家属短信功能需配置联系人并由用户授予短信权限。
- 查看近期服药记录；老人端可以在入口或设置页打开大字模式。

## 当前边界

本版本尚未接入 Spring Boot 后端、老人/家属账号、二维码绑定、跨手机同步或真正的 AI/权威药品相互作用数据库。角色切换只是同一设备上的前端演示入口，两个角色暂时共用本机数据。短信由当前设备调用 Android 短信服务发送，受权限、SIM 卡和运营商限制。

药时安心用于提醒和记录，不提供诊断、剂量调整或漏服医疗建议。药品名称、剂量和用法应以医生指导及药品说明书为准。

## 开发与运行

环境要求：Android Studio、Android SDK 和 JDK 17。项目使用 Kotlin、Jetpack Compose、Material 3、CameraX、Google ML Kit 与 AlarmManager。

```powershell
./gradlew.bat assembleDebug
./gradlew.bat testDebugUnitTest
```

首次运行前，请在 Android 系统中允许通知和精确闹钟；使用 OCR 时允许相机；使用短信预警时配置联系人并授予短信权限。

## 代码结构

| 位置 | 职责 |
| --- | --- |
| `app/src/main/java/com/jian/pillreminder/data` | 用药数据模型和本地 JSON 持久化 |
| `app/src/main/java/com/jian/pillreminder/domain` | 计划计算、说明书解析和本机风险预检 |
| `app/src/main/java/com/jian/pillreminder/notify` | 闹钟、通知、稍后提醒和短信预警 |
| `app/src/main/java/com/jian/pillreminder/ui` | 角色入口、Compose 页面、主题与大字模式 |
| `docs` | 项目范围、实现说明和验收场景 |

