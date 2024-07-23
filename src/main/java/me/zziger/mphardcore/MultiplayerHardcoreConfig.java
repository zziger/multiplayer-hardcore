package me.zziger.mphardcore;

import eu.midnightdust.lib.config.MidnightConfig;

public class MultiplayerHardcoreConfig extends MidnightConfig {
    @Entry(min = 1) public static int defaultLives = 3;
    @Entry(min = 0) public static int maxRescueTimes = 1;
    @Entry() public static boolean enableInSinglePlayer = false;

    @Entry() public static int setLivesOpLevel = 1;
    @Entry() public static int getLivesOpLevel = 1;
    @Entry() public static int rescueOpLevel = 1;
    @Entry() public static int resetRescueOpLevel = 4;

    public static void init() {
        MidnightConfig.init(MultiplayerHardcore.MOD_ID, MultiplayerHardcoreConfig.class);
    }
}
