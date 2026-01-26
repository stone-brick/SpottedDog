package io.github.stone_brick.spotteddog.server.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 服务端配置管理类。
 * 管理模组配置文件，支持冷却时间等设置。
 */
public class ConfigManager {

    private static final String CONFIG_FILE_NAME = "spotteddog_config.json";
    private static final Path CONFIG_DIR = FabricLoader.getInstance().getConfigDir().resolve("spotteddog");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve(CONFIG_FILE_NAME);

    private static Config instance;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * 配置数据类。
     */
    public static class Config {
        @SerializedName("teleport_cooldown_seconds")
        public int teleportCooldownSeconds = 1;

        @SerializedName("max_teleports_per_second")
        public int maxTeleportsPerSecond = 10;

        @SerializedName("allow_all_players_teleport")
        public boolean allowAllPlayersTeleport = false; // 默认仅OP可使用传送功能

        @SerializedName("allow_all_players_public_spot")
        public boolean allowAllPlayersPublicSpot = false; // 默认仅OP可使用公开 Spot 功能

        @SerializedName("allow_all_players_public_spot_teleport")
        public boolean allowAllPlayersPublicSpotTeleport = false; // 默认仅OP可传送到公开 Spot

        @SerializedName("public_spot_cooldown_seconds")
        public int publicSpotCooldownSeconds = 5; // 公开/取消公开 Spot 冷却时间

        @SerializedName("max_public_spot_requests_per_second")
        public int maxPublicSpotRequestsPerSecond = 10; // 每秒最大公开/取消公开请求数
    }

    /**
     * 获取配置实例，如果不存在则创建默认配置。
     */
    public static Config getConfig() {
        if (instance == null) {
            loadOrCreate();
        }
        return instance;
    }

    /**
     * 加载配置，如果不存在则创建默认配置。
     */
    public static synchronized void loadOrCreate() {
        try {
            // 确保配置目录存在
            if (!Files.exists(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
            }

            if (Files.exists(CONFIG_FILE)) {
                // 加载现有配置
                String json = Files.readString(CONFIG_FILE);
                instance = GSON.fromJson(json, Config.class);
                if (instance == null) {
                    instance = new Config();
                    save();
                }
            } else {
                // 创建默认配置
                instance = new Config();
                save();
            }
        } catch (IOException e) {
            instance = new Config();
        }
    }

    /**
     * 保存当前配置到文件。
     */
    public static synchronized void save() {
        try {
            if (!Files.exists(CONFIG_DIR)) {
                Files.createDirectories(CONFIG_DIR);
            }
            String json = GSON.toJson(instance);
            Files.writeString(CONFIG_FILE, json);
        } catch (IOException e) {
            // 忽略保存失败
        }
    }

    /**
     * 获取冷却时间（秒）。
     */
    public static int getTeleportCooldownSeconds() {
        return getConfig().teleportCooldownSeconds;
    }

    /**
     * 检查是否允许所有玩家使用传送功能。
     *
     * @return true 如果允许所有玩家使用传送
     */
    public static boolean isAllowAllPlayersTeleport() {
        return getConfig().allowAllPlayersTeleport;
    }

    /**
     * 检查是否允许所有玩家使用公开 Spot 功能。
     *
     * @return true 如果允许所有玩家使用公开 Spot
     */
    public static boolean isAllowAllPlayersPublicSpot() {
        return getConfig().allowAllPlayersPublicSpot;
    }

    /**
     * 获取公开/取消公开 Spot 的冷却时间（秒）。
     *
     * @return 冷却时间（秒）
     */
    public static int getPublicSpotCooldownSeconds() {
        return getConfig().publicSpotCooldownSeconds;
    }

    /**
     * 获取每秒最大公开/取消公开请求数。
     *
     * @return 最大请求数
     */
    public static int getMaxPublicSpotRequestsPerSecond() {
        return getConfig().maxPublicSpotRequestsPerSecond;
    }

    /**
     * 检查是否允许所有玩家传送到公开 Spot。
     *
     * @return true 如果允许所有玩家传送到公开 Spot
     */
    public static boolean isAllowAllPlayersPublicSpotTeleport() {
        return getConfig().allowAllPlayersPublicSpotTeleport;
    }

    /**
     * 获取配置文件路径（用于调试）。
     */
    public static Path getConfigPath() {
        return CONFIG_FILE;
    }
}
