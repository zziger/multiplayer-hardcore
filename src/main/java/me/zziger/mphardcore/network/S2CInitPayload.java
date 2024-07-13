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

public record S2CInitPayload(int defaultLives) implements CustomPayload {

    public static final Identifier PACKET_ID = Identifier.of(MultiplayerHardcore.MOD_ID, "s2c_init");
    public static final Id<S2CInitPayload> ID = new Id<>(PACKET_ID);

     public static final PacketCodec<RegistryByteBuf, S2CInitPayload> CODEC = PacketCodec.tuple(
             PacketCodecs.INTEGER, S2CInitPayload::defaultLives,
             S2CInitPayload::new
     );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}