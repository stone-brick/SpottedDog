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
| `/spot teleport <名称>` | 传送 (支持 spot 名称或 death/respawn/spawn) |
| `/spot tp <名称>` | 传送 (简写) |
| `/spot list` | 列出当前存档/服务器的标记点 |

## 开发规范

详细规范请查看 Skills：

- **Fabric 模组开发** → `.claude/skills/Fabric模组开发规范/SKILL.md`
- **Java 开发规范** → `.claude/skills/Java开发规范/SKILL.md`
- **本项目开发规范** → `.claude/skills/本项目的开发规范/SKILL.md`
- **Git 提交规范** → `.claude/skills/开发中的Git相关规范/SKILL.md`
- **Skill 管理方式** → `.claude/skills/skill管理方式/SKILL.md`

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
