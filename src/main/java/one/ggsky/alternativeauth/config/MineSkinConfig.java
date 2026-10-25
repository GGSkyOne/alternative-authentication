package one.ggsky.alternativeauth.config;

public class MineSkinConfig {
    private Boolean enabled = false;
    private String apiKey = "";
    private Boolean proxyUrlUpload = false;

    public boolean isEnabled() {
        return Boolean.TRUE.equals(enabled);
    }

    public String getApiKey() {
        return apiKey != null ? apiKey : "";
    }

    public boolean isProxyUrlUpload() {
        return Boolean.TRUE.equals(proxyUrlUpload);
    }
}
