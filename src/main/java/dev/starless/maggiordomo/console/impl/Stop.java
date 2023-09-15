package dev.starless.maggiordomo.console.impl;

import dev.starless.maggiordomo.console.ConsoleCommand;

import java.util.List;

public class Stop implements ConsoleCommand {

    @Override
    public void execute(String[] args) {
        System.exit(0); // Ferma il programma
    }

    @Override
    public List<String> aliases() {
        return List.of("stop", "end");
    }
}
