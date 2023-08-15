package dev.starless.maggiordomo.activity;

import dev.starless.maggiordomo.Bot;
import dev.starless.maggiordomo.interfaces.Service;

import java.util.Map;
import java.util.concurrent.*;

public class ActivityManager implements Service {

    private final Map<String, Future<?>> activityTasks;
    private final ScheduledExecutorService activityService;

    public ActivityManager(int guildSize) {
        activityTasks = new ConcurrentHashMap<>();
        activityService = Executors.newScheduledThreadPool(guildSize);
    }

    @Override
    public void start() {
        Bot.getInstance().getJda().getGuilds().forEach(guild -> startMonitor(guild.getId()));
    }

    @Override
    public void stop() {
        activityService.shutdown();
    }

    public void startMonitor(String guildID) {
        if(activityTasks.containsKey(guildID)) return;

        activityTasks.put(guildID, activityService.scheduleWithFixedDelay(new ActivityChecker(guildID), 0, 1, TimeUnit.HOURS));
    }

    public void stopMonitor(String guildID) {
        Future<?> task = activityTasks.remove(guildID);
        if (task != null) {
            task.cancel(true);
        }
    }
}
