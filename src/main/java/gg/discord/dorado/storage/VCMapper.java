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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Getter
public class VCMapper implements IMapper<VC> {

    @Getter(AccessLevel.NONE) private final VCGateway gateway;

    private final Map<String, List<VC>> pinnedChannels;
    private final Map<String, List<VC>> normalChannels;
    private final Set<String> scheduledForDeletion;

    public VCMapper(MongoStorage storage) {
        gateway = new VCGateway(storage);

        pinnedChannels = new ConcurrentHashMap<>();
        normalChannels = new ConcurrentHashMap<>();
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

        boolean result = gateway.insert(value);
        if (result) {
            addToCache(value);
        }

        return result;
    }

    @Override
    public void update(VC value) {
        gateway.update(value);

        runOperationOnCache((normal, pinned) -> {
            if (value.isPinned()) {
                pinned.remove(value);
                pinned.add(value);
            } else {
                normal.remove(value);
                normal.add(value);
            }
        }, value.getGuild());
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
        if(scheduledForDeletion.contains(vc.getChannel())) return;

        // Aggiorna la stanza in sè
        boolean isPinned = pinnedChannels.getOrDefault(guild.getId(), new ArrayList<>())
                .stream()
                .anyMatch(pinnedVC -> pinnedVC.getChannel().equals(vc.getChannel()));

        // Modifica i dati dell'oggetto stanza
        boolean reverse = !vc.isPinned();
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

        runOperationOnCache((normal, pinned) -> {
            if (isPinned) {
                pinned.remove(vc);
                normal.add(vc);
            } else {
                normal.remove(vc);
                pinned.add(vc);
            }
        }, vc.getGuild());

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

        if(vc != null) {
            runOperationOnCache((normal, pinned) -> {
                if (vc.isPinned()) {
                    pinned.remove(vc);
                } else {
                    normal.remove(vc);
                }
            }, vc.getGuild());
        }

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
        runOperationOnCache((normal, pinned) -> {
            if (vc.isPinned()) {
                pinned.add(vc);
            } else {
                normal.add(vc);
            }
        }, vc.getGuild());
    }

    public void removeFromCache(VC vc) {
        runOperationOnCache((normal, pinned) -> {
            if (vc.isPinned()) {
                pinned.remove(vc);
            } else {
                normal.remove(vc);
            }
        }, vc.getGuild());
    }

    public List<VC> getPartialList(String guild, boolean pinned) {
        return (pinned ? pinnedChannels : normalChannels).getOrDefault(guild, new CopyOnWriteArrayList<>());
    }

    private List<VC> getFullList(String guild) {
        List<VC> vcs = new ArrayList<>();
        vcs.addAll(getPartialList(guild, false)); // Stanze temporanee
        vcs.addAll(getPartialList(guild, true)); // Stanze pinnate

        return vcs;
    }

    private void runOperationOnCache(BiConsumer<List<VC>, List<VC>> action, String guild) {
        List<VC> normal = getPartialList(guild, false);
        List<VC> pinned = getPartialList(guild, true);

        action.andThen((normalVCs, pinnedVCs) -> {
            normalChannels.put(guild, normalVCs);
            pinnedChannels.put(guild, pinnedVCs);
        }).accept(normal, pinned);
    }

    private int getVCNumberExcludingSelf(List<VC> in, String id) {
        return Math.toIntExact(in.stream()
                .filter(vc -> vc.getChannel().equals(id))
                .count());
    }
}