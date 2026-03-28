package one.ggsky.alternativeauth.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializer;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;

import java.util.Locale;
import java.util.Map;

public final class AlternativeAuthUtils {
    private AlternativeAuthUtils() {}

    public static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .registerTypeHierarchyAdapter(PropertyMap.class, (JsonSerializer<PropertyMap>) (src, type, ctx) -> {
            JsonArray arr = new JsonArray();
            for (Map.Entry<String, Property> entry : src.entries()) {
                JsonObject prop = new JsonObject();

                prop.addProperty("name", entry.getKey());
                prop.addProperty("value", entry.getValue().value());

                String signature = entry.getValue().signature();

                if (signature != null) {
                    prop.addProperty("signature", signature);
                }

                arr.add(prop);
            }
            return arr;
        })
        .create();

    public static String normalizeName(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}