package me.zziger.mphardcore;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import me.zziger.mphardcore.network.PlayerStateUpdatePayload;
import me.zziger.mphardcore.network.S2CInitPayload;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.network.packet.s2c.play.ScoreboardDisplayS2CPacket;
import net.minecraft.network.packet.s2c.play.ScoreboardObjectiveUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ScoreboardScoreUpdateS2CPacket;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.number.BlankNumberFormat;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Optional;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class MultiplayerHardcore implements ModInitializer {
    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.

    public static final String MOD_ID = "mphardcore";
    public static final Logger LOGGER = LoggerFactory.getLogger("mphardcore");
    public static MinecraftServer serverInstance = null;
    public static ScoreboardObjective fakeScoreboard = null;

    @Override
    public void onInitialize() {
        MultiplayerHardcoreConfig.init();
        PlayerCompatibilityManager.init();

        PayloadTypeRegistry.playS2C().register(PlayerStateUpdatePayload.ID, PlayerStateUpdatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(S2CInitPayload.ID, S2CInitPayload.CODEC);

        fakeScoreboard = new ScoreboardObjective(null, "fakeLives", null, Text.literal("Lives"), ScoreboardCriterion.RenderType.HEARTS, true, BlankNumberFormat.INSTANCE);

        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            if (alive || newPlayer.getServer() == null || !newPlayer.getServer().isHardcore()) return;
            if (!MultiplayerHardcoreConfig.enableInSinglePlayer && !newPlayer.getServer().isDedicated()) return;
            PlayerLivesState.PlayerData state = PlayerLivesState.getPlayerState(newPlayer);
            if (state.livesLeft <= 0) {
                newPlayer.changeGameMode(GameMode.SPECTATOR);
                newPlayer.sendMessage(Text.translatable("mphardcore.was_last_live"));
            } else {
                PlayerCompatibilityManager.PlayerData playerData = PlayerCompatibilityManager.getPlayerData(newPlayer);
                if (!playerData.compatible) {
                    newPlayer.sendMessage(Text.translatable("mphardcore.lives_left", state.livesLeft));
                }
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            if (player.getServer() == null || !player.getServer().isHardcore()) return;
            if (!MultiplayerHardcoreConfig.enableInSinglePlayer && !player.getServer().isDedicated()) return;
            PlayerCompatibilityManager.PlayerData playerData = PlayerCompatibilityManager.getPlayerData(player);

            if (!playerData.compatible) {
                if (playerData.hasMod) {
                    player.sendMessage(Text.translatable("mphardcore.mod_outdated"));
                } else {
                    player.sendMessage(Text.translatable("mphardcore.install_mod"));
                }

                sender.sendPacket(new ScoreboardObjectiveUpdateS2CPacket(fakeScoreboard, ScoreboardObjectiveUpdateS2CPacket.ADD_MODE));
                sender.sendPacket(new ScoreboardDisplayS2CPacket(ScoreboardDisplaySlot.LIST, fakeScoreboard));
                sender.sendPacket(new ScoreboardScoreUpdateS2CPacket(player.getNameForScoreboard(), fakeScoreboard.getName(), 4, Optional.empty(), Optional.empty()));
            } else {
                ServerPlayNetworking.send(player, new S2CInitPayload(MultiplayerHardcoreConfig.defaultLives));
            }

            PlayerStateUpdatePayload.AnnounceStateOf(player.getServer(), player.getGameProfile());
            PlayerStateUpdatePayload.AnnounceAllStatesTo(player);
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                    literal("hardcore")
                            .requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(1) && serverCommandSource.getServer().isHardcore())
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