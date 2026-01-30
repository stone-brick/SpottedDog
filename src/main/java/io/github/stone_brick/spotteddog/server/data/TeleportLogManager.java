package io.github.stone_brick.spotteddog.server.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.util.WorldSavePath;

/**
 * 服务端传送日志管理器。
 * 负责日志的存储、查询和管理，支持日志轮转。
 *
 * 数据路径: data/<mode>/<world>/teleport_logs.json
 */
public class TeleportLogManager {

    private static final String LOG_FILE = "teleport_logs.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static TeleportLogManager instance;
    private List<TeleportLog> logs = new ArrayList<>();
    private MinecraftServer server;
    private Integer maxLogCount;

    private TeleportLogManager() {
    }

    public static synchronized TeleportLogManager getInstance() {
        if (instance == null) {
            instance = new TeleportLogManager();
        }
        return instance;
    }

    /**
     * 初始化管理器，必须在服务端启动时调用。
     */
    public synchronized void initialize(MinecraftServer server) {
        this.server = server;
        loadLogs();
    }

    /**
     * 获取数据目录根路径。
     */
    private Path getDataRootDirectory() {
        return FabricLoader.getInstance().getConfigDir().resolve("spotteddog").resolve("data");
    }

    /**
     * 获取当前世界的数据目录。
     */
    private Path getWorldDataDirectory() {
        String worldId = getWorldIdentifier();
        String safeName = safeFileName(worldId);
        boolean isSingleplayer = server != null && server.isDedicated() == false;
        String mode = isSingleplayer ? "singleplayer" : "multiplayer";
        return getDataRootDirectory().resolve(mode).resolve(safeName);
    }

    /**
     * 获取世界标识符。
     */
    private String getWorldIdentifier() {
        if (server != null) {
            Path savePath = server.getSavePath(WorldSavePath.ROOT).getParent();
            if (savePath != null) {
                return savePath.getFileName().toString();
            }
        }
        return "server";
    }

    /**
     * 将名称转换为安全的文件夹名称。
     */
    private String safeFileName(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    /**
     * 获取日志文件路径。
     */
    private Path getLogFile() {
        return getWorldDataDirectory().resolve(LOG_FILE);
    }

    /**
     * 加载日志数据。
     */
    private synchronized void loadLogs() {
        try {
            Path file = getLogFile();
            if (Files.exists(file)) {
                String json = Files.readString(file);
                Type listType = new TypeToken<List<TeleportLog>>() {}.getType();
                logs = GSON.fromJson(json, listType);
                if (logs == null) {
                    logs = new ArrayList<>();
                }
            }
        } catch (IOException e) {
            logs = new ArrayList<>();
        }
    }

    /**
     * 保存日志数据。
     */
    private synchronized void saveLogs() {
        try {
            Files.createDirectories(getLogFile().getParent());
            pruneLogsIfNeeded();
            String json = GSON.toJson(logs);
            Files.writeString(getLogFile(), json);
        } catch (IOException e) {
            // 忽略保存失败
        }
    }

    /**
     * 删除多余的旧日志。
     */
    private synchronized void pruneLogsIfNeeded() {
        int maxCount = getMaxLogCount();
        while (logs.size() > maxCount) {
            logs.remove(0);
        }
    }

    /**
     * 获取最大日志条数配置。
     */
    private int getMaxLogCount() {
        if (maxLogCount == null) {
            maxLogCount = io.github.stone_brick.spotteddog.server.config.ConfigManager.getTeleportLogMaxEntries();
        }
        return maxLogCount;
    }

    /**
     * 记录一次传送。
     */
    public synchronized void logTeleport(TeleportLog entry) {
        if (!io.github.stone_brick.spotteddog.server.config.ConfigManager.isTeleportLogEnabled()) {
            return;
        }
        logs.add(entry);
        saveLogs();
    }

    /**
     * 获取最近的日志（倒序，最新的在前）。
     */
    public synchronized List<TeleportLog> getRecentLogs(int count) {
        List<TeleportLog> result = new ArrayList<>(logs);
        Collections.reverse(result);
        if (result.size() > count) {
            return result.subList(0, count);
        }
        return result;
    }

    /**
     * 获取所有日志（倒序）。
     */
    public synchronized List<TeleportLog> getAllLogs() {
        List<TeleportLog> result = new ArrayList<>(logs);
        Collections.reverse(result);
        return result;
    }

    /**
     * 获取日志总数。
     */
    public synchronized int getLogCount() {
        return logs.size();
    }

    /**
     * 清空所有日志。
     */
    public synchronized void clearLogs() {
        logs.clear();
        saveLogs();
    }
}
