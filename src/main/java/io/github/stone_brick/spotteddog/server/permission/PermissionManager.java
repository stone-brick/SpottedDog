package io.github.stone_brick.spotteddog.server.permission;

import io.github.stone_brick.spotteddog.server.config.ConfigManager;
import io.github.stone_brick.spotteddog.server.permission.WhitelistManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

/**
 * 权限管理器。
 * 基于 Minecraft OP 级别管理玩家权限。
 */
public final class PermissionManager {

    /**
     * 权限等级定义。
     */
    public enum PermissionLevel {
        NONE,       // 普通玩家
        MODERATOR,  // 管理员
        ADMIN       // OP
    }

    // 权限标识符常量
    public static final String PERMISSION_TELEPORT = "spotteddog.teleport";
    public static final String PERMISSION_PUBLIC_SPOT = "spotteddog.public";
    public static final String PERMISSION_PUBLIC_SPOT_TELEPORT = "spotteddog.public.teleport";
    public static final String PERMISSION_ADMIN = "spotteddog.admin";

    private PermissionManager() {
        // 工具类，禁止实例化
    }

    /**
     * 检查玩家是否具有指定权限。
     * <p>
     * 权限规则：
     * - OP 玩家拥有所有权限
     * - 白名单玩家优先于全局配置
     * - 普通玩家：根据配置文件决定（allowAllPlayers）
     *
     * @param player     玩家
     * @param permission 权限标识符
     * @return true 如果有权限，false 如果没有权限
     */
    public static boolean hasPermission(ServerPlayerEntity player, String permission) {
        if (player == null || permission == null) {
            return false;
        }

        // 管理员 (OP) 拥有所有权限
        if (hasAdminPermission(player)) {
            return true;
        }

        // 根据权限类型进行细粒度检查（白名单优先于全局配置）
        return switch (permission) {
            case PERMISSION_TELEPORT ->
                WhitelistManager.isPlayerInWhitelist(player.getUuid(), WhitelistManager.WhitelistType.TELEPORT)
                || ConfigManager.isAllowAllPlayersTeleport();
            case PERMISSION_PUBLIC_SPOT ->
                WhitelistManager.isPlayerInWhitelist(player.getUuid(), WhitelistManager.WhitelistType.PUBLIC_SPOT)
                || ConfigManager.isAllowAllPlayersPublicSpot();
            case PERMISSION_PUBLIC_SPOT_TELEPORT ->
                WhitelistManager.isPlayerInWhitelist(player.getUuid(), WhitelistManager.WhitelistType.PUBLIC_SPOT_TELEPORT)
                || ConfigManager.isAllowAllPlayersPublicSpotTeleport();
            case PERMISSION_ADMIN -> false; // 管理员权限仅限 OP
            default -> false;
        };
    }

    /**
     * 获取玩家的权限等级。
     *
     * @param player 玩家
     * @return 权限等级
     */
    public static PermissionLevel getPermissionLevel(ServerPlayerEntity player) {
        if (player == null) {
            return PermissionLevel.NONE;
        }

        if (hasAdminPermission(player)) {
            return PermissionLevel.ADMIN;
        }

        if (hasModeratorPermission(player)) {
            return PermissionLevel.MODERATOR;
        }

        return PermissionLevel.NONE;
    }

    /**
     * 检查玩家是否是管理员 (OP)。
     *
     * @param player 玩家
     * @return true 如果是 OP
     */
    public static boolean hasAdminPermission(ServerPlayerEntity player) {
        if (player == null) {
            return false;
        }
        MinecraftServer server = player.getEntityWorld().getServer();
        if (server == null) {
            return false;
        }
        // 检查玩家的 UUID 是否在 OP 列表中
        String[] opNames = server.getPlayerManager().getOpList().getNames();
        UUID playerUuid = player.getUuid();
        for (String opName : opNames) {
            // OP 列表存储的是玩家名称，需要比较
            if (opName.equalsIgnoreCase(player.getName().getString())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查玩家是否是管理员（2级或以上）。
     * 注意：在 Minecraft 1.21 中，OP 列表不再区分权限级别，
     * 所有 OP 都具有完整权限。此方法用于未来扩展。
     *
     * @param player 玩家
     * @return true 如果是 OP（视为 Moderator+）
     */
    public static boolean hasModeratorPermission(ServerPlayerEntity player) {
        return hasAdminPermission(player);
    }

    /**
     * 检查玩家是否可以执行传送操作。
     *
     * @param player 玩家
     * @return true 如果可以传送
     */
    public static boolean canTeleport(ServerPlayerEntity player) {
        return hasPermission(player, PERMISSION_TELEPORT);
    }

    /**
     * 检查玩家是否可以管理公开 Spot。
     *
     * @param player 玩家
     * @return true 如果可以管理公开 Spot
     */
    public static boolean canManagePublicSpots(ServerPlayerEntity player) {
        return hasPermission(player, PERMISSION_PUBLIC_SPOT);
    }

    /**
     * 检查玩家是否可以传送到公开 Spot。
     *
     * @param player 玩家
     * @return true 如果可以传送到公开 Spot
     */
    public static boolean canTeleportToPublicSpot(ServerPlayerEntity player) {
        return hasPermission(player, PERMISSION_PUBLIC_SPOT_TELEPORT);
    }

    /**
     * 检查玩家是否可以使用管理员功能。
     *
     * @param player 玩家
     * @return true 如果可以使用管理员功能
     */
    public static boolean canUseAdminFeatures(ServerPlayerEntity player) {
        return hasPermission(player, PERMISSION_ADMIN);
    }
}
