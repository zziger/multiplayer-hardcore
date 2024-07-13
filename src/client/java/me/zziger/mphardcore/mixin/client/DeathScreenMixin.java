package me.zziger.mphardcore.mixin.client;

import me.zziger.mphardcore.MultiplayerHardcoreClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmLinkScreen;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(DeathScreen.class)
public abstract class DeathScreenMixin extends Screen {
    protected DeathScreenMixin(Text title) {
        super(title);
    }

    @Unique
    public Text livesLeftText;

    @Shadow
    @Final
    private boolean isHardcore;

    @Shadow @Final private List<ButtonWidget> buttons;

    @Shadow protected abstract void setButtonsActive(boolean active);

    @Unique
    private int getLives() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        assert player != null;
        return MultiplayerHardcoreClient.getLives(player.getUuid());
    }

    @Redirect(method = "init()V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screen/DeathScreen;isHardcore:Z", opcode = Opcodes.GETFIELD))
    private boolean getIsHardcore(DeathScreen instance) {
        if (!MultiplayerHardcoreClient.enabled) return isHardcore;
        return getLives() <= 0;
    }

    @Inject(method = "init()V", at = @At("TAIL"))
    public void init(CallbackInfo ci) {
        Integer lives = getLives();
        this.livesLeftText =
                lives > 0
                        ? Text.translatable("mphardcore.lives_left", new Object[]{Text.literal(Integer.toString(getLives())).formatted(Formatting.RED)})
                        : Text.translatable("mphardcore.no_lives_left").formatted(Formatting.RED);
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/DrawContext;IIF)V", at = @At("TAIL"))
    public void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!MultiplayerHardcoreClient.enabled) return;
        context.drawCenteredTextWithShadow(this.textRenderer, this.livesLeftText, this.width / 2, 115, 16777215);
    }
}
