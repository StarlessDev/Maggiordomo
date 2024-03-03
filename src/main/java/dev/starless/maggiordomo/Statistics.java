package dev.starless.maggiordomo;

import dev.starless.maggiordomo.utils.BotLogger;
import dev.starless.maggiordomo.utils.ConfigUtils;
import lombok.Getter;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class Statistics {

    @Getter private static final Statistics instance = new Statistics();

    private final AtomicInteger joinedGuilds;
    private final AtomicInteger createdChannels;
    private final AtomicInteger executedCommands;

    public Statistics() {
        this.joinedGuilds = new AtomicInteger(0);
        this.createdChannels = new AtomicInteger(0);
        this.executedCommands = new AtomicInteger(0);
    }

    public void load() {
        if (Bot.getInstance().isReady()) {
            joinedGuilds.set(Bot.getInstance().getJda().getGuilds().size());
        }

        consumeFile(root -> {
            createdChannels.set(root.node("channelCreated").getInt(0));
            executedCommands.set(root.node("commandsRan").getInt(0));
        }, false);
    }

    public void save() {
        consumeFile(root -> {
            try {
                root.node("channelCreated").set(createdChannels.get());
                root.node("commandsRan").set(executedCommands.get());
            } catch (SerializationException e) {
                BotLogger.error("Could not set the new values in the stats file! (%s)", e.getMessage());
            }
        }, true);
    }

    public void incrementCommands() {
        executedCommands.incrementAndGet();
    }

    public void incrementChannels() {
        createdChannels.incrementAndGet();
    }

    public void updateGuild(boolean increment) {
        if(increment) joinedGuilds.incrementAndGet();
        else joinedGuilds.incrementAndGet();
    }

    public int getChannelsCreated() {
        return createdChannels.get();
    }

    public int getExecutedCommands() {
        return executedCommands.get();
    }

    private void consumeFile(Consumer<CommentedConfigurationNode> action, boolean save) {
        File configDirectory = new File("configuration");
        if (!configDirectory.isDirectory() && !configDirectory.mkdirs()) {
            BotLogger.error("Cannot create the configuration directory!");
            return;
        }

        File statsFile = new File(configDirectory, "statistics.yml");
        YamlConfigurationLoader loader = ConfigUtils.loaderFromFile(statsFile);
        try {
            CommentedConfigurationNode root = loader.load();
            action.accept(root);

            if (save) loader.save(root);
        } catch (ConfigurateException e) {
            BotLogger.error("Could not load/save file! (%s)", e.getMessage());
        }
    }
}
