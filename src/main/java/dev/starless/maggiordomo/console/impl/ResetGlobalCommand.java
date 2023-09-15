package dev.starless.maggiordomo.console.impl;

import dev.starless.maggiordomo.Bot;
import dev.starless.maggiordomo.console.ConsoleCommand;
import dev.starless.maggiordomo.logging.BotLogger;

import java.util.Collections;
import java.util.List;

public class ResetGlobalCommand implements ConsoleCommand {

    @Override
    public void execute(String[] args) {
        // This command is only useful if you are updating from < 2.1.0
        Bot.getInstance().getJda()
                .updateCommands()
                .queue(success -> BotLogger.info("Successfully reset main command!"));
    }

    @Override
    public List<String> aliases() {
        return Collections.singletonList("resetmaincommand");
    }
}
