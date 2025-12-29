package one.ggsky.alternativeauth.config;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Scanner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import net.fabricmc.loader.api.FabricLoader;
import one.ggsky.alternativeauth.AlternativeAuthentication;

public class AlternativeAuthConfigManager {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static AlternativeAuthConfig config = new AlternativeAuthConfig();

    public static AlternativeAuthConfig getConfig() {
        return config;
    }

    public static void loadConfig() {
        File configurationFile = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("alternative-auth.json")
            .toFile();

        if (!configurationFile.exists()) {
            createDefaultConfig(configurationFile);
            return;
        }

        try (FileReader reader = new FileReader(configurationFile)) {
            config = gson.fromJson(new JsonReader(reader), AlternativeAuthConfig.class);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        boolean upgraded = upgradeConfigIfNeeded();

        if (upgraded) {
            saveConfig(configurationFile);
        }
    }

    private static boolean upgradeConfigIfNeeded() {
        boolean changed = false;

        if (config.getProviders() == null) return false;

        for (AlternativeAuthProvider provider : config.getProviders()) {
            if (provider.getProfileUrl() == null) {
                switch (provider.name().toLowerCase()) {
                    case "mojang" ->
                        provider.setProfileUrl("https://api.minecraftservices.com/minecraft/profile/lookup/name/");
                    case "ely.by" ->
                        provider.setProfileUrl("https://authserver.ely.by/api/users/profiles/minecraft/");
                }

                changed = true;
            }
        }

        return changed;
    }

    private static void saveConfig(File file) {
        try (PrintWriter writer = new PrintWriter(file)) {
            writer.println(gson.toJson(config));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createDefaultConfig(File configurationFile) {
        try {
            configurationFile.getParentFile().mkdirs();
            InputStream stream = AlternativeAuthentication.class
                .getClassLoader()
                .getResourceAsStream("alternative-auth.json");

            if (stream == null) return;

            String content;

            try (Scanner scanner = new Scanner(stream).useDelimiter("\\A")) {
                content = scanner.hasNext() ? scanner.next() : "";
            }

            config = gson.fromJson(content, AlternativeAuthConfig.class);

            try (PrintWriter writer = new PrintWriter(configurationFile)) {
                writer.println(content);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
