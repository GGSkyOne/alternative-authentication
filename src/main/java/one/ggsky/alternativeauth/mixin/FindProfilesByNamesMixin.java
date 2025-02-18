package one.ggsky.alternativeauth.mixin;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.yggdrasil.ProfileNotFoundException;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilGameProfileRepository;
import com.mojang.authlib.yggdrasil.response.ProfileSearchResultsResponse;

import one.ggsky.alternativeauth.config.AlternativeAuthConfig;
import one.ggsky.alternativeauth.config.AlternativeAuthConfigManager;
import one.ggsky.alternativeauth.config.AlternativeAuthProvider;
import one.ggsky.alternativeauth.logger.AlternativeAuthLogger;
import one.ggsky.alternativeauth.logger.AlternativeAuthLoggerManager;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.URL;
import java.text.MessageFormat;
import java.util.*;

@Mixin(YggdrasilGameProfileRepository.class)
public class FindProfilesByNamesMixin {
    @Shadow private YggdrasilAuthenticationService authenticationService;

    AlternativeAuthLogger LOGGER = AlternativeAuthLoggerManager.getLogger();
    AlternativeAuthConfig CONFIG = AlternativeAuthConfigManager.getConfig();

    private static final int ENTRIES_PER_PAGE = 2;
    private static final int MAX_FAIL_COUNT = 3;
    private static final int DELAY_BETWEEN_PAGES = 100;
    private static final int DELAY_BETWEEN_FAILURES = 750;

    @Inject(at = @At("HEAD"), method = "findProfilesByNames", remap = false, cancellable = true)
    public void findProfilesByNames(final String[] names, final Agent agent, ProfileLookupCallback callback, CallbackInfo ci) {
        final Set<String> criteria = Sets.newHashSet();

        for (final String name : names) {
            if (!Strings.isNullOrEmpty(name)) {
                criteria.add(name.toLowerCase());
            }
        }

        final int page = 0;

        for (final List<String> request : Iterables.partition(criteria, ENTRIES_PER_PAGE)) {
            int failCount = 0;
            boolean failed;

            do {
                failed = false;

                try {
                    ProfileSearchResultsResponse response = null;

                    for (AlternativeAuthProvider provider : CONFIG.getProviders()) {
                        final URL url = HttpAuthenticationService.constantURL(provider.getProfilesUrl());
                        response = ((AuthenticationServiceAccessor) authenticationService).callMakeRequest(url, request, ProfileSearchResultsResponse.class, null);

                        if (response != null && response.getProfiles().length != 0) {
                            LOGGER.debug(MessageFormat.format("Response from {0} provider is not null and contains at least 1 element", provider.name()));
                            break;
                        } else {
                            LOGGER.debug(MessageFormat.format("Response from {0} provider is either null or contains no elements", provider.name()));
                        }
                    }

                    failCount = 0;

                    LOGGER.debug(MessageFormat.format("Page {0} returned {1} results, parsing", page, response.getProfiles().length));

                    final Set<String> missing = Sets.newHashSet(request);

                    for (final GameProfile profile : response.getProfiles()) {
                        LOGGER.debug(MessageFormat.format("Successfully looked up profile {0}", profile.toString()));
                        missing.remove(profile.getName().toLowerCase());
                        callback.onProfileLookupSucceeded(profile);
                    }

                    for (final String name : missing) {
                        LOGGER.debug(MessageFormat.format("Couldn't find profile {0}", name));
                        callback.onProfileLookupFailed(new GameProfile(null, name), new ProfileNotFoundException("Server did not find the requested profile"));
                    }

                    try {
                        Thread.sleep(DELAY_BETWEEN_PAGES);
                    } catch (final InterruptedException ignored) {
                    }
                } catch (final AuthenticationException e) {
                    failCount++;

                    if (failCount == MAX_FAIL_COUNT) {
                        for (final String name : request) {
                            LOGGER.debug(MessageFormat.format("Couldn't find profile {} because of a server error", name));
                            callback.onProfileLookupFailed(new GameProfile(null, name), e);
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
}
