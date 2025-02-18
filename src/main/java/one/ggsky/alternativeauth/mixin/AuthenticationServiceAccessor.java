package one.ggsky.alternativeauth.mixin;

import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.yggdrasil.response.Response;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.net.URL;

@Mixin(YggdrasilAuthenticationService.class)
public interface AuthenticationServiceAccessor {
    @Invoker
    <T extends Response> T callMakeRequest(final URL url, final Object input, final Class<T> classOfT) throws AuthenticationException;
}