package one.ggsky.alternativeauth.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.yggdrasil.ProfileActionType;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;

import com.mojang.authlib.yggdrasil.response.ProfileAction;
import one.ggsky.alternativeauth.AlternativeAuthentication;
import one.ggsky.alternativeauth.config.AlternativeAuthConfig;
import one.ggsky.alternativeauth.config.AlternativeAuthProvider;
import one.ggsky.alternativeauth.networking.AlternativeAuthHasJoinedServerResponse;
import one.ggsky.alternativeauth.networking.AlternativeAuthClient;
import one.ggsky.alternativeauth.logger.AlternativeAuthLoggerBase;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.InetAddress;
import java.net.URI;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Mixin(YggdrasilMinecraftSessionService.class)
public class CheckAuthenticationMixin {
    AlternativeAuthLoggerBase LOGGER = AlternativeAuthentication.Logger;
    AlternativeAuthConfig CONFIG = AlternativeAuthentication.getConfig();

    @Inject(at = @At("HEAD"), method = "hasJoinedServer", remap = false, cancellable = true)
    public void CheckAuthentication(String profileName, String serverId, InetAddress address, CallbackInfoReturnable<ProfileResult> cir) throws AuthenticationUnavailableException {
        boolean authException = false;
        boolean authUnavailableException = false;
        AuthenticationUnavailableException authUnavailableExceptionData = null;

        Map<String, Object> arguments = new HashMap<>();

        arguments.put("username", profileName);
        arguments.put("serverId", serverId);

        if (address != null) {
            arguments.put("ip", address.getHostAddress());
        }

        for (AlternativeAuthProvider provider : CONFIG.getProviders()) {
            LOGGER.debug("Trying to authenticate player via " + provider.name());
            LOGGER.debug("Using " + provider.getCheckUrl());

            final URI uri = AlternativeAuthClient.buildUri(provider.getCheckUrl(), arguments);
            
            try {
                final AlternativeAuthHasJoinedServerResponse response = AlternativeAuthClient.get(uri, AlternativeAuthHasJoinedServerResponse.class);

                if (response != null && response.id() != null) {
                    LOGGER.debug("Response is not null");
                    final GameProfile result = new GameProfile(response.id(), profileName);

                    if (response.properties() != null) {
                        PropertyMap properties;
                        LOGGER.debug("Properties is not null");

                        if (provider.getPropertyUrl() != null) {
                            LOGGER.debug("Found property URL, fetching...");
                            LOGGER.debug("in " + provider.getPropertyUrl());

                            URI propertyUri = AlternativeAuthClient.buildUri(MessageFormat.format(provider.getPropertyUrl(), profileName, response.id()), null);
                            AlternativeAuthHasJoinedServerResponse propertyResponse = AlternativeAuthClient.get(propertyUri, AlternativeAuthHasJoinedServerResponse.class);

                            if (propertyResponse != null) {
                                properties = propertyResponse.properties();
                            } else {
                                LOGGER.debug("Property response is null");
                                properties = response.properties();
                            }

                        } else {
                            properties = response.properties();
                        }

                        result.getProperties().putAll(properties);
                    }

                    if (provider.getCustomProperties() != null){
                        LOGGER.debug("Added custom properties");
                        result.getProperties().putAll(provider.getProperties());
                    }

                    final Set<ProfileActionType> profileActions = response.profileActions().stream()
                            .map(ProfileAction::type)
                            .collect(Collectors.toSet());

                    LOGGER.info("Authenticating player via " + provider.name());

                    cir.setReturnValue(new ProfileResult(result, profileActions));
                    cir.cancel();

                    authUnavailableException = false;
                    authException = false;
                    break;
                }
            } catch (AuthenticationUnavailableException exception) {
                authUnavailableException = true;
                authUnavailableExceptionData = exception;
            } catch (AuthenticationException exception) {
                authException = true;
            }
        }

        if (authUnavailableException) {
            throw authUnavailableExceptionData;
        }

        if (authException) {
            cir.setReturnValue(null);
        }

        cir.cancel();
    }
}
