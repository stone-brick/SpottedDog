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

    private static final ConcurrentHashMap<UUID, Long> playerCooldowns = new ConcurrentHashMap<>();
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
}
