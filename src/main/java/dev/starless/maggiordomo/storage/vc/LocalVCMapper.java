package dev.starless.maggiordomo.storage.vc;

import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.user.PlayerRecord;
import dev.starless.maggiordomo.data.user.VC;
import dev.starless.maggiordomo.logging.BotLogger;
import dev.starless.maggiordomo.logging.References;
import dev.starless.maggiordomo.utils.discord.Perms;
import dev.starless.maggiordomo.utils.discord.RestUtils;
import it.ayyjava.storage.MongoStorage;
import it.ayyjava.storage.structures.Query;
import it.ayyjava.storage.structures.QueryBuilder;
import it.ayyjava.storage.structures.mapper.IMapper;
import lombok.AccessLevel;
import lombok.Getter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.managers.channel.concrete.VoiceChannelManager;
import net.dv8tion.jda.api.requests.restaction.AuditableRestAction;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Getter
public class LocalVCMapper implements IMapper<VC> {

    @Getter(AccessLevel.NONE) private final VCGateway gateway;

    private final SortedSet<VC> normalChannels;
    private final SortedSet<VC> pinnedChannels;

    private final Set<String> scheduledForDeletion;
    private final Set<Integer> scheduledForCreation;

    private final ExecutorService createService;

    public LocalVCMapper(MongoStorage storage) {
        gateway = new VCGateway(storage);

        normalChannels = Collections.synchronizedSortedSet(new TreeSet<>());
        pinnedChannels = Collections.synchronizedSortedSet(new TreeSet<>());
        scheduledForDeletion = new LinkedHashSet<>();
        scheduledForCreation = new LinkedHashSet<>();

        createService = Executors.newCachedThreadPool();
    }

    @Override
    public boolean insert(VC value) {
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

        // Ricerca l'oggetto nella cache
        Optional<VC> cachedVC = searchImpl(vc -> vc.equals(value), Optional::empty);
        if(cachedVC.isEmpty()) return; // dovrebbe essere usato insert in questo caso

        // Aggiorna i TreeSet
        removeFromCache(cachedVC.get());
        addToCache(value);

        // Aggiorna il database
        gateway.update(value);
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
        String user = query.get("user");
        if (user == null) return Optional.empty();

        return searchImpl(vc -> vc.getUser().equals(user), () -> gateway.load(query));
    }

    public Optional<VC> searchByID(Query query) {
        String channel = query.get("channel");
        if (channel == null) return Optional.empty();

        return searchImpl(vc -> vc.getChannel().equals(channel), () -> gateway.loadByChannel(query));
    }

    private Optional<VC> searchImpl(Predicate<VC> searchCondition,
                                    Supplier<Optional<VC>> vcSupplier) {

        return searchImpl(searchCondition, vcSupplier, false).or(() -> searchImpl(searchCondition, vcSupplier, true));
    }

    private Optional<VC> searchImpl(Predicate<VC> searchCondition,
                                    Supplier<Optional<VC>> database,
                                    boolean pinned) {

        Supplier<SortedSet<VC>> channelsSupplier = () -> pinned ? pinnedChannels : normalChannels;
        Optional<VC> cache;
        synchronized (channelsSupplier.get()) {
            cache = channelsSupplier.get().stream().filter(searchCondition).findFirst();
            if (cache.isEmpty()) {
                cache = database.get();

                cache.ifPresent(this::addToCache);
            }
        }

        return cache;
    }

    @Override
    public List<VC> bulkSearch(Query query) {
        return gateway.lazyLoad(query);
    }

    // Il codice qua sotto gestisce il processo di creazione delle vc

    public void createVC(VC vc, Role publicRole, Category category) {
        int hashcode = vc.hashCode();
        synchronized (scheduledForCreation) {
            scheduledForCreation.add(hashcode);
        }

        createService.submit(() -> {
            VoiceChannel newChannel = category.createVoiceChannel(vc.getTitle()).complete();
            Member owner = newChannel.getGuild().getMemberById(vc.getUser());
            if (owner == null) { // Non dovrebbe mai accadere
                removeFromCreationSchedule(hashcode);
                return;
            }

            // Permessi per l'owner come prima cosa, per il bot stesso
            // e le preferenze dell'utente, dettate dall'oggetto VC
            VoiceChannelManager manager = newChannel.getManager()
                    .setName(vc.getTitle())
                    .setUserLimit(vc.getSize())
                    .putMemberPermissionOverride(category.getJDA().getSelfUser().getIdLong(),
                            Arrays.asList(Perms.selfPerms),
                            Collections.emptyList())
                    .putMemberPermissionOverride(owner.getIdLong(),
                            Arrays.asList(Perms.ownerPerms),
                            Collections.emptyList());

            // Aggiungi i permessi per @everyone tenendo in conto
            // dello status della stanza
            manager = Perms.setPublicPerms(manager, vc.getStatus(), publicRole, true);

            // Trusta gli utenti
            for (PlayerRecord record : vc.getTrusted()) {
                Member member = category.getGuild().getMemberById(record.user());
                if (member != null) manager = Perms.trust(member, manager);
            }

            // Banna gli utenti
            for (PlayerRecord record : vc.getBanned()) {
                Member member = category.getGuild().getMemberById(record.user());
                if (member != null) manager = Perms.ban(member, manager);
            }

            // Manda l'aggiornamento a discord
            manager.queue(nothing -> {
                // Aggiorna id della stanza e il database
                vc.setChannel(newChannel.getId());

                if (vc.isPinned()) {
                    removeFromCreationSchedule(hashcode);

                    newChannel.getGuild()
                            .moveVoiceMember(owner, newChannel)
                            .queue(RestUtils.emptyConsumer(), RestUtils.throwableConsumer("An error occurred while moving the user! {EXCEPTION}"));
                } else {
                    int movement = pinnedChannels.size();

                    // Movva la stanza sopra alle non pinnate
                    category.modifyVoiceChannelPositions()
                            .selectPosition(newChannel)
                            .moveUp(movement)
                            .queue(nothing2 -> {
                                removeFromCreationSchedule(hashcode);

                                // Movva l'utente nella stanza
                                newChannel.getGuild().moveVoiceMember(owner, newChannel).queue(
                                        RestUtils.emptyConsumer(),
                                        throwable -> BotLogger.warn(owner.getUser().getAsTag() + " left before being moved to his room!"));
                            });
                }

                BotLogger.info("%s just created his voice channel in guild '%s'!",
                        References.user(vc.getUser()),
                        category.getGuild().getName());

                update(vc); // Aggiorna i dati
            }, throwable -> removeFromCreationSchedule(hashcode));
        });
    }

    private synchronized void removeFromCreationSchedule(int hashcode) {
        scheduledForCreation.remove(hashcode);
    }

    public synchronized boolean isBeingCreated(VC vc) {
        return scheduledForCreation.contains(vc.hashCode());
    }

    // Questo codice gestisce il processo di pin/unpin delle vc

    public void togglePinStatus(Guild guild, Settings settings, VC vc) {
        if (scheduledForDeletion.contains(vc.getChannel())) return;

        // Modifica i dati dell'oggetto stanza
        boolean isPinned = pinnedChannels.contains(vc);
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

        operateOnNormal(normal -> {
            if (isPinned) normal.add(vc);
            else normal.remove(vc);
        });

        operateOnPinned(pinned -> {
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
                    int pinnedChannels = this.pinnedChannels.size();

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

    // Qua sotto viene gestita la cancellazione delle vc

    public void scheduleForDeletion(VC vc, AudioChannel channel) {
        scheduleForDeletion(vc, channel, RestUtils.emptyConsumer());
    }

    public boolean isBeingDeleted(AudioChannel channel) {
        return scheduledForDeletion.contains(channel.getId());
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
        if (vc.isPinned()) {
            operateOnPinned(pinned -> pinned.add(vc));
        } else {
            operateOnNormal(normal -> normal.add(vc));
        }
    }

    public void removeFromCache(VC vc) {
        if (vc.isPinned()) {
            operateOnPinned(pinned -> pinned.remove(vc));
        } else {
            operateOnNormal(normal -> normal.remove(vc));
        }
    }

    private void operateOnNormal(Consumer<SortedSet<VC>> action) {
        synchronized (normalChannels) {
            action.accept(normalChannels);
        }
    }

    private void operateOnPinned(Consumer<SortedSet<VC>> action) {
        synchronized (pinnedChannels) {
            action.accept(pinnedChannels);
        }
    }
}