package one.ggsky.alternativeauth.logger;

import org.apache.logging.log4j.Logger;

public class AlternativeAuthLoggerManager {
    private static AlternativeAuthLogger logger;

    public static void configureLogger(Logger initialLogger, boolean debugMode) {
        logger = new AlternativeAuthLogger(initialLogger, debugMode);
    }

    public static AlternativeAuthLogger getLogger() {
        return logger;
    }
}
