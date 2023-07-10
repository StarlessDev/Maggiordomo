package dev.starless.maggiordomo;

import dev.starless.maggiordomo.logging.BotLogger;

import java.util.List;
import java.util.Scanner;

public class Console extends Thread {

    private final Scanner scanner;

    public Console() {
        this.scanner = new Scanner(System.in);
    }

    @Override
    public void run() {
        handle();
    }

    private void handle() {
        while (true) {
            String cmd = scanner.nextLine().toLowerCase();
            if (cmd.equals("stop") || cmd.equals("end")) {
                scanner.close(); // Chiudi lo scanner
                System.exit(0); // Ferma il programma
                return;
            } else if (cmd.equals("stats")) {
                int numberOfGuilds = Bot.getInstance().getJda().getGuilds().size();
                BotLogger.info("The bot has joined %d guilds", numberOfGuilds);
            } else if (cmd.startsWith("guilds")) {
                int page = 0;
                String[] args = cmd.split(" ");
                if (args.length >= 2) {
                    try {
                        page = Integer.parseInt(args[1]);
                    } catch (NumberFormatException ex) {
                        BotLogger.info("");
                        return;
                    }
                }

                List<String> guilds = Bot.getInstance().getJda().getGuilds()
                        .stream()
                        .map(guild -> String.format("%s with %d members", guild.getName(), guild.getMemberCount()))
                        .toList();

                int pageSize = 10;
                int start = pageSize * page; // inclusive
                int end = start + pageSize; // exclusive
                if(end >= guilds.size()) {
                    end = guilds.size();
                }

                if(start < guilds.size()) {
                    StringBuilder sb = new StringBuilder();
                    guilds.subList(start, end).forEach(str -> sb.append("- \"").append(str).append("\"\n"));
                    BotLogger.info(sb.substring(0, sb.length() - 1));
                } else {
                    BotLogger.info("Questa pagina è vuota.");
                }
            }
        }
    }
}
