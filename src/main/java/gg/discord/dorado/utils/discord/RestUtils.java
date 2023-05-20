package gg.discord.dorado.utils.discord;

import gg.discord.dorado.logging.BotLogger;
import lombok.experimental.UtilityClass;

import java.util.function.Consumer;

@UtilityClass
public class RestUtils {

    public <T> Consumer<T> emptyConsumer() {
        return nothing -> {
        };
    }

    public Consumer<Throwable> throwableConsumer() {
        return throwableConsumer("Si è verificato un errore! {EXCEPTION}");
    }

    public Consumer<Throwable> throwableConsumer(String message) {
        return t -> BotLogger.error(message.replace("{EXCEPTION}", t.getMessage()));
    }
}

