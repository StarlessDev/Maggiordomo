package dev.starless.maggiordomo.utils.discord;

import dev.starless.maggiordomo.utils.BotLogger;
import lombok.experimental.UtilityClass;

import java.util.function.Consumer;

@UtilityClass
public class RestUtils {

    public <T> Consumer<T> emptyConsumer() {
        return nothing -> {
        };
    }

    public Consumer<Throwable> throwableConsumer() {
        return throwableConsumer("An error occurred! {EXCEPTION}");
    }

    public Consumer<Throwable> throwableConsumer(String message) {
        return t -> BotLogger.error(message.replace("{EXCEPTION}", t.getMessage()));
    }
}

