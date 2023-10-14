package dev.starless.maggiordomo.activity;

import dev.starless.maggiordomo.interfaces.Service;
import dev.starless.maggiordomo.logging.BotLogger;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ActivityManager implements Service {

    private final JDA jda;
    private final ScheduledExecutorService activityService;

    public ActivityManager(JDA jda) {
        this.jda = jda;
        this.activityService = Executors.newScheduledThreadPool(1);
    }

    @Override
    public void start() {
        activityService.scheduleAtFixedRate(() -> jda.getGuilds().forEach(guild -> new ActivityChecker(guild).run()), 0, 1, TimeUnit.HOURS);
        BotLogger.info("Started activity monitoring service");
    }

    @Override
    public void stop() {
        BotLogger.info("Shutting down activity monitoring service");
        activityService.shutdown();
    }

    public void forceCheck(Guild guild) {
        activityService.execute(new ActivityChecker(guild));
    }
}
