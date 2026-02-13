# SpottedDog

[![License: MIT](https://img.shields.io/badge/license-MIT-blue?style=flat-square)](https://opensource.org/licenses/MIT)
![Environment](https://img.shields.io/badge/Environment-Client%20%26%20Server-4caf50?style=flat-square)
![Static Badge](https://img.shields.io/badge/Minecraft-1.21.11-5395FD?style=flat-square)
![Static Badge](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square)

[![fabric](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/supported/fabric_vector.svg)](https://fabricmc.net/)

一个 Minecraft Fabric 模组，用于 Minecraft 1.21.11，提供标记点（路径点）管理和传送功能。支持单人/多人模式，自动记录死亡点、传送位置保存与恢复，公开标记点分享，以及完善的权限控制体系。

> **Spot**：指玩家保存的位置坐标，即 Minecraft 社区常用的 “Waypoint（路径点）”，这里为了说明和模组命令的风格考虑用更简短的“Spot”。

## 功能特性

- **标记点管理**：添加、删除、重命名和更新标记点（含坐标和朝向）
- **快速传送**：传送到保存的标记点或特殊目标（重生点、死亡点和世界出生点）
- **交互式 Spot 列表**：表格化显示，支持点击快捷操作
- **自动死亡记录**：玩家死亡时自动保存位置，一键返回死亡点
- **公开 Spot**（多人模式）：将 Spot 分享给其他玩家，支持查看和传送到他人公开的 Spot
- **权限管理**：基于 OP 级别的权限控制，支持由白名单控制每个功能的权限配置
- **传送日志审计**：服务端记录所有传送和管理操作
- **数据隔离**：按存档/服务器独立存储数据
- **本地化翻译**：根据游戏语言设置自动切换界面语言

## 安装方法

1. 确保已安装 [Fabric Loader](https://fabricmc.net/use/) 0.18.4 或更高版本
2. 下载本模组的 JAR 文件
3. 将 JAR 文件放入 `.minecraft/mods` 文件夹
4. 启动游戏

## 命令列表

虽然列出了许多命令，但在游戏中使用 `/spot list` 查看 Spot 列表时，大多数操作（T 传送 / R 删除 / U 更新 / E 重命名 / P 公开）都可以直接点击完成，无需手动输入命令。

| 命令 | 功能                  |
|------|---------------------|
| `/spot add <名称>` | 在当前位置添加标记点          |
| `/spot remove <名称>` | 删除指定标记点             |
| `/spot update <名称>` | 更新标记点到当前位置          |
| `/spot rename <旧名> <新名>` | 重命名标记点              |
| `/spot teleport <名称>` | 传送到标记点或特殊目标         |
| `/spot tp <名称>` | 传送到标记点（简写）          |
| `/spot list` | 列出所有保存的标记点（包含公开的）   |
| `/spot public <名称>` | 公开 Spot（仅多人模式）      |
| `/spot unpublic <名称>` | 取消公开 Spot（仅多人模式）    |
| `/spot log list [count]` | 查看传送日志（服务端）         |
| `/spot log clear` | 清除传送日志（服务端）         |
| `/spot whitelist teleport add\|remove\|list <玩家名>` | 传送白名单管理（OP）         |
| `/spot whitelist public add\|remove\|list <玩家名>` | 公开 Spot 白名单管理（OP）   |
| `/spot whitelist publictp add\|remove\|list <玩家名>` | 公开 Spot 传送白名单管理（OP） |

### 特殊传送目标

`/spot teleport` 和 `/spot tp` 命令支持以下特殊目标，在输入 `<名称>` 时可以通过输入 `.` 和 `-` 依靠自动补全进行筛选：

| 目标 | 功能 |
|------|------|
| `.death` | 传送到死亡点 |
| `.respawn` | 传送到重生点 |
| `.spawn` | 传送到世界出生点 |
| `-Spot名-玩家名` | 传送到公开 Spot（仅多人模式） |

**传送时的公开 Spot 全名格式**：`-<Spot名>-<玩家名>`，例如 `-home-stone_brick`

## Spot数据

### 存储结构

Spot 数据保存在 `spotteddog/data/` 目录下：

```
spotteddog/
├── data/
│   ├── singleplayer/
│   │   └── <存档文件夹名>/
│   │       └── spots.json              # 单人模式 Spot
│   │
│   └── multiplayer/
│       └── <服务器文件夹名>/
│           ├── spots.json              # 玩家自己的 Spot
│           ├── public_spots.json       # 公开 Spot（服务端）
│           ├── teleport_whitelist.json         # 传送白名单
│           ├── public_spot_whitelist.json      # 公开 Spot 白名单
│           └── public_spot_teleport_whitelist.json  # 公开 Spot 传送白名单
```

### 公开 Spot

公开 Spot 数据保存在服务端 `spotteddog/data/multiplayer/<服务器>/public_spots.json` 文件中。

## 配置

### 服务端配置

服务端配置文件：`spotteddog/config/spotteddog_config.json`
```json
{
  "teleport_cooldown_seconds": 1,
  "max_teleports_per_second": 10,
  "allow_all_players_teleport": false,
  "allow_all_players_public_spot": false,
  "allow_all_players_public_spot_teleport": false,
  "public_spot_cooldown_seconds": 5,
  "max_public_spot_requests_per_second": 10
}
```

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `teleport_cooldown_seconds` | 1 | 玩家个人传送冷却时间（秒） |
| `max_teleports_per_second` | 10 | 全局每秒最大传送请求数 |
| `allow_all_players_teleport` | false | 是否允许所有玩家使用传送功能 |
| `allow_all_players_public_spot` | false | 是否允许所有玩家公开/取消公开 Spot |
| `allow_all_players_public_spot_teleport` | false | 是否允许所有玩家传送到公开 Spot |
| `public_spot_cooldown_seconds` | 5 | 公开/取消公开 Spot 的玩家冷却时间（秒） |
| `max_public_spot_requests_per_second` | 10 | 全局每秒最大公开/取消公开请求数 |

## 权限管理

### 权限规则

权限检查优先级（从高到低）：
1. **OP 玩家**：始终拥有所有权限
2. **白名单玩家**：在对应白名单中则允许
3. **全局配置**：`allow_all_players_*` 为 true 则允许
4. **默认拒绝**：不满足以上条件则拒绝

### OP 限制

考虑到可能存在临时的OP，为防止权限泄漏，OP 无法修改自己或其他 OP 的白名单状态。

### 白名单管理

白名单提供细粒度的权限控制。当全局配置 `allow_all_players_*` 为 `false` 时，白名单中的玩家仍可使用对应功能。

白名单文件位于服务端 `spotteddog/data/multiplayer/<服务器>/` 目录：
- `teleport_whitelist.json` - 传送白名单
- `public_spot_whitelist.json` - 公开 Spot 白名单
- `public_spot_teleport_whitelist.json` - 公开 Spot 传送白名单

## 版本历史

| 版本 | 变更 |
|------|------|
| 5.4.1 | 公开 Spot 列表排序优化、Spot 操作后自动刷新列表 |
| 5.4.0-SNAPSHOT | 传送日志审计功能、事件系统重构 |
| 5.3.0-SNAPSHOT | 提取表格构建为 SpotTableBuilder、合并公开 Spot 列表到 /spot list、添加交互式操作列 |
| 5.1.0-SNAPSHOT | 白名单权限管理功能、OP 自我保护限制 |
| 5.0.0-SNAPSHOT | 权限管理、独立的公开 Spot 冷却、传送到公开 Spot 权限 |
| 4.3.0-SNAPSHOT | 中文支持、列表预加载、服务端冷却限制 |
| 4.2.0-SNAPSHOT | 数据结构重构、公开 Spot 同步更新、移除 isPublic 字段 |
| 4.1.0-SNAPSHOT | 公开 Spot 功能、移除成功提示消息 |
| 4.0.0-SNAPSHOT | 世界出生点修复、服务端安全优化、单人模式消息优化 |

---

## 开发者指南

### 项目结构

```
src/
├── main/
│   └── java/io/github/stone_brick/spotteddog/
│       ├── Spotteddog.java              # 主模组入口
│       ├── event/                       # 事件系统
│       │   └── AdminLogEvent.java       # 管理日志事件
│       ├── network/
│       │   ├── TeleportType.java        # 传送类型枚举
│       │   ├── c2s/                     # 客户端到服务端负载
│       │   │   ├── TeleportRequestC2SPayload.java
│       │   │   ├── PublicSpotActionC2SPayload.java
│       │   │   ├── PublicSpotListC2SPayload.java
│       │   │   ├── PublicSpotTeleportC2SPayload.java
│       │   │   ├── PublicSpotUpdateC2SPayload.java
│       │   │   └── WhitelistAdminC2SPayload.java
│       │   └── s2c/                     # 服务端到客户端负载
│       │       ├── TeleportConfirmS2CPayload.java
│       │       └── PublicSpotListS2CPayload.java
│       └── server/
│           ├── config/
│           │   ├── ConfigManager.java    # 配置文件管理
│           │   └── CooldownManager.java  # 冷却时间管理
│           ├── permission/
│           │   ├── PermissionManager.java # 权限管理
│           │   └── WhitelistManager.java # 白名单管理
│           ├── data/
│           │   ├── PublicSpot.java      # 公开 Spot 数据模型
│           │   └── PublicSpotManager.java # 公开 Spot 存储管理
│           └── network/
│               ├── TeleportRequestHandler.java  # 服务端传送请求处理
│               ├── PublicSpotHandler.java       # 公开 Spot 请求处理
│               ├── WhitelistAdminHandler.java   # 白名单管理请求处理
│               └── TeleportLogHandler.java     # 传送日志处理
└── client/
    └── java/io/github/stone_brick/spotteddog/client/
        ├── SpotteddogClient.java        # 客户端入口
        ├── command/
        │   ├── SpotCommand.java         # 命令实现
        │   ├── WhitelistAdminCommand.java # 白名单管理命令
        │   ├── TeleportHandler.java     # 传送处理入口
        │   ├── TeleportStrategy.java    # 传送策略接口
        │   ├── SingleplayerTeleportStrategy.java
        │   └── MultiplayerTeleportStrategy.java
        ├── data/
        │   ├── PlayerDataManager.java   # 数据管理
        │   └── Spot.java               # 标记点数据模型
        ├── ui/
        │   └── SpotTableBuilder.java   # 表格构建器
        └── network/
            ├── TeleportConfirmHandler.java   # 客户端传送确认处理
            ├── PublicSpotListHandler.java    # 公开 Spot 列表处理
            └── WhitelistAdminHandler.java   # 白名单管理请求发送
```

### 技术栈

- **Minecraft**: 1.21.11
- **Java**: 21
- **Fabric Loader**: 0.18.4+
- **Fabric API**: 0.141.1+1.21.11
- **构建工具**: Gradle (fabric-loom 插件)