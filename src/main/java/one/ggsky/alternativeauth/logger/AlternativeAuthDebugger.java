package one.ggsky.alternativeauth.logger;

import org.apache.logging.log4j.Logger;

public class AlternativeAuthDebugger extends AlternativeAuthLoggerBase {
    private final Logger LOGGER;

    public AlternativeAuthDebugger(Logger logger) {
        this.LOGGER = logger;
    }

    @Override
    public void info(String message) {
        LOGGER.info(message);
    }

    @Override
    public void debug(String message) {
        LOGGER.info("[AA-Debug] " + message);
    }
}
