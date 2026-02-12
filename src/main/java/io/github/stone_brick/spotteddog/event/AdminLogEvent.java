package io.github.stone_brick.spotteddog.event;

import java.time.Instant;

/**
 * 管理操作日志事件。
 * 当管理员执行白名单或公开 Spot 管理操作时记录此事件。
 *
 * <p>支持的操作类型：</p>
 * <ul>
 *   <li>{@code whitelist_add} - 添加玩家到白名单</li>
 *   <li>{@code whitelist_remove} - 从白名单移除玩家</li>
 *   <li>{@code public_spot} - 公开 Spot</li>
 *   <li>{@code unpublic_spot} - 取消公开 Spot</li>
 * </ul>
 *
 * @see AdminLogEvents
 * @see AdminLogCallback
 */
public class AdminLogEvent {
    private final String operatorName;
    private final String operatorUuid;
    private final String operationType;
    private final String targetPlayer;
    private final String spotName;
    private final String whitelistType;
    private final String timestamp;

    /**
     * 创建管理操作日志事件。
     *
     * @param operatorName 执行操作的 OP 名称
     * @param operatorUuid 执行操作的 OP UUID
     * @param operationType 操作类型（whitelist_add, whitelist_remove, public_spot, unpublic_spot）
     * @param targetPlayer 目标玩家名（白名单操作）
     * @param spotName Spot 名称（公开 Spot 操作）
     * @param whitelistType 白名单类型（teleport, public, publictp）
     */
    public AdminLogEvent(String operatorName, String operatorUuid, String operationType,
                         String targetPlayer, String spotName, String whitelistType) {
        this.operatorName = operatorName;
        this.operatorUuid = operatorUuid;
        this.operationType = operationType;
        this.targetPlayer = targetPlayer;
        this.spotName = spotName;
        this.whitelistType = whitelistType;
        this.timestamp = Instant.now().toString();
    }

    /**
     * 获取执行操作的 OP 名称。
     */
    public String getOperatorName() {
        return operatorName;
    }

    /**
     * 获取执行操作的 OP UUID。
     */
    public String getOperatorUuid() {
        return operatorUuid;
    }

    /**
     * 获取操作类型。
     *
     * @return 操作类型（whitelist_add, whitelist_remove, public_spot, unpublic_spot）
     */
    public String getOperationType() {
        return operationType;
    }

    /**
     * 获取目标玩家名。
     *
     * @return 目标玩家名，白名单操作时有效
     */
    public String getTargetPlayer() {
        return targetPlayer;
    }

    /**
     * 获取 Spot 名称。
     *
     * @return Spot 名称，公开 Spot 操作时有效
     */
    public String getSpotName() {
        return spotName;
    }

    /**
     * 获取白名单类型。
     *
     * @return 白名单类型（teleport, public, publictp），白名单操作时有效
     */
    public String getWhitelistType() {
        return whitelistType;
    }

    /**
     * 获取时间戳。
     *
     * @return ISO 格式时间戳
     */
    public String getTimestamp() {
        return timestamp;
    }
}
