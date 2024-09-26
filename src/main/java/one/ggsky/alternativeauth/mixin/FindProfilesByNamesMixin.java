package one.ggsky.alternativeauth.mixin;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.yggdrasil.ProfileNotFoundException;
import com.mojang.authlib.yggdrasil.YggdrasilGameProfileRepository;
import one.ggsky.alternativeauth.AlternativeAuthentication;
import one.ggsky.alternativeauth.config.AlternativeAuthConfig;
import one.ggsky.alternativeauth.config.AlternativeAuthProvider;
import one.ggsky.alternativeauth.logger.AlternativeAuthLoggerBase;
import one.ggsky.alternativeauth.networking.AlternativeAuthClient;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

@Mixin(YggdrasilGameProfileRepository.class)
public class FindProfilesByNamesMixin {
     private static final Gson gson = new Gson();

    AlternativeAuthLoggerBase LOGGER = AlternativeAuthentication.Logger;
    AlternativeAuthConfig CONFIG = AlternativeAuthentication.getConfig();

    private static final int ENTRIES_PER_PAGE = 2;
    private static final int MAX_FAIL_COUNT = 3;
    private static final int DELAY_BETWEEN_PAGES = 100;
    private static final int DELAY_BETWEEN_FAILURES = 750;

    @Inject(at = @At("HEAD"), method = "findProfilesByNames", remap = false, cancellable = true)
    public void findProfilesByNames(String[] names, ProfileLookupCallback callback, CallbackInfo ci) {
        final Set<String> criteria = Arrays.stream(names)
                .filter(name -> !Strings.isNullOrEmpty(name))
                .collect(Collectors.toSet());

        final int page = 0;

        for (final List<String> request : Iterables.partition(criteria, ENTRIES_PER_PAGE)) {
            final List<String> normalizedRequest = request.stream().map(FindProfilesByNamesMixin::normalizeName).toList();
            final String jsonRequest = gson.toJson(normalizedRequest, new TypeToken<List<String>>() {}.getType());

            int failCount = 0;
            boolean failed;

            do {
                failed = false;

                try {
                    List<GameProfile> response = null;

                    for (AlternativeAuthProvider provider : CONFIG.getProviders()) {
                        response = AlternativeAuthClient.post(AlternativeAuthClient.buildUri(provider.getProfilesUrl(), null), jsonRequest, new TypeToken<List<GameProfile>>(){}.getType());

                        if (response != null && !response.isEmpty()) {
                            LOGGER.debug("Response is not null");
                            break;
                        } else {
                            LOGGER.debug("Response is null");
                        }
                    }

                    final List<GameProfile> profiles = response != null ? response : List.of();
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
                } catch (final MinecraftClientException e) {
                    failCount++;

                    if (failCount == MAX_FAIL_COUNT) {
                        for (final String name : request) {
                            LOGGER.debug(MessageFormat.format("Could not find profile {0} because of a server error", name));
                            callback.onProfileLookupFailed(name, e.toAuthenticationException());
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
