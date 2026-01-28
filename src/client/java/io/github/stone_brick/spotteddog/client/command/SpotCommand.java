package io.github.stone_brick.spotteddog.client.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.github.stone_brick.spotteddog.client.data.PlayerDataManager;
import io.github.stone_brick.spotteddog.client.data.Spot;
import io.github.stone_brick.spotteddog.client.network.PublicSpotListHandler;
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
    private static final int MAX_NAME_LENGTH = 64; // Spot 名称最大长度

    // 自动补全提供者：包含用户保存的 spot 名称、公开 Spot 和特殊目标
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
        List<Spot> spots = dataManager.getAllSpots();
        for (Spot spot : spots) {
            String name = spot.getName();
            if (name.toLowerCase().startsWith(remaining)) {
                builder.suggest("\"" + name + "\"");
            }
        }

        // 添加公开 Spot（带 - 前缀），仅在多人模式下
        if (!MinecraftClient.getInstance().isInSingleplayer()) {
            List<PublicSpotListHandler.PublicSpotInfo> publicSpots = PublicSpotListHandler.getPublicSpots();
            for (PublicSpotListHandler.PublicSpotInfo spot : publicSpots) {
                String fullName = spot.getFullName();
                if (fullName.toLowerCase().startsWith(remaining)) {
                    builder.suggest("\"" + fullName + "\"");
                }
            }
        }

        return builder.buildFuture();
    };

    // 记录是否已请求过公开 Spot 列表（避免重复请求）
    private static long lastPublicSpotRequestTime = 0;
    private static final long REQUEST_COOLDOWN_MS = 5000; // 5秒冷却

    /**
     * 如果距离上次请求超过冷却时间，则向服务器请求公开 Spot 列表。
     */
    private static void requestPublicSpotsIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastPublicSpotRequestTime > REQUEST_COOLDOWN_MS) {
            lastPublicSpotRequestTime = now;
            if (TeleportHandler.getStrategy() instanceof MultiplayerTeleportStrategy strategy) {
                strategy.requestPublicSpotList(null);
            }
        }
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        // 初始化当前世界数据
        String worldId = getWorldIdentifier();
        String worldName = getCurrentWorldName();
        dataManager.setCurrentWorld(worldId, worldName);

        // 在 /spot 后输入内容时触发公开 Spot 更新（仅触发，不添加建议）
        dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal("spot")
                .then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument("trigger", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            String remaining = builder.getRemaining();
                            // 当输入内容非空且不是空格开头时触发更新
                            if (!remaining.isEmpty() && remaining.charAt(0) != ' '
                                    && !MinecraftClient.getInstance().isInSingleplayer()) {
                                requestPublicSpotsIfNeeded();
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            // 不执行任何操作
                            return Command.SINGLE_SUCCESS;
                        })));

        // /spot add <name>
        dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal("spot")
                .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("add")
                        .then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument("name", StringArgumentType.string())
                                .executes(context -> addSpot(getString(context, "name"))))));

        // /spot remove <name>
        dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal("spot")
                .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("remove")
                        .then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument("name", StringArgumentType.string())
                                .suggests(spotNameSuggestions())
                                .executes(context -> removeSpot(getString(context, "name"))))));

        // /spot update <name>
        dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal("spot")
                .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("update")
                        .then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument("name", StringArgumentType.string())
                                .suggests(spotNameSuggestions())
                                .executes(context -> updateSpot(getString(context, "name"))))));

        // /spot rename <oldName> <newName>
        dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal("spot")
                .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("rename")
                        .then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument("oldName", StringArgumentType.string())
                                .suggests(spotNameSuggestions())
                                .then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument("newName", StringArgumentType.string())
                                        .executes(context -> renameSpot(
                                                getString(context, "oldName"),
                                                getString(context, "newName")))))));

        // /spot teleport <target> - 支持 spot 名称和特殊目标 (death/respawn/spawn)
        dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal("spot")
                .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("teleport")
                        .then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument("target", StringArgumentType.string())
                                .suggests(TELEPORT_SUGGESTIONS)
                                .executes(context -> teleport(getString(context, "target"))))));

        // /spot tp <target> - teleport 的简写
        dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal("spot")
                .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("tp")
                        .then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument("target", StringArgumentType.string())
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

        // /spot public <name> - 公开 Spot（仅多人模式）
        dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal("spot")
                .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("public")
                        .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("list")
                                .executes(context -> listPublicSpots()))
                        .then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument("name", StringArgumentType.string())
                                .suggests(spotNameSuggestions())
                                .executes(context -> publishSpot(getString(context, "name"))))));

        // /spot unpublic <name> - 取消公开 Spot（仅多人模式）
        dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal("spot")
                .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("unpublic")
                        .then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument("name", StringArgumentType.string())
                                .suggests(myPublicSpotSuggestions())
                                .executes(context -> unpublishSpot(getString(context, "name"))))));
    }

    // 为 spot 名称提供自动补全
    private static SuggestionProvider<FabricClientCommandSource> spotNameSuggestions() {
        return (context, builder) -> {
            List<Spot> spots = dataManager.getAllSpots();
            for (Spot spot : spots) {
                String name = spot.getName();
                if (name.toLowerCase().startsWith(builder.getRemaining().toLowerCase())) {
                    builder.suggest("\"" + name + "\"");
                }
            }
            return builder.buildFuture();
        };
    }

    // 为自己公开的 spot 名称提供自动补全（不带玩家名前缀）
    private static SuggestionProvider<FabricClientCommandSource> myPublicSpotSuggestions() {
        return (context, builder) -> {
            String playerName = getPlayer().getName().getString();
            List<PublicSpotListHandler.PublicSpotInfo> spots = PublicSpotListHandler.getPublicSpots();
            for (PublicSpotListHandler.PublicSpotInfo spot : spots) {
                if (spot.getOwnerName().equals(playerName)) {
                    String name = spot.getDisplayName();
                    if (name.toLowerCase().startsWith(builder.getRemaining().toLowerCase())) {
                        builder.suggest("\"" + name + "\"");
                    }
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
            player.sendMessage(Text.translatable(message), false);
        }
    }

    private static void sendFeedback(String key, Object... args) {
        ClientPlayerEntity player = getPlayer();
        if (player != null) {
            player.sendMessage(Text.translatable(key, args), false);
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
            sendFeedback("spotteddog.spot.name.cannot.start.with.dot");
            return Command.SINGLE_SUCCESS;
        }

        // 检查名称长度
        if (name.length() > MAX_NAME_LENGTH) {
            sendFeedback("spotteddog.spot.name.too.long", MAX_NAME_LENGTH);
            return Command.SINGLE_SUCCESS;
        }

        if (dataManager.addSpot(name, player.getX(), player.getY(), player.getZ(),
                player.getYaw(), player.getPitch(), getCurrentDimension())) {
            sendFeedback("spotteddog.spot.added", name);
        } else {
            sendFeedback("spotteddog.spot.already.exists", name);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int removeSpot(String name) {
        if (dataManager.removeSpot(name)) {
            sendFeedback("spotteddog.spot.deleted", name);
        } else {
            sendFeedback("spotteddog.spot.not.found", name);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int updateSpot(String name) {
        ClientPlayerEntity player = getPlayer();
        if (player == null) return 0;

        if (dataManager.updateSpotPosition(name, player.getX(), player.getY(), player.getZ(),
                player.getYaw(), player.getPitch(), getCurrentDimension())) {
            sendFeedback("spotteddog.spot.updated", name);

            // 多人模式下同步公开 Spot 的更新
            if (!MinecraftClient.getInstance().isInSingleplayer()) {
                String playerName = player.getName().getString();
                if (PublicSpotListHandler.isSpotPublic(name, playerName)) {
                    PublicSpotListHandler.sendUpdatePublicSpot(name,
                            player.getX(), player.getY(), player.getZ(),
                            player.getYaw(), player.getPitch(), getCurrentDimension());
                }
            }
        } else {
            sendFeedback("spotteddog.spot.not.found", name);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int renameSpot(String oldName, String newName) {
        // 检查新名称是否以 . 开头
        if (newName.startsWith(".")) {
            sendFeedback("spotteddog.spot.name.cannot.start.with.dot");
            return Command.SINGLE_SUCCESS;
        }

        // 检查名称长度
        if (newName.length() > MAX_NAME_LENGTH) {
            sendFeedback("spotteddog.spot.name.too.long", MAX_NAME_LENGTH);
            return Command.SINGLE_SUCCESS;
        }

        // 检查 oldName 是否已公开（自动补全时已刷新缓存）
        boolean wasPublic = false;
        if (!MinecraftClient.getInstance().isInSingleplayer()) {
            ClientPlayerEntity player = getPlayer();
            if (player != null) {
                String playerName = player.getName().getString();
                wasPublic = PublicSpotListHandler.isSpotPublic(oldName, playerName);
            }
        }

        if (dataManager.renameSpot(oldName, newName)) {
            sendFeedback("spotteddog.spot.renamed", oldName, newName);

            // 多人模式下同步公开 Spot 的重命名
            if (wasPublic) {
                PublicSpotListHandler.sendRenamePublicSpot(oldName, newName);
            }
        } else {
            sendFeedback("spotteddog.spot.rename.failed");
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
                return Command.SINGLE_SUCCESS;
            }
            case ".respawn" -> {
                TeleportHandler.teleportToRespawn(player);
                return Command.SINGLE_SUCCESS;
            }
            case ".spawn" -> {
                TeleportHandler.teleportToSpawn(player);
                return Command.SINGLE_SUCCESS;
            }
        }

        // 处理公开 Spot（带 - 前缀）
        if (target.startsWith("-")) {
            if (MinecraftClient.getInstance().isInSingleplayer()) {
                // 单人模式不处理公开 Spot，提示未找到
                sendFeedback("spotteddog.spot.not.found", target);
                return Command.SINGLE_SUCCESS;
            }

            // 多人模式当作公开 Spot 处理
            if (TeleportHandler.getStrategy() instanceof MultiplayerTeleportStrategy strategy) {
                strategy.teleportToPublicSpot(player, target);
            }
            return Command.SINGLE_SUCCESS;
        }

        // 查找用户保存的 spot
        Optional<Spot> spot = dataManager.getSpot(target);
        if (spot.isPresent()) {
            TeleportHandler.teleportToSpot(player, spot.get());
        } else {
            sendFeedback("spotteddog.spot.not.found", target);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int listSpots() {
        List<Spot> spots = dataManager.getAllSpots();
        if (spots.isEmpty()) {
            sendFeedback("spotteddog.spot.list.empty");
        } else {
            sendFeedback("spotteddog.spot.list.header");
            for (Spot spot : spots) {
                sendFeedback("spotteddog.spot.list.item", spot.getName(), spot.getDimension());
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

    private static int publishSpot(String name) {
        ClientPlayerEntity player = getPlayer();
        if (player == null) return 0;

        // 检查是否在单人模式
        if (MinecraftClient.getInstance().isInSingleplayer()) {
            sendFeedback("spotteddog.spot.multiplayer.only");
            return Command.SINGLE_SUCCESS;
        }

        // 检查 Spot 是否存在
        Optional<Spot> spot = dataManager.getSpot(name);
        if (spot.isEmpty()) {
            sendFeedback("spotteddog.spot.not.found", name);
            return Command.SINGLE_SUCCESS;
        }

        // 调用多人模式策略公开 Spot
        TeleportHandler.getStrategy();
        if (TeleportHandler.getStrategy() instanceof MultiplayerTeleportStrategy strategy) {
            strategy.publishSpot(player, spot.get());
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int unpublishSpot(String name) {
        ClientPlayerEntity player = getPlayer();
        if (player == null) return 0;

        // 检查是否在单人模式
        if (MinecraftClient.getInstance().isInSingleplayer()) {
            sendFeedback("spotteddog.spot.multiplayer.only");
            return Command.SINGLE_SUCCESS;
        }

        // 调用多人模式策略取消公开 Spot
        if (TeleportHandler.getStrategy() instanceof MultiplayerTeleportStrategy strategy) {
            strategy.unpublishSpot(player, name);
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int listPublicSpots() {
        ClientPlayerEntity player = getPlayer();
        if (player == null) return 0;

        // 检查是否在单人模式
        if (MinecraftClient.getInstance().isInSingleplayer()) {
            sendFeedback("spotteddog.spot.multiplayer.only");
            return Command.SINGLE_SUCCESS;
        }

        // 请求公开 Spot 列表
        if (TeleportHandler.getStrategy() instanceof MultiplayerTeleportStrategy strategy) {
            strategy.requestPublicSpotListWithCallback(player, spots -> {
                if (spots.isEmpty()) {
                    player.sendMessage(Text.translatable("spotteddog.public.none"), false);
                } else {
                    player.sendMessage(Text.translatable("spotteddog.public.list.count", spots.size()), false);
                    for (PublicSpotListHandler.PublicSpotInfo spot : spots) {
                        String fullName = spot.getFullName();
                        player.sendMessage(Text.translatable("spotteddog.public.list.item", fullName, spot.getDimension()), false);
                    }
                }
            });
        }

        return Command.SINGLE_SUCCESS;
    }
}
