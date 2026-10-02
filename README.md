# Period Widget - 原生安卓桌面小组件

记录经期、预测周期/排卵日/易孕期，支持桌面小组件显示今日状态。

## 功能

- 📅 **桌面小组件**：多尺寸可调，显示今日状态、周期信息、下次经期预测
- 🩸 **经期记录**：添加/删除/清空经期记录
- 🔮 **智能预测**：基于历史数据计算平均周期、排卵日、易孕窗口
- 📊 **日历视图**：月历标记经期/排卵/易孕/下次经期
- 🔔 **后台更新**：WorkManager 每 15 分钟刷新小组件
- 🔐 **本地存储**：Room 数据库，数据不上传

## GitHub Actions 自动构建 APK

### 使用方法

1. **创建 GitHub 仓库**
   - 进 github.com → New repository → 名称 `period-widget` → Public/Private 均可
   - **不要**勾选 "Add a README file" / ".gitignore" / "license"

2. **推送代码**
   ```bash
   cd D:\lqbz\period-widget-native
   git init
   git add .
   git commit -m "Initial commit: Period Widget native app"
   git branch -M main
   git remote add origin https://github.com/<您的用户名>/period-widget.git
   git push -u origin main
   ```

3. **等待构建**
   - 进仓库 → **Actions** 标签页
   - 看到 "Build Debug APK" 工作流在跑（约 2-5 分钟）
   - 绿色 ✅ 表示成功

4. **下载 APK**
   - 点击成功的 workflow run
   - 底部 **Artifacts** → 下载 `app-debug-apk.zip`
   - 解压得到 `app-debug.apk`
   - 传手机安装（微信发给自己 / adb install）

### 手动触发构建
Actions 页面 → 左侧 "Build Debug APK" → **Run workflow** → `main` → `Run workflow`

## 本地开发（可选）

```bash
# 需要：Android SDK (API 34)、JDK 17、Gradle 9.6+
cd period-widget-native
./gradlew assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

## 项目结构

```
period-widget-native/
├── .github/workflows/build.yml    # GitHub Actions 构建配置
├── app/
│   ├── src/main/
│   │   ├── java/com/example/periodwidget/
│   │   │   ├── PeriodWidgetProvider.kt    # 小组件核心
│   │   │   ├── WidgetUpdateService.kt     # 后台更新
│   │   │   ├── CycleCalculator.kt         # 预测算法
│   │   │   ├── PeriodRepository.kt        # 数据层
│   │   │   ├── PeriodDatabase.kt          # Room 数据库
│   │   │   └── MainActivity.kt            # Compose UI
│   │   ├── res/layout/widget_layout.xml   # 小组件布局
│   │   ├── res/xml/widget_info.xml        # 小组件元数据
│   │   └── res/values/colors.xml          # 颜色
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── gradle/wrapper/gradle-wrapper.properties
```

## 隐私说明

- ✅ 完全离线，无网络权限
- ✅ 数据仅存本地 SQLite (Room)
- ✅ 无分析、无上报、无广告
- ✅ GitHub Actions 只编译代码，**不接触任何用户数据**

## 许可证

MIT License - 随意用、改、分发