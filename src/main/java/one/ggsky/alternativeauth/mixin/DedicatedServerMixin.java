package one.ggsky.alternativeauth.mixin;

import net.minecraft.server.dedicated.DedicatedServer;
import one.ggsky.alternativeauth.logger.AlternativeAuthLogger;
import one.ggsky.alternativeauth.logger.AlternativeAuthLoggerManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DedicatedServer.class)
public class DedicatedServerMixin {
    @Unique
    private static final AlternativeAuthLogger LOGGER = AlternativeAuthLoggerManager.getLogger();

    @Inject(
        at = @At("HEAD"),
        method = "enforceSecureProfile",
        cancellable = true
    )
    private void onEnforceSecureProfile(CallbackInfoReturnable<Boolean> cir) {
        LOGGER.debug("Overriding enforceSecureProfile to false for alternative authentication compatibility");
        cir.setReturnValue(false);
    }
}
