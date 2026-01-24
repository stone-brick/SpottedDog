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
    private static final Path SPOTS_FILE = DATA_DIR.resolve("spots.json");

    private static PlayerDataManager instance;
    private List<Spot> spots = new ArrayList<>();

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
        try {
            if (Files.exists(SPOTS_FILE)) {
                String json = Files.readString(SPOTS_FILE);
                Type listType = new TypeToken<List<Spot>>() {}.getType();
                spots = GSON.fromJson(json, listType);
                if (spots == null) {
                    spots = new ArrayList<>();
                }
            }
        } catch (IOException e) {
            spots = new ArrayList<>();
        }
    }

    public void saveSpots() {
        try {
            Files.createDirectories(DATA_DIR);
            String json = GSON.toJson(spots);
            Files.writeString(SPOTS_FILE, json);
        } catch (IOException e) {
        }
    }

    // Spot operations - 新增带 worldIdentifier 参数的方法
    public boolean addSpot(String name, double x, double y, double z, String dimension, String world, String worldIdentifier) {
        if (spots.stream().anyMatch(s -> s.getName().equals(name) && Objects.equals(s.getWorldIdentifier(), worldIdentifier))) {
            return false;
        }
        Spot spot = new Spot(UUID.randomUUID().toString(), name, x, y, z, dimension, world, worldIdentifier);
        spots.add(spot);
        saveSpots();
        return true;
    }

    public boolean removeSpot(String name, String worldIdentifier) {
        boolean removed = spots.removeIf(s -> s.getName().equals(name) && Objects.equals(s.getWorldIdentifier(), worldIdentifier));
        if (removed) {
            saveSpots();
        }
        return removed;
    }

    public boolean updateSpotPosition(String name, double x, double y, double z, String dimension, String world, String worldIdentifier) {
        Optional<Spot> spot = spots.stream().filter(s -> s.getName().equals(name) && Objects.equals(s.getWorldIdentifier(), worldIdentifier)).findFirst();
        if (spot.isPresent()) {
            spot.get().setPosition(x, y, z, dimension, world, worldIdentifier);
            saveSpots();
            return true;
        }
        return false;
    }

    public boolean renameSpot(String oldName, String newName, String worldIdentifier) {
        Optional<Spot> spot = spots.stream().filter(s -> s.getName().equals(oldName) && Objects.equals(s.getWorldIdentifier(), worldIdentifier)).findFirst();
        if (spot.isPresent() && spots.stream().noneMatch(s -> s.getName().equals(newName) && Objects.equals(s.getWorldIdentifier(), worldIdentifier))) {
            spot.get().setName(newName);
            saveSpots();
            return true;
        }
        return false;
    }

    public Optional<Spot> getSpot(String name, String worldIdentifier) {
        return spots.stream().filter(s -> s.getName().equals(name) && Objects.equals(s.getWorldIdentifier(), worldIdentifier)).findFirst();
    }

    // 获取当前世界的所有 spots
    public List<Spot> getAllSpots(String worldIdentifier) {
        return spots.stream()
                .filter(s -> Objects.equals(s.getWorldIdentifier(), worldIdentifier))
                .toList();
    }

    public boolean spotExists(String name, String worldIdentifier) {
        return spots.stream().anyMatch(s -> s.getName().equals(name) && Objects.equals(s.getWorldIdentifier(), worldIdentifier));
    }
}
