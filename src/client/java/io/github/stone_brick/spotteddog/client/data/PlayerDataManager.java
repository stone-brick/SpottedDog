package io.github.stone_brick.spotteddog.client.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 玩家数据管理器。
 * 按存档/服务器分类管理 Spot 数据。
 *
 * 数据结构：
 * - 单人模式: data/singleplayer/<存档文件夹名>/spots.json
 * - 多人模式: data/multiplayer/<服务器地址>/spots.json
 */
public class PlayerDataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DATA_DIR = FabricLoader.getInstance().getConfigDir().resolve("spotteddog").resolve("data");

    private static PlayerDataManager instance;
    private String currentWorldIdentifier;
    private String currentWorldName;
    private List<Spot> spots = new ArrayList<>();

    private PlayerDataManager() {
    }

    public static synchronized PlayerDataManager getInstance() {
        if (instance == null) {
            instance = new PlayerDataManager();
        }
        return instance;
    }

    /**
     * 设置当前世界并加载数据。
     */
    public synchronized void setCurrentWorld(String worldIdentifier, String worldName) {
        this.currentWorldIdentifier = worldIdentifier;
        this.currentWorldName = worldName;
        loadSpots();
    }

    /**
     * 获取当前世界标识符。
     */
    public synchronized String getCurrentWorldIdentifier() {
        return currentWorldIdentifier;
    }

    /**
     * 获取当前世界名称。
     */
    public synchronized String getCurrentWorldName() {
        return currentWorldName;
    }

    /**
     * 获取当前世界的数据文件路径。
     */
    private Path getCurrentWorldFile() {
        if (currentWorldIdentifier == null) {
            throw new IllegalStateException("Current world not set");
        }

        Path worldDir;
        if (currentWorldIdentifier.startsWith("singleplayer:")) {
            // 单人模式: data/singleplayer/<存档名>/spots.json
            String worldDirName = currentWorldIdentifier.substring("singleplayer:".length());
            worldDir = DATA_DIR.resolve("singleplayer").resolve(worldDirName);
        } else {
            // 多人模式: data/multiplayer/<服务器地址>/spots.json
            String serverAddress = currentWorldIdentifier.substring("multiplayer:".length());
            worldDir = DATA_DIR.resolve("multiplayer").resolve(safeFileName(serverAddress));
        }
        return worldDir.resolve("spots.json");
    }

    /**
     * 将服务器地址转换为安全的文件夹名称。
     */
    private String safeFileName(String name) {
        // 替换非法字符为下划线
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private synchronized void loadSpots() {
        spots = new ArrayList<>();
        if (currentWorldIdentifier == null) {
            return;
        }

        try {
            Path file = getCurrentWorldFile();
            if (Files.exists(file)) {
                String json = Files.readString(file);
                Spot[] loaded = GSON.fromJson(json, Spot[].class);
                if (loaded != null) {
                    for (Spot spot : loaded) {
                        spots.add(spot);
                    }
                }
            }
        } catch (IOException e) {
            // 忽略加载错误
        }
    }

    private synchronized void saveSpots() {
        if (currentWorldIdentifier == null) {
            return;
        }

        try {
            Path file = getCurrentWorldFile();
            Files.createDirectories(file.getParent());
            String json = GSON.toJson(spots);
            Files.writeString(file, json);
        } catch (IOException e) {
            // 忽略保存错误
        }
    }

    public synchronized void reload() {
        loadSpots();
    }

    // Spot operations

    public synchronized boolean addSpot(String name, double x, double y, double z, float yaw, float pitch, String dimension) {
        // 检查名称是否重复
        if (spots.stream().anyMatch(s -> s.getName().equals(name))) {
            return false;
        }

        Spot spot = new Spot(name, x, y, z, yaw, pitch, dimension);
        spots.add(spot);
        saveSpots();
        return true;
    }

    public synchronized boolean removeSpot(String name) {
        boolean removed = spots.removeIf(s -> s.getName().equals(name));
        if (removed) {
            saveSpots();
        }
        return removed;
    }

    public synchronized boolean updateSpotPosition(String name, double x, double y, double z, float yaw, float pitch, String dimension) {
        Optional<Spot> spot = spots.stream().filter(s -> s.getName().equals(name)).findFirst();
        if (spot.isPresent()) {
            spot.get().setPositionAndRotation(x, y, z, yaw, pitch, dimension);
            saveSpots();
            return true;
        }
        return false;
    }

    public synchronized boolean renameSpot(String oldName, String newName) {
        Optional<Spot> spot = spots.stream().filter(s -> s.getName().equals(oldName)).findFirst();
        if (spot.isPresent() && spots.stream().noneMatch(s -> s.getName().equals(newName))) {
            spot.get().setName(newName);
            saveSpots();
            return true;
        }
        return false;
    }

    public synchronized Optional<Spot> getSpot(String name) {
        return spots.stream().filter(s -> s.getName().equals(name)).findFirst();
    }

    public synchronized List<Spot> getAllSpots() {
        return new ArrayList<>(spots);
    }

    public synchronized boolean spotExists(String name) {
        return spots.stream().anyMatch(s -> s.getName().equals(name));
    }
}
