package dev.starless.maggiordomo;

import dev.starless.maggiordomo.activity.ActivityManager;
import dev.starless.maggiordomo.commands.CommandManager;
import dev.starless.maggiordomo.commands.interaction.*;
import dev.starless.maggiordomo.commands.interaction.filter.ContainsFilterInteraction;
import dev.starless.maggiordomo.commands.interaction.filter.PatternFilterInteraction;
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
import dev.starless.maggiordomo.localization.Messages;
import dev.starless.maggiordomo.localization.Translations;
import dev.starless.maggiordomo.logging.BotLogger;
import dev.starless.maggiordomo.storage.VCManager;
import dev.starless.maggiordomo.storage.settings.SettingsMapper;
import dev.starless.maggiordomo.storage.vc.LocalVCMapper;
import dev.starless.maggiordomo.utils.discord.Embeds;
import dev.starless.maggiordomo.utils.discord.Perms;
import dev.starless.maggiordomo.utils.discord.References;
import dev.starless.maggiordomo.utils.discord.RestUtils;
import dev.starless.mongo.MongoStorage;
import dev.starless.mongo.api.Query;
import dev.starless.mongo.api.QueryBuilder;
import dev.starless.mongo.schema.MigrationSchema;
import dev.starless.mongo.schema.suppliers.FixedKeySupplier;
import dev.starless.mongo.schema.suppliers.impl.SimpleSupplier;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.Instant;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class Core implements Module {

    private final MongoStorage storage;

    @Getter private VCManager channelMapper;
    @Getter private SettingsMapper settingsMapper;

    private ActivityManager activityManager;
    @Getter private CommandManager commands;

    public Core(Config config) {
        // Translations are already needed for the Settings schema
        Translations.init();
        // Statistics for api purposes
        Statistics.getInstance().load();

        storage = new MongoStorage(BotLogger.getLogger(), config.getString(ConfigEntry.MONGO))
                .migrationSchema(new MigrationSchema(Settings.class)
                        .entry("filterStrings", new HashMap<>())
                        .entry("categories", new FixedKeySupplier("categoryID") {
                            @Override
                            public Object supply(Document document) {
                                // Trasferisce il vecchio id della categoria sul nuovo formato
                                List<String> categories = new ArrayList<>();
                                String legacyID = document.getString(deprecatedKey());
                                if (legacyID != null) {
                                    categories.add(legacyID);
                                }

                                return categories;
                            }
                        })
                        .entry("menuChannelID", new SimpleSupplier("channelID", "-1"))
                        .entry("voiceGeneratorID", new SimpleSupplier("voiceID", "-1"))
                        .entry("language", "en"));
    }

    @Override
    public void onEnable(JDA jda) {
        storage.init();

        channelMapper = new VCManager(storage);
        settingsMapper = new SettingsMapper(storage);
        activityManager = new ActivityManager(jda.getGuilds().size());

        List<Settings> settingsList = settingsMapper.bulkSearch(QueryBuilder.empty());
        jda.getGuilds().forEach(guild -> {
            final String guildID = guild.getId();
            Settings settings = null;

            // Look for the correct Settings object
            Iterator<Settings> it = settingsList.iterator();
            while (it.hasNext()) {
                Settings iterated = it.next();
                if (iterated.getGuild().equals(guildID)) {
                    settings = iterated;
                    it.remove();
                }
            }

            // Handle the case when the settings are not found
            if (settings != null) {
                settingsMapper.getSettings().put(guildID, settings);
            } else {
                settings = new Settings(guild);
                settingsMapper.insert(settings);
            }

            if (settings.hasCategory()) {
                final String voiceGeneratorID = settings.getVoiceGeneratorID();
                settings.forEachCategory(guild, category -> {
                    LocalVCMapper localMapper = channelMapper.getMapper(guild);
                    category.getVoiceChannels().forEach(voiceChannel -> {
                        if (voiceChannel.getId().equals(voiceGeneratorID)) return;

                        Optional<VC> optionalVC = localMapper.searchByID(QueryBuilder.init()
                                .add("guild", guildID)
                                .add("channel", voiceChannel.getId())
                                .create());

                        if (optionalVC.isPresent()) {
                            VC vc = optionalVC.get();
                            // If the room is not pinned and nobody is using it we delete it
                            if (!vc.isPinned() && voiceChannel.getMembers().isEmpty()) {
                                localMapper.scheduleForDeletion(vc, voiceChannel).queue();
                            }
                        }
                    });
                });
            }
        });

        // Delete the settings object that are not used anymore
        BotLogger.info("Found %d unused Settings objects.", settingsList.size());
        settingsList.forEach(settings -> {
            channelMapper.getMapper(settings.getGuild()).purge();
            settingsMapper.delete(settings);
        });

        commands = new CommandManager()
                .name("maggiordomo")
                .both(new SetupCommand())
                .command(new BannedCommand())
                .command(new MenuCommand())
                .command(new PremiumCommand())
                .command(new RecoverCommand())
                .command(new ReloadPermsCommand())
                .command(new FiltersCommand())
                .command(new LanguageCommand())
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
                .interaction(new DeleteInteraction())
                .interaction(new ContainsFilterInteraction())
                .interaction(new PatternFilterInteraction());

        commands.create(jda);
        activityManager.start();
        jda.addEventListener(this);
    }

    @Override
    public void onDisable(JDA jda) {
        jda.removeEventListener(this);

        activityManager.stop();
        Statistics.getInstance().save();

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
        Optional<Settings> savedSettings = settingsMapper.search(QueryBuilder.init()
                .add("guild", e.getGuild().getId())
                .create());

        // Create or get the settings of the guild
        if (savedSettings.isEmpty()) {
            settingsMapper.insert(new Settings(e.getGuild()));
        }

        // Update commands of the guild
        commands.update(e.getGuild());

        // Start activity monitoring
        activityManager.startMonitor(e.getGuild().getId());

        // Update statistics
        Statistics.getInstance().updateGuild(true);

        BotLogger.info("The guild '%s' has just added the bot!", e.getGuild().getName());
    }

    @SubscribeEvent
    public void onGuildLeave(@NotNull GuildLeaveEvent e) {
        String guildID = e.getGuild().getId();
        // Delete the guilds' data alongside all the vc data
        settingsMapper.search(QueryBuilder.init()
                        .add("guild", guildID)
                        .create())
                .ifPresent(settings -> {
                    channelMapper.getMapper(guildID).purge();
                    settingsMapper.delete(settings);
                });

        activityManager.stopMonitor(guildID); // Stop monitoring the guilds' activity
        Statistics.getInstance().updateGuild(false); // Update statistics
        BotLogger.info("The bot departed from the guild '%s'.", e.getGuild().getName());
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
                        localMapper.scheduleForDeletion(vc, channel).queue();
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
        handleQuit(e.getGuild(), e.getChannelLeft(), e.getChannelJoined());
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

    public boolean updateLanguage(Guild guild, Settings settings, String newLanguage) {
        if (settings.getLanguage().equals(newLanguage)) return true;

        if (Translations.getLanguageCodes().contains(newLanguage)) {
            // Change the title of the guide if it has not been changed
            String title = settings.getTitle();
            if (title.equals(Translations.string(Messages.SETTINGS_INTERFACE_TITLE, settings.getLanguage()))) {
                settings.setTitle(Translations.string(Messages.SETTINGS_INTERFACE_TITLE, newLanguage));
            }

            // Do the same for the description
            String desc = settings.getDescriptionRaw();
            if (desc.equals(Translations.string(Messages.SETTINGS_INTERFACE_DESCRIPTION, settings.getLanguage()))) {
                settings.setDescriptionRaw(Translations.string(Messages.SETTINGS_INTERFACE_DESCRIPTION, newLanguage));
            }

            // Change the guide
            updateMenu(guild, settings);

            // Set the new language
            settings.setLanguage(newLanguage);

            // Update cache, database and commands
            settingsMapper.update(settings);
            commands.update(guild);

            return true;
        }

        return false;
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
        String content = "# " + settings.getTitle() + "\n\n" + settings.getDescription();

        MessageCreateBuilder builder = new MessageCreateBuilder()
                .setContent(content)
                .addComponents(buttonRows)
                .setSuppressedNotifications(true);

        // Add the image guide
        FileUpload guide = Translations.guide(settings.getLanguage());
        if (guide != null) {
            builder.addFiles(guide);
        }

        return builder.build();
    }

    public void updateMenu(Guild guild, Settings settings) {
        if (settings.hasNoMenuChannel() || settings.hasNoMenu()) return;

        TextChannel channel = guild.getTextChannelById(settings.getMenuChannelID());
        if (channel != null) {
            channel.retrieveMessageById(settings.getMenuID()).queue(message -> {
                message.delete().queue();

                MessageCreateData data = Bot.getInstance().getCore().createMenu(guild.getId());
                if (data != null) {
                    channel.sendMessage(data).queue(updatedMessage -> {
                        settings.setMenuID(updatedMessage.getId());
                        Bot.getInstance().getCore().getSettingsMapper().update(settings);
                    });
                } else {
                    BotLogger.warn("Could not update the menu of the guild (build failed): " + guild.getName());
                }
            }, throwable -> BotLogger.warn("Could not update the menu of the guild (invalid message): " + guild.getName()));
        } else {
            BotLogger.warn("Could not update the menu of the guild (invalid channel): " + guild.getName());
        }
    }

    private void handleJoin(Guild guild, AudioChannel channel, Member member) {
        LocalVCMapper localMapper = channelMapper.getMapper(guild);
        if (channel == null || localMapper.isBeingDeleted(channel.getId())) return;

        Optional<Settings> cachedSettings = settingsMapper.search(QueryBuilder.init().add("guild", guild.getId()).create());
        if (cachedSettings.isEmpty()) return;

        Settings settings = cachedSettings.get();
        if (settings.isBanned(member)) {
            guild.kickVoiceMember(member).queue(
                    RestUtils.emptyConsumer(),
                    RestUtils.throwableConsumer("Something went wrong when kicking: " + References.user(member.getUser()))
            );
            return;
        }

        Role publicRole = guild.getRoleById(settings.getPublicRole());
        if (publicRole == null) return; // <-- This should never happen, @everyone should be the default value

        VC vc = null;
        // If the user has joined the generator channel
        if (channel.getId().equals(settings.getVoiceGeneratorID())) {
            Category category = settings.getAvailableCategory(guild);
            if (category == null) return; // <-- If this triggers something has gone VERY wrong

            // Find the VC object associated with the user
            Optional<VC> cachedVC = localMapper.search(QueryBuilder.init()
                    .add("guild", guild.getId())
                    .add("user", member.getId())
                    .create());
            if (cachedVC.isPresent()) {
                // Use the preferences to create a new vc or move the user into the existing one
                vc = cachedVC.get();
                VoiceChannel voiceChannel = guild.getVoiceChannelById(vc.getChannel());
                if (voiceChannel != null) {
                    try {
                        guild.moveVoiceMember(member, voiceChannel).complete();
                    } catch (ErrorResponseException ex) {
                        if (ex.getErrorResponse().equals(ErrorResponse.UNKNOWN_CHANNEL)) {
                            // Sometimes this happens...
                            localMapper.createVC(vc, publicRole, category);
                            return;
                        }
                    }
                } else {
                    localMapper.createVC(vc, publicRole, category);
                    return;
                }
            } else {
                // If a VC object does not exist, then create a new one for the user
                vc = new VC(member, settings.getLanguage());
                localMapper.insert(vc);
                localMapper.createVC(vc, publicRole, category);
                return;
            }
        }

        // This part of the code handles the visibility change of the channel
        // when the first user joins a room

        // If the previous code has not found an existing vc
        if (vc == null) {
            // Find a VC object using the channel id, aka try to see if the joined channel is a room
            Optional<VC> cachedVC = localMapper.searchByID(QueryBuilder.init()
                    .add("guild", guild.getId())
                    .add("channel", channel.getId())
                    .create());

            if (cachedVC.isEmpty()) return;

            vc = cachedVC.get();
        }

        // Kick the user if they are banned from the vc they are trying to join
        // (This should not be needed, since banned users do not see the vc they are banned from)
        if (vc.hasPlayerRecord(RecordType.BAN, member.getId())) {
            guild.kickVoiceMember(member).queue(
                    RestUtils.emptyConsumer(),
                    RestUtils.throwableConsumer("Something went wrong when kicking: " + References.user(member.getUser()))
            );
            return;
        } else if (channel.getMembers().size() == 1) { // If the user is the first to join the channel
            channel.upsertPermissionOverride(publicRole)
                    .grant(Permission.VIEW_CHANNEL)
                    .queue(RestUtils.emptyConsumer(),
                            RestUtils.throwableConsumer("Could not set the public role's permissions: {EXCEPTION}"));
        }

        vc.setLastJoin(Instant.now());
        localMapper.update(vc);
    }

    private void handleQuit(Guild guild, AudioChannel channelLeft, AudioChannel channelJoined) {
        LocalVCMapper localMapper = channelMapper.getMapper(guild);
        if (channelLeft == null || localMapper.isBeingDeleted(channelLeft.getId())) return;

        QueryBuilder query = QueryBuilder.init().add("guild", guild.getId());
        localMapper.searchByID(query.add("channel", channelLeft.getId()).create())
                .ifPresent(vc -> {
                    // Se il canale lasciato non è una vc oppure se il canale non è vuoto, ritorna
                    if (!channelLeft.getMembers().isEmpty()) return;

                    // We can use Optional#ifPresent because every guild should have a Settings object
                    settingsMapper.search(query.create()).ifPresent(settings -> {
                        if (vc.isPinned()) {
                            VoiceChannel voiceChannel = guild.getVoiceChannelById(vc.getChannel());
                            if (voiceChannel == null) return;

                            // Setta i permessi nuovi
                            Perms.setPublicPerms(voiceChannel.getManager(),
                                            vc.getStatus(),
                                            guild.getRoleById(settings.getPublicRole()),
                                            false)
                                    .queue(RestUtils.emptyConsumer(), RestUtils.emptyConsumer());
                        } else {
                            RestAction<Void> deletion = localMapper.scheduleForDeletion(vc, channelLeft);
                            if (channelJoined != null && channelJoined.getId().equals(settings.getVoiceGeneratorID())) {
                                // We use RestAction#complete here because the room has to be
                                // deleted for a new room to be created
                                deletion.complete();
                            } else {
                                deletion.queue();
                            }
                        }
                    });
                });
    }


    // Command listener

    @SubscribeEvent
    public void onSlashCommand(@NotNull SlashCommandInteractionEvent e) {
        if (!e.getInteraction().isFromGuild()) return;

        Optional<Settings> opSettings = settingsMapper.search(QueryBuilder.init()
                .add("guild", e.getGuild().getId())
                .create());

        if (opSettings.isEmpty()) return;

        Settings settings = opSettings.get();
        String sub = e.getSubcommandName(); // Ottieni il nome del sottocomando
        if (sub == null) { // Se è nullo, significa che non è stato inserito
            e.reply(new MessageCreateBuilder()
                            .setContent(Translations.string(Messages.COMMAND_NOT_FOUND, settings.getLanguage()))
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
                                if (command.hasPermission(e.getMember(), settings)) {
                                    // Update statistics
                                    Statistics.getInstance().incrementCommands();

                                    BotLogger.info("%s just used the command '%s' (type: Slash) in guild '%s'",
                                            References.user(e.getUser()),
                                            command.getName(),
                                            References.guild(e.getGuild().getId()));

                                    command.execute(settings, e);
                                } else {
                                    e.replyEmbeds(Embeds.errorEmbed(Translations.string(Messages.NO_PERMISSION, settings.getLanguage())))
                                            .setEphemeral(true)
                                            .queue();
                                }
                            },
                            () -> e.reply(new MessageCreateBuilder()
                                            .setContent(Translations.string(Messages.COMMAND_NOT_FOUND, settings.getLanguage()))
                                            .build())
                                    .setEphemeral(true)
                                    .queue());
        }
    }

    @SubscribeEvent
    public void onAutocomplete(@NotNull CommandAutoCompleteInteractionEvent e) {
        if (!e.getInteraction().isFromGuild()) return;

        String subcommand = e.getSubcommandName();
        if (subcommand == null) return;

        // Handle command autocomplete
        settingsMapper.search(QueryBuilder.init()
                        .add("guild", e.getGuild().getId())
                        .create())
                .ifPresent(settings -> commands.getCommands().stream()
                        .filter(command -> command.getName().equalsIgnoreCase(subcommand))
                        .findFirst()
                        .ifPresent(command -> command.autocomplete(settings, e)));
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

        Optional<Settings> opSettings = settingsMapper.search(QueryBuilder.init()
                .add("guild", event.getGuild().getId())
                .create());
        if (opSettings.isEmpty()) return;

        Settings settings = opSettings.get();
        boolean handledCorrectly = handleMenuInteraction(event, settings, event.getMember(), id);
        if (!handledCorrectly && !event.isAcknowledged()) {
            event.replyEmbeds(Embeds.errorEmbed(Translations.string(Messages.GENERIC_ERROR, settings.getLanguage())))
                    .setEphemeral(true)
                    .queue();
        }
    }

    private boolean handleMenuInteraction(IReplyCallback event, Settings settings, Member member, String id) {
        String memberID = member.getId();
        LocalVCMapper localMapper = channelMapper.getMapper(settings.getGuild());

        if (settings.isBanned(member)) {
            event.reply(Translations.string(Messages.NO_PERMISSION_BANNED, settings.getLanguage()))
                    .setEphemeral(true)
                    .queue();
            return false;
        }

        Optional<Interaction> op = commands.getInteractions().stream() // Cerca il comando corrispondente
                .filter(interaction -> id.startsWith(interaction.getName()))
                .findFirst();

        if (op.isPresent()) {
            Interaction interaction = op.get();
            VC vc = localMapper.search(QueryBuilder.init()
                            .add("guild", event.getGuild().getId())
                            .add("user", memberID)
                            .create())
                    .orElse(null);

            if (interaction.needsVC() && vc == null) return false;

            // Se ha il permesso
            if (interaction.hasPermission(event.getMember(), settings)) {
                // Se non è in fase di creazione
                if (vc != null && localMapper.isBeingCreated(vc)) {
                    event.reply(Translations.string(Messages.GENERIC_ERROR, settings.getLanguage()))
                            .setEphemeral(true)
                            .queue();
                    return false;
                }

                boolean isOnCooldown = false;
                // Esegui il comando
                if (event instanceof ButtonInteractionEvent e) {
                    Cooldown.Result result = commands.isOnCooldown(interaction, memberID);
                    // Se è in cooldown, manda un messaggio di avviso
                    if (result.active()) {
                        isOnCooldown = true;

                        event.replyEmbeds(new EmbedBuilder()
                                        .setColor(new Color(213, 178, 70))
                                        .setAuthor("Warning")
                                        .setDescription(Translations.string(Messages.ON_COOLDOWN, settings.getLanguage(), result.nextExecutionInstant().toMillis() / 1000D))
                                        .build())
                                .setEphemeral(true)
                                .queue();
                    } else {
                        vc = interaction.onButtonInteraction(vc, settings, id, e);
                    }
                } else if (event instanceof ModalInteractionEvent e) {
                    vc = interaction.onModalInteraction(vc, settings, id, e);
                } else if (event instanceof StringSelectInteractionEvent e) {
                    vc = interaction.onStringSelected(vc, settings, id, e);
                } else if (event instanceof EntitySelectInteractionEvent e) {
                    vc = interaction.onEntitySelected(vc, settings, id, e);
                } else {
                    return false;
                }

                if (!isOnCooldown) {
                    commands.handleCooldown(interaction, memberID);

                    // Update statistics
                    Statistics.getInstance().incrementCommands();

                    BotLogger.info("%s just used the command '%s' (type: %s) in guild '%s'",
                            References.user(member.getUser()),
                            interaction.getName(),
                            event.getClass().getSimpleName().replace("InteractionEvent", ""),
                            References.guild(settings.getGuild()));
                }

                // Se ritorna null, significa che non c'è
                // bisogno di alcun update al database
                if (vc != null) {
                    localMapper.update(vc); // Aggiorna il database
                }
            } else {
                // Mostra un messaggio di errore
                event.replyEmbeds(Embeds.errorEmbed(Translations.string(Messages.NO_PERMISSION, settings.getLanguage())))
                        .setEphemeral(true)
                        .queue();
            }

            return true;
        }

        return false;
    }
}
