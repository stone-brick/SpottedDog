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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 服务端公开 Spot 管理类。
 * 负责公开 Spot 的存储、查询和管理。
 */
public class PublicSpotManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String PUBLIC_SPOTS_FILE = "public_spots.json";

    private static PublicSpotManager instance;
    private List<PublicSpot> publicSpots = new ArrayList<>();
    private MinecraftServer server;

    private PublicSpotManager() {
    }

    public static synchronized PublicSpotManager getInstance() {
        if (instance == null) {
            instance = new PublicSpotManager();
        }
        return instance;
    }

    /**
     * 初始化管理器，必须在服务端启动时调用。
     */
    public synchronized void initialize(MinecraftServer server) {
        this.server = server;
        loadPublicSpots();
    }

    /**
     * 获取服务端数据目录。
     */
    private Path getDataDirectory() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        return configDir.resolve("spotteddog");
    }

    /**
     * 获取公开 Spot 文件路径。
     */
    private Path getPublicSpotsFile() {
        return getDataDirectory().resolve(PUBLIC_SPOTS_FILE);
    }

    /**
     * 加载公开 Spot 数据。
     */
    private synchronized void loadPublicSpots() {
        try {
            Path file = getPublicSpotsFile();
            if (Files.exists(file)) {
                String json = Files.readString(file);
                Type listType = new TypeToken<List<PublicSpot>>() {}.getType();
                publicSpots = GSON.fromJson(json, listType);
                if (publicSpots == null) {
                    publicSpots = new ArrayList<>();
                }
            }
        } catch (IOException e) {
            publicSpots = new ArrayList<>();
        }
    }

    /**
     * 保存公开 Spot 数据。
     */
    private synchronized void savePublicSpots() {
        try {
            Files.createDirectories(getDataDirectory());
            String json = GSON.toJson(publicSpots);
            Files.writeString(getPublicSpotsFile(), json);
        } catch (IOException e) {
            // 忽略保存失败
        }
    }

    /**
     * 公开一个 Spot。
     *
     * @param ownerName 所有者名称
     * @param ownerUuid 所有者 UUID
     * @param spotName Spot 名称
     * @param x 坐标 X
     * @param y 坐标 Y
     * @param z 坐标 Z
     * @param yaw 朝向 Yaw
     * @param pitch 朝向 Pitch
     * @param dimension 维度
     * @param world 世界名称
     * @param worldIdentifier 世界标识符
     * @return 公开成功返回 true，失败返回 false
     */
    public synchronized boolean publishSpot(String ownerName, String ownerUuid, String spotName,
                                            double x, double y, double z, float yaw, float pitch,
                                            String dimension, String world, String worldIdentifier) {
        // 检查该玩家是否已有同名的公开 Spot
        if (hasPublicSpot(ownerName, spotName, worldIdentifier)) {
            return false;
        }

        PublicSpot spot = new PublicSpot(
                UUID.randomUUID().toString(),
                ownerName,
                ownerUuid,
                spotName,
                x, y, z,
                yaw, pitch,
                dimension,
                world,
                worldIdentifier
        );

        publicSpots.add(spot);
        savePublicSpots();
        return true;
    }

    /**
     * 取消公开一个 Spot。
     *
     * @param ownerName 所有者名称
     * @param spotName Spot 名称
     * @param worldIdentifier 世界标识符
     * @return 取消成功返回 true，失败返回 false
     */
    public synchronized boolean unpublishSpot(String ownerName, String spotName, String worldIdentifier) {
        boolean removed = publicSpots.removeIf(s ->
                s.getOwnerName().equalsIgnoreCase(ownerName) &&
                        s.getDisplayName().equals(spotName) &&
                        s.getWorldIdentifier().equals(worldIdentifier));

        if (removed) {
            savePublicSpots();
        }
        return removed;
    }

    /**
     * 检查指定玩家的 Spot 是否已公开。
     */
    public synchronized boolean hasPublicSpot(String ownerName, String spotName, String worldIdentifier) {
        return publicSpots.stream().anyMatch(s ->
                s.getOwnerName().equals(ownerName) &&
                        s.getDisplayName().equals(spotName) &&
                        s.getWorldIdentifier().equals(worldIdentifier));
    }

    /**
     * 检查 Spot 名称是否已被公开（不允许重复）。
     */
    public synchronized boolean hasPublicSpot(String spotName, String worldIdentifier) {
        return publicSpots.stream().anyMatch(s ->
                s.getDisplayName().equals(spotName) &&
                        s.getWorldIdentifier().equals(worldIdentifier));
    }

    /**
     * 获取所有公开的 Spot。
     */
    public synchronized List<PublicSpot> getAllPublicSpots() {
        return new ArrayList<>(publicSpots);
    }

    /**
     * 获取指定世界的所有公开 Spot。
     */
    public synchronized List<PublicSpot> getPublicSpotsByWorld(String worldIdentifier) {
        return publicSpots.stream()
                .filter(s -> s.getWorldIdentifier().equals(worldIdentifier))
                .toList();
    }

    /**
     * 获取指定玩家的所有公开 Spot（不区分大小写）。
     */
    public synchronized List<PublicSpot> getPublicSpotsByOwner(String ownerName) {
        return publicSpots.stream()
                .filter(s -> s.getOwnerName().equalsIgnoreCase(ownerName))
                .toList();
    }

    /**
     * 获取指定玩家的所有公开 Spot（通过 UUID）。
     */
    public synchronized List<PublicSpot> getPublicSpotsByOwnerUuid(String ownerUuid) {
        return publicSpots.stream()
                .filter(s -> s.getOwnerUuid().equals(ownerUuid))
                .toList();
    }

    /**
     * 通过完整名称获取公开 Spot。
     * 完整名称格式：-spotName-ownerName
     * 例如：-home-stone_brick
     */
    public synchronized Optional<PublicSpot> getPublicSpotByFullName(String fullName, String worldIdentifier) {
        if (!fullName.startsWith("-")) {
            return Optional.empty();
        }

        String withoutPrefix = fullName.substring(1);
        int lastDashIndex = withoutPrefix.lastIndexOf('-');
        if (lastDashIndex == -1 || lastDashIndex == 0 || lastDashIndex == withoutPrefix.length() - 1) {
            return Optional.empty();
        }

        String spotName = withoutPrefix.substring(0, lastDashIndex);
        String ownerName = withoutPrefix.substring(lastDashIndex + 1);

        return publicSpots.stream()
                .filter(s -> s.getOwnerName().equalsIgnoreCase(ownerName))
                .filter(s -> s.getDisplayName().equals(spotName))
                .filter(s -> s.getWorldIdentifier().equals(worldIdentifier))
                .findFirst();
    }

    /**
     * 通过完整名称和玩家 UUID 获取公开 Spot。
     * 使用 UUID 匹配避免同一玩家从不同地址连接时找不到 Spot 的问题。
     * 完整名称格式：-spotName-ownerName
     * 例如：-home-stone_brick
     */
    public synchronized Optional<PublicSpot> getPublicSpotByFullNameWithUuid(String fullName, String ownerUuid) {
        if (!fullName.startsWith("-")) {
            return Optional.empty();
        }

        String withoutPrefix = fullName.substring(1);
        int lastDashIndex = withoutPrefix.lastIndexOf('-');
        if (lastDashIndex == -1 || lastDashIndex == 0 || lastDashIndex == withoutPrefix.length() - 1) {
            return Optional.empty();
        }

        String spotName = withoutPrefix.substring(0, lastDashIndex);
        // ownerName 只用于显示，不用于匹配

        return publicSpots.stream()
                .filter(s -> s.getOwnerUuid().equals(ownerUuid))
                .filter(s -> s.getDisplayName().equals(spotName))
                .findFirst();
    }

    /**
     * 解析公开 Spot 名称。
     * 支持格式：-spotName-ownerName
     * 例如：-home-stone_brick
     */
    public static PublicSpotName parsePublicSpotName(String name) {
        if (!name.startsWith("-")) {
            return null;
        }

        String withoutPrefix = name.substring(1);
        int lastDashIndex = withoutPrefix.lastIndexOf('-');
        if (lastDashIndex == -1 || lastDashIndex == 0 || lastDashIndex == withoutPrefix.length() - 1) {
            return null;
        }

        String spotName = withoutPrefix.substring(0, lastDashIndex);
        String ownerName = withoutPrefix.substring(lastDashIndex + 1);
        return new PublicSpotName(ownerName, spotName);
    }

    /**
     * 公开 Spot 名称解析结果。
     */
    public record PublicSpotName(String ownerName, String spotName) {}

    /**
     * 获取公开 Spot 数量（用于监控）。
     */
    public synchronized int getPublicSpotCount() {
        return publicSpots.size();
    }

    /**
     * 清除所有公开 Spot（用于测试）。
     */
    public synchronized void clearAll() {
        publicSpots.clear();
        savePublicSpots();
    }
}
