package me.zziger.mphardcore;

import com.mojang.authlib.GameProfile;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.UUID;

import static me.zziger.mphardcore.MultiplayerHardcore.LOGGER;
import static me.zziger.mphardcore.MultiplayerHardcore.MOD_ID;

public class PlayerCompatibilityManager {
    public static final Identifier COMPATIBILITY_CHECK = Identifier.of(MOD_ID, "compatibility_check");
    public static final int PROTOCOL_VERSION = 1;

    public static class PlayerData {
        public boolean hasMod = false;
        public int protocolVersion = 0;

        public boolean compatible = false;
    }

    public static HashMap<UUID, PlayerData> players = new HashMap<>();

    public static PlayerData getPlayerData(PlayerEntity player) {
        return players.getOrDefault(player.getUuid(), new PlayerData());
    }

    public static void init() {
        ServerLoginNetworking.registerGlobalReceiver(COMPATIBILITY_CHECK, PlayerCompatibilityManager::onClientResponse);

        ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, synchronizer) -> {
            sender.sendPacket(COMPATIBILITY_CHECK, PacketByteBufs.empty());
        });
    }

    private static void onClientResponse(MinecraftServer server, ServerLoginNetworkHandler handler, boolean understood, PacketByteBuf buf, ServerLoginNetworking.LoginSynchronizer synchronizer, PacketSender responseSender) {
        GameProfile profile = handler.profile;
        assert profile != null;
        PlayerData playerData = new PlayerData();

        if (!understood) {
            LOGGER.info("Player {} connected WITHOUT Multiplayer Hardcore installed - using fallback to scoreboard display", profile.getName());
            playerData.hasMod = false;
            playerData.compatible = false;

            players.put(profile.getId(), playerData);
            return;
        }

        int clientProtocolVersion = buf.readInt();
        if (clientProtocolVersion != PROTOCOL_VERSION) {
            LOGGER.info("Player {} connected with OLD Multiplayer Hardcore - using fallback to scoreboard display", profile.getName());
            playerData.hasMod = true;
            playerData.protocolVersion = clientProtocolVersion;
            playerData.compatible = false;

            players.put(profile.getId(), playerData);
            return;
        }

        LOGGER.info("Player {} connected with Multiplayer Hardcore installed", handler.getConnectionInfo());
        playerData.hasMod = true;
        playerData.protocolVersion = PROTOCOL_VERSION;
        playerData.compatible = true;
        players.put(profile.getId(), playerData);
    }
}
