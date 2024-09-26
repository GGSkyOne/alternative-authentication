package one.ggsky.alternativeauth.networking;

import com.google.gson.annotations.SerializedName;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.yggdrasil.response.ProfileAction;

import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

public class AlternativeAuthHasJoinedServerResponse {
    private String id;
    private Property[] properties;
    private Set<ProfileAction> profileActions;

    @SerializedName("id")
    @Nullable
    public UUID id() {
        return toUuid(id);
    }

    @SerializedName("properties")
    @Nullable
    public PropertyMap properties() {
        PropertyMap map = new PropertyMap();
        
        for (Property property : properties) {
            map.put(property.name(), property);
        }

        return map;
    }

    public Set<ProfileAction> profileActions() {
        return profileActions != null ? profileActions : Set.of();
    }

    private static UUID toUuid(String uuid){
        if (uuid != null) {
            return UUID.fromString(
                uuid
                .replaceFirst( 
                    "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5" 
                )
            );
        } else {
            return null;
        }
    }
}


