package dev.starless.maggiordomo;

import dev.starless.maggiordomo.commands.CommandManager;
import dev.starless.maggiordomo.commands.interaction.*;
import dev.starless.maggiordomo.commands.slash.*;
import dev.starless.maggiordomo.commands.types.Interaction;
import dev.starless.maggiordomo.config.Config;
import dev.starless.maggiordomo.config.ConfigEntry;
import dev.starless.maggiordomo.data.Cooldown;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.enums.RecordType;
import dev.starless.maggiordomo.data.user.UserRecord;
import dev.starless.maggiordomo.data.user.VC;
import dev.starless.maggiordomo.interfaces.Module;
import dev.starless.maggiordomo.logging.BotLogger;
import dev.starless.maggiordomo.utils.discord.References;
import dev.starless.maggiordomo.storage.VCManager;
import dev.starless.maggiordomo.storage.settings.SettingsMapper;
import dev.starless.maggiordomo.storage.vc.LocalVCMapper;
import dev.starless.maggiordomo.tasks.ActivityChecker;
import dev.starless.maggiordomo.utils.discord.Embeds;
import dev.starless.maggiordomo.utils.discord.Perms;
import dev.starless.maggiordomo.utils.discord.RestUtils;
import dev.starless.mongo.MongoStorage;
import dev.starless.mongo.objects.Query;
import dev.starless.mongo.objects.QueryBuilder;
import dev.starless.mongo.objects.Schema;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Core implements Module {

    private final MongoStorage storage;

    @Getter private VCManager channelMapper;
    @Getter private SettingsMapper settingsMapper;

    private ScheduledExecutorService activityService;
    private CommandManager commands;

    public Core(Config config) {
        storage = new MongoStorage(BotLogger.getLogger(), config.getString(ConfigEntry.MONGO))
                .registerSchema(new Schema(Settings.class)
                        .entry("categories", "categoryID", document -> {
                            // Trasferisce il vecchio id della categoria sul nuovo formato
                            List<String> categories = new ArrayList<>();
                            String legacyID = document.getString("categoryID");
                            if (legacyID != null) {
                                categories.add(legacyID);
                            }

                            return categories;
                        })
                        .entry("channelID", "-1")
                        .entry("voiceID", "-1")
                        .entry("publicRole", "-1")
                        .entry("maxInactivity", -1L)
                        .entry("title", "Comandi disponibili :books:")
                        .entry("descriptionRaw", """
                                Entra in {CHANNEL} per creare la tua stanza e usa questo pannello per **personalizzarla**.
                                Ad ogni bottone è associata una __emoji__: qua sotto puoi leggere la spiegazione dei vari comandi e poi cliccare sul pulsante corrispondente per eseguirlo."""));
    }

    @Override
    public void onEnable(JDA jda) {
        storage.init();

        activityService = Executors.newScheduledThreadPool(jda.getGuilds().size());
        channelMapper = new VCManager(storage);
        settingsMapper = new SettingsMapper(storage);

        jda.getGuilds().forEach(guild -> {
            String guildID = guild.getId();
            Optional<Settings> cachedSettings = settingsMapper.search(QueryBuilder.init()
                    .add("guild", guildID)
                    .create());

            Settings settings;
            if (cachedSettings.isPresent()) {
                settings = cachedSettings.get();
                settingsMapper.getSettings().put(guildID, settings);
            } else {
                settings = new Settings(guild);
                settingsMapper.insert(settings);
            }

            if (settings.hasCategory()) {
                settings.forEachCategory(guild, category -> {
                    LocalVCMapper localMapper = channelMapper.getMapper(guild);
                    category.getVoiceChannels().forEach(voiceChannel -> {
                        if (voiceChannel.getId().equals(settings.getVoiceID())) return;

                        Optional<VC> optionalVC = localMapper.searchByID(QueryBuilder.init()
                                .add("guild", guildID)
                                .add("channel", voiceChannel.getId())
                                .create());

                        if (optionalVC.isPresent()) {
                            VC vc = optionalVC.get();
                            // Se non è lockata e non ci sono persone dentro,
                            // elimina la stanza
                            if (!vc.isPinned() && voiceChannel.getMembers().size() == 0) {
                                localMapper.scheduleForDeletion(vc, voiceChannel);
                            }
                        }
                    });
                });
            }

            // Attiva il servizio di controllo di attività
            activityService.scheduleWithFixedDelay(new ActivityChecker(guildID), 0, 1, TimeUnit.HOURS);
        });

        commands = new CommandManager()
                .name("maggiordomo")
                .both(new SetupCommand())
                .command(new BannedCommand())
                .command(new MenuCommand())
                .command(new PremiumCommand())
                .command(new RecoverCommand())
                .command(new ReloadPermsCommand())
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

        int interrupted = 0;
        for (Guild guild : jda.getGuilds()) {
            LocalVCMapper localMapper = channelMapper.getMapper(guild);
            ExecutorService createService = localMapper.getCreateService();

            createService.shutdown();
            try {
                boolean success = createService.awaitTermination(15, TimeUnit.SECONDS);
                if (!success) {
                    interrupted++;
                    createService.shutdownNow();
                }

            } catch (InterruptedException e) {
                BotLogger.error("CreateService shutdown sequence was interrupted while waiting!");
            }
        }

        String debug = interrupted == 0
                ? "CreateService terminated successfully."
                : interrupted + " CreateService(s) got interrupted forcefully!";
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

        BotLogger.info("The guild '%s' has just added the bot!", e.getGuild().getName());
    }

    @SubscribeEvent
    public void onGuildMemberRemove(@NotNull GuildMemberRemoveEvent event) {
        // Se l'utente lascia la gilda, cancelliamo la vc
        LocalVCMapper localMapper = channelMapper.getMapper(event.getGuild());
        localMapper.search(QueryBuilder.init()
                        .add("guild", event.getGuild().getId())
                        .add("user", event.getUser().getId())
                        .create())
                .ifPresent(vc -> {
                    VoiceChannel channel = event.getGuild().getVoiceChannelById(vc.getChannel());
                    if (channel != null) {
                        localMapper.scheduleForDeletion(vc, channel);
                    }
                });
    }

    @SubscribeEvent
    public void onVoiceChannelDelete(@NotNull ChannelDeleteEvent event) {
        if (event.getChannelType().equals(ChannelType.VOICE)) {
            LocalVCMapper localMapper = channelMapper.getMapper(event.getGuild());
            if (localMapper.isBeingDeleted(event.getChannel().getId())) return;

            localMapper.searchByID(QueryBuilder.init()
                            .add("guild", event.getGuild().getId())
                            .add("channel", event.getChannel().getId())
                            .create())
                    .ifPresent(vc -> {
                        localMapper.removeFromCache(vc);

                        if (vc.isPinned()) {
                            vc.setPinned(false);
                            localMapper.getGateway().update(vc);
                        }
                    });
        }
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
                        .map(UserRecord::user)
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

    public MessageCreateData createMenu(String guild) {
        Optional<Settings> op = settingsMapper.search(QueryBuilder.init()
                .add("guild", guild)
                .create());

        if (op.isEmpty()) return null;
        Settings settings = op.get();

        // Crea la lista di bottoni
        List<ActionRow> buttonRows = new ArrayList<>();
        Stack<Button> row = new Stack<>();

        List<Interaction> actualInteractions = commands.getMenuInteractions();
        int commandNumber = actualInteractions.size();
        for (int i = 0; i < commandNumber; i++) {
            Interaction cmd = actualInteractions.get(i);
            row.add(Button.of(ButtonStyle.SECONDARY, cmd.getName(), cmd.emoji()));

            if (row.size() == 4 || i == commandNumber - 1) {
                buttonRows.add(ActionRow.of(row));
                row.clear();
            }
        }

        // Crea il messaggio di aiuto
        String content = """
                # %s

                %s
                """.formatted(settings.getTitle(), settings.getDescription());

        // Crea il messaggio d'aito
        MessageCreateBuilder builder = new MessageCreateBuilder()
                .addFiles(FileUpload.fromData(getClass().getResourceAsStream("/guide.png"), "guide.png"))
                .setContent(content)
                .addComponents(buttonRows);

        return builder.build();
    }

    private void handleJoin(Guild guild, AudioChannel channel, Member member) {
        LocalVCMapper localMapper = channelMapper.getMapper(guild);
        if (channel == null || localMapper.isBeingDeleted(channel.getId())) return;

        String guildID = guild.getId();
        QueryBuilder builder = QueryBuilder.init().add("guild", guildID);
        // Controlla se la gilda ha una categoria setuppata
        settingsMapper.search(builder.create()).ifPresent(settings -> {
            // Prendi gli oggetti utili
            Category category = settings.getAvailableCategory(guild);
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
        if (channel == null || localMapper.isBeingDeleted(channel.getId())) return;

        QueryBuilder query = QueryBuilder.init().add("guild", guild.getId());
        localMapper.searchByID(query.add("channel", channel.getId()).create())
                .ifPresent(vc -> {
                    // Se il canale lasciato non è una vc oppure se il canale non è vuoto, ritorna
                    if (channel.getMembers().size() != 0) return;

                    if (vc.isPinned()) {
                        settingsMapper.search(query.create())
                                .ifPresent(settings -> {
                                    VoiceChannel voiceChannel = guild.getVoiceChannelById(vc.getChannel());
                                    if (voiceChannel == null) return;

                                    // Setta i permessi nuovi
                                    Perms.setPublicPerms(voiceChannel.getManager(),
                                                    vc.getStatus(),
                                                    guild.getRoleById(settings.getPublicRole()),
                                                    false)
                                            .queue(RestUtils.emptyConsumer(), RestUtils.emptyConsumer());
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
            commands.getCommands().stream()
                    .filter(command -> command.getName().equalsIgnoreCase(sub))
                    .findFirst()
                    .ifPresentOrElse(
                            command -> {
                                if (command.hasPermission(e.getMember(), settings.get())) {
                                    BotLogger.info("%s just used the command '%s' (type: Slash) in guild '%s'",
                                            References.user(e.getUser()),
                                            command.getName(),
                                            References.guild(e.getGuild().getId()));

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

        handleInteraction(event, event.getButton().getId());
    }

    @SubscribeEvent
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        handleInteraction(event, event.getModalId());
    }

    @SubscribeEvent
    public void onSelectMenuInteraction(@NotNull StringSelectInteractionEvent event) {
        handleInteraction(event, event.getComponentId());
    }

    @SubscribeEvent
    public void onEntityMenuInteraction(@NotNull EntitySelectInteractionEvent event) {
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
                .ifPresent(settings -> {
                    Optional<Interaction> op = commands.getInteractions().stream() // Cerca il comando corrispondente
                            .filter(interaction -> id.startsWith(interaction.getName()))
                            .findFirst();

                    if (op.isPresent()) {
                        Interaction interaction = op.get();
                        VC vc = localMapper.search(builder.add("user", user).create()).orElse(null);

                        if (interaction.needsVC() && vc == null) return;

                        // Se ha il permesso
                        if (interaction.hasPermission(event.getMember(), settings)) {
                            // Se non è in fase di creazione
                            if (vc != null && localMapper.isBeingCreated(vc)) {
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
                                Cooldown.Result result = commands.isOnCooldown(interaction, user);
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
                                    vc = interaction.execute(vc, settings, id, e);
                                }
                            } else if (event instanceof ModalInteractionEvent e) {
                                vc = interaction.execute(vc, settings, id, e);
                            } else if (event instanceof StringSelectInteractionEvent e) {
                                vc = interaction.execute(vc, settings, id, e);
                            } else if (event instanceof EntitySelectInteractionEvent e) {
                                vc = interaction.execute(vc, settings, id, e);
                            } else {
                                return;
                            }

                            if (!isOnCooldown) {
                                commands.handleCooldown(interaction, user);

                                BotLogger.info("%s just used the command '%s' (type: %s) in guild '%s'",
                                        References.user(user),
                                        interaction.getName(),
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
                    }
                });

        return success.get();
    }
}
