package me.zziger.mphardcore.mixin;

import me.zziger.mphardcore.MultiplayerHardcore;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.level.LevelInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelInfo.class)
public class LevelInfoMixin {
	@Inject(at = @At("HEAD"), method = "isHardcore()Z", cancellable = true)
	private void init(CallbackInfoReturnable<Boolean> cir) {
		if (MultiplayerHardcore.serverInstance != null && MultiplayerHardcore.serverInstance.isDedicated())
			cir.setReturnValue(MultiplayerHardcore.serverInstance.isHardcore());
	}
}