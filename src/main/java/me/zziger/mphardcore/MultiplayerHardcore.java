package me.zziger.mphardcore;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.BuiltInExceptions;
import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.zziger.mphardcore.network.C2SInitPayload;
import me.zziger.mphardcore.network.PlayerStateUpdatePayload;
import me.zziger.mphardcore.network.S2CInitPayload;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.message.SentMessage;
import net.minecraft.network.message.SignedMessage;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Objects;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class MultiplayerHardcore implements ModInitializer {
    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.

    public static final String MOD_ID = "mphardcore";
    public static final Logger LOGGER = LoggerFactory.getLogger("mphardcore");
    public static MinecraftServer serverInstance = null;

    @Override
    public void onInitialize() {
        MultiplayerHardcoreConfig.init();

        PayloadTypeRegistry.playS2C().register(PlayerStateUpdatePayload.ID, PlayerStateUpdatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(S2CInitPayload.ID, S2CInitPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(C2SInitPayload.ID, C2SInitPayload.CODEC);

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (alive) return;
            PlayerLivesState.PlayerData state = PlayerLivesState.getPlayerState(newPlayer);
            if (state.livesLeft <= 0) {
                newPlayer.changeGameMode(GameMode.SPECTATOR);
                newPlayer.sendMessage(Text.translatable("mphardcore.was_last_live"));
            } else {
                newPlayer.sendMessage(Text.translatable("mphardcore.lives_left", state.livesLeft));
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (!server.isDedicated()) return;
            ServerPlayerEntity player = handler.getPlayer();
            PlayerStateUpdatePayload.AnnounceStateOf(player, PlayerLivesState.getPlayerState(player));
            PlayerStateUpdatePayload.AnnounceAllStatesTo(player);
            ServerPlayNetworking.send(player, new S2CInitPayload(MultiplayerHardcoreConfig.defaultLives));
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    literal("hardcore")
                            .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(1))
                            .then(
                                    literal("set_lives").then(
                                            argument("player", GameProfileArgumentType.gameProfile()).then(
                                                    argument("lives", IntegerArgumentType.integer(0))
                                                            .executes(context -> {
                                                                int lives = IntegerArgumentType.getInteger(context, "lives");
                                                                Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument(context, "player");
                                                                if (profiles.isEmpty()) return 0;

                                                                profiles.forEach(profile -> {
                                                                    PlayerLivesState.PlayerData state = PlayerLivesState.getPlayerState(context.getSource().getServer(), profile);
                                                                    state.livesLeft = lives;
                                                                    PlayerStateUpdatePayload.AnnounceStateOf(context.getSource().getServer(), profile, state);
                                                                    context.getSource().sendFeedback(() -> Text.translatable("mphardcore.set_player_lives_to", new Object[]{profile.getName(), lives}), true);
                                                                });

                                                                return 1;
                                                            })
                                            )
                                    )
                            ).then(
                                    literal("get_lives").then(
                                            argument("player", GameProfileArgumentType.gameProfile())
                                                    .executes(context -> {
                                                        Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument(context, "player");
                                                        if (profiles.isEmpty()) return 0;

                                                        profiles.forEach(profile -> {
                                                            PlayerLivesState.PlayerData state = PlayerLivesState.getPlayerState(context.getSource().getServer(), profile);
                                                            context.getSource().sendFeedback(() -> Text.translatable("mphardcore.player_lives_are", new Object[]{profile.getName(), state.livesLeft}), false);
                                                        });

                                                        return 1;
                                                    })
                                    )
                            ).then(
                                    literal("rescue").then(
                                            argument("player", GameProfileArgumentType.gameProfile())
                                                    .executes(context -> {
                                                        Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument(context, "player");
                                                        if (profiles.size() != 1) return 0;

                                                        GameProfile profile = profiles.stream().findFirst().get();
                                                        PlayerLivesState.PlayerData state = PlayerLivesState.getPlayerState(context.getSource().getServer(), profile);
                                                        ServerPlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer(profile.getId());

                                                        if (player == null || !player.isSpectator() || state.livesLeft >= 1) {
                                                            context.getSource().sendFeedback(() -> Text.translatable("mphardcore.player_doesnt_need_rescue", profile.getName()), false);
                                                            return 0;
                                                        }

                                                        if (state.rescuedTimes >= MultiplayerHardcoreConfig.maxRescueTimes) {
                                                            context.getSource().sendFeedback(() -> Text.translatable("mphardcore.player_was_rescued_already", profile.getName(), MultiplayerHardcoreConfig.maxRescueTimes), false);
                                                            return 0;
                                                        }

                                                        state.livesLeft = 1;
                                                        state.rescuedTimes++;
                                                        PlayerStateUpdatePayload.AnnounceStateOf(context.getSource().getServer(), profile, state);
                                                        player.changeGameMode(GameMode.SURVIVAL);

                                                        ServerWorld world = context.getSource().getServer().getWorld(World.OVERWORLD);
                                                        assert world != null;
                                                        Vec3d spawnPos = new Vec3d(world.getSpawnPos().getX(), world.getSpawnPos().getY(), world.getSpawnPos().getZ());
                                                        player.teleportTo(new TeleportTarget(world, spawnPos, Vec3d.ZERO, 0, 0, TeleportTarget.NO_OP));
                                                        context.getSource().sendFeedback(() -> Text.translatable("mphardcore.rescued_player", profile.getName()), true);

                                                        return 1;

                                                    })
                                    )
                            ).then(
                                    literal("reset_rescue")
                                            .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(4))
                                            .then(
                                                    argument("player", GameProfileArgumentType.gameProfile())
                                                            .executes(context -> {
                                                                Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument(context, "player");
                                                                if (profiles.size() != 1) return 0;

                                                                GameProfile profile = profiles.stream().findFirst().get();
                                                                PlayerLivesState.PlayerData state = PlayerLivesState.getPlayerState(context.getSource().getServer(), profile);
                                                                state.rescuedTimes = 0;
                                                                context.getSource().sendFeedback(() -> Text.translatable("mphardcore.player_rescue_limit_was_reset", profile.getName()), true);
                                                                return 1;
                                                            })
                                            )
                            )
            );
        });

        LOGGER.info("Multiplayer Hardcore loaded");
    }
}