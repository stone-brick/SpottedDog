package io.github.stone_brick.spotteddog.client.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.github.stone_brick.spotteddog.client.data.PlayerDataManager;
import io.github.stone_brick.spotteddog.client.data.Spot;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;

@Environment(EnvType.CLIENT)
public class SpotCommand {
    private static final PlayerDataManager dataManager = PlayerDataManager.getInstance();
    private static final String[] SPECIAL_TARGETS = {"death", "respawn", "spawn"};

    // 自动补全提供者：包含用户保存的 spot 名称和特殊目标（带 . 前缀）
    private static final SuggestionProvider<FabricClientCommandSource> TELEPORT_SUGGESTIONS = (context, builder) -> {
        String remaining = builder.getRemaining().toLowerCase();

        // 添加特殊目标（带 . 前缀）
        for (String target : SPECIAL_TARGETS) {
            String suggestion = "." + target;
            if (suggestion.startsWith(remaining)) {
                builder.suggest(suggestion);
            }
        }

        // 添加用户保存的 spot 名称
        List<Spot> spots = dataManager.getAllSpots(getWorldIdentifier());
        for (Spot spot : spots) {
            String name = spot.getName();
            if (name.toLowerCase().startsWith(remaining)) {
                builder.suggest(name);
            }
        }

        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        // /spot add <name>
        dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal("spot")
                .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("add")
                        .then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument("name", StringArgumentType.word())
                                .executes(context -> addSpot(getString(context, "name"))))));

        // /spot remove <name>
        dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal("spot")
                .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("remove")
                        .then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument("name", StringArgumentType.word())
                                .suggests(spotNameSuggestions())
                                .executes(context -> removeSpot(getString(context, "name"))))));

        // /spot update <name>
        dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal("spot")
                .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("update")
                        .then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument("name", StringArgumentType.word())
                                .suggests(spotNameSuggestions())
                                .executes(context -> updateSpot(getString(context, "name"))))));

        // /spot rename <oldName> <newName>
        dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal("spot")
                .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("rename")
                        .then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument("oldName", StringArgumentType.word())
                                .suggests(spotNameSuggestions())
                                .then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument("newName", StringArgumentType.word())
                                        .executes(context -> renameSpot(
                                                getString(context, "oldName"),
                                                getString(context, "newName")))))));

        // /spot teleport <target> - 支持 spot 名称和特殊目标 (death/respawn/spawn)
        dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal("spot")
                .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("teleport")
                        .then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument("target", StringArgumentType.word())
                                .suggests(TELEPORT_SUGGESTIONS)
                                .executes(context -> teleport(getString(context, "target"))))));

        // /spot tp <target> - teleport 的简写
        dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal("spot")
                .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("tp")
                        .then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument("target", StringArgumentType.word())
                                .suggests(TELEPORT_SUGGESTIONS)
                                .executes(context -> teleport(getString(context, "target"))))));

        // /spot list
        dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal("spot")
                .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("list")
                        .executes(context -> listSpots())));

        // /spot debug - 测试命令，查看 getUserData 返回内容
        dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal("spot")
                .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("debug")
                        .executes(context -> debugUserData())));
    }

    // 为 spot 名称提供自动补全
    private static SuggestionProvider<FabricClientCommandSource> spotNameSuggestions() {
        return (context, builder) -> {
            List<Spot> spots = dataManager.getAllSpots(getWorldIdentifier());
            for (Spot spot : spots) {
                String name = spot.getName();
                if (name.toLowerCase().startsWith(builder.getRemaining().toLowerCase())) {
                    builder.suggest(name);
                }
            }
            return builder.buildFuture();
        };
    }

    private static ClientPlayerEntity getPlayer() {
        return MinecraftClient.getInstance().player;
    }

    private static void sendFeedback(String message) {
        ClientPlayerEntity player = getPlayer();
        if (player != null) {
            player.sendMessage(Text.literal(message), false);
        }
    }

    private static String getCurrentDimension() {
        return MinecraftClient.getInstance().world != null ?
                MinecraftClient.getInstance().world.getRegistryKey().getValue().toString() : "minecraft:overworld";
    }

    // 获取当前世界的唯一标识符
    private static String getWorldIdentifier() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.isInSingleplayer()) {
            // 单人模式：使用存档文件夹名称
            IntegratedServer server = client.getServer();
            if (server != null) {
                // WorldSavePath.ROOT 返回 ".", 需要取 parent 获取存档文件夹名
                Path worldPath = server.getSavePath(WorldSavePath.ROOT).getParent();
                String worldDir = worldPath != null ? worldPath.getFileName().toString() : "unknown";
                return "singleplayer:" + worldDir;
            }
            return "singleplayer:unknown";
        } else {
            // 多人模式：使用服务器地址
            ServerInfo serverEntry = client.getCurrentServerEntry();
            if (serverEntry != null) {
                return "multiplayer:" + serverEntry.address;
            }
            return "multiplayer:unknown";
        }
    }

    private static String getCurrentWorldName() {
        return MinecraftClient.getInstance().isInSingleplayer() ? "Singleplayer" : "Server";
    }

    private static int addSpot(String name) {
        ClientPlayerEntity player = getPlayer();
        if (player == null) return 0;

        // 检查名称是否以 . 开头
        if (name.startsWith(".")) {
            sendFeedback("[SpottedDog] 标记点名称不能以 '.' 开头");
            return Command.SINGLE_SUCCESS;
        }

        String worldId = getWorldIdentifier();
        if (dataManager.addSpot(name, player.getX(), player.getY(), player.getZ(),
                player.getYaw(), player.getPitch(), getCurrentDimension(), getCurrentWorldName(), worldId)) {
            sendFeedback("[SpottedDog] 已添加标记点: " + name);
        } else {
            sendFeedback("[SpottedDog] 标记点 '" + name + "' 已存在");
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int removeSpot(String name) {
        String worldId = getWorldIdentifier();
        if (dataManager.removeSpot(name, worldId)) {
            sendFeedback("[SpottedDog] 已删除标记点: " + name);
        } else {
            sendFeedback("[SpottedDog] 未找到标记点: " + name);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int updateSpot(String name) {
        ClientPlayerEntity player = getPlayer();
        if (player == null) return 0;

        String worldId = getWorldIdentifier();
        if (dataManager.updateSpotPosition(name, player.getX(), player.getY(), player.getZ(),
                player.getYaw(), player.getPitch(), getCurrentDimension(), getCurrentWorldName(), worldId)) {
            sendFeedback("[SpottedDog] 已更新标记点: " + name);
        } else {
            sendFeedback("[SpottedDog] 未找到标记点: " + name);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int renameSpot(String oldName, String newName) {
        // 检查新名称是否以 . 开头
        if (newName.startsWith(".")) {
            sendFeedback("[SpottedDog] 标记点名称不能以 '.' 开头");
            return Command.SINGLE_SUCCESS;
        }

        String worldId = getWorldIdentifier();
        if (dataManager.renameSpot(oldName, newName, worldId)) {
            sendFeedback("[SpottedDog] 已将 '" + oldName + "' 重命名为 '" + newName + "'");
        } else {
            sendFeedback("[SpottedDog] 重命名失败: 旧名称不存在或新名称已存在");
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int teleport(String target) {
        ClientPlayerEntity player = getPlayer();
        if (player == null) return 0;

        String lowerTarget = target.toLowerCase();

        // 处理特殊目标（带 . 前缀）
        switch (lowerTarget) {
            case ".death" -> {
                TeleportHandler.teleportToDeath(player);
                sendFeedback("[SpottedDog] 已传送到死亡点");
                return Command.SINGLE_SUCCESS;
            }
            case ".respawn" -> {
                if (TeleportHandler.teleportToRespawn(player)) {
                    sendFeedback("[SpottedDog] 已传送到重生点");
                } else {
                    sendFeedback("[SpottedDog] 未设置重生点");
                }
                return Command.SINGLE_SUCCESS;
            }
            case ".spawn" -> {
                TeleportHandler.teleportToSpawn(player);
                sendFeedback("[SpottedDog] 已传送到世界出生点");
                return Command.SINGLE_SUCCESS;
            }
        }

        // 查找用户保存的 spot
        String worldId = getWorldIdentifier();
        Optional<Spot> spot = dataManager.getSpot(target, worldId);
        if (spot.isPresent()) {
            TeleportHandler.teleportToSpot(player, spot.get());
            sendFeedback("[SpottedDog] 已传送到: " + target);
        } else {
            sendFeedback("[SpottedDog] 未找到标记点: " + target);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int listSpots() {
        String worldId = getWorldIdentifier();
        List<Spot> spots = dataManager.getAllSpots(worldId);
        if (spots.isEmpty()) {
            sendFeedback("[SpottedDog] 没有保存的标记点");
        } else {
            sendFeedback("[SpottedDog] 标记点列表:");
            for (Spot spot : spots) {
                sendFeedback("  - " + spot.getName() + " (" + spot.getDimension() + ")");
            }
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int debugUserData() {
        ClientPlayerEntity player = getPlayer();
        if (player == null) return 0;

        MinecraftClient client = MinecraftClient.getInstance();

        // 测试 getCurrentServerEntry
        ServerInfo serverEntry = client.getCurrentServerEntry();
        sendFeedback("[SpottedDog] ===== Debug Info =====");
        sendFeedback("isInSingleplayer: " + client.isInSingleplayer());
        sendFeedback("getCurrentServerEntry: " + (serverEntry != null ? serverEntry.address : "null"));

        if (client.isInSingleplayer()) {
            IntegratedServer server = client.getServer();
            sendFeedback("getServer: " + (server != null ? "not null" : "null"));

            if (server != null) {
                // 测试获取存档信息
                String levelName = server.getSaveProperties().getLevelName();
                Path worldPath = server.getSavePath(WorldSavePath.ROOT).getParent();
                String worldDir = worldPath != null ? worldPath.getFileName().toString() : "unknown";
                sendFeedback("levelName: " + levelName);
                sendFeedback("worldDir: " + worldDir);
                sendFeedback("worldIdentifier: " + getWorldIdentifier());

                var playerManager = server.getPlayerManager();
                List<ServerPlayerEntity> list = playerManager.getPlayerList();

                if (list != null) {
                    sendFeedback("getPlayerList size: " + list.size());

                    for (ServerPlayerEntity playerEntity : list) {
                        if (playerEntity.getRespawn() != null) {
                            sendFeedback("  respawnPos: " + playerEntity.getRespawn().respawnData().getPos());
                        }
                        sendFeedback("  lastDeathPos: " + playerEntity.getLastDeathPos());
                    }
                } else {
                    sendFeedback("getPlayerList: null");
                }
            }
        }

        return Command.SINGLE_SUCCESS;
    }
}
