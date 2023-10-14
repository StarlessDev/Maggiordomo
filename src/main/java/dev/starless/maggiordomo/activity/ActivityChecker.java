package dev.starless.maggiordomo.activity;

import dev.starless.maggiordomo.Bot;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.user.VC;
import dev.starless.maggiordomo.storage.vc.LocalVCMapper;
import dev.starless.mongo.api.QueryBuilder;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Guild;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@RequiredArgsConstructor
public class ActivityChecker implements Runnable {

    private final Guild guild;

    @Override
    public void run() {
        String guildID = guild.getId();
        Settings settings = Bot.getInstance().getCore().getSettingsMapper()
                .getSettings()
                .get(guildID);

        long maxInactivity = settings.getMaxInactivity();
        if (maxInactivity == -1) return;

        Instant now = Instant.now();

        LocalVCMapper localMapper = Bot.getInstance().getCore()
                .getChannelMapper()
                .getMapper(guildID);

        localMapper.bulkSearch(QueryBuilder.init()
                        .add("guild", guildID)
                        .create())
                .stream()
                .filter(VC::isPinned)
                .filter(vc -> now.isAfter(vc.getLastJoin().plus(maxInactivity, ChronoUnit.DAYS)))
                .forEach(vc -> Optional.ofNullable(guild.getVoiceChannelById(vc.getChannel()))
                        .ifPresent(voiceChannel ->
                                localMapper.scheduleForDeletion(
                                        vc,
                                        voiceChannel,
                                        success -> {
                                            vc.setPinned(false);
                                            localMapper.update(vc);
                                        }).queue()
                        ));
    }
}
