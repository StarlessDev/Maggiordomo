package dev.starless.maggiordomo;

import dev.starless.maggiordomo.logging.BotLogger;
import lombok.Getter;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;

public class Main {

    @Getter private static final String version = "2.0.0";

    public static void main(String[] args) {
        try {
            Bot.getInstance().start();
        } catch (InvalidTokenException | IllegalArgumentException ex) {
            BotLogger.error("An error occurred while starting the bot: " + ex.getMessage());
            System.exit(0);
        }
    }
}