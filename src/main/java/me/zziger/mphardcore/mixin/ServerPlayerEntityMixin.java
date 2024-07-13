package me.zziger.mphardcore.mixin;

import me.zziger.mphardcore.PlayerLivesState;
import me.zziger.mphardcore.network.PlayerStateUpdatePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {

	@Inject(method= "onDeath(Lnet/minecraft/entity/damage/DamageSource;)V", at=@At("HEAD"))
	private void beforeDeath(DamageSource damageSource, CallbackInfo info){
		PlayerEntity player = (PlayerEntity) (Object) this;
		PlayerLivesState.PlayerData state = PlayerLivesState.getPlayerState(player);
		state.livesLeft = Math.max(state.livesLeft - 1, 0);
		PlayerStateUpdatePayload.AnnounceStateOf(player.getServer(), player.getGameProfile(), state);
	}

	@Inject(method= "onDeath(Lnet/minecraft/entity/damage/DamageSource;)V", at=@At("TAIL"))
	private void afterDeath(DamageSource damageSource, CallbackInfo info){
		PlayerEntity player = (PlayerEntity) (Object) this;
		PlayerLivesState.PlayerData state = PlayerLivesState.getPlayerState(player);
		if (state.livesLeft == 1) {
			player.getServer().getPlayerManager().getPlayerList().forEach((innerPlayer) -> {
				innerPlayer.sendMessage(Text.translatable("mphardcore.has_only_one_life_left", player.getName()));
			});
		}
	}
}