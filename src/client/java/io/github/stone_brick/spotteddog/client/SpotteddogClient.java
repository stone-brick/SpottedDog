package io.github.stone_brick.spotteddog.client;

import io.github.stone_brick.spotteddog.client.command.SpotCommand;
import io.github.stone_brick.spotteddog.client.command.WhitelistAdminCommand;
import io.github.stone_brick.spotteddog.client.network.PublicSpotListHandler;
import io.github.stone_brick.spotteddog.client.network.TeleportConfirmHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

public class SpotteddogClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // 注册客户端网络处理器
        TeleportConfirmHandler.register();
        PublicSpotListHandler.register();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            SpotCommand.register(dispatcher);
            WhitelistAdminCommand.register(dispatcher);
        });
    }
}
