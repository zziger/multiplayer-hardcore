package me.zziger.mphardcore;

import me.zziger.mphardcore.network.C2SInitPayload;
import me.zziger.mphardcore.network.PlayerStateUpdatePayload;
import me.zziger.mphardcore.network.S2CInitPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MultiplayerHardcoreClient implements ClientModInitializer {
	private static final Map<UUID, Integer> livesLeft = new HashMap<UUID, Integer>();
	public static int defaultLives;
	public static boolean enabled = false;

	@Override
	public void onInitializeClient() {
		defaultLives = 1;

		ClientPlayConnectionEvents.INIT.register((handler, client) -> {
			enabled = false;
		});

		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
			ClientPlayNetworking.send(new C2SInitPayload(false));
		});

		ClientPlayNetworking.registerGlobalReceiver(PlayerStateUpdatePayload.ID, (payload, context) -> {
			livesLeft.put(payload.playerUUID(), payload.livesLeft());
		});

		ClientPlayNetworking.registerGlobalReceiver(S2CInitPayload.ID, (payload, context) -> {
			defaultLives = payload.defaultLives();
			enabled = true;
		});
	}

	public static int getLives(UUID playerUUID) {
		return livesLeft.getOrDefault(playerUUID, 0);
	}
}