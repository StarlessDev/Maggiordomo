package dev.starless.maggiordomo.console;

import dev.starless.maggiordomo.console.impl.Guilds;
import dev.starless.maggiordomo.console.impl.ResetGlobalCommand;
import dev.starless.maggiordomo.console.impl.Stats;
import dev.starless.maggiordomo.console.impl.Stop;
import dev.starless.maggiordomo.utils.BotLogger;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public class Console extends Thread {

    private final Scanner scanner;
    private final List<ConsoleCommand> commands;

    public Console() {
        this.scanner = new Scanner(System.in);
        this.commands = List.of(new Guilds(), new Stats(), new Stop(), new ResetGlobalCommand());
    }

    @Override
    public void run() {
        handle();
    }

    private void handle() {
        while (true) {
            String input = scanner.nextLine();
            String[] rawArgs = input.split(" ");
            int argsLength = rawArgs.length;

            String commandName = rawArgs[0].toLowerCase();
            String[] args = argsLength == 1 ? new String[0] : Arrays.copyOfRange(rawArgs, 1, argsLength);

            Optional<ConsoleCommand> command = commands.stream()
                    .filter(cmd -> cmd.aliases().contains(commandName))
                    .findFirst();
            if (command.isPresent()) {
                command.get().execute(args);
            } else {
                BotLogger.info("Comando sconosciuto.");
            }
        }
    }
}
