package io.github.stone_brick.spotteddog.client.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 客户端传送日志管理器。
 * 单人模式下记录本地传送日志。
 */
public class TeleportLogManager {

    private static final String LOG_FILE = "teleport_logs.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DATA_DIR = FabricLoader.getInstance().getConfigDir().resolve("spotteddog").resolve("data");

    private static TeleportLogManager instance;
    private List<ClientTeleportLog> logs = new ArrayList<>();

    private TeleportLogManager() {
    }

    public static synchronized TeleportLogManager getInstance() {
        if (instance == null) {
            instance = new TeleportLogManager();
        }
        return instance;
    }

    /**
     * 获取当前世界的数据目录。
     */
    private Path getWorldDataDirectory() {
        MinecraftClient client = MinecraftClient.getInstance();
        String worldId;
        if (client.isInSingleplayer() && client.getServer() != null) {
            Path worldPath = client.getServer().getSavePath(WorldSavePath.ROOT).getParent();
            worldId = worldPath.getFileName().toString();
        } else {
            worldId = "unknown";
        }
        String safeName = worldId.replaceAll("[\\\\/:*?\"<>|]", "_");
        return DATA_DIR.resolve("singleplayer").resolve(safeName);
    }

    private Path getLogFile() {
        return getWorldDataDirectory().resolve(LOG_FILE);
    }

    private synchronized void loadLogs() {
        try {
            Path file = getLogFile();
            if (Files.exists(file)) {
                String json = Files.readString(file);
                Type listType = new TypeToken<List<ClientTeleportLog>>() {}.getType();
                logs = GSON.fromJson(json, listType);
                if (logs == null) {
                    logs = new ArrayList<>();
                }
            }
        } catch (IOException e) {
            logs = new ArrayList<>();
        }
    }

    private synchronized void saveLogs() {
        try {
            Files.createDirectories(getLogFile().getParent());
            // 保留最近 1000 条
            while (logs.size() > 1000) {
                logs.remove(0);
            }
            String json = GSON.toJson(logs);
            Files.writeString(getLogFile(), json);
        } catch (IOException e) {
            // 忽略保存失败
        }
    }

    /**
     * 记录一次传送。
     */
    public synchronized void logTeleport(String type, String spotName,
                                          String sourceDim, double sourceX, double sourceY, double sourceZ,
                                          String targetDim, double targetX, double targetY, double targetZ) {
        loadLogs();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        ClientTeleportLog log = new ClientTeleportLog();
        log.timestamp = Instant.now().toString();
        log.playerName = client.player.getName().getString();
        log.playerUuid = client.player.getUuid().toString();
        log.teleportType = type;
        log.spotName = spotName;
        log.sourceDimension = sourceDim;
        log.sourceX = sourceX;
        log.sourceY = sourceY;
        log.sourceZ = sourceZ;
        log.targetDimension = targetDim;
        log.targetX = targetX;
        log.targetY = targetY;
        log.targetZ = targetZ;

        logs.add(log);
        saveLogs();
    }

    /**
     * 获取最近的日志（倒序，最新的在前）。
     */
    public synchronized List<ClientTeleportLog> getRecentLogs(int count) {
        loadLogs();
        List<ClientTeleportLog> result = new ArrayList<>(logs);
        Collections.reverse(result);
        return result.size() > count ? result.subList(0, count) : result;
    }

    /**
     * 获取日志总数。
     */
    public synchronized int getLogCount() {
        loadLogs();
        return logs.size();
    }

    /**
     * 清空所有日志。
     */
    public synchronized void clearLogs() {
        logs.clear();
        saveLogs();
    }

    /**
     * 客户端日志数据类。
     */
    public static class ClientTeleportLog {
        public String timestamp;
        public String playerName;
        public String playerUuid;
        public String teleportType;
        public String spotName;
        public String sourceDimension;
        public double sourceX, sourceY, sourceZ;
        public String targetDimension;
        public double targetX, targetY, targetZ;
    }
}
