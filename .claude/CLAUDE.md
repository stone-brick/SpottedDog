# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 提供在此仓库中工作的指导。

## 项目概述

SpottedDog 是一个 Minecraft Fabric 模组，用于 Minecraft 1.21.11，使用 Java 21。提供标记点管理和传送功能。

## 架构

- `src/main/java` - 通用代码（客户端和服务端都可运行）
- `src/client/java` - 客户端专用代码（命令、渲染等）

## 主要命令

| 命令 | 功能 |
|------|------|
| `/spot add <名称>` | 添加标记点 |
| `/spot remove <名称>` | 删除标记点 |
| `/spot update <名称>` | 更新位置 |
| `/spot rename <旧名> <新名>` | 重命名 |
| `/spot teleport <名称>` | 传送 (支持 spot 名称或 .death/.respawn/.spawn) |
| `/spot tp <名称>` | 传送 (简写) |
| `/spot list` | 列出当前存档/服务器的标记点 |

## 开发规范

详细规范请查看 Skills：

| Skill | 说明 |
|-------|------|
| **本项目开发规范** | 项目特定开发约定、设计原则、代码组织规则 |
| **Fabric 模组开发规范** | Fabric模组开发、入口点配置、源集架构、Minecraft源码参考 |
| **Java 开发规范** | Java代码风格、命名规范、注释要求 |
| **Git 提交规范** | Git提交规范、推送策略、构建检查 |
| **Skill 管理方式** | skill文件的增删改操作指南 |

**技能关联**：
- 编写本项目代码 → 先看「本项目开发规范」
- 涉及 Fabric 开发 → 参考「Fabric 模组开发规范」
- 编写代码风格 → 参考「Java 开发规范」

> **提示**：随着项目发展，可扩展或修改 Skill 文件以更新规范。详见 `skill管理方式/SKILL.md`

## Minecraft 源码参考

Minecraft 源码已移动到 `Fabric模组开发规范` skill 目录下：
- `.claude/skills/Fabric模组开发规范/minecraft-clientOnly/` - 客户端专用源码
- `.claude/skills/Fabric模组开发规范/minecraft-common/` - 客户端/服务端通用源码


## 项目进度

- **已完成**：客户端单人模式功能（标记点 CRUD、传送、死亡点记录）
- **已完成**：客户端多人模式功能（网络传送、spawn/respawn/death/spot）
- **已完成**：服务端传送请求处理
- **已完成**：存档/服务器隔离存储（不同存档和服务器数据分离管理）
- **已完成**：朝向保存与恢复（添加/更新时保存 yaw/pitch，传送时应用）
- **已完成**：特殊目标使用 . 前缀（.death/.respawn/.spawn），避免与用户 spot 名称冲突
- **已完成**：使用正确的世界出生点获取方式（`server.getSpawnPoint()`）
- **已完成**：服务端安全优化（传送冷却时间、配置文件）
- **已完成**：公开 Spot 功能（多人模式下可将 Spot 公开给其他玩家）

## 网络架构

多人模式下采用 C2S/S2C 网络通信模式：
- `TeleportRequestC2SPayload` - 客户端发送传送请求（类型、目标名、坐标、维度）
- `TeleportConfirmS2CPayload` - 服务端返回传送结果（成功/失败、消息）
- `TeleportRequestHandler` - 服务端处理传送请求，执行实际传送
- `TeleportConfirmHandler` - 客户端处理传送确认，显示结果消息

**位置数据获取策略**：
- `spawn`/`respawn`/`death`：服务端从 ServerPlayerEntity 获取
- `spot`：客户端发送坐标，服务端执行传送

## 存档隔离实现经验

**问题**：玩家可能给不同存档命名相同名称，导致无法区分。

**解决方案**：使用存档文件夹名称而非显示名称来区分：
```java
// 正确获取存档文件夹名称
Path worldPath = server.getSavePath(WorldSavePath.ROOT).getParent();
String worldDir = worldPath.getFileName().toString();
// worldIdentifier 格式: "singleplayer:<存档文件夹名>"
```

**注意**：`WorldSavePath.ROOT` 返回 `.`，需要 `getParent()` 获取存档文件夹路径。

**数据隔离策略**：
- `worldIdentifier` 字段标识数据归属
- 单人模式：`singleplayer:<存档文件夹名>`
- 多人模式：`multiplayer:<服务器地址>`
- 使用 `Objects.equals()` 处理旧数据 `worldIdentifier` 为 null 的情况

## 朝向保存与恢复实现

**数据模型**：
- Spot 添加 `yaw` 和 `pitch` 字段（float 类型）
- 添加/更新时保存 `player.getYaw()` 和 `player.getPitch()`

**传送时朝向策略**：
| 类型 | 坐标偏移 | 朝向来源 |
|------|----------|----------|
| `spot` | 无偏移 | 保存的 yaw/pitch |
| `spawn`/`respawn`/`death` | +0.5 | 玩家当前朝向 |

**实现要点**：
- 特殊目标使用服务端 `player.getYaw()` 获取玩家当前朝向
- spot 使用客户端发送的保存朝向
- 服务端根据 type 判断使用哪种朝向来源

## 特殊目标 . 前缀实现

**问题**：玩家自定义 spot 名称可能与特殊目标（death/respawn/spawn）冲突。

**解决方案**：特殊目标使用 `.` 前缀与用户 spot 区分：
- 特殊目标：`.death`、`.respawn`、`.spawn`
- 用户 spot：不允许以 `.` 开头

**实现要点**：
- 自动补全建议时特殊目标添加 `.` 前缀
- `teleport` 方法匹配 `.death` 等带点形式
- `addSpot`/`renameSpot` 时拒绝以 `.` 开头的名称

**效果**：
| 命令 | 结果 |
|------|------|
| `/spot tp .death` | 传送到死亡点 |
| `/spot tp death` | 传送到用户 spot "death" |
| `/spot add .home` | 拒绝（提示不能以 '.' 开头） |

## 世界出生点获取实现

**问题**：使用 `getSpawnPoint().getPos()` 可能返回默认位置 (0, 64, 0)，而不是世界创建时确定的实际出生点。

**解决方案**：使用 `MinecraftServer.getSpawnPoint()` 获取正确的出生点坐标：

```java
// 单人模式和服务端通用
BlockPos spawnPos = server.getSpawnPoint().getPos();
double targetX = spawnPos.getX() + 0.5;
double targetY = spawnPos.getY();
double targetZ = spawnPos.getZ() + 0.5;
```

**实现要点**：
- `MinecraftServer.getSpawnPoint()` 返回 `WorldProperties.SpawnPoint`，包含实际的世界出生点坐标
- 该方法在 `MinecraftServer` 类中是 public 的，可直接调用
- 适用于单人模式（`IntegratedServer`）和多人模式（`DedicatedServer`）

## 服务端安全优化

### 传送冷却时间

**问题**：玩家可能频繁发送传送请求，造成服务端压力或滥用。

**解决方案**：使用 `CooldownManager` 跟踪每个玩家的最后传送时间：

```java
// 检查是否在冷却中
if (CooldownManager.isInCooldown(player)) {
    int remaining = CooldownManager.getRemainingCooldown(player);
    // 返回冷却中消息
}

// 传送成功后更新冷却时间
CooldownManager.updateLastTeleport(player);
```

**冷却时间配置**：通过 `config/spotteddog/spotteddog_config.json` 管理：
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

**配置管理**：
- 配置文件路径：`config/spotteddog/spotteddog_config.json`
- 首次使用自动创建默认值
- 支持热重载（重新调用 `ConfigManager.loadOrCreate()`）

### 配置管理类

| 类 | 职责 |
|---|------|
| `ConfigManager` | 配置文件读写，默认值创建 |
| `CooldownManager` | 玩家冷却时间跟踪，全局速率限制 |

## 单人模式消息优化

**问题**：单人模式传送后没有成功提示消息。

**解决方案**：在 `SingleplayerTeleportStrategy` 各传送方法中添加成功消息：

```java
// 传送成功后显示消息
player.sendMessage(net.minecraft.text.Text.literal("[SpottedDog] 已传送到 xxx"), false);
```

**消息列表**：
| 类型 | 消息 |
|------|------|
| spot | "[SpottedDog] 已传送到标记点: xxx" |
| spawn | "[SpottedDog] 已传送到出生点" |
| death | "[SpottedDog] 已传送到死亡点" |
| respawn | "[SpottedDog] 已传送到重生点" |

## 版本历史

| 版本 | 变更 |
|------|------|
| 4.0-SNAPSHOT | 世界出生点修复、服务端安全优化、单人模式消息优化、公开 Spot 功能 |
| 3.2 | 特殊目标 . 前缀实现 |
| 3.1 | 朝向保存与恢复 |
| 3.0 | 存档/服务器隔离存储 |

## 公开 Spot 功能

**功能概述**：在多人模式下，玩家可以将自己的 Spot 公开给其他玩家，其他玩家可以直接传送到公开的 Spot。

### 命名规则

公开 Spot 使用 `-` 前缀，格式为：`-Spot名-玩家名`
- 示例：`-home-stone_brick`

**规则**：
- 玩家本地 Spot 不能以 `-` 开头（与特殊目标一致）
- 不允许重复的 Spot 名称（同一世界内）
- 公开 Spot 仅在多人模式下可用

### 命令列表

| 命令 | 功能 |
|------|------|
| `/spot public <名称>` | 公开当前世界的指定 Spot |
| `/spot unpublic <名称>` | 取消公开指定的 Spot |
| `/spot public list` | 列出当前世界所有公开的 Spot |
| `/spot tp -Spot名-玩家名` | 传送到其他玩家公开的 Spot |

### 数据存储

**服务端配置文件**：`config/spotteddog/public_spots.json`

```json
[
  {
    "id": "uuid",
    "owner_name": "玩家名",
    "owner_uuid": "玩家UUID",
    "display_name": "Spot名称",
    "x": 0.0,
    "y": 64.0,
    "z": 0.0,
    "yaw": 0.0,
    "pitch": 0.0,
    "dimension": "minecraft:overworld",
    "world": "Server",
    "world_identifier": "multiplayer:服务器地址",
    "created_at": 1234567890
  }
]
```

### 网络通信

| Payload | 方向 | 用途 |
|---------|------|------|
| `PublicSpotActionC2SPayload` | C2S | 公开/取消公开 Spot 请求 |
| `PublicSpotListC2SPayload` | C2S | 获取公开 Spot 列表请求 |
| `PublicSpotListS2CPayload` | S2C | 返回公开 Spot 列表 |
| `PublicSpotTeleportC2SPayload` | C2S | 传送到公开 Spot 请求 |

### 核心类

| 类 | 职责 |
|---|------|
| `PublicSpot` | 公开 Spot 数据模型 |
| `PublicSpotManager` | 服务端公开 Spot 存储管理 |
| `PublicSpotHandler` | 服务端网络请求处理 |
| `PublicSpotListHandler` | 客户端公开 Spot 列表处理 |
