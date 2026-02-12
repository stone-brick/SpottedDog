package io.github.stone_brick.spotteddog.server.permission;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import io.github.stone_brick.spotteddog.event.AdminLogEvent;
import io.github.stone_brick.spotteddog.event.AdminLogEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 白名单管理器。
 * 管理三种功能的白名单：teleport、public_spot、public_spot_teleport
 */
public final class WhitelistManager {

    /**
     * 白名单类型枚举。
     */
    public enum WhitelistType {
        TELEPORT("teleport_whitelist.json"),
        PUBLIC_SPOT("public_spot_whitelist.json"),
        PUBLIC_SPOT_TELEPORT("public_spot_teleport_whitelist.json");

        private final String fileName;

        WhitelistType(String fileName) {
            this.fileName = fileName;
        }

        public String getFileName() {
            return fileName;
        }
    }

    /**
     * 白名单条目。
     */
    public static class WhitelistEntry {
        @SerializedName("uuid")
        public String uuid;

        @SerializedName("name")
        public String name;

        public WhitelistEntry() {
        }

        public WhitelistEntry(String uuid, String name) {
            this.uuid = uuid;
            this.name = name;
        }
    }

    /**
     * 白名单数据类。
     */
    public static class WhitelistData {
        @SerializedName("players")
        public List<WhitelistEntry> players = new ArrayList<>();
    }

    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("spotteddog");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // 缓存各类型白名单数据
    private static WhitelistData teleportWhitelist;
    private static WhitelistData publicSpotWhitelist;
    private static WhitelistData publicSpotTeleportWhitelist;

    private WhitelistManager() {
        // 工具类，禁止实例化
    }

    /**
     * 获取指定类型的白名单数据。
     */
    public static synchronized WhitelistData getWhitelist(WhitelistType type) {
        WhitelistData data = switch (type) {
            case TELEPORT -> teleportWhitelist;
            case PUBLIC_SPOT -> publicSpotWhitelist;
            case PUBLIC_SPOT_TELEPORT -> publicSpotTeleportWhitelist;
        };

        if (data == null) {
            data = loadWhitelist(type);
            switch (type) {
                case TELEPORT -> teleportWhitelist = data;
                case PUBLIC_SPOT -> publicSpotWhitelist = data;
                case PUBLIC_SPOT_TELEPORT -> publicSpotTeleportWhitelist = data;
            }
        }
        return data;
    }

    /**
     * 加载白名单数据。
     */
    private static WhitelistData loadWhitelist(WhitelistType type) {
        Path file = CONFIG_DIR.resolve(type.getFileName());

        try {
            if (!Files.exists(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
            }

            if (Files.exists(file)) {
                String json = Files.readString(file);
                WhitelistData data = GSON.fromJson(json, WhitelistData.class);
                if (data != null) {
                    return data;
                }
            }
        } catch (IOException e) {
            // 忽略加载失败
        }

        // 返回默认空配置
        return new WhitelistData();
    }

    /**
     * 保存白名单数据。
     */
    public static synchronized void saveWhitelist(WhitelistType type, WhitelistData data) {
        Path file = CONFIG_DIR.resolve(type.getFileName());

        try {
            if (!Files.exists(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
            }
            String json = GSON.toJson(data);
            Files.writeString(file, json);

            // 更新缓存
            switch (type) {
                case TELEPORT -> teleportWhitelist = data;
                case PUBLIC_SPOT -> publicSpotWhitelist = data;
                case PUBLIC_SPOT_TELEPORT -> publicSpotTeleportWhitelist = data;
            }
        } catch (IOException e) {
            // 忽略保存失败
        }
    }

    /**
     * 检查玩家是否在指定类型的白名单中。
     *
     * @param playerUuid 玩家 UUID
     * @param type       白名单类型
     * @return true 如果在白名单中
     */
    public static boolean isPlayerInWhitelist(UUID playerUuid, WhitelistType type) {
        WhitelistData data = getWhitelist(type);
        if (data.players == null) {
            return false;
        }

        String uuidString = playerUuid.toString().toLowerCase();
        for (WhitelistEntry entry : data.players) {
            if (entry.uuid != null && entry.uuid.equalsIgnoreCase(uuidString)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 将玩家添加到白名单。
     *
     * @param operator 执行操作的 OP 玩家（可为 null，如果为 null 则不触发事件）
     * @param playerUuid 玩家 UUID
     * @param playerName 玩家名称
     * @param type       白名单类型
     * @return true 如果添加成功（玩家原本不在白名单中）
     */
    public static synchronized boolean addPlayerToWhitelist(ServerPlayerEntity operator, UUID playerUuid, String playerName, WhitelistType type) {
        WhitelistData data = getWhitelist(type);

        if (data.players == null) {
            data.players = new ArrayList<>();
        }

        String uuidString = playerUuid.toString().toLowerCase();

        // 检查是否已存在
        for (WhitelistEntry entry : data.players) {
            if (entry.uuid != null && entry.uuid.equalsIgnoreCase(uuidString)) {
                // 更新玩家名称
                entry.name = playerName;
                saveWhitelist(type, data);
                return false;
            }
        }

        // 添加新条目
        data.players.add(new WhitelistEntry(uuidString, playerName));
        saveWhitelist(type, data);

        // 触发事件
        if (operator != null) {
            fireAdminEvent(operator, "whitelist_add", playerName, null, type.name());
        }

        return true;
    }

    /**
     * 将玩家从白名单移除。
     *
     * @param operator 执行操作的 OP 玩家（可为 null，如果为 null 则不触发事件）
     * @param playerUuid 玩家 UUID
     * @param playerName 玩家名称（用于日志记录）
     * @param type       白名单类型
     * @return true 如果移除成功（玩家原本在白名单中）
     */
    public static synchronized boolean removePlayerFromWhitelist(ServerPlayerEntity operator, UUID playerUuid, String playerName, WhitelistType type) {
        WhitelistData data = getWhitelist(type);
        if (data.players == null) {
            return false;
        }

        String uuidString = playerUuid.toString().toLowerCase();
        boolean removed = data.players.removeIf(entry ->
                entry.uuid != null && entry.uuid.equalsIgnoreCase(uuidString));

        if (removed) {
            saveWhitelist(type, data);

            // 触发事件
            if (operator != null) {
                fireAdminEvent(operator, "whitelist_remove", playerName, null, type.name());
            }
        }
        return removed;
    }

    /**
     * 通过玩家名称精确查找在线玩家 UUID。
     *
     * @param server Minecraft 服务器实例
     * @param name   玩家名称
     * @return 玩家 UUID（如果找不到则返回 null）
     */
    public static UUID findOnlinePlayerUuid(MinecraftServer server, String name) {
        if (server == null) {
            return null;
        }

        var playerManager = server.getPlayerManager();
        if (playerManager != null) {
            var player = playerManager.getPlayer(name);
            if (player != null) {
                return player.getUuid();
            }
        }
        return null;
    }

    /**
     * 获取白名单中所有条目。
     */
    public static List<WhitelistEntry> getAllEntries(WhitelistType type) {
        WhitelistData data = getWhitelist(type);
        return data.players != null ? new ArrayList<>(data.players) : new ArrayList<>();
    }

    /**
     * 触发管理操作日志事件。
     */
    private static void fireAdminEvent(ServerPlayerEntity operator, String operationType,
                                       String targetPlayer, String spotName, String whitelistType) {
        AdminLogEvent event = new AdminLogEvent(
                operator.getName().getString(),
                operator.getUuid().toString(),
                operationType,
                targetPlayer,
                spotName,
                whitelistType
        );
        AdminLogEvents.ADMIN_OPERATION.invoker().onAdminOperation(event);
    }
}
