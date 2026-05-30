# MediaStore Viewer

一款基于 **Jetpack Compose** 的 Android 媒体文件浏览器，用于查看设备上 **MediaStore** 中所有的图片与视频，并支持按目录浏览、收藏筛选、字段级详情查看以及全屏媒体预览。

## 功能

- **目录浏览** — 按存储桶（Bucket）自动分组展示所有媒体文件，同时提供"全部"和"收藏"两个虚拟分组
- **文件列表** — 浏览每个目录下的图片和视频，支持搜索过滤
- **字段详情** — 查看任一媒体文件的 **完整 ContentProvider 字段**（适合调试和开发者使用），字段名和值均可搜索
- **媒体预览** — 图片支持双指缩放和双击放大/还原；视频基于 **ExoPlayer** 播放，带系统控制器
- **权限适配** — Android 13 (Tiramisu) 及以上使用细粒度媒体权限 (`READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO`)；低版本使用 `READ_EXTERNAL_STORAGE`
- **动态取色** — Android 12+ 支持 Material You 动态颜色，同时提供深色/浅色主题
- **边缘到边缘** — 适配全面屏，状态栏透明沉浸

## 技术栈

| 类别 | 库 / 工具 |
| --- | --- |
| UI | Jetpack Compose + Material 3 |
| 导航 | Navigation Compose |
| 架构 | ViewModel + StateFlow |
| 图片加载 | Coil |
| 视频播放 | Media3 ExoPlayer |
| 构建 | Gradle Kotlin DSL + Version Catalog |
| 语言 | Kotlin 2.2 |

## 项目结构

```
app/src/main/java/com/senk/mediastoreviewer/
├── data/
│   ├── MediaItem.kt          # 媒体文件数据模型
│   └── MediaRepository.kt    # ContentResolver 查询封装
├── navigation/
│   └── AppNavigation.kt      # 路由定义与 NavHost
├── ui/
│   ├── screens/
│   │   ├── DirectoryListScreen.kt  # 目录列表页
│   │   ├── FileListScreen.kt       # 文件列表页
│   │   ├── FileDetailScreen.kt     # 字段详情页
│   │   └── MediaViewerScreen.kt    # 图片/视频全屏预览页
│   └── theme/
│       ├── Color.kt          # 调色板
│       ├── Theme.kt          # 主题配置
│       └── Type.kt           # 字体样式
├── viewmodel/
│   └── MediaViewModel.kt     # 状态管理
└── MainActivity.kt           # 入口 Activity + 权限请求
```

## 构建运行

### 前提条件

- **Android Studio**（最新稳定版）
- **JDK 11+**
- **Android SDK 36**（带 API Level 35 及以上的设备或模拟器）

### 步骤

```bash
# 克隆项目
git clone <仓库地址>
cd mediastoreviewer

# 使用 Gradle Wrapper 构建
./gradlew assembleDebug

# 安装到设备
./gradlew installDebug
```

或在 Android Studio 中直接打开项目，同步 Gradle 后运行。

### 运行环境要求

- **最低 SDK**: 35 (Android 15)
- **目标 SDK**: 36
- 需要授予"照片和视频"权限

## 导航流程

```
目录列表 → 文件列表 → 字段详情
                ↘       → 全屏预览
```

## 许可证

待定
