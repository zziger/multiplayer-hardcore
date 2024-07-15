package me.zziger.mphardcore;

import eu.midnightdust.lib.config.MidnightConfig;

public class MultiplayerHardcoreConfig extends MidnightConfig {
    @Entry(min = 1) public static int defaultLives = 3;
    @Entry(min = 0) public static int maxRescueTimes = 1;
    @Entry() public static boolean enableInSinglePlayer = false;

    public static void init() {
        MidnightConfig.init(MultiplayerHardcore.MOD_ID, MultiplayerHardcoreConfig.class);
    }
}
