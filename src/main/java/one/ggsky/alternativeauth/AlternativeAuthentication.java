package one.ggsky.alternativeauth;

import net.fabricmc.api.DedicatedServerModInitializer;
import one.ggsky.alternativeauth.config.AlternativeAuthConfigManager;
import one.ggsky.alternativeauth.logger.AlternativeAuthLoggerManager;

import org.apache.logging.log4j.LogManager;

public class AlternativeAuthentication implements DedicatedServerModInitializer {
    public static final String MOD_ID = "alternative-auth";

	@Override
	public void onInitializeServer() {
		AlternativeAuthConfigManager.loadConfig();
        AlternativeAuthLoggerManager.configureLogger(LogManager.getLogger(MOD_ID), AlternativeAuthConfigManager.getConfig().isDebuggerEnabled());

		AlternativeAuthLoggerManager.getLogger().info("Alternative Authentication is now powering your Minecraft server! \uD83D\uDD10");
	}
}
