package one.ggsky.alternativeauth.util;

import com.google.common.collect.Iterables;
import com.google.gson.JsonObject;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import one.ggsky.alternativeauth.logger.AlternativeAuthLogger;
import one.ggsky.alternativeauth.logger.AlternativeAuthLoggerManager;
import one.ggsky.alternativeauth.skin.SkinVariant;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;

public final class SkinUtils {
    private static final AlternativeAuthLogger LOGGER = AlternativeAuthLoggerManager.getLogger();

    private SkinUtils() {}

    public record SkinData(String url, SkinVariant variant) {}

    public static Property getPlayerSkin(PropertyMap properties) {
        if (properties == null) return null;
        return Iterables.getFirst(properties.get("textures"), null);
    }

    public static SkinData getSkinUrl(Property skin) {
        if (skin == null) return null;
        JsonObject textureJson = skinPropertyToJson(skin);
        if (textureJson == null || !textureJson.has("textures")) return null;

        JsonObject textures = textureJson.getAsJsonObject("textures");
        if (!textures.has("SKIN")) return null;

        JsonObject skinTexture = textures.getAsJsonObject("SKIN");
        if (!skinTexture.has("url")) return null;

        String url = skinTexture.get("url").getAsString();
        SkinVariant variant = SkinVariant.CLASSIC;

        if (skinTexture.has("metadata")) {
            JsonObject metadata = skinTexture.getAsJsonObject("metadata");
            if (metadata.has("model")) {
                String model = metadata.get("model").getAsString();
                variant = "slim".equalsIgnoreCase(model) ? SkinVariant.SLIM : SkinVariant.CLASSIC;
            }
        }

        return new SkinData(url, variant);
    }

    public static boolean isMojangTexture(String url, Property property) {
        if (property == null || property.signature() == null || property.signature().isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host == null) return false;
            String normalizedHost = host.toLowerCase(Locale.ROOT);
            return normalizedHost.equals("textures.minecraft.net") || normalizedHost.endsWith(".minecraft.net");
        } catch (Exception e) {
            return false;
        }
    }

    public static JsonObject skinPropertyToJson(Property property) {
        if (property == null || property.value() == null) return null;
        try {
            byte[] decoded = Base64.getDecoder().decode(property.value());
            JsonObject json = AlternativeAuthUtils.GSON.fromJson(
                    new String(decoded, StandardCharsets.UTF_8), JsonObject.class);
            if (json != null) {
                json.remove("timestamp");
            }
            return json;
        } catch (Exception e) {
            LOGGER.debug("Could not parse skin property: " + e.getMessage());
            return null;
        }
    }
}
