package io.github.stone_brick.spotteddog.server.config;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 玩家传送冷却时间管理器。
 * 跟踪每个玩家的最后传送时间，防止频繁传送。
 * 同时提供全局速率限制，防止服务端过载。
 */
public class CooldownManager {

    // 公开 Spot 列表请求冷却时间（毫秒）
    private static final long PUBLIC_LIST_COOLDOWN_MS = 5000;

    private static final ConcurrentHashMap<UUID, Long> playerCooldowns = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Long> publicListRequests = new ConcurrentHashMap<>();
    private static final AtomicLong teleportsThisSecond = new AtomicLong(0);
    private static long lastSecondBoundary = System.currentTimeMillis() / 1000;

    /**
     * 检查玩家是否在冷却中。
     *
     * @param player 玩家
     * @return true 如果在冷却中，false 可以传送
     */
    public static boolean isInCooldown(ServerPlayerEntity player) {
        Long lastTeleport = playerCooldowns.get(player.getUuid());
        if (lastTeleport == null) {
            return false;
        }

        long now = System.currentTimeMillis();
        long cooldownMs = ConfigManager.getTeleportCooldownSeconds() * 1000L;
        return (now - lastTeleport) < cooldownMs;
    }

    /**
     * 获取玩家剩余冷却时间（秒）。
     *
     * @param player 玩家
     * @return 剩余冷却时间，0 表示没有冷却
     */
    public static int getRemainingCooldown(ServerPlayerEntity player) {
        Long lastTeleport = playerCooldowns.get(player.getUuid());
        if (lastTeleport == null) {
            return 0;
        }

        long now = System.currentTimeMillis();
        long cooldownMs = ConfigManager.getTeleportCooldownSeconds() * 1000L;
        long remaining = cooldownMs - (now - lastTeleport);

        return remaining > 0 ? (int) Math.ceil(remaining / 1000.0) : 0;
    }

    /**
     * 检查全局速率限制。
     *
     * @return true 如果可以继续传送，false 如果超出限制
     */
    public static boolean checkGlobalRateLimit() {
        long now = System.currentTimeMillis() / 1000;
        if (now != lastSecondBoundary) {
            // 新的一秒，重置计数器
            teleportsThisSecond.set(0);
            lastSecondBoundary = now;
        }
        return teleportsThisSecond.get() < ConfigManager.getConfig().maxTeleportsPerSecond;
    }

    /**
     * 尝试增加全局传送计数。
     *
     * @return true 如果成功增加，false 如果超出限制
     */
    public static boolean tryIncrementGlobalCount() {
        long now = System.currentTimeMillis() / 1000;
        if (now != lastSecondBoundary) {
            // 新的一秒，重置计数器
            teleportsThisSecond.set(0);
            lastSecondBoundary = now;
        }
        long current;
        long max = ConfigManager.getConfig().maxTeleportsPerSecond;
        do {
            current = teleportsThisSecond.get();
            if (current >= max) {
                return false;
            }
        } while (!teleportsThisSecond.compareAndSet(current, current + 1));
        return true;
    }

    /**
     * 更新玩家的最后传送时间（开始冷却）。
     *
     * @param player 玩家
     */
    public static void updateLastTeleport(ServerPlayerEntity player) {
        playerCooldowns.put(player.getUuid(), System.currentTimeMillis());
    }

    /**
     * 清除玩家的冷却时间（用于测试或管理命令）。
     *
     * @param player 玩家
     */
    public static void clearCooldown(ServerPlayerEntity player) {
        playerCooldowns.remove(player.getUuid());
    }

    /**
     * 清除所有冷却时间（用于测试）。
     */
    public static void clearAllCooldowns() {
        playerCooldowns.clear();
        publicListRequests.clear();
        teleportsThisSecond.set(0);
    }

    /**
     * 获取当前冷却中的玩家数量（用于监控）。
     */
    public static int getCooldownCount() {
        return playerCooldowns.size();
    }

    /**
     * 获取当前秒的传送次数（用于监控）。
     */
    public static int getCurrentSecondCount() {
        long now = System.currentTimeMillis() / 1000;
        if (now != lastSecondBoundary) {
            return 0;
        }
        return (int) teleportsThisSecond.get();
    }

    // ===== 公开 Spot 列表请求冷却管理 =====

    /**
     * 检查玩家是否可以请求公开 Spot 列表。
     *
     * @param player 玩家
     * @return true 如果可以请求，false 如果在冷却中
     */
    public static boolean canRequestPublicList(ServerPlayerEntity player) {
        Long lastRequest = publicListRequests.get(player.getUuid());
        if (lastRequest == null) {
            return true;
        }
        long now = System.currentTimeMillis();
        return (now - lastRequest) >= PUBLIC_LIST_COOLDOWN_MS;
    }

    /**
     * 记录玩家的公开列表请求时间。
     *
     * @param player 玩家
     */
    public static void recordPublicListRequest(ServerPlayerEntity player) {
        publicListRequests.put(player.getUuid(), System.currentTimeMillis());
    }

    /**
     * 获取玩家公开列表请求的剩余冷却时间（秒）。
     *
     * @param player 玩家
     * @return 剩余冷却时间，0 表示没有冷却
     */
    public static int getPublicListRemainingCooldown(ServerPlayerEntity player) {
        Long lastRequest = publicListRequests.get(player.getUuid());
        if (lastRequest == null) {
            return 0;
        }
        long now = System.currentTimeMillis();
        long remaining = PUBLIC_LIST_COOLDOWN_MS - (now - lastRequest);
        return remaining > 0 ? (int) Math.ceil(remaining / 1000.0) : 0;
    }

    // ===== 公开/取消公开 Spot 冷却管理 =====

    private static final ConcurrentHashMap<UUID, Long> publicSpotCooldowns = new ConcurrentHashMap<>();
    private static final AtomicLong publicSpotRequestsThisSecond = new AtomicLong(0);
    private static long publicSpotLastSecondBoundary = System.currentTimeMillis() / 1000;

    /**
     * 检查玩家是否在公开 Spot 冷却中。
     *
     * @param player 玩家
     * @return true 如果在冷却中，false 可以操作
     */
    public static boolean isInPublicSpotCooldown(ServerPlayerEntity player) {
        Long lastRequest = publicSpotCooldowns.get(player.getUuid());
        if (lastRequest == null) {
            return false;
        }

        long now = System.currentTimeMillis();
        long cooldownMs = ConfigManager.getPublicSpotCooldownSeconds() * 1000L;
        return (now - lastRequest) < cooldownMs;
    }

    /**
     * 获取玩家公开 Spot 剩余冷却时间（秒）。
     *
     * @param player 玩家
     * @return 剩余冷却时间，0 表示没有冷却
     */
    public static int getPublicSpotRemainingCooldown(ServerPlayerEntity player) {
        Long lastRequest = publicSpotCooldowns.get(player.getUuid());
        if (lastRequest == null) {
            return 0;
        }

        long now = System.currentTimeMillis();
        long cooldownMs = ConfigManager.getPublicSpotCooldownSeconds() * 1000L;
        long remaining = cooldownMs - (now - lastRequest);

        return remaining > 0 ? (int) Math.ceil(remaining / 1000.0) : 0;
    }

    /**
     * 检查公开 Spot 全局速率限制。
     *
     * @return true 如果可以继续，false 如果超出限制
     */
    public static boolean checkPublicSpotGlobalRateLimit() {
        long now = System.currentTimeMillis() / 1000;
        if (now != publicSpotLastSecondBoundary) {
            publicSpotRequestsThisSecond.set(0);
            publicSpotLastSecondBoundary = now;
        }
        return publicSpotRequestsThisSecond.get() < ConfigManager.getMaxPublicSpotRequestsPerSecond();
    }

    /**
     * 尝试增加公开 Spot 全局计数。
     *
     * @return true 如果成功增加，false 如果超出限制
     */
    public static boolean tryIncrementPublicSpotGlobalCount() {
        long now = System.currentTimeMillis() / 1000;
        if (now != publicSpotLastSecondBoundary) {
            publicSpotRequestsThisSecond.set(0);
            publicSpotLastSecondBoundary = now;
        }
        long current;
        long max = ConfigManager.getMaxPublicSpotRequestsPerSecond();
        do {
            current = publicSpotRequestsThisSecond.get();
            if (current >= max) {
                return false;
            }
        } while (!publicSpotRequestsThisSecond.compareAndSet(current, current + 1));
        return true;
    }

    /**
     * 更新玩家的最后公开 Spot 操作时间（开始冷却）。
     *
     * @param player 玩家
     */
    public static void updateLastPublicSpotRequest(ServerPlayerEntity player) {
        publicSpotCooldowns.put(player.getUuid(), System.currentTimeMillis());
    }

    /**
     * 清除玩家的公开 Spot 冷却时间。
     *
     * @param player 玩家
     */
    public static void clearPublicSpotCooldown(ServerPlayerEntity player) {
        publicSpotCooldowns.remove(player.getUuid());
    }

    /**
     * 清除所有公开 Spot 冷却时间。
     */
    public static void clearAllPublicSpotCooldowns() {
        publicSpotCooldowns.clear();
        publicSpotRequestsThisSecond.set(0);
    }
}
