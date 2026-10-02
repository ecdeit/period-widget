# Period Widget · 经期助手

安卓桌面小组件：记录经期，预测下次经期、排卵日与易孕期。数据只存在本机。

## 功能

| 功能 | 说明 |
|------|------|
| 桌面小组件 | 今日状态、周期长度、下次经期；点「＋记录」直接跳转录入 |
| 智能预测 | 按历史记录算平均周期，预测下次经期、排卵日、易孕窗口 |
| 日历视图 | 月历标记经期/排卵/易孕/下次经期，可翻月 |
| 历史记录 | 增删记录，显示每次持续天数 |
| 自动刷新 | WorkManager 每 30 分钟刷新；开机后自动恢复 |
| 隐私 | 无网络权限，数据仅存本地 Room 数据库 |

## 预测逻辑

平均周期取相邻两次经期开始日的间隔，**只采纳 21–45 天的区间**，把明显异常的长间隔（例如 91 天）排除，避免一次漏记就带偏后续所有预测。记录不足两条时使用 28 天默认值。

排卵日 = 上次经期开始日 + (平均周期 − 14)；易孕期 = 排卵日前 5 天至后 1 天。

## 在 GitHub Actions 上编译（无需本地环境）

1. 在 GitHub 新建仓库（**不要**勾选 README/.gitignore/license）
2. 把本目录下的文件上传到仓库（网页拖拽，或用 git）
3. 进 **Actions** 标签页，允许工作流运行
4. 绿色的 **Build Debug APK** 跑完 → 底部 **Artifacts** 下载 `app-debug-apk.zip`
5. 解压出 `app-debug.apk`，传到手机安装

首次编译约 3–5 分钟，之后走缓存会快很多。

## 本地编译

需要 JDK 17 与 Android SDK（platform 34、build-tools 34.0.0），然后：

```bash
gradle assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

`local.properties` 指向本机 SDK 路径，不应提交。

## 项目结构

```
period-widget-native/
├── .github/workflows/build.yml     CI：编译并上传 APK
├── app/src/main/
│   ├── java/com/example/periodwidget/
│   │   ├── CycleCalculator.kt      预测算法（纯函数，可单测）
│   │   ├── PeriodEntity.kt         Room 实体
│   │   ├── PeriodDao.kt            数据库接口
│   │   ├── PeriodDatabase.kt       Room 数据库
│   │   ├── PeriodRepository.kt     数据访问 + ViewModel
│   │   ├── PeriodWidgetProvider.kt 小组件宿主回调
│   │   ├── WidgetUpdateWorker.kt   后台读取并渲染小组件
│   │   ├── WidgetStyle.kt          状态 → 图标/颜色映射
│   │   ├── BootReceiver.kt         开机恢复刷新任务
│   │   └── MainActivity.kt         Compose 记录界面
│   └── res/                        小组件布局、图标、颜色、文案
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## 技术选型说明

- **AGP 8.5.2 + Gradle 8.9**：AGP 8.5 要求 Gradle ≥ 8.7，8.9 是验证过的组合。
- **WorkManager 承载刷新**：`AppWidgetProvider.onUpdate` 跑在主线程，不能读数据库，因此只负责调度，渲染放到 `WidgetUpdateWorker` 的 IO 线程。
- **日期存 ISO 字符串而非时间戳**：经期是「日历日」而非「时刻」，用文本存储可避免时区变化导致的日期偏移，删除也能按开始日精确匹配。
