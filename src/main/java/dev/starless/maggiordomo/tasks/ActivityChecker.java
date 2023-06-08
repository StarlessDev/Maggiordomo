package dev.starless.maggiordomo.tasks;

import dev.starless.maggiordomo.Bot;
import dev.starless.maggiordomo.data.user.VC;
import dev.starless.maggiordomo.logging.BotLogger;
import dev.starless.maggiordomo.storage.vc.LocalVCMapper;
import it.ayyjava.storage.structures.QueryBuilder;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Guild;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
public class ActivityChecker implements Runnable {

    private final String guildID;

    @Override
    public void run() {
        Guild guild = Bot.getInstance().getJda().getGuildById(guildID);
        if (guild == null) {
            BotLogger.info("Cannot find guild " + guildID);
            return;
        }

        Instant now = Instant.now();
        AtomicInteger cleaned = new AtomicInteger(0);

        LocalVCMapper localMapper = Bot.getInstance().getCore()
                .getChannelMapper()
                .getMapper(guildID);

        localMapper.bulkSearch(QueryBuilder.init()
                        .add("guild", guildID)
                        .create())
                .stream()
                .filter(VC::isPinned)
                .filter(vc -> now.isAfter(vc.getLastJoin().plus(7, ChronoUnit.DAYS)))
                .forEach(vc -> Optional.ofNullable(guild.getVoiceChannelById(vc.getChannel()))
                        .ifPresent(voiceChannel ->
                                localMapper.scheduleForDeletion(
                                        vc,
                                        voiceChannel,
                                        success -> {
                                            vc.setPinned(false);
                                            localMapper.update(vc);

                                            cleaned.incrementAndGet();
                                        })));

        if (cleaned.get() > 0) {
            BotLogger.info(String.format("Cleaned %d inactive locked rooms!", cleaned.get()));
        }
    }
}
