# MediaStore Viewer

一款基于 **Jetpack Compose** 的 Android 媒体文件浏览器，用于查看设备 **MediaStore** 中的图片与视频，支持按目录浏览、OpenGL 照片墙、全屏媒体预览以及字段级详情查看。

## 功能

- **目录浏览** — 按存储桶（Bucket）自动分组，特殊分组（全部/相机/视频/收藏/Raw）置顶，剩余相册按字母排列
- **无感刷新** — 页面可见时自动刷新数据，已有数据时静默更新，无需手动操作
- **OpenGL 照片墙** — 基于 `GLSurfaceView` 的高性能网格渲染，支持惯性滚动（fling），纹理 LRU 缓存，加载渐进式占位
- **全屏媒体预览** — 图片：Coil 加载 + 双指缩放/双击放大；视频：ExoPlayer 播放；单击可切换沉浸模式（隐藏标题栏/状态栏/导航栏）
- **字段详情** — 查看任一媒体文件的完整 ContentProvider 列值，适合调试和开发者使用，支持字段搜索
- **权限适配** — Android 13+ 使用细粒度权限 (`READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO`)
- **Material You** — Android 12+ 动态取色，同时提供深色/浅色主题
- **边缘到边缘** — 全面屏适配，状态栏透明沉浸

## 技术栈

| 类别 | 库 / 工具 |
| --- | --- |
| UI | Jetpack Compose + Material 3 |
| 导航 | Navigation Compose |
| 架构 | ViewModel + StateFlow |
| 图片加载 | Coil |
| 视频播放 | Media3 ExoPlayer |
| OpenGL | GLSurfaceView + GLES 2.0 (自定义渲染器) |
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
│   ├── glview/
│   │   ├── GLPhotoWall.kt        # Compose 桥接 — AndroidView + 异步纹理加载
│   │   ├── GLPhotoWallRenderer.kt # GLSurfaceView.Renderer — 视口裁剪、滚动、点击/拖拽
│   │   ├── GLTextureManager.kt    # 线程安全 LRU 纹理缓存（max 64）
│   │   ├── GLThumbnailLoader.kt   # ContentResolver.loadThumbnail 封装
│   │   └── GLShaders.kt           # 顶点/片段着色器 + createProgram
│   ├── screens/
│   │   ├── DirectoryListScreen.kt # 目录列表页（3 列 LazyVerticalGrid）
│   │   ├── PhotoWallScreen.kt     # OpenGL 照片墙页（按目录筛选）
│   │   ├── MediaViewerScreen.kt   # 图片/视频全屏预览页
│   │   └── FileDetailScreen.kt    # 字段详情页（含搜索过滤）
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
- **Android SDK 36**（需要 API Level 35 及以上的设备或模拟器）

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
目录列表 → 照片墙（按目录筛选） → 全屏预览（图片/视频）
                                     ↓
                                  字段详情
```

## 许可证

待定
