package me.zziger.mphardcore.mixin;

import me.zziger.mphardcore.MultiplayerHardcoreConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {
	@Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;isHardcore()Z"), method = "onClientStatus(Lnet/minecraft/network/packet/c2s/play/ClientStatusC2SPacket;)V")
	private boolean init(MinecraftServer instance) {
		if (!instance.isDedicated() && instance.isHardcore() && !MultiplayerHardcoreConfig.enableInSinglePlayer) return true;
		return false;
	}
}