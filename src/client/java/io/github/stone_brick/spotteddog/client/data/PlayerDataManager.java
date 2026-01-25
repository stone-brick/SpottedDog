package io.github.stone_brick.spotteddog.client.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 玩家数据管理器。
 * 按存档/服务器分组管理 Spot 数据。
 */
public class PlayerDataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DATA_DIR = FabricLoader.getInstance().getConfigDir().resolve("spotteddog");
    private static final Path SPOTS_SINGLEPLAYER_FILE = DATA_DIR.resolve("spots_singleplayer.json");
    private static final Path SPOTS_MULTIPLAYER_FILE = DATA_DIR.resolve("spots_multiplayer.json");

    // 存档/服务器数据
    private static class WorldData {
        String worldIdentifier;
        String worldName;
        List<Spot> spots = new ArrayList<>();

        WorldData() {}

        WorldData(String worldIdentifier, String worldName) {
            this.worldIdentifier = worldIdentifier;
            this.worldName = worldName;
        }
    }

    private static PlayerDataManager instance;
    private Map<String, WorldData> singleplayerData = new HashMap<>();
    private Map<String, WorldData> multiplayerData = new HashMap<>();

    private PlayerDataManager() {
        loadSpots();
    }

    public static synchronized PlayerDataManager getInstance() {
        if (instance == null) {
            instance = new PlayerDataManager();
        }
        return instance;
    }

    public void reload() {
        loadSpots();
    }

    private void loadSpots() {
        singleplayerData = loadWorldDataMap(SPOTS_SINGLEPLAYER_FILE);
        multiplayerData = loadWorldDataMap(SPOTS_MULTIPLAYER_FILE);
    }

    private Map<String, WorldData> loadWorldDataMap(Path file) {
        Map<String, WorldData> map = new HashMap<>();
        try {
            if (Files.exists(file)) {
                String json = Files.readString(file);
                Type listType = new TypeToken<List<WorldData>>() {}.getType();
                List<WorldData> list = GSON.fromJson(json, listType);
                if (list != null) {
                    for (WorldData wd : list) {
                        map.put(wd.worldIdentifier, wd);
                    }
                }
            }
        } catch (IOException e) {
            // 忽略加载错误
        }
        return map;
    }

    private void saveWorldDataMap(Path file, Map<String, WorldData> map) {
        try {
            Files.createDirectories(DATA_DIR);
            List<WorldData> list = new ArrayList<>(map.values());
            String json = GSON.toJson(list);
            Files.writeString(file, json);
        } catch (IOException e) {
            // 忽略保存错误
        }
    }

    private Map<String, WorldData> getWorldDataMap(String worldIdentifier) {
        if (worldIdentifier != null && worldIdentifier.startsWith("singleplayer:")) {
            return singleplayerData;
        }
        return multiplayerData;
    }

    private void saveWorldData(String worldIdentifier) {
        if (worldIdentifier != null && worldIdentifier.startsWith("singleplayer:")) {
            saveWorldDataMap(SPOTS_SINGLEPLAYER_FILE, singleplayerData);
        } else {
            saveWorldDataMap(SPOTS_MULTIPLAYER_FILE, multiplayerData);
        }
    }

    private WorldData getOrCreateWorldData(String worldIdentifier, String worldName) {
        Map<String, WorldData> map = getWorldDataMap(worldIdentifier);
        WorldData wd = map.get(worldIdentifier);
        if (wd == null) {
            wd = new WorldData(worldIdentifier, worldName);
            map.put(worldIdentifier, wd);
        }
        return wd;
    }

    // Spot operations
    public boolean addSpot(String name, double x, double y, double z, float yaw, float pitch, String dimension, String world, String worldIdentifier) {
        Map<String, WorldData> map = getWorldDataMap(worldIdentifier);
        WorldData wd = getOrCreateWorldData(worldIdentifier, world);

        // 检查名称是否重复
        if (wd.spots.stream().anyMatch(s -> s.getName().equals(name))) {
            return false;
        }

        Spot spot = new Spot(name, x, y, z, yaw, pitch, dimension);
        wd.spots.add(spot);
        saveWorldData(worldIdentifier);
        return true;
    }

    public boolean removeSpot(String name, String worldIdentifier) {
        Map<String, WorldData> map = getWorldDataMap(worldIdentifier);
        WorldData wd = map.get(worldIdentifier);
        if (wd == null) return false;

        boolean removed = wd.spots.removeIf(s -> s.getName().equals(name));
        if (removed) {
            saveWorldData(worldIdentifier);
        }
        return removed;
    }

    public boolean updateSpotPosition(String name, double x, double y, double z, float yaw, float pitch, String dimension, String world, String worldIdentifier) {
        Map<String, WorldData> map = getWorldDataMap(worldIdentifier);
        WorldData wd = map.get(worldIdentifier);
        if (wd == null) return false;

        Optional<Spot> spot = wd.spots.stream().filter(s -> s.getName().equals(name)).findFirst();
        if (spot.isPresent()) {
            spot.get().setPositionAndRotation(x, y, z, yaw, pitch, dimension);
            saveWorldData(worldIdentifier);
            return true;
        }
        return false;
    }

    public boolean renameSpot(String oldName, String newName, String worldIdentifier) {
        Map<String, WorldData> map = getWorldDataMap(worldIdentifier);
        WorldData wd = map.get(worldIdentifier);
        if (wd == null) return false;

        Optional<Spot> spot = wd.spots.stream().filter(s -> s.getName().equals(oldName)).findFirst();
        if (spot.isPresent() && wd.spots.stream().noneMatch(s -> s.getName().equals(newName))) {
            spot.get().setName(newName);
            saveWorldData(worldIdentifier);
            return true;
        }
        return false;
    }

    public Optional<Spot> getSpot(String name, String worldIdentifier) {
        Map<String, WorldData> map = getWorldDataMap(worldIdentifier);
        WorldData wd = map.get(worldIdentifier);
        if (wd == null) return Optional.empty();
        return wd.spots.stream().filter(s -> s.getName().equals(name)).findFirst();
    }

    // 获取指定世界的所有 Spot
    public List<Spot> getAllSpots(String worldIdentifier) {
        Map<String, WorldData> map = getWorldDataMap(worldIdentifier);
        WorldData wd = map.get(worldIdentifier);
        if (wd == null) return new ArrayList<>();
        return new ArrayList<>(wd.spots);
    }

    public boolean spotExists(String name, String worldIdentifier) {
        Map<String, WorldData> map = getWorldDataMap(worldIdentifier);
        WorldData wd = map.get(worldIdentifier);
        if (wd == null) return false;
        return wd.spots.stream().anyMatch(s -> s.getName().equals(name));
    }

    // 设置 Spot 公开状态
    public boolean setSpotPublic(String name, String worldIdentifier, boolean isPublic) {
        Map<String, WorldData> map = getWorldDataMap(worldIdentifier);
        WorldData wd = map.get(worldIdentifier);
        if (wd == null) return false;

        Optional<Spot> spot = wd.spots.stream().filter(s -> s.getName().equals(name)).findFirst();
        if (spot.isPresent()) {
            spot.get().setPublic(isPublic);
            saveWorldData(worldIdentifier);
            return true;
        }
        return false;
    }

    // 获取 Spot 公开状态
    public boolean isSpotPublic(String name, String worldIdentifier) {
        Map<String, WorldData> map = getWorldDataMap(worldIdentifier);
        WorldData wd = map.get(worldIdentifier);
        if (wd == null) return false;
        return wd.spots.stream().anyMatch(s -> s.getName().equals(name) && s.isPublic());
    }
}
