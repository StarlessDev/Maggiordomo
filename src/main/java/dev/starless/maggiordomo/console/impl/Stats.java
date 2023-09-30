package dev.starless.maggiordomo.console.impl;

import dev.starless.maggiordomo.Bot;
import dev.starless.maggiordomo.Statistics;
import dev.starless.maggiordomo.console.ConsoleCommand;
import dev.starless.maggiordomo.logging.BotLogger;
import net.dv8tion.jda.api.entities.Guild;

import java.util.Collections;
import java.util.List;

public class Stats implements ConsoleCommand {

    @Override
    public void execute(String[] args) {
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
        Statistics stats = Statistics.getInstance();

        String message = """
                        Maggiordomo's stats:
                        The bot has joined %d guilds.
                        The guilds have in average %.1f members.
                        Smallest guild has %d members.
                        Biggest guild has %d members.
                        
                        Commands executed: %d.
                        Rooms served: %d.
                        """.formatted(count, averageSize, min, max, stats.getExecutedCommands(), stats.getChannelsCreated());
        BotLogger.info(message);
    }

    @Override
    public List<String> aliases() {
        return Collections.singletonList("stats");
    }
}
