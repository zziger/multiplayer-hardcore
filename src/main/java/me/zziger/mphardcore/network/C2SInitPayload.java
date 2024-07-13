package me.zziger.mphardcore.network;

import me.zziger.mphardcore.MultiplayerHardcore;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record C2SInitPayload(boolean dummy) implements CustomPayload {

    public static final Identifier PACKET_ID = Identifier.of(MultiplayerHardcore.MOD_ID, "c2s_init");
    public static final Id<C2SInitPayload> ID = new Id<>(PACKET_ID);

     public static final PacketCodec<RegistryByteBuf, C2SInitPayload> CODEC = PacketCodec.tuple(
             PacketCodecs.BOOL, C2SInitPayload::dummy,
             C2SInitPayload::new
     );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}