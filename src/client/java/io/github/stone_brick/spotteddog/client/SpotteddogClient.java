package io.github.stone_brick.spotteddog.client;

import io.github.stone_brick.spotteddog.client.command.SpotCommand;
import io.github.stone_brick.spotteddog.client.data.PlayerDataManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

public class SpotteddogClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                SpotCommand.register(dispatcher));
    }
}
