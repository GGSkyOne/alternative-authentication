package one.ggsky.alternativeauth.config;

import java.util.List;

public class AlternativeAuthConfig {
    private Integer configVersion = 1;
    private Boolean debugMode = false;
    private Boolean preventFallbackIfPlayerExists = false;
    private MineSkinConfig mineSkin = new MineSkinConfig();
    private List<AlternativeAuthProvider> providers = List.of();

    public int getConfigVersion() {
        return configVersion != null ? configVersion : 0;
    }

    public Boolean isDebugModeEnabled() {
        return debugMode;
    }

    public boolean isPreventFallbackIfPlayerExists() {
        return Boolean.TRUE.equals(preventFallbackIfPlayerExists);
    }

    public MineSkinConfig getMineSkin() {
        return mineSkin != null ? mineSkin : new MineSkinConfig();
    }

    public List<AlternativeAuthProvider> getProviders() {
        return providers;
    }
}
