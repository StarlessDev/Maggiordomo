package dev.starless.maggiordomo;

import dev.starless.maggiordomo.logging.BotLogger;
import net.dv8tion.jda.api.entities.Guild;

import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

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
                int count = 0;
                int guildSizes = 0;
                int min = Integer.MAX_VALUE;
                int max = Integer.MIN_VALUE;

                for (Guild guild : Bot.getInstance().getJda().getGuilds()) {
                    int memberCount = guild.getMemberCount();
                    count++;
                    guildSizes += memberCount;

                    if(min >= memberCount) min = memberCount;
                    if(max <= memberCount) max = memberCount;
                }

                double averageSize = (double) guildSizes / (double) count;
                String message = """
                        Maggiordomo's stats:
                        The bot has joined %d guilds.
                        The guilds have in average %.1f members.
                        Smallest guild has %d members.
                        Biggest guild has %d members.
                        """.formatted(count, averageSize, min, max);
                BotLogger.info(message);
            } else if (cmd.startsWith("guilds")) {
                int page = 0;
                String[] args = cmd.split(" ");
                if (args.length >= 2) {
                    try {
                        page = Math.max(0, Integer.parseInt(args[1]) - 1);
                    } catch (NumberFormatException ex) {
                        BotLogger.info("Please provide a valid page number.");
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
                    StringBuilder sb = new StringBuilder(String.format("Joined guilds (page %d)\n", page + 1));
                    guilds.subList(start, end).forEach(str -> sb.append("- \"").append(str).append("\"\n"));
                    BotLogger.info(sb.substring(0, sb.length() - 1));
                } else {
                    BotLogger.info("This page is empty.");
                }
            } else if(cmd.startsWith("resetmaincommand")) {
                // This command is only useful if you are updating from < 2.1.0
                Bot.getInstance().getJda()
                        .updateCommands()
                        .queue(success -> BotLogger.info("Successfully reset main command!"));
            }
        }
    }
}
