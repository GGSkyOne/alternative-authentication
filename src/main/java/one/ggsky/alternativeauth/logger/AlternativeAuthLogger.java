package one.ggsky.alternativeauth.logger;

import org.apache.logging.log4j.Logger;

public class AlternativeAuthLogger {
    private final Logger logger;
    private final boolean debugMode;

    public AlternativeAuthLogger(Logger logger, boolean debugMode) {
        this.logger = logger;
        this.debugMode = debugMode;

        if (debugMode) {
            logger.warn("Debugger for Alternative Authentication enabled");
        }
    }

    public void info(String message) {
        logger.info(message);
    }

    public void debug(String message) {
        if (debugMode) {
            logger.info("[AA-Debug] " + message);
        }
    }
}