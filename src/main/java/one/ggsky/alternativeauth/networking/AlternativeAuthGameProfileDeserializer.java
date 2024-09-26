package one.ggsky.alternativeauth.networking;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;

import one.ggsky.alternativeauth.AlternativeAuthentication;
import one.ggsky.alternativeauth.logger.AlternativeAuthLoggerBase;

import java.lang.reflect.Type;
import java.util.UUID;

public class AlternativeAuthGameProfileDeserializer implements JsonDeserializer<GameProfile> {
    static AlternativeAuthLoggerBase LOGGER = AlternativeAuthentication.Logger;

    @Override
    public GameProfile deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        String idString = jsonObject.get("id").getAsString();
        UUID id;

        try {
            id = toUuid(idString);
        } catch (IllegalArgumentException exception) {
            LOGGER.debug("Invalid UUID format: " + idString);
            id = null;
        }

        String name = jsonObject.get("name").getAsString();
        return new GameProfile(id, name);
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
