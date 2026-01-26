# SpottedDog

一个 Minecraft Fabric 模组，用于 Minecraft 1.21.11，提供标记点管理和传送功能。

## 功能特性

- **标记点管理**：添加、删除、重命名和更新标记点位置（含朝向）
- **快速传送**：一键传送到保存的标记点或特殊位置（死亡点、重生点、世界出生点）
- **智能策略模式**：根据游戏模式（单人/多人）自动选择最佳传送方式
- **自动死亡记录**：自动记录玩家死亡位置
- **朝向保存与恢复**：传送时保持玩家当前朝向（特殊目标）或恢复保存的朝向（自定义标记点）
- **服务端安全优化**：传送冷却时间、全局速率限制、公开列表请求冷却
- **公开 Spot**（多人模式）：将你的 Spot 公开给其他玩家，其他玩家可直接传送到公开 Spot
- **简洁消息**：传送成功无打扰提示，失败时显示错误信息

## 安装方法

1. 确保已安装 [Fabric Loader](https://fabricmc.net/use/) 0.18.4 或更高版本
2. 下载本模组的 JAR 文件
3. 将 JAR 文件放入 `.minecraft/mods` 文件夹
4. 启动游戏

## 命令列表

| 命令 | 功能 |
|------|------|
| `/spot add <名称>` | 在当前位置添加标记点 |
| `/spot remove <名称>` | 删除指定标记点 |
| `/spot update <名称>` | 更新标记点到当前位置 |
| `/spot rename <旧名> <新名>` | 重命名标记点 |
| `/spot teleport <名称>` | 传送到标记点或特殊目标 |
| `/spot tp <名称>` | 传送到标记点（简写） |
| `/spot list` | 列出所有保存的标记点 |
| `/spot public <名称>` | 公开 Spot（仅多人模式） |
| `/spot unpublic <名称>` | 取消公开 Spot（仅多人模式） |
| `/spot public list` | 列出公开的 Spot（仅多人模式） |

### 特殊传送目标

`/spot teleport` 命令支持以下特殊目标：

- `.death` - 传送到死亡点
- `.respawn` - 传送到重生点
- `.spawn` - 传送到世界出生点
- `-Spot名-玩家名` - 传送到公开 Spot（仅多人模式）

**公开 Spot 示例**：`-home-stone_brick`

## 构建方法

```bash
# 构建模组
./gradlew build

# 运行客户端测试
./gradlew runClient

# 生成数据（配方、战利品表、标签等）
./gradlew generateData

# 清理构建产物
./gradlew clean

# 发布到本地 Maven
./gradlew publishToMavenLocal
```

## 项目结构

```
src/
├── main/
│   └── java/io/github/stone_brick/spotteddog/
│       ├── Spotteddog.java              # 主模组入口
│       ├── network/
│       │   ├── TeleportType.java        # 传送类型枚举
│       │   ├── c2s/                     # 客户端到服务端负载
│       │   │   ├── TeleportRequestC2SPayload.java
│       │   │   ├── PublicSpotActionC2SPayload.java
│       │   │   ├── PublicSpotListC2SPayload.java
│       │   │   ├── PublicSpotTeleportC2SPayload.java
│       │   │   └── PublicSpotUpdateC2SPayload.java
│       │   └── s2c/                     # 服务端到客户端负载
│       │       ├── TeleportConfirmS2CPayload.java
│       │       └── PublicSpotListS2CPayload.java
│       └── server/
│           ├── config/
│           │   ├── ConfigManager.java   # 配置文件管理
│           │   └── CooldownManager.java # 冷却时间管理
│           ├── data/
│           │   ├── PublicSpot.java      # 公开 Spot 数据模型
│           │   └── PublicSpotManager.java # 公开 Spot 存储管理
│           └── network/
│               ├── TeleportRequestHandler.java  # 服务端传送请求处理
│               └── PublicSpotHandler.java       # 公开 Spot 请求处理
└── client/
    └── java/io/github/stone_brick/spotteddog/client/
        ├── SpotteddogClient.java        # 客户端入口
        ├── command/
        │   ├── SpotCommand.java         # 命令实现
        │   ├── TeleportHandler.java     # 传送处理入口
        │   ├── TeleportStrategy.java    # 传送策略接口
        │   ├── SingleplayerTeleportStrategy.java
        │   └── MultiplayerTeleportStrategy.java
        ├── data/
        │   ├── PlayerDataManager.java   # 数据管理
        │   └── Spot.java                # 标记点数据模型
        └── network/
            ├── TeleportConfirmHandler.java   # 客户端传送确认处理
            └── PublicSpotListHandler.java    # 公开 Spot 列表处理
```

## 技术栈

- **Minecraft**: 1.21.11
- **Java**: 21
- **Fabric Loader**: 0.18.4+
- **Fabric API**: 0.141.1+1.21.11
- **构建工具**: Gradle (fabric-loom 插件)

## 配置

### 标记点数据

标记点数据保存在 `config/spotteddog/` 目录下：
- `spots_singleplayer.json` - 单人模式存档的 Spot
- `spots_multiplayer.json` - 多人模式服务器的 Spot

### 服务端配置

服务端配置文件：`config/spotteddog/spotteddog_config.json`
```json
{
  "teleport_cooldown_seconds": 1,
  "max_teleports_per_second": 10
}
```
| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `teleport_cooldown_seconds` | 1 | 玩家个人冷却时间（秒） |
| `max_teleports_per_second` | 10 | 全局每秒最大传送请求数 |

### 公开 Spot 数据

公开 Spot 数据保存在服务端 `config/spotteddog/public_spots.json` 文件中。

## 版本历史

| 版本 | 变更 |
|------|------|
| 4.3-SNAPSHOT | 中文支持、列表预加载、服务端冷却限制 |
| 4.2-SNAPSHOT | 数据结构重构、公开 Spot 同步更新、移除 isPublic 字段 |
| 4.1-SNAPSHOT | 公开 Spot 功能、移除成功提示消息 |
| 4.0-SNAPSHOT | 世界出生点修复、服务端安全优化、单人模式消息优化 |
| 3.2 | 特殊目标 . 前缀实现 |
| 3.1 | 朝向保存与恢复 |
| 3.0 | 存档/服务器隔离存储 |

## 许可证

All Rights Reserved
