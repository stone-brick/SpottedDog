package io.github.stone_brick.spotteddog.event;

import io.github.stone_brick.spotteddog.server.data.TeleportLog;
import io.github.stone_brick.spotteddog.server.data.TeleportLogManager;

/**
 * 默认管理操作日志处理器。
 * 监听 {@link AdminLogEvents#ADMIN_OPERATION} 事件并将管理操作记录到日志文件。
 *
 * <p>此处理器在模组初始化时自动注册，负责将所有管理操作记录到
 * {@link TeleportLogManager}，以便后续审计查询。</p>
 *
 * @see AdminLogEvents
 * @see TeleportLogManager
 */
public class DefaultAdminLogLogger implements AdminLogCallback {

    @Override
    public void onAdminOperation(AdminLogEvent event) {
        // 构建日志条目
        TeleportLog log = TeleportLog.builder()
                .type("admin_operation")
                .operator(event.getOperatorName(), event.getOperatorUuid())
                .teleportType(event.getOperationType())
                .spotName(event.getSpotName())
                .targetPlayer(event.getTargetPlayer())
                .whitelistType(event.getWhitelistType())
                .build();

        // 保存到日志管理器
        TeleportLogManager.getInstance().logAdminOperation(log);
    }
}
