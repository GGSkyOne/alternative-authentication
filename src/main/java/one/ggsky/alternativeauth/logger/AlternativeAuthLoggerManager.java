package one.ggsky.alternativeauth.logger;

import org.apache.logging.log4j.LogManager;

public class AlternativeAuthLoggerManager {
    private static AlternativeAuthLogger logger = new AlternativeAuthLogger(LogManager.getLogger("alternative-auth"), false);

    public static void configureLogger(boolean debugMode) {
        logger = new AlternativeAuthLogger(LogManager.getLogger("alternative-auth"), debugMode);
    }

    public static AlternativeAuthLogger getLogger() {
        return logger;
    }
}
