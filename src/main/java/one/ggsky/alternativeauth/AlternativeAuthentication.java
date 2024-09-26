package one.ggsky.alternativeauth;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import net.fabricmc.api.DedicatedServerModInitializer;

import net.fabricmc.loader.api.FabricLoader;
import one.ggsky.alternativeauth.config.AlternativeAuthConfig;
import one.ggsky.alternativeauth.logger.AlternativeAuthDebugger;
import one.ggsky.alternativeauth.logger.AlternativeAuthLogger;
import one.ggsky.alternativeauth.logger.AlternativeAuthLoggerBase;
import org.apache.logging.log4j.LogManager;

import java.io.*;
import java.nio.file.Files;
import java.util.Scanner;

public class AlternativeAuthentication implements DedicatedServerModInitializer {
    public static final String MOD_ID = "alternative-auth";

	private static AlternativeAuthConfig config;
	private static final Gson gson = new Gson();

	private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger(MOD_ID);
	public static AlternativeAuthLoggerBase Logger;

	@Override
	public void onInitializeServer() {
		File configurationFile = FabricLoader.getInstance().getConfigDir().resolve( "alternative-auth.json" ).toFile();

		try {
			config = gson.fromJson(new JsonReader(new FileReader(configurationFile)), AlternativeAuthConfig.class);
		} catch (FileNotFoundException exception) {
			try {
				Files.createFile(configurationFile.toPath());
				InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("alternative-auth.json");

				if (inputStream != null) {
					config = gson.fromJson(new JsonReader(new BufferedReader(new InputStreamReader(inputStream))), AlternativeAuthConfig.class);
				}

				inputStream = this.getClass().getClassLoader().getResourceAsStream("alternative-auth.json");

				if (inputStream != null) {
					PrintWriter printWriter = new PrintWriter(configurationFile);
					Scanner scanner = new Scanner(inputStream);

					while (scanner.hasNextLine()) {
						printWriter.println(scanner.nextLine());
					}

					scanner.close();
					printWriter.flush();
					printWriter.close();
				}

				inputStream.close();
			} catch (IOException ex) {
				LOGGER.error("An error occurred when creating new config for Alternative Authentication!");
				throw new RuntimeException(ex);
			}
		}

		if (config.isDebuggerEnabled()){
            LOGGER.warn("Debugger for Alternative Authentication enabled");
            Logger = new AlternativeAuthDebugger(LOGGER);
		} else {
			Logger = new AlternativeAuthLogger(LOGGER);
		}

		if (config != null) {
			LOGGER.info("Alternative Authentication is now powering your Minecraft server! \uD83D\uDD10");
		}

	}

	public static AlternativeAuthConfig getConfig() {
		return config;
	}
}