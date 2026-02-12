package io.github.stone_brick.spotteddog.event;

import io.github.stone_brick.spotteddog.server.data.TeleportLog;
import io.github.stone_brick.spotteddog.server.data.TeleportLogManager;

/**
 * 默认传送日志处理器。
 * 监听 {@link TeleportLogEvents#TELEPORT} 事件并将传送记录保存到日志文件。
 *
 * <p>此处理器在模组初始化时自动注册，负责将所有传送操作记录到
 * {@link TeleportLogManager}，以便后续查询和分析。</p>
 *
 * @see TeleportLogEvents
 * @see TeleportLogManager
 */
public class DefaultTeleportLogLogger implements TeleportLogCallback {

    @Override
    public void onTeleport(TeleportLogEvent event) {
        // 构建日志条目，使用事件中包含的源位置和目标位置
        TeleportLog log = TeleportLog.builder()
                .playerName(event.getPlayer().getName().getString())
                .playerUuid(event.getPlayer().getUuid().toString())
                .teleportType(event.getTeleportType())
                .spotName(event.getSpotName())
                .source(
                        event.getSourceDimension(),
                        event.getSourceX(),
                        event.getSourceY(),
                        event.getSourceZ()
                )
                .target(
                        event.getTargetDimension().getValue().toString(),
                        event.getTargetX(),
                        event.getTargetY(),
                        event.getTargetZ()
                )
                .build();

        // 保存到日志管理器
        TeleportLogManager.getInstance().logTeleport(log);
    }
}
