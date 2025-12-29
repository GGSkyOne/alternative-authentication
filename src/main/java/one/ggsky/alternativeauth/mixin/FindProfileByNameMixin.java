package one.ggsky.alternativeauth.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.authlib.yggdrasil.YggdrasilGameProfileRepository;
import one.ggsky.alternativeauth.config.AlternativeAuthConfig;
import one.ggsky.alternativeauth.config.AlternativeAuthConfigManager;
import one.ggsky.alternativeauth.config.AlternativeAuthProvider;
import one.ggsky.alternativeauth.logger.AlternativeAuthLogger;
import one.ggsky.alternativeauth.logger.AlternativeAuthLoggerManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.text.MessageFormat;
import java.util.Locale;
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
    private void findProfileByName(String name, CallbackInfoReturnable<Optional<GameProfile>> cir) {
        for (AlternativeAuthProvider provider : CONFIG.getProviders()) {
            try {
                GameProfile profile = client.get(HttpAuthenticationService.constantURL(provider.getProfileUrl() + normalizeName(name)), GameProfile.class);

                if (profile != null) {
                    cir.setReturnValue(Optional.of(profile));
                    return;
                }

            } catch (MinecraftClientException e) {
                LOGGER.debug(MessageFormat.format("Provider {0} failed for {1}", provider.name(), name));
            }
        }

        LOGGER.warn("Couldn't find profile with name: " + name);
        cir.setReturnValue(Optional.empty());
    }

    @Unique
    private static String normalizeName(final String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}