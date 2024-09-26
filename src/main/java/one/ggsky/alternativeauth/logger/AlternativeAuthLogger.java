package one.ggsky.alternativeauth.logger;

import org.apache.logging.log4j.Logger;

public class AlternativeAuthLogger extends AlternativeAuthLoggerBase {
    private final Logger LOGGER;

    public AlternativeAuthLogger(Logger logger) {
        this.LOGGER = logger;
    }

    @Override
    public void info(String message) {
        LOGGER.info(message);
    }

    @Override
    public void debug(String message) {
        return;
    }
}
