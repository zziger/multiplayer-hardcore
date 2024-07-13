package me.zziger.mphardcore;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.UUID;

public class PlayerLivesState extends PersistentState {
    static public class PlayerData {
        public int livesLeft;
        public int rescuedTimes = 0;

        PlayerData(int livesLeft) {
            this.livesLeft = livesLeft;
        }
    }

    public HashMap<UUID, PlayerData> players = new HashMap<>();


    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup lookup) {
        NbtCompound playersNbt = new NbtCompound();
        players.forEach((uuid, playerData) -> {
            NbtCompound playerNbt = new NbtCompound();

            playerNbt.putInt("livesLeft", playerData.livesLeft);
            playerNbt.putInt("rescuedTimes", playerData.rescuedTimes);

            playersNbt.put(uuid.toString(), playerNbt);
        });
        nbt.put("players", playersNbt);
        return nbt;
    }

    public static PlayerLivesState createFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        PlayerLivesState state = new PlayerLivesState();

        NbtCompound playersNbt = tag.getCompound("players");
        playersNbt.getKeys().forEach(key -> {
            PlayerData playerData = new PlayerData(playersNbt.getCompound(key).getInt("livesLeft"));
            playerData.rescuedTimes = playersNbt.getCompound(key).getInt("rescuedTimes");

            UUID uuid = UUID.fromString(key);
            state.players.put(uuid, playerData);
        });

        return state;
    }

    private static Type<PlayerLivesState> type = new Type<>(
            PlayerLivesState::new, // If there's no 'StateSaverAndLoader' yet create one
            PlayerLivesState::createFromNbt, // If there is a 'StateSaverAndLoader' NBT, parse it with 'createFromNbt'
            null // Supposed to be an 'DataFixTypes' enum, but we can just pass null
    );

    public static PlayerLivesState getServerState(MinecraftServer server) {
        // (Note: arbitrary choice to use 'World.OVERWORLD' instead of 'World.END' or 'World.NETHER'.  Any work)
        PersistentStateManager persistentStateManager = server.getWorld(World.OVERWORLD).getPersistentStateManager();

        // The first time the following 'getOrCreate' function is called, it creates a brand new 'StateSaverAndLoader' and
        // stores it inside the 'PersistentStateManager'. The subsequent calls to 'getOrCreate' pass in the saved
        // 'StateSaverAndLoader' NBT on disk to our function 'StateSaverAndLoader::createFromNbt'.
        PlayerLivesState state = persistentStateManager.getOrCreate(type, MultiplayerHardcore.MOD_ID);

        // If state is not marked dirty, when Minecraft closes, 'writeNbt' won't be called and therefore nothing will be saved.
        // Technically it's 'cleaner' if you only mark state as dirty when there was actually a change, but the vast majority
        // of mod writers are just going to be confused when their data isn't being saved, and so it's best just to 'markDirty' for them.
        // Besides, it's literally just setting a bool to true, and the only time there's a 'cost' is when the file is written to disk when
        // there were no actual change to any of the mods state (INCREDIBLY RARE).
        state.markDirty();

        return state;
    }

    public static PlayerData getPlayerState(LivingEntity player) {
        PlayerLivesState serverState = getServerState(player.getWorld().getServer());
        PlayerData playerState = serverState.players.computeIfAbsent(player.getUuid(), uuid -> new PlayerData(MultiplayerHardcoreConfig.defaultLives));

        return playerState;
    }

    public static PlayerData getPlayerState(MinecraftServer server, GameProfile player) {
        PlayerLivesState serverState = getServerState(server);
        PlayerData playerState = serverState.players.getOrDefault(player.getId(), new PlayerData(MultiplayerHardcoreConfig.defaultLives));

        return playerState;
    }
}
