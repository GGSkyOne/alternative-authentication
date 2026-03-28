package one.ggsky.alternativeauth.logger;

import org.apache.logging.log4j.Logger;

public class AlternativeAuthLogger {
    private final Logger logger;
    private volatile boolean debugMode;

    public AlternativeAuthLogger(Logger logger) {
        this.logger = logger;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;

        if (debugMode) {
            logger.info("Debug mode enabled for Alternative Authentication");
        }
    }

    public void info(String message) {
        logger.info(message);
    }

    public void warn(String message) {
        logger.warn(message);
    }

    public void debug(String message) {
        if (debugMode) {
            logger.info(message);
        }
    }
}
