package io.github.stone_brick.spotteddog.event;

/**
 * 管理操作日志事件回调接口。
 *
 * <p>用于监听 {@link AdminLogEvents#ADMIN_OPERATION} 事件。</p>
 *
 * @see AdminLogEvents
 * @see AdminLogEvent
 */
@FunctionalInterface
public interface AdminLogCallback {

    /**
     * 当执行管理操作时调用。
     *
     * @param event 管理操作日志事件，包含操作者、类型和目标信息
     */
    void onAdminOperation(AdminLogEvent event);
}
