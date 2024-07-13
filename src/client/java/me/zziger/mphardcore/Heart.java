package me.zziger.mphardcore;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class Heart {
    private static final long COOLDOWN_TICKS = 20L;
    private static final long SCORE_DECREASE_HIGHLIGHT_TICKS = 20L;
    private static final long SCORE_INCREASE_HIGHLIGHT_TICKS = 10L;
    private int score;
    private int prevScore;
    private long lastScoreChangeTick;
    private long highlightEndTick;

    public Heart(int score) {
        this.prevScore = score;
        this.score = score;
    }

    public void tick(int score, long currentTick) {
        if (score != this.score) {
            long l = score < this.score ? 20L : 10L;
            this.highlightEndTick = currentTick + l;
            this.score = score;
            this.lastScoreChangeTick = currentTick;
        }

        if (currentTick - this.lastScoreChangeTick > 20L) {
            this.prevScore = score;
        }

    }

    public int getPrevScore() {
        return this.prevScore;
    }

    public boolean useHighlighted(long currentTick) {
        return this.highlightEndTick > currentTick && (this.highlightEndTick - currentTick) % 6L >= 3L;
    }
}