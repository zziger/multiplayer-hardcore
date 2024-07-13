package me.zziger.mphardcore.network;

import com.mojang.authlib.GameProfile;
import me.zziger.mphardcore.MultiplayerHardcore;
import me.zziger.mphardcore.PlayerLivesState;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.Objects;
import java.util.UUID;

public record PlayerStateUpdatePayload(UUID playerUUID, int livesLeft) implements CustomPayload {

    public static final Identifier PACKET_ID = Identifier.of(MultiplayerHardcore.MOD_ID, "player_state_update");
    public static final CustomPayload.Id<PlayerStateUpdatePayload> ID = new CustomPayload.Id<>(PACKET_ID);

     public static final PacketCodec<RegistryByteBuf, PlayerStateUpdatePayload> CODEC = PacketCodec.tuple(
            Uuids.PACKET_CODEC, PlayerStateUpdatePayload::playerUUID,
             PacketCodecs.INTEGER, PlayerStateUpdatePayload::livesLeft,
             PlayerStateUpdatePayload::new
     );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static void AnnounceStateOf(PlayerEntity player, PlayerLivesState.PlayerData state) {
        PlayerStateUpdatePayload payload = new PlayerStateUpdatePayload(player.getUuid(), state.livesLeft);

        Objects.requireNonNull(player.getServer()).getPlayerManager().getPlayerList().forEach(serverPlayer -> {
            ServerPlayNetworking.send(serverPlayer, payload);
        });
    }

    public static void AnnounceStateOf(MinecraftServer server, GameProfile player, PlayerLivesState.PlayerData state) {
        PlayerStateUpdatePayload payload = new PlayerStateUpdatePayload(player.getId(), state.livesLeft);

        Objects.requireNonNull(server).getPlayerManager().getPlayerList().forEach(serverPlayer -> {
            ServerPlayNetworking.send(serverPlayer, payload);
        });
    }

    public static void AnnounceAllStatesTo(ServerPlayerEntity player) {
        PlayerLivesState.PlayerData personalState = PlayerLivesState.getPlayerState(player);
        ServerPlayNetworking.send(player, new PlayerStateUpdatePayload(player.getUuid(), personalState.livesLeft));

        Objects.requireNonNull(player.getServer()).getPlayerManager().getPlayerList().forEach(serverPlayer -> {
            if (serverPlayer == player) return;
            PlayerLivesState.PlayerData state = PlayerLivesState.getPlayerState(serverPlayer);
            ServerPlayNetworking.send(player, new PlayerStateUpdatePayload(serverPlayer.getUuid(), state.livesLeft));
        });
    }
}