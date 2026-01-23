# SpottedDog 需求文档

## 项目概述

Minecraft Fabric 模组，提供标记点管理和传送功能。主要面向多人服务器，完全兼容单人游戏。

## 核心功能

### 1. 本地标记点管理（无需服务端）

| 功能 | 命令 | 说明 |
|------|------|------|
| 添加标记点 | `/spot add <名称>` | 在当前位置保存为标记点 |
| 删除标记点 | `/spot remove <名称>` | 删除指定名称的标记点 |
| 更新位置 | `/spot update <名称>` | 将标记点位置更新到当前位置 |
| 重命名 | `/spot rename <旧名> <新名>` | 重命名标记点 |
| 传送 | `/spot teleport <名称>` | 传送到指定标记点 |
| 列表 | `/spot list` | 显示所有个人标记点 |

### 2. 快速传送（无需服务端）

| 功能 | 命令 | 说明 |
|------|------|------|
| 死亡点 | `/spot death` | 传送到上次死亡位置 |
| 重生点 | `/spot respawn` | 传送到重生点 |
| 世界出生点 | `/spot spawn` | 传送到世界出生点 |

**自动记录**：玩家死亡时自动记录死亡位置

### 3. 公开地点系统（需服务端）

#### 玩家功能

| 功能 | 命令 | 说明 |
|------|------|------|
| 同步公开地点 | `/spot public sync` | 从服务器获取公开地点列表 |
| 列出公开地点 | `/spot public list` | 显示所有公开地点 |
| 传送到公开地点 | `/spot public teleport <名称>` | 传送到公开地点 |
| 提交公开申请 | `/spot public request <名称>` | 申请将当前位置设为公开地点 |

#### 管理员功能

| 功能 | 命令 | 说明 |
|------|------|------|
| 查看待处理申请 | `/spot public admin requests` | 显示待处理申请数量 |
| 批准申请 | `/spot public admin approve <ID>` | 批准公开地点申请 |
| 拒绝申请 | `/spot public admin reject <ID>` | 拒绝公开地点申请 |
| 删除公开地点 | `/spot public admin remove <名称>` | 删除公开地点 |

**权限要求**：管理员命令需要 OP 2级 或 创造模式

## 数据存储

### 本地存储路径
`config/spotteddog/`

| 文件 | 内容 |
|------|------|
| `spots.json` | 个人标记点数据 |
| `death_point.json` | 死亡点记录 |
| `public_spots.json` | 同步的公开地点 |

### 数据格式

**标记点 (Spot)**
```json
{
  "id": "uuid",
  "name": "home",
  "x": 0.0,
  "y": 64.0,
  "z": 0.0,
  "dimension": "minecraft:overworld",
  "world": "Server"
}
```

**死亡点 (DeathPoint)**
```json
{
  "x": 0.0,
  "y": 64.0,
  "z": 0.0,
  "dimension": "minecraft:overworld",
  "world": "Server"
}
```

## 技术栈

- Minecraft: 1.21.11
- Java: 21
- Fabric Loader: 0.18.4
- Fabric API: 0.141.1+1.21.11
- 构建工具: Gradle + fabric-loom

## 命令结构

```
/spot
  ├── add <name>
  ├── remove <name>
  ├── update <name>
  ├── rename <oldName> <newName>
  ├── teleport <name>
  ├── death
  ├── respawn
  ├── spawn
  ├── list
  └── public
      ├── sync
      ├── list
      ├── teleport <name>
      ├── request <name>
      └── admin
          ├── requests
          ├── approve <id>
          ├── reject <id>
          └── remove <name>
```
