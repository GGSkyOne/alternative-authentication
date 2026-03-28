package one.ggsky.alternativeauth.mixin;

import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.authlib.yggdrasil.YggdrasilGameProfileRepository;
import com.mojang.authlib.yggdrasil.response.NameAndId;
import one.ggsky.alternativeauth.config.AlternativeAuthConfig;
import one.ggsky.alternativeauth.config.AlternativeAuthConfigManager;
import one.ggsky.alternativeauth.config.AlternativeAuthProvider;
import one.ggsky.alternativeauth.logger.AlternativeAuthLogger;
import one.ggsky.alternativeauth.logger.AlternativeAuthLoggerManager;
import one.ggsky.alternativeauth.util.AlternativeAuthUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(YggdrasilGameProfileRepository.class)
public class FindProfileByNameMixin {
    @Unique
    private static final AlternativeAuthLogger LOGGER = AlternativeAuthLoggerManager.getLogger();

    @Unique
    private static final AlternativeAuthConfig CONFIG = AlternativeAuthConfigManager.getConfig();

    @Shadow @Final
    private MinecraftClient client;

    @Inject(
        at = @At("HEAD"),
        method = "findProfileByName",
        remap = false,
        cancellable = true
    )
    private void findProfileByName(String name, CallbackInfoReturnable<Optional<NameAndId>> cir) {
        for (AlternativeAuthProvider provider : CONFIG.getProviders()) {
            LOGGER.debug("Trying provider " + provider.getName());

            try {
                NameAndId profile = client.get(HttpAuthenticationService.constantURL(provider.getProfileUrl() + AlternativeAuthUtils.normalizeName(name)), NameAndId.class);

                if (profile != null) {
                    LOGGER.debug("Resolved '" + name + "' to UUID " + profile.id());

                    cir.setReturnValue(Optional.of(profile));
                    return;
                }

                LOGGER.debug("Provider " + provider.getName() + " returned no result");
            } catch (MinecraftClientException e) {
                LOGGER.debug("Provider " + provider.getName() + " failed: " + e.getMessage());
            }
        }

        LOGGER.warn("Couldn't find profile with name: " + name);
        cir.setReturnValue(Optional.empty());
    }
}
