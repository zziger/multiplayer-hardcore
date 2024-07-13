package me.zziger.mphardcore.network;

import com.mojang.authlib.GameProfile;
import me.zziger.mphardcore.MultiplayerHardcore;
import me.zziger.mphardcore.PlayerCompatibilityManager;
import me.zziger.mphardcore.PlayerLivesState;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.s2c.play.ScoreboardScoreUpdateS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

import java.util.Objects;
import java.util.Optional;
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

    public static void SendPayload(ServerPlayerEntity targetPlayer, GameProfile statePlayer, PlayerStateUpdatePayload payload) {
        if (PlayerCompatibilityManager.getPlayerData(targetPlayer).compatible) {
            ServerPlayNetworking.send(targetPlayer, payload);
        } else {
            targetPlayer.networkHandler.sendPacket(new ScoreboardScoreUpdateS2CPacket(statePlayer.getName(), MultiplayerHardcore.fakeScoreboard.getName(), payload.livesLeft * 2, Optional.empty(), Optional.empty()));
        }
    }

    public static void SendPayload(ServerPlayerEntity targetPlayer, GameProfile statePlayer, PlayerLivesState.PlayerData state) {
        SendPayload(targetPlayer, statePlayer, new PlayerStateUpdatePayload(statePlayer.getId(), state.livesLeft));
    }

    public static void AnnounceStateOf(MinecraftServer server, GameProfile statePlayer, PlayerLivesState.PlayerData state) {
        PlayerStateUpdatePayload payload = new PlayerStateUpdatePayload(statePlayer.getId(), state.livesLeft);

        Objects.requireNonNull(server).getPlayerManager().getPlayerList().forEach(targetPlayer -> {
            SendPayload(targetPlayer, statePlayer, payload);
        });
    }

    public static void AnnounceStateOf(MinecraftServer server, GameProfile statePlayer) {
        AnnounceStateOf(server, statePlayer, PlayerLivesState.getPlayerState(server, statePlayer));
    }

    public static void AnnounceAllStatesTo(ServerPlayerEntity targetPlayer) {
        PlayerLivesState.PlayerData personalState = PlayerLivesState.getPlayerState(targetPlayer);
        SendPayload(targetPlayer, targetPlayer.getGameProfile(), personalState);

        Objects.requireNonNull(targetPlayer.getServer()).getPlayerManager().getPlayerList().forEach(statePlayer -> {
            if (statePlayer == targetPlayer) return;
            PlayerLivesState.PlayerData state = PlayerLivesState.getPlayerState(statePlayer);
            SendPayload(targetPlayer, statePlayer.getGameProfile(), state);
        });
    }
}