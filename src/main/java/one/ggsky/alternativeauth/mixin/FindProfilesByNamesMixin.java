package one.ggsky.alternativeauth.mixin;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.authlib.yggdrasil.ProfileNotFoundException;
import com.mojang.authlib.yggdrasil.YggdrasilGameProfileRepository;
import com.mojang.authlib.yggdrasil.response.ProfileSearchResultsResponse;

import one.ggsky.alternativeauth.config.AlternativeAuthConfig;
import one.ggsky.alternativeauth.config.AlternativeAuthConfigManager;
import one.ggsky.alternativeauth.config.AlternativeAuthProvider;
import one.ggsky.alternativeauth.logger.AlternativeAuthLogger;
import one.ggsky.alternativeauth.logger.AlternativeAuthLoggerManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.Proxy;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

@Mixin(YggdrasilGameProfileRepository.class)
public class FindProfilesByNamesMixin {
    AlternativeAuthLogger LOGGER = AlternativeAuthLoggerManager.getLogger();
    AlternativeAuthConfig CONFIG = AlternativeAuthConfigManager.getConfig();

    private static final int ENTRIES_PER_PAGE = 2;
    private static final int MAX_FAIL_COUNT = 3;
    private static final int DELAY_BETWEEN_PAGES = 100;
    private static final int DELAY_BETWEEN_FAILURES = 750;

    @Inject(at = @At("HEAD"), method = "findProfilesByNames", remap = false, cancellable = true)
    public void findProfilesByNames(String[] names, ProfileLookupCallback callback, CallbackInfo ci) {
        final MinecraftClient client = MinecraftClient.unauthenticated(Proxy.NO_PROXY);

        final Set<String> criteria = Arrays.stream(names)
            .filter(name -> !Strings.isNullOrEmpty(name))
            .collect(Collectors.toSet());

        final int page = 0;

        for (final List<String> request : Iterables.partition(criteria, ENTRIES_PER_PAGE)) {
            final List<String> normalizedRequest = request.stream().map(FindProfilesByNamesMixin::normalizeName).toList();

            int failCount = 0;
            boolean failed;

            do {
                failed = false;

                try {
                    ProfileSearchResultsResponse response = null;

                    for (AlternativeAuthProvider provider : CONFIG.getProviders()) {
                        final URL url = HttpAuthenticationService.constantURL(provider.getProfilesUrl());
                        response = client.post(url, normalizedRequest ,ProfileSearchResultsResponse.class);

                        if (response != null && !response.profiles().isEmpty()) {
                            LOGGER.debug(MessageFormat.format("Response from {0} provider is not null and contains at least 1 element", provider.name()));
                            break;
                        } else {
                            LOGGER.debug(MessageFormat.format("Response from {0} provider is either null or contains no elements", provider.name()));
                        }
                    }

                    final List<GameProfile> profiles = response != null ? response.profiles() : List.of();
                    failCount = 0;

                    LOGGER.debug(MessageFormat.format("Page {0} returned {1} results, parsing", page, profiles.size()));

                    final Set<String> received = new HashSet<>(profiles.size());

                    for (final GameProfile profile : profiles) {
                        LOGGER.debug(MessageFormat.format("Successfully looked up profile {0}", profile));
                        received.add(normalizeName(profile.getName()));
                        callback.onProfileLookupSucceeded(profile);
                    }

                    for (final String name : request) {
                        if (received.contains(normalizeName(name))) {
                            continue;
                        }

                        LOGGER.debug(MessageFormat.format("Could not find profile {0}", name));
                        callback.onProfileLookupFailed(name, new ProfileNotFoundException("Server did not find the requested profile"));
                    }

                    try {
                        Thread.sleep(DELAY_BETWEEN_PAGES);
                    } catch (final InterruptedException ignored) {
                    }
                } catch (final MinecraftClientException exception) {
                    failCount++;

                    if (failCount == MAX_FAIL_COUNT) {
                        for (final String name : request) {
                            LOGGER.debug(MessageFormat.format("Could not find profile {0} because of a server error", name));
                            callback.onProfileLookupFailed(name, exception.toAuthenticationException());
                        }
                    } else {
                        try {
                            Thread.sleep(DELAY_BETWEEN_FAILURES);
                        } catch (final InterruptedException ignored) {
                        }
                        failed = true;
                    }
                }
            } while (failed);
        }

        ci.cancel();
    }

    private static String normalizeName(final String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
