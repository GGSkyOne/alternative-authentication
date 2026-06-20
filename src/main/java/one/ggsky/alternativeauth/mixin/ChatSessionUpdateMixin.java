package one.ggsky.alternativeauth.mixin;

import net.minecraft.network.protocol.game.ServerboundChatSessionUpdatePacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import one.ggsky.alternativeauth.logger.AlternativeAuthLogger;
import one.ggsky.alternativeauth.logger.AlternativeAuthLoggerManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ChatSessionUpdateMixin {
    @Unique
    private static final AlternativeAuthLogger LOGGER = AlternativeAuthLoggerManager.getLogger();

    @Inject(
        at = @At("HEAD"),
        method = "handleChatSessionUpdate",
        cancellable = true
    )
    private void handleChatSessionUpdate(ServerboundChatSessionUpdatePacket packet, CallbackInfo ci) {
        LOGGER.debug("Cancelling chat session update to prevent profile key validation kick");
        ci.cancel();
    }
}
