package io.github.stone_brick.spotteddog.event;

/**
 * 传送日志事件回调接口。
 *
 * <p>用于监听 {@link TeleportLogEvents#TELEPORT} 事件。</p>
 *
 * @see TeleportLogEvents
 * @see TeleportLogEvent
 */
@FunctionalInterface
public interface TeleportLogCallback {

    /**
     * 当玩家执行传送操作时调用。
     *
     * <p>事件在传送执行<strong>之后</strong>触发。</p>
     *
     * @param event 传送日志事件，包含源位置、目标位置和玩家信息
     */
    void onTeleport(TeleportLogEvent event);
}
