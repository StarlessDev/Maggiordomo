package dev.starless.maggiordomo.console;

import java.util.List;

public interface ConsoleCommand {

    void execute(String[] args);

    List<String> aliases();
}
