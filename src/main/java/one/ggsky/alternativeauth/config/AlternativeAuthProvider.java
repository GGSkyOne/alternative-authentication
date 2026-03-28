package one.ggsky.alternativeauth.config;

public class AlternativeAuthProvider {
    private String name;
    private String checkUrl;
    private String profileUrl;
    private String profilesUrl;
    private String propertyUrl;

    public String getName() {
        return name;
    }

    public String getCheckUrl() {
        return checkUrl;
    }

    public String getProfileUrl() {
        return profileUrl;
    }

    public String getProfilesUrl() {
        return profilesUrl;
    }

    public String getPropertyUrl() {
        return propertyUrl;
    }
}
