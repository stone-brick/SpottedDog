package io.github.stone_brick.spotteddog.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * 传送日志事件工厂。
 * 提供对玩家传送事件的监听能力，用于记录传送日志。
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * TeleportLogEvents.TELEPORT.register(event -> {
 *     // 处理传送日志事件
 *     System.out.println("Player teleported: " + event.getPlayer().getName());
 * });
 * }</pre>
 *
 * @see TeleportLogEvent
 * @see TeleportLogCallback
 */
public final class TeleportLogEvents {

    private TeleportLogEvents() {
        // 防止实例化
    }

    /**
     * 传送日志事件。
     * 当玩家执行传送操作（spot, spawn, respawn, death, public_spot）时触发。
     *
     * <p>事件在传送执行<strong>之后</strong>触发，此时玩家已经在目标位置。</p>
     *
     * <p>回调可以访问：</p>
     * <ul>
     *   <li>{@link TeleportLogEvent#getPlayer()} - 执行传送的玩家</li>
     *   <li>{@link TeleportLogEvent#getTeleportType()} - 传送类型</li>
     *   <li>{@link TeleportLogEvent#getSpotName()} - Spot 名称（可为 null）</li>
     *   <li>{@link TeleportLogEvent#getTargetDimension()} - 目标维度</li>
     *   <li>{@link TeleportLogEvent#getTargetX()} / {@link TeleportLogEvent#getTargetY()} / {@link TeleportLogEvent#getTargetZ()} - 目标坐标</li>
     * </ul>
     */
    public static final Event<TeleportLogCallback> TELEPORT = EventFactory.createArrayBacked(
            TeleportLogCallback.class,
            callbacks -> event -> {
                for (TeleportLogCallback callback : callbacks) {
                    callback.onTeleport(event);
                }
            }
    );
}
