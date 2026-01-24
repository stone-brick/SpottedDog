# SpottedDog

一个 Minecraft Fabric 模组，用于 Minecraft 1.21.11，提供标记点管理和传送功能。

## 功能特性

- **标记点管理**：添加、删除、重命名和更新标记点位置（含朝向）
- **快速传送**：一键传送到保存的标记点或特殊位置（死亡点、重生点、世界出生点）
- **智能策略模式**：根据游戏模式（单人/多人）自动选择最佳传送方式
- **自动死亡记录**：自动记录玩家死亡位置
- **朝向保存与恢复**：传送时保持玩家当前朝向（特殊目标）或恢复保存的朝向（自定义标记点）

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
| `/spot list` | 列出所有保存的标记点 |

### 特殊传送目标

`/spot teleport` 命令支持以下特殊目标：

- `death` - 传送到死亡点
- `respawn` - 传送到重生点
- `spawn` - 传送到世界出生点

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
│   ├── java/io/github/stone_brick/spotteddog/
│   │   └── Spotteddog.java          # 主模组入口
│   └── resources/
│       ├── fabric.mod.json
│       └── spotteddog.mixins.json
└── client/
    └── java/io/github/stone_brick/spotteddog/client/
        ├── SpotteddogClient.java    # 客户端入口
        ├── command/
        │   ├── SpotCommand.java     # 命令实现
        │   ├── TeleportHandler.java # 传送处理
        │   ├── TeleportStrategy.java
        │   ├── SingleplayerTeleportStrategy.java
        │   └── MultiplayerTeleportStrategy.java
        └── data/
            ├── PlayerDataManager.java # 数据管理
            └── Spot.java              # 标记点数据模型
```

## 技术栈

- **Minecraft**: 1.21.11
- **Java**: 21
- **Fabric Loader**: 0.18.4+
- **Fabric API**: 0.141.1+1.21.11
- **构建工具**: Gradle (fabric-loom 插件)

## 配置

标记点数据保存在 `config/spotteddog/spots.json` 文件中。

## 许可证

All Rights Reserved
