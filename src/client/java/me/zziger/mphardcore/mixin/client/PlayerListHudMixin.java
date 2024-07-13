package me.zziger.mphardcore.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.zziger.mphardcore.Heart;
import me.zziger.mphardcore.MultiplayerHardcoreClient;
import me.zziger.mphardcore.MultiplayerHardcoreConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.UUID;

@Mixin(PlayerListHud.class)
public class PlayerListHudMixin {
    @Final
    @Shadow
    private InGameHud inGameHud;

    @Final
    @Shadow
    private MinecraftClient client;

    @Unique
    private static final Identifier CONTAINER_HEART_BLINKING_TEXTURE = Identifier.ofVanilla("hud/heart/container_hardcore_blinking");
    @Unique
    private static final Identifier CONTAINER_HEART_TEXTURE = Identifier.ofVanilla("hud/heart/container_hardcore");
    @Unique
    private static final Identifier FULL_HEART_BLINKING_TEXTURE = Identifier.ofVanilla("hud/heart/hardcore_full_blinking");
    @Unique
    private static final Identifier FULL_HEART_TEXTURE = Identifier.ofVanilla("hud/heart/hardcore_full");
    @Unique
    private static final int SECTION_SIZE = 45;

    @Unique
    private final Map<UUID, Heart> hardcoreHearts = new Object2ObjectOpenHashMap<>();

    @Unique
    private void renderCustomHearts(int y, int left, int right, UUID uuid, DrawContext context, int score) {
        Heart heart = (Heart) this.hardcoreHearts.computeIfAbsent(uuid, (uuid2) -> {
            return new Heart(score);
        });
        heart.tick(score, (long) this.inGameHud.getTicks());
        int heartCount = Math.max(score, heart.getPrevScore());
        int heartBgCount = Math.max(score, Math.max(heart.getPrevScore(), MultiplayerHardcoreClient.defaultLives));
        boolean bl = heart.useHighlighted((long) this.inGameHud.getTicks());

        int k = MathHelper.floor(Math.min((float) (right - left - 4) / (float) heartBgCount, 9.0F));
        int l;
        if (k <= 3) {
            float f = MathHelper.clamp((float) score / 10.0f, 0.0F, 1.0F);
            MutableText text = Text.translatable("mphardcore.n_lives", new Object[]{score}).formatted(Formatting.RED);
            if (right - this.client.textRenderer.getWidth(text) < left) {
                text = Text.literal(Integer.toString(score));
            }

            context.drawTextWithShadow(this.client.textRenderer, text, (right + left - this.client.textRenderer.getWidth(text)) / 2, y, 16777215);
        } else {
            Identifier identifier = bl ? CONTAINER_HEART_BLINKING_TEXTURE : CONTAINER_HEART_TEXTURE;

            for (l = heartCount; l < heartBgCount; ++l) {
                context.drawGuiTexture(identifier, left + l * k, y, 9, 9);
            }

            for (l = 0; l < heartCount; ++l) {
                context.drawGuiTexture(identifier, left + l * k, y, 9, 9);
                if (bl) {
                    if (l < heart.getPrevScore()) {
                        context.drawGuiTexture(FULL_HEART_BLINKING_TEXTURE, left + l * k, y, 9, 9);
                    }
                }

                if (l < score) {
                    context.drawGuiTexture(FULL_HEART_TEXTURE, left + l * k, y, 9, 9);
                }
            }

        }
    }

    @Shadow
    private boolean visible;

    @Inject(at = @At("HEAD"), method = "setVisible(Z)V")
    private void setVisible(boolean visible, CallbackInfo ci) {
        if (this.visible != visible) {
            hardcoreHearts.clear();
        }
    }


    @Inject(at = @At("HEAD"), method = "renderLatencyIcon(Lnet/minecraft/client/gui/DrawContext;IIILnet/minecraft/client/network/PlayerListEntry;)V")
    private void init(DrawContext context, int width, int x, int y, PlayerListEntry entry, CallbackInfo ci) {
        if (!MultiplayerHardcoreClient.enabled) return;
        int baseX = x + width - 11 - SECTION_SIZE - 3;
        renderCustomHearts(y, baseX, baseX + SECTION_SIZE, entry.getProfile().getId(), context, MultiplayerHardcoreClient.getLives(entry.getProfile().getId()));
    }

    @ModifyArg(method = "render(Lnet/minecraft/client/gui/DrawContext;ILnet/minecraft/scoreboard/Scoreboard;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/PlayerListHud;renderScoreboardObjective(Lnet/minecraft/scoreboard/ScoreboardObjective;ILnet/minecraft/client/gui/hud/PlayerListHud$ScoreDisplayEntry;IILjava/util/UUID;Lnet/minecraft/client/gui/DrawContext;)V"),
            index = 4)
    private int modifyScoreboardMax(int x) {
        if (!MultiplayerHardcoreClient.enabled) return x;
        return x - SECTION_SIZE - 3;
    }

    @Inject(at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(II)I", shift = At.Shift.BY, by = -20),
            method = "render(Lnet/minecraft/client/gui/DrawContext;ILnet/minecraft/scoreboard/Scoreboard;Lnet/minecraft/scoreboard/ScoreboardObjective;)V",
            slice = @Slice(
                    from = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;isInSingleplayer()Z"),
                    to = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/TextRenderer;wrapLines(Lnet/minecraft/text/StringVisitable;I)Ljava/util/List;")
            )
    )
    private void render(DrawContext context, int scaledWindowWidth, Scoreboard scoreboard, ScoreboardObjective objective, CallbackInfo ci, @Local(index = 14) LocalIntRef test) {
        if (!MultiplayerHardcoreClient.enabled) return;
        test.set(test.get() + SECTION_SIZE + 3);
    }
}