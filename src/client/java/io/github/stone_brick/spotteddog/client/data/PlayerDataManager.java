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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class PlayerDataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path DATA_DIR = FabricLoader.getInstance().getConfigDir().resolve("spotteddog");
    private static final Path SPOTS_SINGLEPLAYER_FILE = DATA_DIR.resolve("spots_singleplayer.json");
    private static final Path SPOTS_MULTIPLAYER_FILE = DATA_DIR.resolve("spots_multiplayer.json");
    private static final Path SPOTS_FILE = DATA_DIR.resolve("spots.json"); // 旧文件，兼容迁移

    private static PlayerDataManager instance;
    private List<Spot> singleplayerSpots = new ArrayList<>();
    private List<Spot> multiplayerSpots = new ArrayList<>();

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
        // 优先加载新格式文件
        singleplayerSpots = loadSpotsFromFile(SPOTS_SINGLEPLAYER_FILE);
        multiplayerSpots = loadSpotsFromFile(SPOTS_MULTIPLAYER_FILE);

        // 如果新格式文件不存在，尝试从旧文件迁移
        if (singleplayerSpots.isEmpty() && multiplayerSpots.isEmpty() && Files.exists(SPOTS_FILE)) {
            List<Spot> oldSpots = loadSpotsFromFile(SPOTS_FILE);
            for (Spot spot : oldSpots) {
                if (spot.getWorldIdentifier() != null && spot.getWorldIdentifier().startsWith("singleplayer:")) {
                    singleplayerSpots.add(spot);
                } else {
                    multiplayerSpots.add(spot);
                }
            }
            // 保存为新格式
            saveSpotsToFile(SPOTS_SINGLEPLAYER_FILE, singleplayerSpots);
            saveSpotsToFile(SPOTS_MULTIPLAYER_FILE, multiplayerSpots);
        }
    }

    private List<Spot> loadSpotsFromFile(Path file) {
        try {
            if (Files.exists(file)) {
                String json = Files.readString(file);
                Type listType = new TypeToken<List<Spot>>() {}.getType();
                List<Spot> spots = GSON.fromJson(json, listType);
                return spots != null ? spots : new ArrayList<>();
            }
        } catch (IOException e) {
            // 忽略加载错误
        }
        return new ArrayList<>();
    }

    private void saveSpotsToFile(Path file, List<Spot> spots) {
        try {
            Files.createDirectories(DATA_DIR);
            String json = GSON.toJson(spots);
            Files.writeString(file, json);
        } catch (IOException e) {
            // 忽略保存错误
        }
    }

    // 根据 worldIdentifier 获取对应的 Spot 列表
    private List<Spot> getSpotList(String worldIdentifier) {
        if (worldIdentifier != null && worldIdentifier.startsWith("singleplayer:")) {
            return singleplayerSpots;
        }
        return multiplayerSpots;
    }

    // 保存指定类型的 Spot 列表
    private void saveSpotList(String worldIdentifier) {
        if (worldIdentifier != null && worldIdentifier.startsWith("singleplayer:")) {
            saveSpotsToFile(SPOTS_SINGLEPLAYER_FILE, singleplayerSpots);
        } else {
            saveSpotsToFile(SPOTS_MULTIPLAYER_FILE, multiplayerSpots);
        }
    }

    public void saveAll() {
        saveSpotsToFile(SPOTS_SINGLEPLAYER_FILE, singleplayerSpots);
        saveSpotsToFile(SPOTS_MULTIPLAYER_FILE, multiplayerSpots);
    }

    // Spot operations
    public boolean addSpot(String name, double x, double y, double z, float yaw, float pitch, String dimension, String world, String worldIdentifier) {
        List<Spot> spots = getSpotList(worldIdentifier);
        if (spots.stream().anyMatch(s -> s.getName().equals(name) && Objects.equals(s.getWorldIdentifier(), worldIdentifier))) {
            return false;
        }
        Spot spot = new Spot(UUID.randomUUID().toString(), name, x, y, z, yaw, pitch, dimension, world, worldIdentifier, false);
        spots.add(spot);
        saveSpotList(worldIdentifier);
        return true;
    }

    public boolean removeSpot(String name, String worldIdentifier) {
        List<Spot> spots = getSpotList(worldIdentifier);
        boolean removed = spots.removeIf(s -> s.getName().equals(name) && Objects.equals(s.getWorldIdentifier(), worldIdentifier));
        if (removed) {
            saveSpotList(worldIdentifier);
        }
        return removed;
    }

    public boolean updateSpotPosition(String name, double x, double y, double z, float yaw, float pitch, String dimension, String world, String worldIdentifier) {
        List<Spot> spots = getSpotList(worldIdentifier);
        Optional<Spot> spot = spots.stream().filter(s -> s.getName().equals(name) && Objects.equals(s.getWorldIdentifier(), worldIdentifier)).findFirst();
        if (spot.isPresent()) {
            spot.get().setPositionAndRotation(x, y, z, yaw, pitch, dimension, world, worldIdentifier);
            saveSpotList(worldIdentifier);
            return true;
        }
        return false;
    }

    public boolean renameSpot(String oldName, String newName, String worldIdentifier) {
        List<Spot> spots = getSpotList(worldIdentifier);
        Optional<Spot> spot = spots.stream().filter(s -> s.getName().equals(oldName) && Objects.equals(s.getWorldIdentifier(), worldIdentifier)).findFirst();
        if (spot.isPresent() && spots.stream().noneMatch(s -> s.getName().equals(newName) && Objects.equals(s.getWorldIdentifier(), worldIdentifier))) {
            spot.get().setName(newName);
            saveSpotList(worldIdentifier);
            return true;
        }
        return false;
    }

    public Optional<Spot> getSpot(String name, String worldIdentifier) {
        List<Spot> spots = getSpotList(worldIdentifier);
        return spots.stream().filter(s -> s.getName().equals(name) && Objects.equals(s.getWorldIdentifier(), worldIdentifier)).findFirst();
    }

    // 获取当前世界的所有 spots
    public List<Spot> getAllSpots(String worldIdentifier) {
        List<Spot> spots = getSpotList(worldIdentifier);
        return spots.stream()
                .filter(s -> Objects.equals(s.getWorldIdentifier(), worldIdentifier))
                .toList();
    }

    public boolean spotExists(String name, String worldIdentifier) {
        List<Spot> spots = getSpotList(worldIdentifier);
        return spots.stream().anyMatch(s -> s.getName().equals(name) && Objects.equals(s.getWorldIdentifier(), worldIdentifier));
    }

    // 设置 Spot 公开状态
    public boolean setSpotPublic(String name, String worldIdentifier, boolean isPublic) {
        List<Spot> spots = getSpotList(worldIdentifier);
        Optional<Spot> spot = spots.stream().filter(s -> s.getName().equals(name) && Objects.equals(s.getWorldIdentifier(), worldIdentifier)).findFirst();
        if (spot.isPresent()) {
            spot.get().setPublic(isPublic);
            saveSpotList(worldIdentifier);
            return true;
        }
        return false;
    }

    // 获取 Spot 公开状态
    public boolean isSpotPublic(String name, String worldIdentifier) {
        List<Spot> spots = getSpotList(worldIdentifier);
        return spots.stream().anyMatch(s -> s.getName().equals(name) && Objects.equals(s.getWorldIdentifier(), worldIdentifier) && s.isPublic());
    }
}
