package gg.discord.dorado.storage;

import gg.discord.dorado.data.Settings;
import gg.discord.dorado.data.user.VC;
import gg.discord.dorado.utils.discord.RestUtils;
import it.ayyjava.storage.MongoStorage;
import it.ayyjava.storage.structures.Query;
import it.ayyjava.storage.structures.QueryBuilder;
import it.ayyjava.storage.structures.mapper.IMapper;
import lombok.AccessLevel;
import lombok.Getter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Getter
public class VCMapper implements IMapper<VC> {

    @Getter(AccessLevel.NONE)
    private final VCGateway gateway;

    private final SortedSet<VC> normalChannels;
    private final SortedSet<VC> pinnedChannels;
    private final Set<String> scheduledForDeletion;

    public VCMapper(MongoStorage storage) {
        gateway = new VCGateway(storage);

        normalChannels = Collections.synchronizedSortedSet(new TreeSet<>());
        pinnedChannels = Collections.synchronizedSortedSet(new TreeSet<>());
        scheduledForDeletion = new LinkedHashSet<>();
    }

    @Override
    public boolean insert(VC value) {
        // Guarda se nella cache c'è già un valore
        Query query = QueryBuilder.init()
                .add("guild", value.getGuild())
                .add("user", value.getUser())
                .create();

        Optional<VC> cache = search(query);
        if (cache.isPresent()) return false; // Si deve usare update() per aggiornare un valore

        value.updateLastModification();
        boolean result = gateway.insert(value);
        if (result) {
            addToCache(value);
        }

        return result;
    }

    @Override
    public void update(VC value) {
        value.updateLastModification();
        gateway.update(value);

        if (value.isPinned()) {
            runPinnedCacheOperation(pinned -> {
                pinned.remove(value);
                pinned.add(value);
            });
        } else {
            runNormalCacheOperation(normal -> {
                normal.remove(value);
                normal.add(value);
            });
        }
    }

    @Override
    public int delete(VC value) {
        int removed = gateway.remove(value);
        if (removed != 0) {
            removeFromCache(value);
        }

        return removed;
    }

    @Override
    public Optional<VC> search(Query query) {
        String guild = query.get("guild");
        String user = query.get("user");
        if (guild == null || user == null) return Optional.empty();

        List<VC> vcs = getFullList(guild);
        Optional<VC> cache = vcs.stream().filter(vc -> vc.getUser().equals(user)).findFirst();
        if (cache.isEmpty()) {
            cache = gateway.load(query);

            cache.ifPresent(this::addToCache);
        }

        return cache;
    }

    public Optional<VC> searchByID(Query query) {
        String guild = query.get("guild");
        String channel = query.get("channel");
        if (guild == null || channel == null) return Optional.empty();

        List<VC> vcs = getFullList(guild);
        Optional<VC> cache = vcs.stream().filter(vc -> vc.getChannel().equals(channel)).findFirst();
        if (cache.isEmpty()) {
            cache = gateway.loadByChannel(query);

            cache.ifPresent(this::addToCache);
        }

        return cache;
    }

    @Override
    public List<VC> bulkSearch(Query query) {
        return gateway.lazyLoad(query);
    }

    public void togglePinStatus(Guild guild, Settings settings, VC vc) {
        if (scheduledForDeletion.contains(vc.getChannel())) return;

        // Modifica i dati dell'oggetto stanza
        boolean isPinned = getPartialList(guild.getId(), true).contains(vc);
        boolean reverse = !isPinned;
        vc.setPinned(reverse);

        if (isPinned) {
            unpin(guild, vc, settings);

            VoiceChannel voiceChannel = guild.getVoiceChannelById(vc.getChannel());
            if (voiceChannel != null && voiceChannel.getMembers().size() == 0) {
                scheduleForDeletion(vc, voiceChannel);

                gateway.update(vc);
                return;
            }
        } else {
            pin(guild, vc.getChannel(), settings);
        }

        runNormalCacheOperation(normal -> {
            if(isPinned) normal.add(vc);
            else normal.remove(vc);
        });

        runPinnedCacheOperation(pinned -> {
            if (isPinned) pinned.remove(vc);
            else pinned.add(vc);
        });

        gateway.update(vc);
    }

    private void pin(Guild guild, String id, Settings settings) {
        VoiceChannel channel = guild.getVoiceChannelById(id);
        if (channel != null) {
            Category category = guild.getCategoryById(settings.getCategoryID());
            if (category != null) {
                List<VoiceChannel> channels = category.getVoiceChannels()
                        .stream()
                        .skip(1)
                        .toList();

                int channelIndex = channels.indexOf(channel);
                int totalChannels = channels.size() - 1;

                int movement = totalChannels - channelIndex;
                if (movement == 0) return;

                category.modifyVoiceChannelPositions()
                        .selectPosition(channel)
                        .moveDown(movement)
                        .queue(RestUtils.emptyConsumer(), RestUtils.throwableConsumer());
            }
        }
    }

    private void unpin(Guild guild, VC vc, Settings settings) {
        VoiceChannel channel = guild.getVoiceChannelById(vc.getChannel());
        if (channel != null) {
            if (channel.getMembers().size() == 0) {
                scheduleForDeletion(vc, channel); // Cancella la stanza se è vuota
            } else {
                Category category = guild.getCategoryById(settings.getCategoryID());
                if (category != null) {
                    List<VoiceChannel> channels = category.getVoiceChannels()
                            .stream()
                            .skip(1)
                            .toList();

                    int channelIndex = channels.indexOf(channel); // Qua non serve il +1, perchè devo mettere la stanza sotto ad un'altra
                    int totalChannels = channels.size() - 1;
                    int pinnedChannels = getPartialList(guild.getId(), true).size();

                    int movement = channelIndex - (totalChannels - pinnedChannels);
                    if (movement == 0) return;

                    category.modifyVoiceChannelPositions()
                            .selectPosition(channel)
                            .moveUp(movement)
                            .queue(RestUtils.emptyConsumer(), RestUtils.throwableConsumer());
                }
            }
        }
    }

    public boolean isBeingDeleted(AudioChannel channel) {
        return scheduledForDeletion.contains(channel.getId());
    }

    public void scheduleForDeletion(VC vc, AudioChannel channel) {
        scheduleForDeletion(vc, channel, RestUtils.emptyConsumer());
    }

    public void scheduleForDeletion(VC vc, AudioChannel channel, Consumer<Void> success) {
        String id = channel.getId();
        if (scheduledForDeletion.contains(id)) return;

        scheduledForDeletion.add(id);

        if (vc != null) removeFromCache(vc);

        queueDeletion(channel.delete(), success, Duration.ofSeconds(-1L), id);
    }

    private void queueDeletion(AuditableRestAction<Void> action,
                               Consumer<Void> consumer,
                               Duration duration,
                               String id) {
        action.queueAfter(duration.toSeconds(),
                TimeUnit.SECONDS,
                consumer.andThen(nothing -> scheduledForDeletion.remove(id)),
                throwable -> queueDeletion(action, consumer, duration.abs().plusSeconds(30), id));
    }

    // Utility methods used in all the class

    public void addToCache(VC vc) {
        if(vc.isPinned()) {
            runPinnedCacheOperation(pinned -> pinned.add(vc));
        } else {
            runNormalCacheOperation(normal -> normal.add(vc));
        }
    }

    public void removeFromCache(VC vc) {
        if(vc.isPinned()) {
            runPinnedCacheOperation(pinned -> pinned.remove(vc));
        } else {
            runNormalCacheOperation(normal -> normal.remove(vc));
        }
    }

    // These getter methods below are for read-only

    public Set<VC> getPartialList(String guild, boolean pinned) {
        Set<VC> copy;
        synchronized (pinned ? this.pinnedChannels : this.normalChannels) {
            copy = new LinkedHashSet<>(pinned ? this.pinnedChannels : this.normalChannels);
        }

        return copy.stream().filter(vc -> vc.getGuild().equals(guild)).collect(Collectors.toUnmodifiableSet());
    }

    private List<VC> getFullList(String guild) {
        List<VC> vcs = new ArrayList<>();
        vcs.addAll(getPartialList(guild, false)); // Stanze temporanee
        vcs.addAll(getPartialList(guild, true)); // Stanze pinnate

        return vcs;
    }

    // Utility methods to synchronize read/write operations

    private void runNormalCacheOperation(Consumer<Set<VC>> action) {
        synchronized (normalChannels) {
            action.accept(normalChannels);
        }
    }

    private void runPinnedCacheOperation(Consumer<Set<VC>> action) {
        synchronized (pinnedChannels) {
            action.accept(pinnedChannels);
        }
    }
}