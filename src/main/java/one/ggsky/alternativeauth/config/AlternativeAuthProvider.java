package one.ggsky.alternativeauth.config;

import com.google.gson.annotations.SerializedName;

public class AlternativeAuthProvider {
    private String name;
    private String check_url;
    private String profiles_url;

    @SerializedName("name")
    public String name() {
        return name;
    }

    @SerializedName("check_url")
    public String getCheckUrl() {
        return check_url;
    }

    @SerializedName("profiles_url")
    public String getProfilesUrl() {
        return profiles_url;
    }
}
