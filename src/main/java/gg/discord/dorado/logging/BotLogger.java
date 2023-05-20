package gg.discord.dorado.logging;

import ch.qos.logback.classic.Logger;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import org.slf4j.LoggerFactory;

@UtilityClass
public class BotLogger {

    @Getter private final Logger logger = (Logger) LoggerFactory.getLogger("Maggiordomo");

    public void setup() {
        System.setProperty("LOG4J_FORMAT_MSG_NO_LOOKUPS", "true"); // Log4J CVEs fix
        logger.addAppender(new FileAppender());
    }

    public void info(String message, Object... objects) {
        logger.info(String.format(message, objects));
    }

    public void warn(String message, Object... objects) {
        logger.warn(String.format(message, objects));
    }

    public void error(String message, Object... objects) {
        logger.error(String.format(message, objects));
    }
}
