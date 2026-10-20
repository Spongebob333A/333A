# 实现说明

## 当前模块

| 模块 | 位置 | 当前实现 |
| --- | --- | --- |
| 本地数据 | `data/Models.kt`、`data/MedRepository.kt` | Kotlin Serialization 将药品、服药记录、库存和显示偏好持久化到应用私有 JSON 文件；新增字段均有默认值，旧数据可继续读取 |
| 角色入口 | `data/UserRole.kt`、`ui/screens/RoleSelectionScreen.kt`、`MainActivity.kt` | 启动选择老人端或家属端；老人端隐藏药品编辑入口，家属端展示方案管理入口；角色切换回到选择页 |
| 家属药品管理 | `ui/screens/MedicationsScreen.kt`、`ui/screens/EditMedicationScreen.kt` | 编辑现有药品计划；保留 OCR 预填、单位下拉、日历、库存以及暂停/恢复能力 |
| 风险预检 | `domain/MedicationRiskAnalyzer.kt`、`ui/screens/MedicationRiskReviewDialog.kt` | 保存前检查药名重复和备注中的风险提示，并展示二次确认。它不是 AI，不包含临床相互作用知识 |
| 老人服药 | `ui/screens/TodayScreen.kt`、`ui/MedViewModel.kt` | 老人端查看今日计划并反馈已服用、稍后提醒或跳过；稍后提醒复用持久化延时闹钟机制 |
| 库存和预警 | `data/MedRepository.kt`、`notify` | 已服用状态变更时扣减/回补库存；阈值触发本机通知和已授权的短信发送 |
| 大字显示 | `ui/theme/Theme.kt`、`data/Models.kt`、设置页 | 保存大字模式偏好；只提高系统字体缩放，不放大 dp 布局尺寸，减少按钮和卡片溢出 |
| OCR | `domain/LeafletParser.kt`、`ui/screens/ScanLeafletScreen.kt` | CameraX 拍摄，ML Kit 本机识别；解析结果用于预填，家属确认后才保存 |

## 保存药品的交互

1. 家属进入“添加药品”，选择手动录入或拍摄说明书。
2. OCR 识别结果回填到表单，药名、剂量、单位和提醒安排仍由家属核对。
3. 点击保存后先运行本机资料预检，展示重名或备注提示及功能边界。
4. 家属点击“我已核对，继续保存”后才写入本机 JSON，并重新安排该药提醒。
5. 风险预检未连接 AI 或医学知识库，所以“未发现提示”不等于“药物组合安全”。

## 服药和库存规则

1. 闹钟触发后显示通知；老人可从通知或今日页面反馈。
2. “已服用”写入带实际时间的记录，并按单次剂量扣减库存。
3. “稍后提醒”创建一个可持久化的延时提醒，不修改库存和服药状态。
4. “跳过”记录跳过状态，不扣库存。
5. 未操作不等于已服用；超时提醒只能表示尚未确认，短信文案不得断言老人已经漏服。

## 待后端实现

Spring Boot 服务仍需承担注册认证、家庭关系/二维码绑定、用药方案同步、老人反馈上传、低库存与未确认事件通知，以及数据库持久化。跨设备一致性、重试和重复事件幂等也需要在 API 与数据模型中设计。当前实现没有网络同步，两个角色在本机共享一份数据。

