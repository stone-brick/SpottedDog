package io.github.stone_brick.spotteddog.client.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.github.stone_brick.spotteddog.client.network.WhitelistAdminHandler;
import io.github.stone_brick.spotteddog.server.permission.WhitelistManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.List;

import static com.mojang.brigadier.arguments.StringArgumentType.getString;

/**
 * 白名单管理命令。
 * 仅 OP 可使用。
 *
 * 命令格式：
 * /spot whitelist teleport add <玩家名>
 * /spot whitelist teleport remove <玩家名>
 * /spot whitelist teleport list
 *
 * 同理支持 public 和 publictp
 */
@Environment(EnvType.CLIENT)
public final class WhitelistAdminCommand {

    private WhitelistAdminCommand() {
        // 工具类，禁止实例化
    }

    /**
     * 注册白名单管理命令。
     */
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        // /spot whitelist <type> <action> [玩家名]
        dispatcher.register(
                LiteralArgumentBuilder.<FabricClientCommandSource>literal("spot")
                        .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal("whitelist")
                                .then(registerWhitelistType("teleport", WhitelistManager.WhitelistType.TELEPORT))
                                .then(registerWhitelistType("public", WhitelistManager.WhitelistType.PUBLIC_SPOT))
                                .then(registerWhitelistType("publictp", WhitelistManager.WhitelistType.PUBLIC_SPOT_TELEPORT))
                        )
        );
    }

    /**
     * 为指定白名单类型注册命令。
     */
    private static LiteralArgumentBuilder<FabricClientCommandSource> registerWhitelistType(
            String typeName, WhitelistManager.WhitelistType type) {

        LiteralArgumentBuilder<FabricClientCommandSource> typeBuilder =
                LiteralArgumentBuilder.<FabricClientCommandSource>literal(typeName);

        // /spot whitelist <type> add <玩家名>
        typeBuilder = typeBuilder.then(
                LiteralArgumentBuilder.<FabricClientCommandSource>literal("add")
                        .then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument("player", StringArgumentType.word())
                                .suggests(onlinePlayerSuggestions())
                                .executes(context -> addPlayer(
                                        getString(context, "player"),
                                        type,
                                        context.getSource()
                                ))
                        )
        );

        // /spot whitelist <type> remove <玩家名>
        typeBuilder = typeBuilder.then(
                LiteralArgumentBuilder.<FabricClientCommandSource>literal("remove")
                        .then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument("player", StringArgumentType.word())
                                .suggests(whitelistPlayerSuggestions(type))
                                .executes(context -> removePlayer(
                                        getString(context, "player"),
                                        type,
                                        context.getSource()
                                ))
                        )
        );

        // /spot whitelist <type> list
        typeBuilder = typeBuilder.then(
                LiteralArgumentBuilder.<FabricClientCommandSource>literal("list")
                        .executes(context -> listWhitelist(type, context.getSource()))
        );

        return typeBuilder;
    }

    /**
     * 发送反馈消息。
     */
    private static void sendFeedback(FabricClientCommandSource source, String message) {
        source.sendFeedback(Text.literal(message));
    }

    /**
     * 添加玩家到白名单。
     */
    private static int addPlayer(String playerName, WhitelistManager.WhitelistType type,
                                 FabricClientCommandSource source) {
        // 发送请求给服务端处理
        WhitelistAdminHandler.sendWhitelistAction(playerName, type, true);

        sendFeedback(source, "[SpottedDog] 已提交白名单操作请求");

        return Command.SINGLE_SUCCESS;
    }

    /**
     * 从白名单移除玩家。
     */
    private static int removePlayer(String playerName, WhitelistManager.WhitelistType type,
                                    FabricClientCommandSource source) {
        // 发送请求给服务端处理
        WhitelistAdminHandler.sendWhitelistAction(playerName, type, false);

        sendFeedback(source, "[SpottedDog] 已提交白名单操作请求");

        return Command.SINGLE_SUCCESS;
    }

    /**
     * 列出白名单中的玩家。
     */
    private static int listWhitelist(WhitelistManager.WhitelistType type, FabricClientCommandSource source) {
        List<WhitelistManager.WhitelistEntry> entries = WhitelistManager.getAllEntries(type);

        source.sendFeedback(Text.literal("[SpottedDog] " + getTypeName(type) + " 白名单:"));

        if (entries.isEmpty()) {
            source.sendFeedback(Text.literal("  (空)"));
        } else {
            for (WhitelistManager.WhitelistEntry entry : entries) {
                source.sendFeedback(Text.literal("  - " + entry.name + " (" + entry.uuid + ")"));
            }
        }

        source.sendFeedback(Text.literal("共 " + entries.size() + " 名玩家"));

        return Command.SINGLE_SUCCESS;
    }

    /**
     * 获取白名单类型的中文名称。
     */
    private static String getTypeName(WhitelistManager.WhitelistType type) {
        return switch (type) {
            case TELEPORT -> "传送";
            case PUBLIC_SPOT -> "公开 Spot";
            case PUBLIC_SPOT_TELEPORT -> "公开 Spot 传送";
        };
    }

    /**
     * 在线玩家自动补全提供者。
     */
    private static SuggestionProvider<FabricClientCommandSource> onlinePlayerSuggestions() {
        return (context, builder) -> {
            String remaining = builder.getRemaining().toLowerCase();
            MinecraftClient client = MinecraftClient.getInstance();

            if (client.getServer() != null) {
                var playerManager = client.getServer().getPlayerManager();
                if (playerManager != null) {
                    var players = playerManager.getPlayerList();
                    for (var player : players) {
                        String name = player.getName().getString();
                        if (name.toLowerCase().startsWith(remaining)) {
                            builder.suggest(name);
                        }
                    }
                }
            }

            return builder.buildFuture();
        };
    }

    /**
     * 白名单玩家自动补全提供者。
     */
    private static SuggestionProvider<FabricClientCommandSource> whitelistPlayerSuggestions(
            WhitelistManager.WhitelistType type) {
        return (context, builder) -> {
            String remaining = builder.getRemaining().toLowerCase();
            List<WhitelistManager.WhitelistEntry> entries = WhitelistManager.getAllEntries(type);

            for (WhitelistManager.WhitelistEntry entry : entries) {
                if (entry.name != null && entry.name.toLowerCase().startsWith(remaining)) {
                    builder.suggest(entry.name);
                }
            }

            return builder.buildFuture();
        };
    }
}
