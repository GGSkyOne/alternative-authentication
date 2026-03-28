package one.ggsky.alternativeauth.config;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Scanner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.fabricmc.loader.api.FabricLoader;
import one.ggsky.alternativeauth.AlternativeAuthentication;
import one.ggsky.alternativeauth.logger.AlternativeAuthLogger;
import one.ggsky.alternativeauth.logger.AlternativeAuthLoggerManager;

public class AlternativeAuthConfigManager {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final AlternativeAuthLogger LOGGER = AlternativeAuthLoggerManager.getLogger();
    private static AlternativeAuthConfig config = new AlternativeAuthConfig();

    public static AlternativeAuthConfig getConfig() {
        return config;
    }

    public static void loadConfig() {
        File configFile = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("alternative-auth.json")
            .toFile();

        if (!configFile.exists()) {
            createDefaultConfig(configFile);
            return;
        }

        JsonObject raw;

        try (FileReader reader = new FileReader(configFile)) {
            raw = gson.fromJson(reader, JsonObject.class);
        } catch (IOException e) {
            LOGGER.warn("Failed to load config file: " + e.getMessage());
            return;
        }

        if (needsMigration(raw)) {
            LOGGER.info("Migrating config to v1...");
            migrateToV1(raw);
            saveRawConfig(configFile, raw);
        }

        config = gson.fromJson(raw, AlternativeAuthConfig.class);
    }

    private static boolean needsMigration(JsonObject obj) {
        JsonElement v = obj.get("configVersion");
        return v == null || v.isJsonNull() || v.getAsInt() < 1;
    }

    private static void migrateToV1(JsonObject obj) {
        obj.addProperty("configVersion", 1);

        if (obj.has("debug") && !obj.has("debugMode")) {
            obj.add("debugMode", obj.get("debug"));
            obj.remove("debug");
        } else if (!obj.has("debugMode")) {
            obj.addProperty("debugMode", false);
        }

        if (!obj.has("preventFallbackIfPlayerExists")) {
            obj.addProperty("preventFallbackIfPlayerExists", false);
        }

        if (!obj.has("providers")) return;

        for (JsonElement el : obj.getAsJsonArray("providers")) {
            JsonObject provider = el.getAsJsonObject();

            renameField(provider, "check_url", "checkUrl");
            renameField(provider, "profile_url", "profileUrl");
            renameField(provider, "profiles_url", "profilesUrl");
            renameField(provider, "property_url", "propertyUrl");

            if (!provider.has("profileUrl") || provider.get("profileUrl").isJsonNull()) {
                if (!provider.has("name")) continue;

                switch (provider.get("name").getAsString().toLowerCase()) {
                    case "mojang" -> provider.addProperty("profileUrl",
                        "https://api.minecraftservices.com/minecraft/profile/lookup/name/");
                    case "ely.by" -> provider.addProperty("profileUrl",
                        "https://authserver.ely.by/api/users/profiles/minecraft/");
                }
            }
        }
    }

    private static void renameField(JsonObject obj, String from, String to) {
        if (obj.has(from) && !obj.has(to)) {
            obj.add(to, obj.get(from));
            obj.remove(from);
        }
    }

    private static void saveRawConfig(File file, JsonObject obj) {
        try (PrintWriter writer = new PrintWriter(file)) {
            writer.println(gson.toJson(obj));
        } catch (IOException e) {
            LOGGER.warn("Failed to save config file: " + e.getMessage());
        }
    }

    private static void createDefaultConfig(File configFile) {
        try {
            configFile.getParentFile().mkdirs();

            InputStream stream = AlternativeAuthentication.class
                .getClassLoader()
                .getResourceAsStream("alternative-auth.json");

            if (stream == null) return;

            String content;

            try (Scanner scanner = new Scanner(stream).useDelimiter("\\A")) {
                content = scanner.hasNext() ? scanner.next() : "";
            }

            config = gson.fromJson(content, AlternativeAuthConfig.class);

            try (PrintWriter writer = new PrintWriter(configFile)) {
                writer.println(content);
            }
        } catch (IOException e) {
            LOGGER.warn("Failed to create default config: " + e.getMessage());
        }
    }
}
