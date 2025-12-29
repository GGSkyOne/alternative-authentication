package one.ggsky.alternativeauth.config;

public class AlternativeAuthProvider {
    private String name;
    private String check_url;
    private String profile_url;
    private String profiles_url;
    private String property_url;

    public String name() {
        return name;
    }

    public String getCheckUrl() {
        return check_url;
    }

    public String getProfileUrl() {
        return profile_url;
    }

    public void setProfileUrl(String profile_url) {
        this.profile_url = profile_url;
    }

    public String getProfilesUrl() {
        return profiles_url;
    }

    public String getPropertyUrl() {
        return property_url;
    }
}
