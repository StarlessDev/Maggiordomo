package dev.starless.maggiordomo.console.impl;

import dev.starless.maggiordomo.Bot;
import dev.starless.maggiordomo.console.ConsoleCommand;
import dev.starless.maggiordomo.BotLogger;

import java.util.Collections;
import java.util.List;

public class Guilds implements ConsoleCommand {

    @Override
    public void execute(String[] args) {
        int page = 0;
        if (args.length >= 1) {
            try {
                page = Math.max(0, Integer.parseInt(args[0]) - 1);
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
    }

    @Override
    public List<String> aliases() {
        return Collections.singletonList("guilds");
    }
}
