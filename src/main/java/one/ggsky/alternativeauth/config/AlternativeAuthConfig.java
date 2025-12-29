package one.ggsky.alternativeauth.config;

import java.util.List;

public class AlternativeAuthConfig {
    private Boolean debug = false;
    private List<AlternativeAuthProvider> providers = List.of();

    public Boolean isDebugModeEnabled() {
        return debug;
    }

    public List<AlternativeAuthProvider> getProviders() {
        return providers;
    }
}
