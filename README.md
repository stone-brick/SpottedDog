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
- **白名单权限**（OP）：细粒度控制普通玩家的功能权限
- **本地化支持**：支持中文和英文，根据游戏语言设置自动切换
- **简洁消息**：传送成功无打扰提示，失败时显示错误信息
- **传送日志审计**（服务端）：记录所有传送和管理操作，禁止清除日志以保证审计完整性

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
| `/spot list` | 列出所有保存的标记点（包含公开 Spot） |
| `/spot public <名称>` | 公开 Spot（仅多人模式） |
| `/spot unpublic <名称>` | 取消公开 Spot（仅多人模式） |
| `/spot log list [count]` | 查看传送日志（服务端） |
| `/spot log clear` | 清除传送日志（服务端） |
| `/spot whitelist teleport add\|remove\|list <玩家名>` | 传送白名单管理（OP） |
| `/spot whitelist public add\|remove\|list <玩家名>` | 公开 Spot 白名单管理（OP） |
| `/spot whitelist publictp add\|remove\|list <玩家名>` | 公开 Spot 传送白名单管理（OP） |

### 特殊传送目标

`/spot teleport` 和 `/spot tp` 命令支持以下特殊目标：

| 目标 | 功能 |
|------|------|
| `.death` | 传送到死亡点 |
| `.respawn` | 传送到重生点 |
| `.spawn` | 传送到世界出生点 |
| `-Spot名-玩家名` | 传送到公开 Spot（仅多人模式） |

**公开 Spot 命名格式**：`-<Spot名>-<玩家名>`，例如 `-home-stone_brick`

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

### 标记点数据

标记点数据保存在 `spotteddog/data/` 目录下：

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

### 公开 Spot 数据

公开 Spot 数据保存在服务端 `spotteddog/data/multiplayer/<服务器>/public_spots.json` 文件中。

## 权限管理

### 权限规则

权限检查优先级（从高到低）：
1. **OP 玩家**：始终拥有所有权限
2. **白名单玩家**：在对应白名单中则允许
3. **全局配置**：`allow_all_players_*` 为 true 则允许
4. **默认拒绝**：不满足以上条件则拒绝

### OP 限制

OP 无法修改自己或其他 OP 的白名单状态。

### 白名单管理

白名单提供细粒度的权限控制。当全局配置 `allow_all_players_*` 为 `false` 时，白名单中的玩家仍可使用对应功能。

白名单文件位于服务端 `spotteddog/data/multiplayer/<服务器>/` 目录：
- `teleport_whitelist.json` - 传送白名单
- `public_spot_whitelist.json` - 公开 Spot 白名单
- `public_spot_teleport_whitelist.json` - 公开 Spot 传送白名单

## 版本历史

| 版本 | 变更 |
|------|------|
| 5.4.1 | 公开 Spot 列表排序优化（私有 Spot → 自己公开的 Spot → 其他玩家公开的 Spot）、Spot 操作后自动刷新列表 |
| 5.5.0-SNAPSHOT | 传送日志审计功能、事件系统重构 |
| 5.3.0-SNAPSHOT | 提取表格构建为 SpotTableBuilder、合并公开 Spot 列表到 /spot list、添加交互式操作列 |
| 5.1.0-SNAPSHOT | 白名单权限管理功能、OP 自我保护限制 |
| 5.0.0-SNAPSHOT | 权限管理、独立的公开 Spot 冷却、传送到公开 Spot 权限 |
| 4.3.0-SNAPSHOT | 中文支持、列表预加载、服务端冷却限制 |
| 4.2.0-SNAPSHOT | 数据结构重构、公开 Spot 同步更新、移除 isPublic 字段 |
| 4.1.0-SNAPSHOT | 公开 Spot 功能、移除成功提示消息 |
| 4.0.0-SNAPSHOT | 世界出生点修复、服务端安全优化、单人模式消息优化 |

---

## 开发者指南

### 构建方法

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

## 许可证

MIT License

Copyright (c) 2026 stone_brick

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRING. NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
