package one.ggsky.alternativeauth.config;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Scanner;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import net.fabricmc.loader.api.FabricLoader;
import one.ggsky.alternativeauth.AlternativeAuthentication;

public class AlternativeAuthConfigManager {
    private static final Gson gson = new Gson();
    private static AlternativeAuthConfig config;

    public static AlternativeAuthConfig getConfig() {
        return config;
    }

    public static void loadConfig() {
        File configurationFile = FabricLoader.getInstance().getConfigDir().resolve("alternative-auth.json").toFile();

        if (!configurationFile.exists()) {
            createDefaultConfig(configurationFile);
        }

        try {
            config = gson.fromJson(new JsonReader(new FileReader(configurationFile)), AlternativeAuthConfig.class);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private static void createDefaultConfig(File configurationFile) {
        try {
            configurationFile.getParentFile().mkdirs();
            InputStream stream = AlternativeAuthentication.class.getClassLoader().getResourceAsStream("alternative-auth.json");

            if (stream != null) {
                String content;

                try (Scanner scanner = new Scanner(stream).useDelimiter("\\A")) {
                    content = scanner.hasNext() ? scanner.next() : "";
                }

                stream.close();

                config = gson.fromJson(content, AlternativeAuthConfig.class);

                try (PrintWriter writer = new PrintWriter(configurationFile)) {
                    writer.println(content);
                }
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}
