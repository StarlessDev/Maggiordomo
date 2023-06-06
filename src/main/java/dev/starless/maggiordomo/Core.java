package dev.starless.maggiordomo;

import dev.starless.maggiordomo.commands.interaction.*;
import dev.starless.maggiordomo.commands.manager.CommandManager;
import dev.starless.maggiordomo.commands.slash.*;
import dev.starless.maggiordomo.commands.types.Interaction;
import dev.starless.maggiordomo.config.Config;
import dev.starless.maggiordomo.config.ConfigEntry;
import dev.starless.maggiordomo.data.Cooldown;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.enums.RecordType;
import dev.starless.maggiordomo.data.user.PlayerRecord;
import dev.starless.maggiordomo.data.user.VC;
import dev.starless.maggiordomo.interfaces.Module;
import dev.starless.maggiordomo.logging.BotLogger;
import dev.starless.maggiordomo.logging.References;
import dev.starless.maggiordomo.storage.VCManager;
import dev.starless.maggiordomo.storage.settings.SettingsMapper;
import dev.starless.maggiordomo.storage.vc.LocalVCMapper;
import dev.starless.maggiordomo.utils.discord.Embeds;
import dev.starless.maggiordomo.utils.discord.Perms;
import dev.starless.maggiordomo.utils.discord.RestUtils;
import it.ayyjava.storage.MongoStorage;
import it.ayyjava.storage.structures.Query;
import it.ayyjava.storage.structures.QueryBuilder;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.managers.channel.concrete.VoiceChannelManager;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Core implements Module {

    private final MongoStorage storage;
    private final ScheduledExecutorService activityService;

    @Getter private VCManager channelMapper;
    @Getter private SettingsMapper settingsMapper;

    private CommandManager commands;

    public Core(Config config) {
        storage = new MongoStorage(BotLogger.getLogger(), config.getString(ConfigEntry.MONGO));
        activityService = Executors.newScheduledThreadPool(3);
    }

    @Override
    public void onEnable(JDA jda) {
        storage.init();

        channelMapper = new VCManager(storage);
        settingsMapper = new SettingsMapper(storage);

        jda.getGuilds().forEach(guild -> {
            final String id = guild.getId();
            Optional<Settings> cachedSettings = settingsMapper.search(QueryBuilder.init()
                    .add("guild", id)
                    .create());

            LocalVCMapper localMapper = channelMapper.getMapper(guild);
            Settings settings;
            if (cachedSettings.isPresent()) {
                settings = cachedSettings.get();
                settingsMapper.getSettings().put(id, settings);
            } else {
                settings = new Settings(guild);
                settingsMapper.insert(settings);
            }

            if (settings.hasCategory()) {
                Category category = guild.getCategoryById(settings.getCategoryID());
                if (category != null) {
                    category.getVoiceChannels().forEach(voiceChannel -> {
                        if (voiceChannel.getId().equals(settings.getVoiceID())) return;

                        Query query = QueryBuilder.init()
                                .add("guild", id)
                                .add("channel", voiceChannel.getId())
                                .create();

                        Optional<VC> optionalVC = localMapper.searchByID(query);
                        if (optionalVC.isPresent()) {
                            VC vc = optionalVC.get();
                            vc.setTitle(voiceChannel.getName());
                            vc.setSize(voiceChannel.getUserLimit());

                            // Se non è lockata è non ci sono persone dentro,
                            // elimina la stanza
                            if (!vc.isPinned() && voiceChannel.getMembers().size() == 0) {
                                localMapper.scheduleForDeletion(vc, voiceChannel);
                            }

                            localMapper.update(vc);
                        } else {
                            localMapper.scheduleForDeletion(null, voiceChannel);
                            BotLogger.info("Found an invalid vc %s in '%s'", voiceChannel.getName(), guild.getName());
                        }
                    });
                } else {
                    BotLogger.info("Found an invalid category in " + guild.getName());

                    settings.reset();
                    settingsMapper.update(settings);
                }
            }

            // Attiva il servizio di controllo di attività
            activityService.scheduleWithFixedDelay(() -> {
                Guild gld = jda.getGuildById(id);
                if (gld == null) {
                    BotLogger.info("Cannot find guild " + id);
                    return;
                }

                Instant now = Instant.now();
                AtomicInteger cleaned = new AtomicInteger(0);
                localMapper.bulkSearch(QueryBuilder.init()
                                .add("guild", id)
                                .create())
                        .stream()
                        .filter(VC::isPinned)
                        .filter(vc -> now.isAfter(vc.getLastJoin().plus(5, ChronoUnit.DAYS)))
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
            }, 0, 1, TimeUnit.HOURS);
        });

        commands = new CommandManager()
                .name("maggiordomo")
                .command(new BannedCommand())
                .command(new MenuCommand())
                .command(new PremiumCommand())
                .command(new RecoverCommand())
                .command(new ReloadPermsCommand())
                .command(new SetupCommand())
                .interaction(new BanInteraction())
                .interaction(new UnbanInteraction())
                .interaction(new TrustInteraction())
                .interaction(new UntrustInteraction())
                .interaction(new PinInteraction())
                .interaction(new TitleInteraction())
                .interaction(new SizeInteraction())
                .interaction(new StatusInteraction())
                .interaction(new KickInteraction())
                .interaction(new ListInteraction())
                .interaction(new ResetDataInteraction())
                .interaction(new DeleteInteraction());

        commands.createMainCommand(jda);

        jda.addEventListener(this);
    }

    @Override
    public void onDisable(JDA jda) {
        jda.removeEventListener(this);

        int interruputed = 0;
        for (Guild guild : jda.getGuilds()) {
            LocalVCMapper localMapper = channelMapper.getMapper(guild);
            ExecutorService createService = localMapper.getCreateService();

            createService.shutdown();
            try {
                boolean success = createService.awaitTermination(15, TimeUnit.SECONDS);
                if (!success) {
                    interruputed++;
                    createService.shutdownNow();
                }

            } catch (InterruptedException e) {
                BotLogger.error("CreateService shutdown sequence was interrupted while waiting!");
            }
        }

        String debug = interruputed == 0
                ? "CreateService terminated successfully."
                : interruputed + " CreateService(s) got interrupted forcefully!";
        BotLogger.info(debug);

        storage.close();
    }

    @SubscribeEvent
    public void onGuildJoin(@NotNull GuildJoinEvent e) {
        Optional<Settings> settings = settingsMapper.search(QueryBuilder.init()
                .add("guild", e.getGuild().getId())
                .create());

        if (settings.isEmpty()) {
            settingsMapper.insert(new Settings(e.getGuild()));
        }
    }

    @SubscribeEvent
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        // Se l'utente lascia la gilda, cancelliamo la vc
        Query query = QueryBuilder.init()
                .add("guild", event.getGuild().getId())
                .add("user", event.getUser().getId())
                .create();

        LocalVCMapper localMapper = channelMapper.getMapper(event.getGuild());
        localMapper.search(query).ifPresent(vc -> {
            VoiceChannel channel = event.getGuild().getVoiceChannelById(vc.getChannel());
            if (channel != null) {
                localMapper.scheduleForDeletion(vc, channel);
            }
        });
    }

    @SubscribeEvent
    public void onGuildVoiceUpdate(@NotNull GuildVoiceUpdateEvent e) {
        // Quando un utente si sposta tra una vc e l'altra
        // questo evento viene lanciato invece che gli altri due
        handleQuit(e.getGuild(), e.getChannelLeft());
        handleJoin(e.getGuild(), e.getChannelJoined(), e.getMember());
    }

    @SubscribeEvent
    public void onRoleRemoved(@NotNull GuildMemberRoleRemoveEvent event) {
        QueryBuilder builder = QueryBuilder.init().add("guild", event.getGuild().getId());

        settingsMapper.search(builder.create()).ifPresent(settings -> {
            boolean isNotPremium = event.getMember()
                    .getRoles()
                    .stream()
                    .noneMatch(role -> settings.getPremiumRoles().contains(role.getId()));

            if (isNotPremium) {
                LocalVCMapper localMapper = channelMapper.getMapper(event.getGuild());
                localMapper.search(builder.add("user", event.getMember().getId()).create()).ifPresent(vc -> {
                    if (vc.isPinned()) localMapper.togglePinStatus(event.getGuild(), settings, vc);
                });
            }
        });
    }

    @SubscribeEvent
    public void onMessage(@NotNull MessageReceivedEvent event) {
        if (!event.isFromGuild()) return;

        if (event.getGuildChannel() instanceof VoiceChannel voiceChannel) {
            Query query = QueryBuilder.init()
                    .add("guild", event.getGuild().getId())
                    .add("channel", event.getChannel().getId())
                    .create();

            channelMapper.getMapper(event.getGuild()).searchByID(query).ifPresent(vc -> {
                List<String> joinedMembers = voiceChannel.getMembers()
                        .stream()
                        .map(Member::getId)
                        .toList();

                List<String> trusted = vc.getTotalRecords()
                        .stream()
                        .filter(record -> record.type().equals(RecordType.TRUST))
                        .map(PlayerRecord::user)
                        .toList();

                String memberID = event.getMember().getId();
                boolean notJoined = !joinedMembers.contains(memberID);
                boolean notTrusted = !trusted.contains(memberID);
                boolean notOwner = !vc.getUser().equals(memberID);

                if (notJoined && notTrusted && notOwner) {
                    event.getMessage().delete().queue();
                }
            });
        }
    }

    public void sendMenu(TextChannel channel) {
        // Crea la lista di bottoni
        List<ActionRow> buttonRows = new ArrayList<>();
        Stack<Button> row = new Stack<>();
        int commandNumber = commands.getMenuCommands().size();
        for (int i = 0; i < commandNumber; i++) {
            Interaction cmd = commands.getMenuCommands().get(i);
            row.add(Button.of(ButtonStyle.SECONDARY, cmd.getName(), cmd.emoji()));

            if (row.size() == 4 || i == commandNumber - 1) {
                buttonRows.add(ActionRow.of(row));
                row.clear();
            }
        }

        // Crea il messaggio di aiuto
        String content = """
                # Comandi disponibili :books:
                
                Puoi usare questo pannello per **personalizzare** la tua stanza privata.
                Ad ogni bottone è associata una __emoji__: qua sotto puoi leggere la spiegazione dei vari comandi e poi cliccare sul pulsante corrispondente per eseguirlo.
                """;

        // Crea il messaggio d'aito
        MessageCreateBuilder builder = new MessageCreateBuilder()
                .setContent(content)
                .addFiles(FileUpload.fromData(getClass().getResourceAsStream("/guide.png"), "guide.png"));

        // Setta i bottoni
        channel.sendMessage(builder.build())
                .addComponents(buttonRows)
                .queue();
    }

    private void handleJoin(Guild guild, AudioChannel channel, Member member) {
        LocalVCMapper localMapper = channelMapper.getMapper(guild);
        if (channel == null || localMapper.isBeingDeleted(channel)) return;

        String id = guild.getId();
        QueryBuilder builder = QueryBuilder.init().add("guild", id);
        // Controlla se la gilda ha una categoria setuppata
        settingsMapper.search(builder.create()).ifPresent(settings -> {
            // Prendi gli oggetti utili
            Category category = guild.getCategoryById(settings.getCategoryID());
            Role publicRole = guild.getRoleById(settings.getPublicRole());
            if (publicRole == null || category == null) return;

            // Cerca una vc nel database che corrisponda all'utente
            Optional<VC> cachedMemberVC = localMapper.search(builder.add("user", member.getId()).create());

            // Se il canale è il canale di creazione della vc
            if (channel.getId().equals(settings.getVoiceID())) {
                // Se esistono dei dati salvati...
                if (cachedMemberVC.isPresent()) {
                    VC vc = cachedMemberVC.get(); // ...ottienili
                    VoiceChannel voiceChannel = guild.getVoiceChannelById(vc.getChannel());
                    if (voiceChannel != null) { // Se la sua vc è già stata creata
                        guild.moveVoiceMember(member, voiceChannel).complete(); // Sposta l'utente
                    } else {
                        localMapper.createVC(vc, publicRole, category); // Altrimenti creala
                        return;
                    }
                } else {
                    // ...oppure crea un nuovo oggetto VC e la stanza collegata
                    VC vc = new VC(member);
                    localMapper.insert(vc);
                    localMapper.createVC(vc, publicRole, category);
                    return;
                }
            }

            Optional<VC> cachedChannelVC = localMapper.searchByID(builder.add("channel", channel.getId()).create());
            cachedChannelVC.flatMap(vc -> Optional.ofNullable(guild.getVoiceChannelById(vc.getChannel())))
                    .ifPresent(voiceChannel -> { // Se questa vc esiste
                        // Se è il primo a entrare (cioè prima non c'era nessuno)
                        if (voiceChannel.getMembers().size() == 1) {
                            channel.upsertPermissionOverride(publicRole)
                                    .grant(Permission.VIEW_CHANNEL)
                                    .queue(RestUtils.emptyConsumer(),
                                            RestUtils.throwableConsumer("Impossibile settare i permessi del pubblico: {EXCEPTION}"));
                        }
                    });
        });

        // Check anti-move
        // Controlla se questa è una vc
        localMapper.searchByID(builder.add("channel", channel.getId()).create()).ifPresent(vc -> {
            boolean isBanned = vc.getTotalRecords() // Prende tutti i record della vc
                    .stream()
                    .filter(record -> record.type().equals(RecordType.BAN)) // Prende solo quelli relativi ai ban
                    .anyMatch(record -> record.user().equals(member.getId())); // Cerca una corrispondenza dell'id

            if (isBanned) { // Se è bannato kickalo
                guild.kickVoiceMember(member).complete();
                return;
            }

            vc.setLastJoin(Instant.now());
            localMapper.update(vc);
        });
    }

    private void handleQuit(Guild guild, AudioChannel channel) {
        LocalVCMapper localMapper = channelMapper.getMapper(guild);
        if (channel == null || localMapper.isBeingDeleted(channel)) return;

        QueryBuilder builder = QueryBuilder.init().add("guild", guild.getId());
        localMapper.searchByID(builder.add("channel", channel.getId()).create()).ifPresent(vc -> {
            // Se il canale lasciato non è una vc oppure se il canale non è vuoto, ritorna
            if (channel.getMembers().size() != 0) return;

            if (vc.isPinned()) {
                settingsMapper.search(builder.create()).ifPresent(settings -> {
                    VoiceChannel voiceChannel = guild.getVoiceChannelById(vc.getChannel());
                    if (voiceChannel == null) return;

                    // Setta i permessi nuovi
                    VoiceChannelManager manager = voiceChannel.getManager();
                    manager = Perms.setPublicPerms(manager,
                            vc.getStatus(),
                            guild.getRoleById(settings.getPublicRole()),
                            false);
                    manager.queue();
                });
            } else {
                localMapper.scheduleForDeletion(vc, channel);
            }
        });
    }


    // Command listener

    @SubscribeEvent
    public void onSlashCommand(@NotNull SlashCommandInteractionEvent e) {
        if (!e.getInteraction().isFromGuild()) return;

        Optional<Settings> settings = settingsMapper.search(QueryBuilder.init()
                .add("guild", e.getGuild().getId())
                .create());

        if (settings.isEmpty()) return;

        String sub = e.getSubcommandName(); // Ottieni il nome del sottocomando
        if (sub == null) { // Se è nullo, significa che non è stato inserito
            e.reply(new MessageCreateBuilder()
                            .setContent("Non hai specificato nessun comando ❌")
                            .build())
                    .setEphemeral(true)
                    .queue();
        } else {
            // Altrimenti cerchiamo di eseguire il comando,
            // scegliendo tra quelli disponibili
            commands.getSlashCommands().stream()
                    .filter(command -> command.getName().equalsIgnoreCase(sub))
                    .findFirst()
                    .ifPresentOrElse(
                            command -> {
                                if (command.hasPermission(e.getMember(), settings.get())) {
                                    BotLogger.info("%s just used the command '%s' (type: Slash) in guild '%s'",
                                            e.getUser().getAsTag(),
                                            command.getName(),
                                            e.getGuild().getName());

                                    command.execute(settings.get(), e);
                                } else {
                                    e.replyEmbeds(Embeds.errorEmbed("Non hai il permesso di usare questa funzione! :books:"))
                                            .setEphemeral(true)
                                            .queue();
                                }
                            },
                            () -> e.reply(new MessageCreateBuilder()
                                            .setContent("Comando non trovato 😵")
                                            .build())
                                    .setEphemeral(true)
                                    .queue());
        }
    }

    @SubscribeEvent
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        if (!event.isFromGuild()) return;

        Optional<Settings> cachedSettings = settingsMapper.search(QueryBuilder.init()
                .add("guild", event.getGuild().getId())
                .create());
        // Condizioni:
        // 1. La gilda deve avere una categoria creata
        // 2. L'ID del canale dell'interazione deve essere uguale a quello presente nell'oggetto VCCategory
        cachedSettings.ifPresent(category -> {
            if (category.getChannelID().equals(event.getInteraction().getMessageChannel().getId())) {
                handleInteraction(event, event.getButton().getId());
            }
        });
    }

    @SubscribeEvent
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        handleInteraction(event, event.getModalId());
    }

    @SubscribeEvent
    public void onSelectMenuInteraction(@NotNull StringSelectInteractionEvent event) {
        handleInteraction(event, event.getComponentId());
    }

    private void handleInteraction(IReplyCallback event, String id) {
        if (!event.isFromGuild() || id == null) return;

        if (!handleMenuInteraction(event, event.getGuild().getId(), event.getUser().getId(), id)) {
            event.replyEmbeds(Embeds.errorEmbed("Errore. Stanza non trovata! :confused:"))
                    .setEphemeral(true)
                    .queue();
        }
    }

    private boolean handleMenuInteraction(IReplyCallback event, String guild, String user, String id) {
        AtomicBoolean success = new AtomicBoolean(false);
        QueryBuilder builder = QueryBuilder.init().add("guild", guild);

        LocalVCMapper localMapper = channelMapper.getMapper(guild);
        settingsMapper.search(builder.create())
                .ifPresent(settings ->
                        commands.getMenuCommands().stream() // Cerca il comando corrispondente
                                .filter(buttonCommand -> id.startsWith(buttonCommand.getName()))
                                .findFirst()
                                .ifPresent(command -> localMapper.search(builder.add("user", user).create())
                                        .ifPresent(vc -> {
                                            // Se ha il permesso
                                            if (command.hasPermission(event.getMember(), settings)) {
                                                // Se non è in fase di creazione
                                                if (localMapper.isBeingCreated(vc)) {
                                                    event.reply("""
                                                                    La tua stanza è in fase di creazione.
                                                                    Attendi un secondo! ⏳""")
                                                            .setEphemeral(true)
                                                            .queue();
                                                    return;
                                                }

                                                boolean isOnCooldown = false;
                                                // Esegui il comando
                                                if (event instanceof ButtonInteractionEvent e) {
                                                    Cooldown.Result result = commands.isOnCooldown(command, user);
                                                    // Se è in cooldown, manda un messaggio di avviso
                                                    if (result.active()) {
                                                        isOnCooldown = true;
                                                        String content = """
                                                                Questo comando ha un **cooldown**!
                                                                Per favore, attendi ancora __%.1fs__"""
                                                                .formatted(result.nextExecutionInstant().toMillis() / 1000D);

                                                        event.replyEmbeds(new EmbedBuilder()
                                                                        .setColor(new Color(213, 178, 70))
                                                                        .setAuthor("Avviso")
                                                                        .setDescription(content)
                                                                        .build())
                                                                .setEphemeral(true)
                                                                .queue();
                                                    } else {
                                                        vc = command.execute(vc, settings, id, e);
                                                    }
                                                } else if (event instanceof ModalInteractionEvent e) {
                                                    vc = command.execute(vc, settings, id, e);
                                                } else if (event instanceof StringSelectInteractionEvent e) {
                                                    vc = command.execute(vc, settings, id, e);
                                                } else {
                                                    return;
                                                }

                                                if (!isOnCooldown) {
                                                    commands.handleCooldown(command, user);

                                                    BotLogger.info("%s just used the command '%s' (type: %s) in guild '%s'",
                                                            References.user(user),
                                                            command.getName(),
                                                            event.getClass().getSimpleName().replace("InteractionEvent", ""),
                                                            References.guild(guild));
                                                }

                                                // Se ritorna null, significa che non c'è
                                                // bisogno di alcun update al database
                                                if (vc != null) {
                                                    localMapper.update(vc); // Aggiorna il database
                                                }
                                            } else {
                                                // Mostra un messaggio di errore
                                                event.replyEmbeds(Embeds.errorEmbed("Non hai il permesso di usare questa funzione! :books:"))
                                                        .setEphemeral(true)
                                                        .queue();
                                            }

                                            success.set(true); // Ritorna true
                                        })));

        return success.get();
    }
}
