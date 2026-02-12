package io.github.stone_brick.spotteddog.event;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

/**
 * 传送日志事件。
 * 当玩家执行传送操作时记录此事件。
 *
 * <p>事件在传送执行<strong>之后</strong>触发，包含完整的源位置和目标位置信息。</p>
 *
 * <p>支持的传送类型：</p>
 * <ul>
 *   <li>{@code spot} - 传送到玩家自己的 Spot</li>
 *   <li>{@code spawn} - 传送到世界出生点</li>
 *   <li>{@code respawn} - 传送到重生点</li>
 *   <li>{@code death} - 传送到死亡点</li>
 *   <li>{@code public_spot} - 传送到公开 Spot</li>
 * </ul>
 *
 * @see TeleportLogEvents
 */
public class TeleportLogEvent {
    private final ServerPlayerEntity player;
    private final String teleportType;
    private final String spotName;
    private final String sourceDimension;
    private final double sourceX;
    private final double sourceY;
    private final double sourceZ;
    private final RegistryKey<World> targetDimension;
    private final double targetX;
    private final double targetY;
    private final double targetZ;

    /**
     * 创建传送日志事件。
     *
     * @param player 执行传送的玩家
     * @param teleportType 传送类型（spot, spawn, respawn, death, public_spot）
     * @param spotName Spot 名称（可为 null）
     * @param sourceDimension 源维度
     * @param sourceX 源 X 坐标
     * @param sourceY 源 Y 坐标
     * @param sourceZ 源 Z 坐标
     * @param targetDimension 目标维度
     * @param targetX 目标 X 坐标
     * @param targetY 目标 Y 坐标
     * @param targetZ 目标 Z 坐标
     */
    public TeleportLogEvent(ServerPlayerEntity player, String teleportType, String spotName,
                            String sourceDimension, double sourceX, double sourceY, double sourceZ,
                            RegistryKey<World> targetDimension,
                            double targetX, double targetY, double targetZ) {
        this.player = player;
        this.teleportType = teleportType;
        this.spotName = spotName;
        this.sourceDimension = sourceDimension;
        this.sourceX = sourceX;
        this.sourceY = sourceY;
        this.sourceZ = sourceZ;
        this.targetDimension = targetDimension;
        this.targetX = targetX;
        this.targetY = targetY;
        this.targetZ = targetZ;
    }

    /**
     * 获取执行传送的玩家。
     *
     * @return 玩家实体
     */
    public ServerPlayerEntity getPlayer() {
        return player;
    }

    /**
     * 获取传送类型。
     *
     * @return 传送类型（spot, spawn, respawn, death, public_spot）
     */
    public String getTeleportType() {
        return teleportType;
    }

    /**
     * 获取 Spot 名称。
     *
     * @return Spot 名称，非 Spot 类型传送时为 null
     */
    public String getSpotName() {
        return spotName;
    }

    /**
     * 获取源维度。
     *
     * @return 源世界的维度标识符
     */
    public String getSourceDimension() {
        return sourceDimension;
    }

    /**
     * 获取源 X 坐标。
     *
     * @return 源 X 坐标
     */
    public double getSourceX() {
        return sourceX;
    }

    /**
     * 获取源 Y 坐标。
     *
     * @return 源 Y 坐标
     */
    public double getSourceY() {
        return sourceY;
    }

    /**
     * 获取源 Z 坐标。
     *
     * @return 源 Z 坐标
     */
    public double getSourceZ() {
        return sourceZ;
    }

    /**
     * 获取目标维度。
     *
     * @return 目标世界的 RegistryKey
     */
    public RegistryKey<World> getTargetDimension() {
        return targetDimension;
    }

    /**
     * 获取目标 X 坐标。
     *
     * @return 目标 X 坐标
     */
    public double getTargetX() {
        return targetX;
    }

    /**
     * 获取目标 Y 坐标。
     *
     * @return 目标 Y 坐标
     */
    public double getTargetY() {
        return targetY;
    }

    /**
     * 获取目标 Z 坐标。
     *
     * @return 目标 Z 坐标
     */
    public double getTargetZ() {
        return targetZ;
    }
}
