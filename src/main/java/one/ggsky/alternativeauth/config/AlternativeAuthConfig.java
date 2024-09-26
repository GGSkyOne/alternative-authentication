package one.ggsky.alternativeauth.config;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AlternativeAuthConfig {
    private Boolean debug;
    private List<AlternativeAuthProvider> providers;

    @SerializedName("debug")
    public Boolean isDebuggerEnabled() {
        return debug;
    }

    @SerializedName("providers")
    public List<AlternativeAuthProvider> getProviders() {
        return providers;
    }
}
