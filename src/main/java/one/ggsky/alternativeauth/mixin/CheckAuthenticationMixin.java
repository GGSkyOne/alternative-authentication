package one.ggsky.alternativeauth.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.yggdrasil.ProfileActionType;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import com.mojang.authlib.yggdrasil.response.HasJoinedMinecraftServerResponse;
import com.mojang.authlib.yggdrasil.response.ProfileAction;

import one.ggsky.alternativeauth.config.AlternativeAuthConfig;
import one.ggsky.alternativeauth.config.AlternativeAuthConfigManager;
import one.ggsky.alternativeauth.config.AlternativeAuthProvider;
import one.ggsky.alternativeauth.logger.AlternativeAuthLogger;
import one.ggsky.alternativeauth.logger.AlternativeAuthLoggerManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.InetAddress;
import java.net.Proxy;
import java.net.URL;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Mixin(YggdrasilMinecraftSessionService.class)
public class CheckAuthenticationMixin {
    AlternativeAuthLogger LOGGER = AlternativeAuthLoggerManager.getLogger();
    AlternativeAuthConfig CONFIG = AlternativeAuthConfigManager.getConfig();

    @Inject(at = @At("HEAD"), method = "hasJoinedServer", remap = false, cancellable = true)
    public void CheckAuthentication(String profileName, String serverId, InetAddress address, CallbackInfoReturnable<ProfileResult> cir) throws AuthenticationUnavailableException {
        final MinecraftClient client = MinecraftClient.unauthenticated(Proxy.NO_PROXY);

        Map<String, Object> arguments = new HashMap<>();

        arguments.put("username", profileName);
        arguments.put("serverId", serverId);

        if (address != null) {
            arguments.put("ip", address.getHostAddress());
        }

        for (AlternativeAuthProvider provider : CONFIG.getProviders()) {
            LOGGER.debug("Trying to authenticate player via " + provider.name());
            LOGGER.debug("Using " + provider.getCheckUrl());

            final URL url = HttpAuthenticationService.concatenateURL(HttpAuthenticationService.constantURL(provider.getCheckUrl()), HttpAuthenticationService.buildQuery(arguments));

            try {
                final HasJoinedMinecraftServerResponse response = client.get(url, HasJoinedMinecraftServerResponse.class);

                if (response != null && response.id() != null) {
                    LOGGER.debug("Response is not null");
                    final GameProfile result = new GameProfile(response.id(), profileName);

                    if (response.properties() != null) {
                        PropertyMap properties;
                        LOGGER.debug("Properties is not null");

                        if (provider.getPropertyUrl() != null) {
                            LOGGER.debug(MessageFormat.format("Found {0} property URL, fetching {1}", provider.name(), MessageFormat.format(provider.getPropertyUrl(), profileName, response.id())));

                            final URL propertyUrl = HttpAuthenticationService.concatenateURL(HttpAuthenticationService.constantURL(MessageFormat.format(provider.getPropertyUrl(), profileName, response.id())), null);
                            final HasJoinedMinecraftServerResponse propertyResponse = client.get(propertyUrl, HasJoinedMinecraftServerResponse.class);

                            if (propertyResponse != null) {
                                LOGGER.debug("Properties is not null");
                                properties = propertyResponse.properties();
                            } else {
                                LOGGER.debug("Property response is null, falling back to initial properties");
                                properties = response.properties();
                            }
                        } else {
                            properties = response.properties();
                        }

                        result.getProperties().putAll(properties);
                    }

                    final Set<ProfileActionType> profileActions = response.profileActions().stream()
                        .map(ProfileAction::type)
                        .collect(Collectors.toSet());

                    LOGGER.info("Authenticating player via " + provider.name());
                    cir.setReturnValue(new ProfileResult(result, profileActions));
                    break;
                } else {
                    cir.setReturnValue(null);
                }
            } catch (final MinecraftClientException exception) {
                if (exception.toAuthenticationException() instanceof final AuthenticationUnavailableException unavailable) {
                    throw unavailable;
                }

                cir.setReturnValue(null);
            }
        }

        cir.cancel();
    }
}
