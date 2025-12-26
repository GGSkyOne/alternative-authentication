package one.ggsky.alternativeauth.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.authlib.yggdrasil.YggdrasilGameProfileRepository;
import com.mojang.authlib.yggdrasil.response.NameAndId;
import com.mojang.authlib.yggdrasil.response.ProfileSearchResultsResponse;
import one.ggsky.alternativeauth.config.AlternativeAuthConfig;
import one.ggsky.alternativeauth.config.AlternativeAuthConfigManager;
import one.ggsky.alternativeauth.config.AlternativeAuthProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.Proxy;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Mixin(value = YggdrasilGameProfileRepository.class, remap = false)
public class FindProfileByNameMixin {

    @Inject(method = "findProfileByName", at = @At("HEAD"), cancellable = true)
    private void alternativeAuth$findProfileByName(String name,
                                                   CallbackInfoReturnable<Optional<GameProfile>> cir) {
        if (name == null || name.isEmpty()) {
            return;
        }

        AlternativeAuthConfig config = AlternativeAuthConfigManager.getConfig();

        final MinecraftClient client = MinecraftClient.unauthenticated(Proxy.NO_PROXY);

        // просто сразу в lowerCase вместо normalizeName()
        final List<String> request = List.of(name.toLowerCase(Locale.ROOT));

        for (AlternativeAuthProvider provider : config.getProviders()) {
            try {
                URL url = HttpAuthenticationService.constantURL(provider.getProfilesUrl());
                ProfileSearchResultsResponse response =
                        client.post(url, request, ProfileSearchResultsResponse.class);

                if (response != null && !response.profiles().isEmpty()) {
                    NameAndId p = response.profiles().get(0);
                    GameProfile gameProfile = new GameProfile(p.id(), p.name());
                    cir.setReturnValue(Optional.of(gameProfile));
                    return;
                }

            } catch (MinecraftClientException ignored) {
                // просто пробуем следующий провайдер
            }
        }

        cir.setReturnValue(Optional.empty());
    }
}
