package one.ggsky.alternativeauth;

import net.fabricmc.api.DedicatedServerModInitializer;
import one.ggsky.alternativeauth.config.AlternativeAuthConfigManager;
import one.ggsky.alternativeauth.logger.AlternativeAuthLoggerManager;

public class AlternativeAuthentication implements DedicatedServerModInitializer {
	@Override
	public void onInitializeServer() {
		AlternativeAuthConfigManager.loadConfig();
        AlternativeAuthLoggerManager.configureLogger(AlternativeAuthConfigManager.getConfig().isDebugModeEnabled());

		AlternativeAuthLoggerManager.getLogger().info("Alternative Authentication is now powering your Minecraft server! \uD83D\uDD10");
	}
}