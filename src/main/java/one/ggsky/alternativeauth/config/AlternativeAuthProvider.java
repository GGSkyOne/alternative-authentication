package one.ggsky.alternativeauth.config;

import com.google.gson.annotations.SerializedName;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;

public class AlternativeAuthProvider {
    private String name;
    private String check_url;
    private String profiles_url;
    private String property_url;
    private Property[] custom_properties;

    @SerializedName("name")
    public String name() {
        return name;
    }

    @SerializedName("check_url")
    public String getCheckUrl() {
        return check_url;
    }

    @SerializedName("check_url")
    public String getProfilesUrl() {
        return profiles_url;
    }

    @SerializedName("property_url")
    public String getPropertyUrl() {
        return property_url;
    }

    @SerializedName("custom_properties")
    public Property[] getCustomProperties() {
        return custom_properties;
    }

    public PropertyMap getProperties() {
        PropertyMap map = new PropertyMap();

        for (Property property : custom_properties){
            map.put(property.name(), property);
        }
        
        return map;
    }
}
