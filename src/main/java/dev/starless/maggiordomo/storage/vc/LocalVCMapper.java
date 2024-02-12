package dev.starless.maggiordomo.storage.vc;

import dev.starless.maggiordomo.Statistics;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.user.UserRecord;
import dev.starless.maggiordomo.data.user.VC;
import dev.starless.maggiordomo.BotLogger;
import dev.starless.maggiordomo.utils.discord.Perms;
import dev.starless.maggiordomo.utils.discord.References;
import dev.starless.maggiordomo.utils.discord.RestUtils;
import dev.starless.mongo.MongoStorage;
import dev.starless.mongo.api.Query;
import dev.starless.mongo.api.QueryBuilder;
import dev.starless.mongo.api.mapper.IMapper;
import lombok.Getter;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.managers.channel.concrete.VoiceChannelManager;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.RestAction;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Getter
public class LocalVCMapper implements IMapper<VC> {

    private final VCGateway gateway;
    private final String guildID;

    private final Set<VC> normalChannels;
    private final Set<VC> pinnedChannels;

    private final Set<String> scheduledForDeletion;
    private final Set<Integer> scheduledForCreation;

    private final ExecutorService createService;

    public LocalVCMapper(MongoStorage storage, String guild) {
        gateway = new VCGateway(storage);
        guildID = guild;

        normalChannels = Collections.synchronizedSet(new HashSet<>());
        pinnedChannels = Collections.synchronizedSet(new HashSet<>());

        scheduledForDeletion = new LinkedHashSet<>();
        scheduledForCreation = new LinkedHashSet<>();

        createService = Executors.newCachedThreadPool();
    }

    @Override
    public boolean insert(VC value) {
        return gateway.insert(value);
    }

    @Override
    public void update(VC value) {
        // Update the cache if present
        if (removeFromCache(value)) {
            addToCache(value);
        }

        // Update the value in the database
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

    public void purge() {
        gateway.removeByGuild(QueryBuilder.init()
                .add("guild", guildID)
                .create());
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

        return searchImpl(searchCondition, Optional::empty, false).or(() -> searchImpl(searchCondition, vcSupplier, true));
    }

    private Optional<VC> searchImpl(Predicate<VC> searchCondition,
                                    Supplier<Optional<VC>> database,
                                    boolean pinned) {

        Supplier<Set<VC>> channelsSupplier = () -> pinned ? pinnedChannels : normalChannels;
        Optional<VC> cache;
        synchronized (channelsSupplier.get()) {
            cache = channelsSupplier.get().stream().filter(searchCondition).findFirst();
            if (cache.isEmpty()) {
                cache = database.get();
            }
        }

        return cache;
    }

    @Override
    public List<VC> bulkSearch(Query query) {
        return gateway.lazyLoad(query);
    }

    // Il codice qua sotto gestisce il processo di creazione delle vc

    public void createVC(VC vc, Role publicRole, Set<String> bannedRoles, Category category) {
        int hashcode = vc.hashCode();
        synchronized (scheduledForCreation) {
            scheduledForCreation.add(hashcode);
        }

        try {
            category.createVoiceChannel(vc.getTitle()).queue(newChannel -> {
                Member owner = newChannel.getGuild().getMemberById(vc.getUser());
                if (owner == null) { // Non dovrebbe mai accadere
                    removeFromCreationSchedule(hashcode);
                    return;
                }

                // Permessi per l'owner come prima cosa, per il bot stesso
                // e le preferenze dell'utente, dettate dall'oggetto VC
                VoiceChannelManager manager = newChannel.getManager()
                        .setUserLimit(vc.getSize())
                        .putMemberPermissionOverride(category.getJDA().getSelfUser().getIdLong(),
                                Perms.voiceSelfPerms,
                                Collections.emptyList())
                        .putMemberPermissionOverride(owner.getIdLong(),
                                Perms.voiceOwnerPerms,
                                Collections.emptyList());

                // Aggiungi i permessi per @everyone tenendo in conto
                // dello status della stanza
                manager = Perms.setPublicPerms(manager, vc.getStatus(), publicRole, true);

                // Aggiungi i ruoli bannati
                for (String bannedRole : bannedRoles) {
                    Role role = newChannel.getGuild().getRoleById(bannedRole);
                    if (role == null) continue;

                    manager = Perms.ban(role, manager);
                }

                // Trusta gli utenti
                for (String trustedId : vc.getTrusted()) {
                    Member member = category.getGuild().getMemberById(trustedId);
                    if (member != null) manager = Perms.trust(member, manager);
                }

                // Banna gli utenti
                for (String bannedId : vc.getBanned()) {
                    Member member = category.getGuild().getMemberById(bannedId);
                    if (member != null) manager = Perms.ban(member, manager);
                }

                Consumer<? super Throwable> errorHandler = throwable -> {
                    // Se la stanza non riesce ad essere spostata
                    // allora cancellala e fai riprovare all'utente
                    removeFromCreationSchedule(hashcode);
                    scheduleForDeletion(vc, newChannel).queue();
                };

                // Manda l'aggiornamento a discord
                manager.queue(nothing -> {
                    // Aggiorna id della stanza e il database
                    vc.setChannel(newChannel.getId());
                    vc.setCategory(category.getId());

                    if (vc.isPinned()) {
                        removeFromCreationSchedule(hashcode);

                        if (owner.getVoiceState() != null && owner.getVoiceState().inAudioChannel()) {
                            newChannel.getGuild().moveVoiceMember(owner, newChannel)
                                    .queue(RestUtils.emptyConsumer(), RestUtils.throwableConsumer("An error occurred while moving the user! {EXCEPTION}"));
                        }
                    } else {
                        int movement = pinnedChannels.size();

                        // Movva la stanza sopra alle non pinnate
                        category.modifyVoiceChannelPositions()
                                .selectPosition(newChannel)
                                .moveUp(movement)
                                .queue(nothing2 -> {
                                    removeFromCreationSchedule(hashcode);

                                    // Movva l'utente nella stanza
                                    try {
                                        newChannel.getGuild().moveVoiceMember(owner, newChannel).queue(RestUtils.emptyConsumer(), errorHandler);
                                    } catch (IllegalStateException | InsufficientPermissionException ex) {
                                        errorHandler.accept(ex);
                                    }
                                }, errorHandler);
                    }

                    // Update statistics
                    Statistics.getInstance().incrementChannels();

                    BotLogger.info("%s just created his voice channel in guild '%s'!",
                            References.user(vc.getUser()),
                            category.getGuild().getName());

                    // Add vc to cache and update the object to the database
                    update(vc);
                    addToCache(vc);
                }, throwable -> removeFromCreationSchedule(hashcode));
            });
        } catch (ErrorResponseException ex) {
            removeFromCreationSchedule(hashcode);

            if (ex.getErrorResponse().equals(ErrorResponse.INVALID_FORM_BODY)) {
                // Someone might have a nickname containing bad words
                // which are not allowed in servers in the discovery page.
                boolean hasDisallowedName = ex.getSchemaErrors().stream()
                        .map(schemaError -> schemaError.getErrors()
                                .stream()
                                .map(ErrorResponseException.ErrorCode::getCode)
                                .toList())
                        .anyMatch(errors -> errors.contains("INVALID_COMMUNITY_PROPERTY_NAME"));
                if (hasDisallowedName) { // If that's the case we censor the name
                    vc.setTitle("*".repeat(5));

                    createVC(vc, publicRole, bannedRoles, category); // and create the vc again
                }
            }
        }
    }

    private synchronized void removeFromCreationSchedule(int hashcode) {
        scheduledForCreation.remove(hashcode);
    }

    public synchronized boolean isBeingCreated(VC vc) {
        return scheduledForCreation.contains(vc.hashCode());
    }

    // Questo codice gestisce il processo di pin/unpin delle vc

    public synchronized void togglePinStatus(Guild guild, Settings settings, VC vc) {
        if (scheduledForDeletion.contains(vc.getChannel())) return;

        // Modifica i dati dell'oggetto stanza
        boolean isPinned = pinnedChannels.contains(vc);

        if (isPinned) {
            unpin(guild, settings, vc);

            VoiceChannel voiceChannel = guild.getVoiceChannelById(vc.getChannel());
            if (voiceChannel != null && voiceChannel.getMembers().isEmpty()) {
                scheduleForDeletion(vc, voiceChannel).complete();

                vc.setPinned(false);
                gateway.update(vc);
                return;
            }
        } else {
            pin(guild, settings, vc.getChannel());
        }

        operateOnNormal(normal -> {
            if (isPinned) normal.add(vc);
            else normal.remove(vc);
        });

        operateOnPinned(pinned -> {
            if (isPinned) pinned.remove(vc);
            else pinned.add(vc);
        });

        vc.setPinned(!isPinned);
        gateway.update(vc);
    }

    private void pin(Guild guild, Settings settings, String id) {
        VoiceChannel channel = guild.getVoiceChannelById(id);
        if (channel != null) {
            Category category = channel.getParentCategory();
            if (category != null) {
                List<VoiceChannel> channels = getVoiceChannelsInCategory(category, settings.isMainCategory(category.getId()));

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

    private void unpin(Guild guild, Settings settings, VC vc) {
        VoiceChannel channel = guild.getVoiceChannelById(vc.getChannel());
        if (channel != null) {
            if (channel.getMembers().isEmpty()) {
                scheduleForDeletion(vc, channel).complete(); // Cancella la stanza se è vuota
            } else {
                Category category = channel.getParentCategory();
                if (category != null) {
                    List<VoiceChannel> channels = getVoiceChannelsInCategory(category, settings.isMainCategory(category.getId()));

                    int channelIndex = channels.indexOf(channel);
                    int normalSize;
                    synchronized (normalChannels) {
                        normalSize = normalChannels.size();
                    }

                    int movement = channelIndex - normalSize;
                    if (movement == 0) return;

                    category.modifyVoiceChannelPositions()
                            .selectPosition(channel)
                            .moveUp(movement)
                            .queue(RestUtils.emptyConsumer(), RestUtils.throwableConsumer());
                }
            }
        }
    }

    public List<VC> getCreatedVCs() {
        List<VC> vcs = new ArrayList<>();
        operateOnNormal(vcs::addAll);
        operateOnPinned(vcs::addAll);

        return vcs.stream().filter(VC::hasChannel).toList();
    }

    private List<VoiceChannel> getVoiceChannelsInCategory(Category category, boolean skip) {
        Stream<VoiceChannel> channelStream = category.getVoiceChannels().stream();
        if (skip) {
            channelStream = channelStream.skip(1);
        }
        return channelStream.toList();
    }

    // Qua sotto viene gestita la cancellazione delle vc

    public boolean isBeingDeleted(String channelID) {
        return scheduledForDeletion.contains(channelID);
    }

    public RestAction<Void> scheduleForDeletion(@NotNull VC vc, AudioChannel channel) {
        return scheduleForDeletion(vc, channel, RestUtils.emptyConsumer());
    }

    public RestAction<Void> scheduleForDeletion(@NotNull VC vc, AudioChannel channel, Consumer<Void> success) {
        String id = channel.getId();
        if (scheduledForDeletion.contains(id)) return null;

        scheduledForDeletion.add(id);

        return channel.delete()
                .onErrorMap(throwable -> {
                    scheduledForDeletion.remove(id);
                    return null;
                }).onSuccess(success.andThen(nothing -> {
                    vc.setChannel("-1");
                    gateway.update(vc);

                    removeFromCache(vc);
                    scheduledForDeletion.remove(id);
                }));
    }

    // Utility methods used in all the class

    public void addToCache(VC vc) {
        if (vc.isPinned()) {
            operateOnPinned(pinned -> pinned.add(vc));
        } else {
            operateOnNormal(normal -> normal.add(vc));
        }
    }

    public boolean removeFromCache(VC vc) {
        AtomicBoolean removed = new AtomicBoolean(false);
        if (vc.isPinned()) {
            operateOnPinned(pinned -> removed.set(pinned.remove(vc)));
        } else {
            operateOnNormal(normal -> removed.set(normal.remove(vc)));
        }

        return removed.get();
    }

    private void operateOnNormal(Consumer<Set<VC>> action) {
        synchronized (normalChannels) {
            action.accept(normalChannels);
        }
    }

    private void operateOnPinned(Consumer<Set<VC>> action) {
        synchronized (pinnedChannels) {
            action.accept(pinnedChannels);
        }
    }
}