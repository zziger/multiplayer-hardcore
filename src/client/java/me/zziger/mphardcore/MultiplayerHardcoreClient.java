package me.zziger.mphardcore;

import io.netty.buffer.Unpooled;
import me.zziger.mphardcore.network.PlayerStateUpdatePayload;
import me.zziger.mphardcore.network.S2CInitPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.PacketByteBuf;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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

		ClientPlayNetworking.registerGlobalReceiver(PlayerStateUpdatePayload.ID, (payload, context) -> {
			livesLeft.put(payload.playerUUID(), payload.livesLeft());
		});

		ClientLoginNetworking.registerGlobalReceiver(PlayerCompatibilityManager.COMPATIBILITY_CHECK, (client, handler, buf, callbacksConsumer) -> {
			PacketByteBuf outBuf = new PacketByteBuf(Unpooled.buffer());
			outBuf.writeInt(PlayerCompatibilityManager.PROTOCOL_VERSION);
			return CompletableFuture.completedFuture(outBuf);
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