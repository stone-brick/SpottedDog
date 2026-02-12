package io.github.stone_brick.spotteddog.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * 管理操作日志事件工厂。
 * 提供对管理员操作（白名单管理、公开 Spot 管理）的监听能力，用于记录管理日志。
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * AdminLogEvents.ADMIN_OPERATION.register(event -> {
 *     // 处理管理操作日志事件
 *     System.out.println("Admin operation: " + event.getOperationType());
 * });
 * }</pre>
 *
 * @see AdminLogEvent
 * @see AdminLogCallback
 */
public final class AdminLogEvents {

    private AdminLogEvents() {
        // 防止实例化
    }

    /**
     * 管理操作日志事件。
     * 当管理员执行白名单或公开 Spot 管理操作时触发。
     *
     * <p>支持的的操作类型：</p>
     * <ul>
     *   <li>{@code whitelist_add} - 添加玩家到白名单</li>
     *   <li>{@code whitelist_remove} - 从白名单移除玩家</li>
     *   <li>{@code public_spot} - 公开 Spot</li>
     *   <li>{@code unpublic_spot} - 取消公开 Spot</li>
     * </ul>
     *
     * <p>回调可以访问：</p>
     * <ul>
     *   <li>{@link AdminLogEvent#getOperatorName()} - 执行操作的 OP 名称</li>
     *   <li>{@link AdminLogEvent#getOperationType()} - 操作类型</li>
     *   <li>{@link AdminLogEvent#getTargetPlayer()} - 目标玩家名</li>
     *   <li>{@link AdminLogEvent#getSpotName()} - Spot 名称</li>
     *   <li>{@link AdminLogEvent#getWhitelistType()} - 白名单类型</li>
     * </ul>
     */
    public static final Event<AdminLogCallback> ADMIN_OPERATION = EventFactory.createArrayBacked(
            AdminLogCallback.class,
            callbacks -> event -> {
                for (AdminLogCallback callback : callbacks) {
                    callback.onAdminOperation(event);
                }
            }
    );
}
