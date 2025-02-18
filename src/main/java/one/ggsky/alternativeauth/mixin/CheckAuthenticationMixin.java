package one.ggsky.alternativeauth.mixin;

import com.mojang.authlib.AuthenticationService;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import com.mojang.authlib.yggdrasil.response.HasJoinedMinecraftServerResponse;

import one.ggsky.alternativeauth.config.AlternativeAuthConfig;
import one.ggsky.alternativeauth.config.AlternativeAuthConfigManager;
import one.ggsky.alternativeauth.config.AlternativeAuthProvider;
import one.ggsky.alternativeauth.logger.AlternativeAuthLogger;
import one.ggsky.alternativeauth.logger.AlternativeAuthLoggerManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.InetAddress;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Mixin(YggdrasilMinecraftSessionService.class)
public abstract class CheckAuthenticationMixin {
    @Shadow public abstract AuthenticationService getAuthenticationService();

    @Inject(at = @At("HEAD"), method = "hasJoinedServer", remap = false, cancellable = true)
    public void CheckAuthentication(final GameProfile user, String serverId, InetAddress address, CallbackInfoReturnable<GameProfile> cir) throws AuthenticationUnavailableException {
        AlternativeAuthLogger LOGGER = AlternativeAuthLoggerManager.getLogger();
        AlternativeAuthConfig CONFIG = AlternativeAuthConfigManager.getConfig();

        AuthenticationServiceAccessor authenticationService = ((AuthenticationServiceAccessor) getAuthenticationService());

        final Map<String, Object> arguments = new HashMap<>();

        arguments.put("username", user.getName());
        arguments.put("serverId", serverId);

        if (address != null) {
            arguments.put("ip", address.getHostAddress());
        }

        for (AlternativeAuthProvider provider : CONFIG.getProviders()) {
            LOGGER.debug("Trying to authenticate player via " + provider.name());
            LOGGER.debug("Using " + provider.getCheckUrl());

            final URL url = HttpAuthenticationService.concatenateURL(HttpAuthenticationService.constantURL(provider.getCheckUrl()), HttpAuthenticationService.buildQuery(arguments));
            
            try {
                final HasJoinedMinecraftServerResponse response = authenticationService.callMakeRequest(url, null, HasJoinedMinecraftServerResponse.class);

                if (response != null && response.getId() != null) {
                    LOGGER.debug("Response is not null");
                    final GameProfile result = new GameProfile(response.getId(), user.getName());

                    if (response.getProperties() != null) {
                        LOGGER.debug("Properties is not null");
                        result.getProperties().putAll(response.getProperties());
                    }

                    LOGGER.info("Authenticating player via " + provider.name());
                    cir.setReturnValue(result);
                    break;
                } else {
                    cir.setReturnValue(null);
                }
            } catch (final AuthenticationUnavailableException exception) {
                throw exception;
            } catch (final AuthenticationException ignored) {
                cir.setReturnValue(null);
            }
        }

        cir.cancel();
    }
}
