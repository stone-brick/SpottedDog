package io.github.stone_brick.spotteddog.client.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.github.stone_brick.spotteddog.client.data.PlayerDataManager;
import io.github.stone_brick.spotteddog.client.data.Spot;
import io.github.stone_brick.spotteddog.client.network.PublicSpotListHandler;
import io.github.stone_brick.spotteddog.client.ui.SpotTableBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.server.integrated.IntegratedServer;
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
     * 更新权限缓存（供 PublicSpotListHandler 调用）
     */
    public static void updatePermissions(boolean canTeleport, boolean canManagePublicSpots) {
        PermissionChecker.setPermissions(canTeleport, canManagePublicSpots);
    }

    /**
     * 刷新权限信息（多人模式）- 通过请求公开 Spot 列表获取权限
     */
    private static void refreshPermissionsIfNeeded() {
        if (MinecraftClient.getInstance().isInSingleplayer()) {
            // 单人模式：无需公开 Spot 功能
            PermissionChecker.setPermissions(true, false);
            return;
        }

        // 请求公开 Spot 列表，响应中会包含权限信息
        PublicSpotListHandler.requestPublicSpots();
    }

    /**
     * 如果距离上次请求超过冷却时间，则向服务器请求公开 Spot 列表。
     */
    private static void requestPublicSpotsIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastPublicSpotRequestTime > REQUEST_COOLDOWN_MS) {
            lastPublicSpotRequestTime = now;
            if (SpotHandler.getStrategy() instanceof MultiplayerSpotStrategy strategy) {
                strategy.requestPublicSpotList(null);
            }
        }
    }

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        // 初始化当前世界数据
        String worldId = getWorldIdentifier();
        String worldName = getCurrentWorldName();
        dataManager.setCurrentWorld(worldId, worldName);

        // 在 /spot 后输入内容时触发公开 Spot 更新和权限刷新（仅触发，不添加建议）
        dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal("spot")
                .then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument("trigger", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            String remaining = builder.getRemaining();
                            // 当输入内容非空且不是空格开头时触发更新
                            if (!remaining.isEmpty() && remaining.charAt(0) != ' '
                                    && !MinecraftClient.getInstance().isInSingleplayer()) {
                                requestPublicSpotsIfNeeded();
                                refreshPermissionsIfNeeded();
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
                        .then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument("name", StringArgumentType.string())
                                .suggests(spotNameSuggestions())
                                .executes(context -> publishSpot(getString(context, "name"))))));

        // /spot unpublic <name> - 取消公开 Spot（仅多人模式）
        dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal("spot")
                .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("unpublic")
                        .then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument("name", StringArgumentType.string())
                                .suggests(myPublicSpotSuggestions())
                                .executes(context -> unpublishSpot(getString(context, "name"))))));

        // /spot log list [count] - 查看传送日志
        dispatcher.register(LiteralArgumentBuilder.<FabricClientCommandSource>literal("spot")
                .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("log")
                        .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("list")
                                .executes(context -> listTeleportLogs(10))
                                .then(RequiredArgumentBuilder.<FabricClientCommandSource, Integer>argument("count", IntegerArgumentType.integer(1, 100))
                                        .executes(context -> listTeleportLogs(IntegerArgumentType.getInteger(context, "count")))))
                        .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("clear")
                                .executes(context -> clearTeleportLogs()))));
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

    /**
     * 发送系统消息（会淡出）。
     */
    private static void sendSystemMessage(String key) {
        ClientPlayerEntity player = getPlayer();
        if (player != null) {
            player.sendMessage(Text.translatable(key), true);
        }
    }

    /**
     * 发送系统消息（会淡出）。
     */
    private static void sendSystemMessage(String key, Object... args) {
        ClientPlayerEntity player = getPlayer();
        if (player != null) {
            player.sendMessage(Text.translatable(key, args), true);
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
            sendSystemMessage("spotteddog.spot.name.cannot.start.with.dot");
            return Command.SINGLE_SUCCESS;
        }

        // 检查名称长度
        if (name.length() > MAX_NAME_LENGTH) {
            sendSystemMessage("spotteddog.spot.name.too.long", MAX_NAME_LENGTH);
            return Command.SINGLE_SUCCESS;
        }

        if (dataManager.addSpot(name, player.getX(), player.getY(), player.getZ(),
                player.getYaw(), player.getPitch(), getCurrentDimension())) {
            sendSystemMessage("spotteddog.spot.added", name);
        } else {
            sendSystemMessage("spotteddog.spot.already.exists", name);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int removeSpot(String name) {
        if (dataManager.removeSpot(name)) {
            sendSystemMessage("spotteddog.spot.deleted", name);
        } else {
            sendSystemMessage("spotteddog.spot.not.found", name);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int updateSpot(String name) {
        ClientPlayerEntity player = getPlayer();
        if (player == null) return 0;

        if (dataManager.updateSpotPosition(name, player.getX(), player.getY(), player.getZ(),
                player.getYaw(), player.getPitch(), getCurrentDimension())) {
            sendSystemMessage("spotteddog.spot.updated", name);

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
            sendSystemMessage("spotteddog.spot.not.found", name);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int renameSpot(String oldName, String newName) {
        // 检查新名称是否以 . 开头
        if (newName.startsWith(".")) {
            sendSystemMessage("spotteddog.spot.name.cannot.start.with.dot");
            return Command.SINGLE_SUCCESS;
        }

        // 检查名称长度
        if (newName.length() > MAX_NAME_LENGTH) {
            sendSystemMessage("spotteddog.spot.name.too.long", MAX_NAME_LENGTH);
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
            sendSystemMessage("spotteddog.spot.renamed", oldName, newName);

            // 多人模式下同步公开 Spot 的重命名
            if (wasPublic) {
                PublicSpotListHandler.sendRenamePublicSpot(oldName, newName);
            }
        } else {
            sendSystemMessage("spotteddog.spot.rename.failed");
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
                SpotHandler.teleportToDeath(player);
                return Command.SINGLE_SUCCESS;
            }
            case ".respawn" -> {
                SpotHandler.teleportToRespawn(player);
                return Command.SINGLE_SUCCESS;
            }
            case ".spawn" -> {
                SpotHandler.teleportToSpawn(player);
                return Command.SINGLE_SUCCESS;
            }
        }

        // 处理公开 Spot（带 - 前缀）
        if (target.startsWith("-")) {
            if (MinecraftClient.getInstance().isInSingleplayer()) {
                // 单人模式不处理公开 Spot，提示未找到
                sendSystemMessage("spotteddog.spot.not.found", target);
                return Command.SINGLE_SUCCESS;
            }

            // 多人模式当作公开 Spot 处理
            if (SpotHandler.getStrategy() instanceof MultiplayerSpotStrategy strategy) {
                strategy.teleportToPublicSpot(player, target);
            }
            return Command.SINGLE_SUCCESS;
        }

        // 查找用户保存的 spot
        Optional<Spot> spot = dataManager.getSpot(target);
        if (spot.isPresent()) {
            SpotHandler.teleportToSpot(player, spot.get());
        } else {
            sendFeedback("spotteddog.spot.not.found", target);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int listSpots() {
        List<Spot> privateSpots = dataManager.getAllSpots();
        List<PublicSpotListHandler.PublicSpotInfo> publicSpots = PublicSpotListHandler.getPublicSpots();
        boolean isSingleplayer = MinecraftClient.getInstance().isInSingleplayer();

        if (privateSpots.isEmpty() && (isSingleplayer || publicSpots.isEmpty())) {
            sendSystemMessage("spotteddog.spot.list.empty");
            return Command.SINGLE_SUCCESS;
        }

        if (isSingleplayer) {
            // 单人模式只显示私有 Spot
            displaySpotTable(privateSpots, null, true);
        } else {
            // 多人模式：显示私有 Spot + 公开 Spot（使用缓存数据）
            displaySpotTable(privateSpots, publicSpots, false);
        }
        return Command.SINGLE_SUCCESS;
    }

    // 列宽配置
    private static final int COL_NAME_WIDTH = 55;
    private static final int COL_DIM_WIDTH = 25;
    private static final int COL_COORD_WIDTH = 80;
    private static final int COL_VIS_WIDTH = 40;
    private static final int COL_OP_WIDTH = 40;
    private static final int COL_SPACING = 4;

    // 单人模式列配置
    private static final int[] SINGLEPLAYER_COLS = {COL_NAME_WIDTH, COL_DIM_WIDTH, COL_COORD_WIDTH, COL_OP_WIDTH};
    // 多人模式列配置（包含可见性）
    private static final int[] MULTIPLAYER_COLS = {COL_NAME_WIDTH, COL_DIM_WIDTH, COL_COORD_WIDTH, COL_VIS_WIDTH, COL_OP_WIDTH};

    /**
     * Spot 操作类型枚举
     */
    private enum SpotAction {
        TELEPORT('T', "spotteddog.action.teleport"),
        REMOVE('R', "spotteddog.action.remove"),
        UPDATE('U', "spotteddog.action.update"),
        RENAME('E', "spotteddog.action.rename"),
        PUBLIC('P', "spotteddog.action.public"),
        UNPUBLIC('P', "spotteddog.action.unpublish");

        final char symbol;
        final String translationKey;

        SpotAction(char symbol, String translationKey) {
            this.symbol = symbol;
            this.translationKey = translationKey;
        }

        /**
         * 获取带颜色的操作符号
         */
        String getColoredSymbol() {
            return switch (this) {
                case TELEPORT -> "§bT";
                case REMOVE -> "§cR";
                case UPDATE -> "§aU";
                case RENAME -> "§eE";
                case PUBLIC, UNPUBLIC -> "§dP";
            };
        }

        /**
         * 获取操作的命令
         */
        String getCommand(String spotName) {
            return switch (this) {
                case TELEPORT -> "/spot tp \"" + spotName + "\"";
                case REMOVE -> "/spot remove \"" + spotName + "\"";
                case UPDATE -> "/spot update \"" + spotName + "\"";
                case RENAME -> "/spot rename \"" + spotName + "\" ";
                case PUBLIC -> "/spot public \"" + spotName + "\"";
                case UNPUBLIC -> "/spot unpublic \"" + spotName + "\"";
            };
        }
    }

    /**
     * 获取私有 Spot 的可用操作列表（按权限过滤）
     */
    private static SpotAction[] getPrivateSpotActions(Spot spot, boolean isSingleplayer) {
        if (isSingleplayer) {
            // 单人模式：T、R、U、E 可用，P（公开）不可用
            java.util.List<SpotAction> actions = new java.util.ArrayList<>();
            actions.add(SpotAction.TELEPORT);
            actions.add(SpotAction.REMOVE);
            actions.add(SpotAction.UPDATE);
            actions.add(SpotAction.RENAME);
            return actions.toArray(new SpotAction[0]);
        }

        // 多人模式
        String playerName = MinecraftClient.getInstance().player.getName().getString();
        boolean isOwner = spot.getName() != null; // 本地 Spot 都是自己的

        // 收集可用操作
        java.util.List<SpotAction> actions = new java.util.ArrayList<>();

        // T (传送)：需要传送权限
        if (PermissionChecker.canTeleport()) {
            actions.add(SpotAction.TELEPORT);
        }

        // R/U/E (删除/更新/重命名)：仅自己的 Spot
        if (isOwner) {
            actions.add(SpotAction.REMOVE);
            actions.add(SpotAction.UPDATE);
            actions.add(SpotAction.RENAME);

            // P (公开/取消公开)：需要公开 Spot 权限
            if (PermissionChecker.canManagePublicSpots()) {
                // 根据当前公开状态显示"公开"或"取消公开"
                boolean isPublic = PublicSpotListHandler.isSpotPublic(spot.getName(), playerName);
                actions.add(isPublic ? SpotAction.UNPUBLIC : SpotAction.PUBLIC);
            }
        }

        return actions.toArray(new SpotAction[0]);
    }

    /**
     * 权限检查工具类（多人模式从服务端同步）
     * 权限值由 refreshPermissionsIfNeeded() 在命令执行前刷新
     */
    private static class PermissionChecker {
        private static boolean canTeleport = true;
        private static boolean canManagePublicSpots = false;

        static synchronized boolean canTeleport() {
            return canTeleport;
        }

        static synchronized boolean canManagePublicSpots() {
            return canManagePublicSpots;
        }

        static synchronized void setPermissions(boolean teleport, boolean managePublic) {
            canTeleport = teleport;
            canManagePublicSpots = managePublic;
        }
    }

    private static void sendTableHeader(boolean isSingleplayer) {
        net.minecraft.client.font.TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int[] widths = isSingleplayer ? SINGLEPLAYER_COLS : MULTIPLAYER_COLS;

        String name = Text.translatable("spotteddog.list.header.name").getString();
        String dim = Text.translatable("spotteddog.list.header.dimension").getString();
        String coord = Text.translatable("spotteddog.list.header.coord").getString();
        String action = Text.translatable("spotteddog.list.header.action").getString();

        if (isSingleplayer) {
            sendFeedback(padToWidth(tr, widths, name, dim, coord, action));
        } else {
            String vis = Text.translatable("spotteddog.list.header.visibility").getString();
            sendFeedback(padToWidth(tr, widths, name, dim, coord, vis, action));
        }

        // 分隔线
        String sep = "§7";
        int totalWidth = calculateTotalWidth(widths);
        // padToWidth 会在列之间添加 COL_SPACING，需要加上
        if (widths.length > 1) {
            totalWidth += COL_SPACING * (widths.length - 1);
        }
        while (tr.getWidth(sep) <= totalWidth) {
            sep += "-";
        }
        sendFeedback(sep);
    }

    private static void sendTableRow(Spot spot, boolean isSingleplayer) {
        net.minecraft.client.font.TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int[] widths = isSingleplayer ? SINGLEPLAYER_COLS : MULTIPLAYER_COLS;

        String coord = String.format("§f[%.0f, %.0f, %.0f]", spot.getX(), spot.getY(), spot.getZ());
        String dimShort = formatDimension(spot.getDimension());
        String dimFull = localizeDimension(spot.getDimension());
        String coordFull = String.format("X: %.1f  Y: %.1f  Z: %.1f", spot.getX(), spot.getY(), spot.getZ());

        // 获取可用操作列表
        SpotAction[] availableActions = getPrivateSpotActions(spot, isSingleplayer);

        if (isSingleplayer) {
            sendTableRowWithAction(tr, widths,
                    new String[]{spot.getName(), dimShort, coord},
                    new String[]{spot.getName(), dimFull, coordFull},
                    availableActions, spot.getName());
        } else {
            String playerName = MinecraftClient.getInstance().player.getName().getString();
            String visibility = PublicSpotListHandler.isSpotPublic(spot.getName(), playerName)
                    ? "§a" + Text.translatable("spotteddog.visibility.public").getString()
                    : "§7" + Text.translatable("spotteddog.visibility.private").getString();

            sendTableRowWithAction(tr, widths,
                    new String[]{spot.getName(), dimShort, coord, visibility},
                    new String[]{spot.getName(), dimFull, coordFull, null},
                    availableActions, spot.getName());
        }
    }

    /**
     * 合并显示 Spot 表格（玩家 Spot + 其他玩家公开 Spot）。
     */
    private static void displaySpotTable(List<Spot> privateSpots, List<PublicSpotListHandler.PublicSpotInfo> publicSpots, boolean isSingleplayer) {
        // 标题
        sendFeedback("spotteddog.spot.list.header");

        // 玩家 Spot 列表
        if (privateSpots != null && !privateSpots.isEmpty()) {
            SpotTableBuilder.sendTableHeader(isSingleplayer);
            for (Spot spot : privateSpots) {
                SpotTableBuilder.SpotAction[] actions = getPrivateSpotActionsWithPermission(spot);
                SpotTableBuilder.sendPrivateSpotRow(spot, isSingleplayer, actions);
            }
        }

        // 过滤出其他玩家的公开 Spot（排除玩家自己的）
        if (publicSpots != null && !publicSpots.isEmpty()) {
            String playerName = MinecraftClient.getInstance().player.getName().getString();
            List<PublicSpotListHandler.PublicSpotInfo> otherPublicSpots = new java.util.ArrayList<>();
            for (PublicSpotListHandler.PublicSpotInfo spot : publicSpots) {
                if (!spot.getOwnerName().equals(playerName)) {
                    otherPublicSpots.add(spot);
                }
            }

            if (!otherPublicSpots.isEmpty()) {
                // 分隔符
                SpotTableBuilder.sendSeparator(MinecraftClient.getInstance().textRenderer, SpotTableBuilder.MULTIPLAYER_COLS);
                // 显示其他玩家公开 Spot 行
                SpotTableBuilder.SpotAction[] actions = PermissionChecker.canTeleport()
                        ? new SpotTableBuilder.SpotAction[]{SpotTableBuilder.SpotAction.TELEPORT}
                        : new SpotTableBuilder.SpotAction[0];
                for (PublicSpotListHandler.PublicSpotInfo spot : otherPublicSpots) {
                    SpotTableBuilder.sendOtherPublicSpotRow(spot, actions);
                }
            }
        }

        // 如果没有任何数据
        if (privateSpots != null && privateSpots.isEmpty() && publicSpots != null && publicSpots.isEmpty()) {
            sendSystemMessage("spotteddog.spot.list.empty");
        }
    }

    /**
     * 获取私有 Spot 的可用操作列表（按权限过滤）
     */
    private static SpotTableBuilder.SpotAction[] getPrivateSpotActionsWithPermission(Spot spot) {
        String playerName = MinecraftClient.getInstance().player.getName().getString();
        boolean isOwner = spot.getName() != null;

        java.util.List<SpotTableBuilder.SpotAction> actions = new java.util.ArrayList<>();

        // T (传送)
        if (PermissionChecker.canTeleport()) {
            actions.add(SpotTableBuilder.SpotAction.TELEPORT);
        }

        // R/U/E (删除/更新/重命名)
        if (isOwner) {
            actions.add(SpotTableBuilder.SpotAction.REMOVE);
            actions.add(SpotTableBuilder.SpotAction.UPDATE);
            actions.add(SpotTableBuilder.SpotAction.RENAME);

            // P/取消公开
            if (PermissionChecker.canManagePublicSpots()) {
                boolean isPublic = PublicSpotListHandler.isSpotPublic(spot.getName(), playerName);
                actions.add(isPublic ? SpotTableBuilder.SpotAction.UNPUBLIC : SpotTableBuilder.SpotAction.PUBLIC);
            }
        }

        return actions.toArray(new SpotTableBuilder.SpotAction[0]);
    }

    /**
     * 发送其他玩家公开 Spot 表格行（只显示 T 传送）。
     */
    private static void sendOtherPublicSpotTableRow(net.minecraft.client.font.TextRenderer tr,
                                               PublicSpotListHandler.PublicSpotInfo spot) {
        String displayName = spot.getDisplayName();
        String fullName = spot.getFullName();
        String coord = String.format("§f[%.0f, %.0f, %.0f]", spot.getX(), spot.getY(), spot.getZ());
        String dimShort = formatDimension(spot.getDimension());
        String dimFull = localizeDimension(spot.getDimension());
        String coordFull = String.format("X: %.1f  Y: %.1f  Z: %.1f", spot.getX(), spot.getY(), spot.getZ());
        String visibility = "§a" + Text.translatable("spotteddog.visibility.public").getString();

        // 公开 Spot 只能传送
        SpotAction[] actions = PermissionChecker.canTeleport()
                ? new SpotAction[]{SpotAction.TELEPORT}
                : new SpotAction[0];

        // Hover 信息：名称列显示完整名称（不含前缀）和所有者
        String nameHover = spot.getDisplayName() + "\n" + Text.translatable("spotteddog.list.spot.owner", spot.getOwnerName()).getString();

        sendTableRowWithAction(tr, MULTIPLAYER_COLS,
                new String[]{displayName, dimShort, coord, visibility},
                new String[]{nameHover, dimFull, coordFull, null},
                actions, fullName);
    }

    /**
     * 发送带操作列的行（支持点击事件）
     */
    private static void sendTableRowWithAction(net.minecraft.client.font.TextRenderer tr, int[] widths,
                                               String[] parts, String[] hoverTexts,
                                               SpotAction[] actions, String spotName) {
        if (parts == null) parts = new String[0];
        if (hoverTexts == null) hoverTexts = new String[parts.length];

        // 处理普通列（不包含操作列）
        net.minecraft.text.MutableText fullText = net.minecraft.text.Text.empty();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i] != null ? parts[i] : "";
            String full = hoverTexts[i] != null ? hoverTexts[i] : part;
            String processed = truncateIfNeeded(tr, part, widths, i);

            net.minecraft.text.MutableText partText = net.minecraft.text.Text.literal(processed);
            if (full != null && !full.equals(processed)) {
                partText.setStyle(partText.getStyle()
                        .withHoverEvent(new net.minecraft.text.HoverEvent.ShowText(
                                net.minecraft.text.Text.literal(full))));
            }
            fullText.append(partText);

            // 添加空格填充
            int spacesLen = (widths[i] - tr.getWidth(processed)) / tr.getWidth(" ");
            if (spacesLen > 0) {
                fullText.append(net.minecraft.text.Text.literal(" ".repeat(spacesLen)));
            }
            fullText.append(net.minecraft.text.Text.literal(" ".repeat(COL_SPACING)));
        }

        // 添加操作列 - 为每个操作符号单独创建带 click 事件的组件
        if (actions != null && actions.length > 0) {
            for (int i = 0; i < actions.length; i++) {
                SpotAction action = actions[i];
                net.minecraft.text.MutableText actionText = net.minecraft.text.Text.literal(action.getColoredSymbol());

                // 添加 hover 提示（显示操作名称）
                String actionDesc = Text.translatable(action.translationKey).getString();
                actionText.setStyle(actionText.getStyle()
                        .withHoverEvent(new net.minecraft.text.HoverEvent.ShowText(
                                net.minecraft.text.Text.literal(actionDesc))));

                // 添加 click 事件将命令填入聊天栏（玩家确认后执行）
                String command = action.getCommand(spotName);
                actionText.setStyle(actionText.getStyle()
                        .withClickEvent(new net.minecraft.text.ClickEvent.SuggestCommand(command)));

                fullText.append(actionText);

                // 添加空格分隔（除最后一个外）
                if (i < actions.length - 1) {
                    fullText.append(net.minecraft.text.Text.literal("§7 "));
                }
            }
        } else {
            // 无操作时显示 "-"
            fullText.append(net.minecraft.text.Text.literal("§7-"));
        }

        ClientPlayerEntity player = getPlayer();
        if (player != null) {
            player.sendMessage(fullText, false);
        }
    }

    private static int calculateTotalWidth(int[] widths) {
        int total = 0;
        for (int width : widths) total += width;
        // 加上列间距（n列有 n-1 个间距）
        if (widths.length > 1) {
            total += COL_SPACING * (widths.length - 1);
        }
        return total;
    }

    private static String padToWidth(net.minecraft.client.font.TextRenderer tr, int[] widths, String... parts) {
        if (parts == null) parts = new String[0];
        int totalTargetWidth = 0;
        int totalTextWidth = 0;
        String[] processedParts = new String[parts.length];
        int[] textWidths = new int[parts.length];

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i] != null ? parts[i] : "";
            processedParts[i] = truncateIfNeeded(tr, part, widths, i);
            textWidths[i] = tr.getWidth(processedParts[i]);

            totalTextWidth += textWidths[i];
            if (i < widths.length) {
                totalTargetWidth += widths[i];
            }
        }
        int totalSpacesNeeded = totalTargetWidth - totalTextWidth;

        StringBuilder sb = new StringBuilder();
        int spacesRemaining = totalSpacesNeeded;
        for (int i = 0; i < processedParts.length; i++) {
            sb.append(processedParts[i]);
            if (i < widths.length && spacesRemaining > 0) {
                int spacesToAdd = Math.min(spacesRemaining, (widths[i] - textWidths[i]) / tr.getWidth(" "));
                if (spacesToAdd > 0) {
                    sb.append(" ".repeat(spacesToAdd));
                    spacesRemaining -= spacesToAdd;
                }
            }

            // 添加列间距（除最后一列外）
            if (i < processedParts.length - 1) {
                sb.append(" ".repeat(COL_SPACING));
            }
        }
        return sb.toString();
    }

    /**
     * 如果文本超出指定列宽，截断并添加 "..."，否则返回原文本。
     */
    private static String truncateIfNeeded(net.minecraft.client.font.TextRenderer tr, String part, int[] widths, int colIndex) {
        if (part == null || part.isEmpty()) return "";
        if (colIndex >= widths.length) return part;

        int partWidth = tr.getWidth(part);
        int colWidth = widths[colIndex];

        if (partWidth > colWidth) {
            String truncated = truncateToWidth(tr, part, colWidth - tr.getWidth("..."));
            return truncated + "...";
        }
        return part;
    }

    /**
     * 截断文本使其宽度不超过指定值（不含省略号宽度）。
     */
    private static String truncateToWidth(net.minecraft.client.font.TextRenderer tr, String text, int maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            sb.append(c);
            if (tr.getWidth(sb.toString()) > maxWidth) {
                sb.deleteCharAt(sb.length() - 1);
                break;
            }
        }
        return sb.toString();
    }

    private static String formatDimension(String dimension) {
        return switch (dimension) {
            case "minecraft:overworld", "overworld" -> "§aO";
            case "minecraft:the_nether", "nether" -> "§cN";
            case "minecraft:the_end", "the_end", "end" -> "§dE";
            default -> dimension.substring(0, 1).toUpperCase();
        };
    }

    private static String localizeDimension(String dimension) {
        return switch (dimension) {
            case "minecraft:overworld", "overworld" -> Text.translatable("spotteddog.dimension.overworld").getString();
            case "minecraft:the_nether", "nether" -> Text.translatable("spotteddog.dimension.nether").getString();
            case "minecraft:the_end", "the_end", "end" -> Text.translatable("spotteddog.dimension.the_end").getString();
            default -> dimension;
        };
    }

    private static int debugUserData() {
        ClientPlayerEntity player = getPlayer();
        if (player == null) return 0;

        MinecraftClient client = MinecraftClient.getInstance();

        // 测试表头和分隔线宽度
        net.minecraft.client.font.TextRenderer tr = client.textRenderer;
        sendFeedback("[SpottedDog] ===== Table Width Debug =====");
        sendFeedback("SINGLEPLAYER_COLS total: " + SpotTableBuilder.calculateTotalWidth(SpotTableBuilder.SINGLEPLAYER_COLS));
        sendFeedback("MULTIPLAYER_COLS total: " + SpotTableBuilder.calculateTotalWidth(SpotTableBuilder.MULTIPLAYER_COLS));

        // 测试分隔线宽度
        StringBuilder sep = new StringBuilder("§7");
        int mpWidth = SpotTableBuilder.calculateTotalWidth(SpotTableBuilder.MULTIPLAYER_COLS);
        while (tr.getWidth(sep.toString()) < mpWidth) {
            sep.append("-");
        }
        sendFeedback("Separator width: " + tr.getWidth(sep.toString()) + " chars: " + sep.length());
        sendFeedback("Separator text: " + sep);

        return Command.SINGLE_SUCCESS;
    }

    private static int publishSpot(String name) {
        ClientPlayerEntity player = getPlayer();
        if (player == null) return 0;

        // 检查 Spot 是否存在
        Optional<Spot> spot = dataManager.getSpot(name);
        if (spot.isEmpty()) {
            sendSystemMessage("spotteddog.spot.not.found", name);
            return Command.SINGLE_SUCCESS;
        }

        // 使用策略模式（单人模式会提示不可用）
        SpotHandler.publishSpot(player, spot.get());
        return Command.SINGLE_SUCCESS;
    }

    private static int unpublishSpot(String name) {
        ClientPlayerEntity player = getPlayer();
        if (player == null) return 0;

        // 使用策略模式（单人模式会提示不可用）
        SpotHandler.unpublishSpot(player, name);
        return Command.SINGLE_SUCCESS;
    }


    private static int listTeleportLogs(int count) {
        ClientPlayerEntity player = getPlayer();
        if (player == null) return 0;

        // 使用策略模式
        SpotHandler.showLogs(player, count);
        return Command.SINGLE_SUCCESS;
    }

    private static int clearTeleportLogs() {
        ClientPlayerEntity player = getPlayer();
        if (player == null) return 0;

        // 使用策略模式
        SpotHandler.clearLogs(player);
        return Command.SINGLE_SUCCESS;
    }
}
