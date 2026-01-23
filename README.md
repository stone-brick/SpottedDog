# SpottedDog

![Minecraft Version](https://img.shields.io/badge/Minecraft-1.21.11-yellow)
![Fabric API](https://img.shields.io/badge/Fabric_API-0.141.1-blue)
![License](https://img.shields.io/badge/License-MIT-green)

SpottedDog 是一个 Minecraft Fabric 模组，提供标记点（waypoint）管理和传送功能。

## 功能特性

### 标记点管理

| 命令 | 功能 | 示例 |
|------|------|------|
| `/spot add <名称>` | 在当前位置保存标记点 | `/spot add home` |
| `/spot remove <名称>` | 删除标记点 | `/spot remove home` |
| `/spot update <名称>` | 更新标记点位置 | `/spot update home` |
| `/spot rename <旧名> <新名>` | 重命名标记点 | `/spot rename home mybase` |
| `/spot list` | 列出所有标记点 | `/spot list` |

### 传送功能

| 命令 | 功能 |
|------|------|
| `/spot teleport <名称>` | 传送到保存的标记点 |
| `/spot teleport death` | 传送到死亡点 |
| `/spot teleport respawn` | 传送到重生点 |
| `/spot teleport spawn` | 传送到世界出生点 |

> **提示**：使用 Tab 键可以自动补全标记点名称和特殊目标（death/respawn/spawn）。

## 安装方法

1. 确保已安装 [Fabric Loader](https://fabricmc.net/use/) 和 [Fabric API](https://modrinth.com/mod/fabric-api)
2. 下载最新版本的 `SpottedDog-*.jar`
3. 将模组文件放入 `.minecraft/mods/` 文件夹
4. 启动游戏

## 配置文件

标记点数据保存在：
```
world/spotteddog_spots.json
```

## 开发

### 环境要求

- Java 21
- Gradle 9.x
- Fabric Loom 1.15.2+

### 构建命令

```bash
# 构建模组（输出到 build/libs/）
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
├── main/java/io/github/stone_brick/spotteddog/
│   └── Spotteddog.java          # 主入口
├── client/java/io/github/stone_brick/spotteddog/
│   ├── SpotteddogClient.java    # 客户端入口
│   ├── command/                 # 命令实现
│   │   ├── SpotCommand.java             # 命令注册
│   │   ├── TeleportHandler.java         # 传送调度器
│   │   ├── TeleportStrategy.java        # 策略接口
│   │   ├── SingleplayerTeleportStrategy.java
│   │   └── MultiplayerTeleportStrategy.java
│   └── data/                    # 数据管理
│       ├── PlayerDataManager.java
│       └── Spot.java
└── resources/
    ├── fabric.mod.json
    └── spotteddog.mixins.json
```

### 架构设计

- **策略模式**：单人模式和多人模式使用不同的传送策略
- **客户端命令**：所有命令在客户端执行，通过 `ServerPlayerEntity` 调用服务器方法
- **数据持久化**：标记点数据以 JSON 格式保存在存档目录

## 技术栈

- **Minecraft**: 1.21.11
- **Fabric Loader**: 0.18.4+
- **Fabric API**: 0.141.1+
- **Mappings**: Yarn 1.21.11+build.4
- **Gradle**: 9.x

## 许可证

本项目采用 MIT 许可证开源。

## 贡献

欢迎提交 Issue 和 Pull Request！
