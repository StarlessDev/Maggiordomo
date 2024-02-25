package dev.starless.maggiordomo.commands.slash;

import dev.starless.maggiordomo.Bot;
import dev.starless.maggiordomo.commands.types.Interaction;
import dev.starless.maggiordomo.commands.types.Slash;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.VC;
import dev.starless.maggiordomo.localization.Messages;
import dev.starless.maggiordomo.localization.Translations;
import dev.starless.maggiordomo.BotLogger;
import dev.starless.maggiordomo.utils.discord.Perms;
import dev.starless.maggiordomo.utils.discord.References;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.requests.restaction.ChannelAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;

import java.util.*;
import java.util.function.Consumer;

public class SetupCommand implements Slash, Interaction {

    private final Map<Integer, Emoji> daysEmojis = new HashMap<>();

    public SetupCommand() {
        daysEmojis.put(3, Emoji.fromUnicode("3️⃣"));
        daysEmojis.put(4, Emoji.fromUnicode("4️⃣"));
        daysEmojis.put(5, Emoji.fromUnicode("5️⃣"));
        daysEmojis.put(6, Emoji.fromUnicode("6️⃣"));
        daysEmojis.put(7, Emoji.fromUnicode("7️⃣"));
    }

    @Override
    public void execute(Settings settings, SlashCommandInteractionEvent e) {
        MessageCreateData message = new MessageCreateBuilder()
                .setContent(Translations.string(Messages.COMMAND_SETUP_EXPLANATION, settings.getLanguage()))
                .setActionRow(Button.primary("setup:role", Translations.string(Messages.COMMAND_SETUP_START_BUTTON_LABEL, settings.getLanguage())))
                .setSuppressEmbeds(true)
                .setAllowedMentions(Collections.emptyList())
                .build();

        e.reply(message).queue();
    }

    @Override
    public VC onButtonInteraction(VC vc, Settings settings, String id, ButtonInteractionEvent e) {
        String[] args = id.split(":");
        if (args.length >= 2) {
            String element = args[1];
            String continueButton = Translations.string(Messages.COMMAND_SETUP_CONTINUE_BUTTON_LABEL, settings.getLanguage());
            String content = null;

            MessageEditBuilder builder = new MessageEditBuilder();
            List<ActionRow> rows = new ArrayList<>();

            switch (element) {
                case "role" -> {
                    content = Translations.string(Messages.COMMAND_SETUP_STEPS_ROLE_CONTENT,
                            settings.getLanguage(),
                            References.roleName(e.getGuild(), settings.getPublicRole()));

                    EntitySelectMenu roleSelector = EntitySelectMenu.create("setup:role", EntitySelectMenu.SelectTarget.ROLE)
                            .setPlaceholder(Translations.string(Messages.COMMAND_SETUP_STEPS_ROLE_SELECTOR_PLACEHOLDER, settings.getLanguage()))
                            .build();

                    rows.add(ActionRow.of(roleSelector));
                    rows.add(ActionRow.of(
                            Button.secondary("setup:embed", continueButton),
                            Button.danger("setup:reset_role", Translations.string(Messages.COMMAND_SETUP_STEPS_ROLE_RESET, settings.getLanguage()))
                    ));
                }
                case "reset_role" -> {
                    updatePublicRole(e, settings, e.getGuild().getPublicRole());

                    content = Translations.string(
                            Messages.COMMAND_SETUP_STEPS_ROLE_CONTENT,
                            settings.getLanguage(), e.getGuild().getPublicRole().getAsMention()
                    );
                    rows.addAll(e.getMessage().getActionRows());
                }
                case "embed" -> {
                    content = Translations.string(Messages.COMMAND_SETUP_STEPS_INTERFACE_CONTENT, settings.getLanguage());

                    rows.add(ActionRow.of(
                            Button.success("setup:embed_preview", Translations.string(Messages.COMMAND_SETUP_STEPS_INTERFACE_PREVIEW_BUTTON, settings.getLanguage())),
                            Button.primary("setup:embed_impl", Translations.string(Messages.COMMAND_SETUP_STEPS_INTERFACE_EDIT_BUTTON, settings.getLanguage())),
                            Button.secondary("setup:inactivity", continueButton)
                    ));
                }
                case "embed_impl" -> {
                    e.replyModal(Modal.create("setup:embed_impl", Translations.string(Messages.MEMBER_MODAL_TITLE, settings.getLanguage()))
                                    .addActionRow(TextInput.create("title", Translations.string(Messages.COMMAND_SETUP_STEPS_INTERFACE_MODAL_TITLE, settings.getLanguage()), TextInputStyle.SHORT)
                                            .setValue(settings.getTitle())
                                            .setMaxLength(128)
                                            .build())
                                    .addActionRow(TextInput.create("desc", Translations.string(Messages.COMMAND_SETUP_STEPS_INTERFACE_MODAL_DESC, settings.getLanguage()), TextInputStyle.PARAGRAPH)
                                            .setValue(settings.getDescriptionRaw())
                                            .setMaxLength(1024)
                                            .build())
                                    .build())
                            .queue();

                    return null;
                }
                case "embed_preview" -> {
                    String reply = Translations.string(Messages.COMMAND_SETUP_STEPS_INTERFACE_PREVIEW, settings.getLanguage()) +
                            "\n\n# " + settings.getTitle() +
                            "\n" + settings.getDescription();

                    e.reply(reply).setEphemeral(true).queue();

                    return null;
                }
                case "inactivity" -> {
                    content = Translations.string(Messages.COMMAND_SETUP_STEPS_INACTIVITY_CONTENT, settings.getLanguage());

                    StringSelectMenu.Builder menu = StringSelectMenu.create("setup:inactivity")
                            .setPlaceholder(Translations.string(Messages.COMMAND_SETUP_STEPS_INACTIVITY_SELECTION_PLACEHOLDER, settings.getLanguage()))
                            .addOption(Translations.string(Messages.COMMAND_SETUP_STEPS_INACTIVITY_SELECTION_DEFAULT, settings.getLanguage()), "-1", Emoji.fromUnicode("❌"));

                    for (int i = 3; i <= 7; i++) {
                        menu.addOption(i + " " + Translations.string(Messages.COMMAND_SETUP_STEPS_INACTIVITY_DAYS, settings.getLanguage()),
                                String.valueOf(i),
                                daysEmojis.getOrDefault(i, Emoji.fromUnicode("❓")));
                    }

                    String defaultOption = String.valueOf(settings.getMaxInactivity());
                    rows.add(ActionRow.of(menu.setDefaultValues(defaultOption).build()));
                    rows.add(ActionRow.of(Button.secondary("setup:inactivity_impl", continueButton)));
                }
                case "inactivity_impl" -> {
                    e.deferReply(true).queue();

                    completeSetup(e.getGuild(), e.getHook(), settings);
                }
                default -> {
                    return null;
                }
            }

            if (e.getInteraction().isAcknowledged()) {
                e.getMessage().delete().queue();
            }

            if (content != null) {
                e.editMessage(builder.setContent(content)
                                .setAllowedMentions(Collections.emptyList())
                                .setComponents(rows)
                                .build())
                        .setReplace(true)
                        .queue();
            }
        }

        return null;
    }

    @Override
    public VC onModalInteraction(VC vc, Settings settings, String id, ModalInteractionEvent e) {
        String[] args = id.split(":");
        if (args.length >= 2) {
            String element = args[1];
            if (element.equals("embed_impl")) {
                ModalMapping titleMapping = e.getValue("title");
                ModalMapping descMapping = e.getValue("desc");

                if (titleMapping != null) {
                    settings.setTitle(titleMapping.getAsString());
                }

                if (descMapping != null) {
                    settings.setDescriptionRaw(descMapping.getAsString());
                }

                // Se il menu è già stato mandato, aggiornalo
                if (!settings.getMenuID().equals("-1")) {
                    Bot.getInstance().getCore().updateMenu(e.getGuild(), settings);
                }

                Bot.getInstance().getCore().getSettingsMapper().update(settings);

                e.reply(Translations.string(Messages.COMMAND_SETUP_STEPS_INTERFACE_UPDATED, settings.getLanguage()))
                        .setEphemeral(true)
                        .queue();
            }
        }

        return null;
    }

    @Override
    public VC onStringSelected(VC vc, Settings settings, String id, StringSelectInteractionEvent e) {
        String[] args = id.split(":");
        if (args.length >= 2) {
            String element = args[1];
            if (element.equals("inactivity")) {
                List<String> selections = e.getValues();
                if (selections.isEmpty()) return null;

                String days = selections.get(0);
                try {
                    settings.setMaxInactivity(Long.parseLong(days));
                    Bot.getInstance().getCore().getSettingsMapper().update(settings);
                } catch (NumberFormatException ignored) {
                    // ignorala
                }

                e.getMessage().delete().queue();
                e.deferReply(true).queue();

                completeSetup(e.getGuild(), e.getHook(), settings);
            }
        }

        return null;
    }

    @Override
    public VC onEntitySelected(VC vc, Settings settings, String id, EntitySelectInteractionEvent e) {
        String[] args = id.split(":");
        if (args.length >= 2) {
            String element = args[1];
            if (element.equals("role")) {
                List<Role> roles = e.getMentions().getRoles();
                if (roles.isEmpty()) return null;

                updatePublicRole(e, settings, roles.get(0));
                e.editMessage(Translations.string(
                        Messages.COMMAND_SETUP_STEPS_ROLE_CONTENT,
                        settings.getLanguage(),
                        roles.get(0).getAsMention())
                ).queue();
            }
        }

        return null;
    }

    private void updatePublicRole(IReplyCallback event, Settings settings, Role newRole) {
        Perms.updatePublicPerms(event.getGuild(), settings, event.getGuild().getRoleById(settings.getPublicRole()), newRole);

        settings.setPublicRole(newRole.getId());
        Bot.getInstance().getCore().getSettingsMapper().update(settings);
    }

    private void completeSetup(Guild guild, InteractionHook hook, Settings settings) {
        Consumer<Throwable> errorHandler = throwable -> {
            BotLogger.error("Something went wrong (%s) while setting up the guild '%s'",
                    throwable.getMessage(),
                    guild.getName());

            hook.sendMessage(">>> " + Translations.string(Messages.GENERIC_ERROR, settings.getLanguage()))
                    .setEphemeral(true)
                    .queue();
        };

        Category mainCategory = settings.getMainCategory(guild);
        if (mainCategory == null) { // Se non c'è una categoria significa che dobbiamo fare il setup completo
            if (!settings.getCategories().isEmpty()) {
                settings.getCategories().clear();
            }

            Role usersRole = guild.getRoleById(settings.getPublicRole());
            if (usersRole != null) {
                // Vieta la visione a @everyone se non è il ruolo pubblico scelto
                Role everyone = guild.getPublicRole();
                boolean excludeEveryone = !everyone.getId().equals(usersRole.getId());
                long selfID = Bot.getInstance().getJda().getSelfUser().getIdLong();

                Category category = settings.createCategory(guild);
                if (category != null) {
                    // Dashboard textchannel
                    ChannelAction<TextChannel> createDashboard = category.createTextChannel("dashboard")
                            .addMemberPermissionOverride(selfID,
                                    List.of(Permission.MESSAGE_MANAGE, Permission.MESSAGE_SEND),
                                    Collections.emptyList())
                            // Users' permissions
                            .addRolePermissionOverride(usersRole.getIdLong(), Perms.dashboardAllowedPerms, Perms.dashboardDeniedPerms);

                    // Room generator voicechannel
                    ChannelAction<VoiceChannel> createGenerator = category.createVoiceChannel("create")
                            // Bot's permissions
                            .addMemberPermissionOverride(selfID, Perms.voiceSelfPerms, Collections.emptyList())
                            // Users' permissions
                            .addRolePermissionOverride(usersRole.getIdLong(), Perms.createAllowedPerms, Perms.createDeniedPerms);

                    if (excludeEveryone) {
                        createDashboard.addRolePermissionOverride(everyone.getIdLong(),
                                0,
                                Permission.ALL_PERMISSIONS);

                        createGenerator.addRolePermissionOverride(everyone.getIdLong(),
                                List.of(Permission.VOICE_SPEAK, Permission.VOICE_USE_VAD),
                                Collections.singletonList(Permission.VIEW_CHANNEL));
                    }

                    // Crea tutti i canali
                    createDashboard.queue(textChannel -> {
                        settings.setMenuChannelID(textChannel.getId());

                        createGenerator.queue(voiceChannel -> {
                            settings.setVoiceGeneratorID(voiceChannel.getId());

                            // Aggiorna la cache e il documento nel db
                            Bot.getInstance().getCore().getSettingsMapper().update(settings);

                            // Manda il menu nel canale testuale
                            MessageCreateData data = Bot.getInstance().getCore().createMenu(guild.getId());
                            if (data != null) {
                                textChannel.sendMessage(data).queue(message -> {
                                    settings.setMenuID(message.getId());
                                    Bot.getInstance().getCore().getSettingsMapper().update(settings);
                                });
                            } else {
                                textChannel.sendMessage(new MessageCreateBuilder()
                                                .setContent(Translations.string(Messages.COMMAND_SETUP_MENU_ERROR, settings.getLanguage()))
                                                .build())
                                        .queue();
                            }

                            // Manda il feedback all'utente
                            hook.sendMessage(">>> " + Translations.string(Messages.COMMAND_SETUP_SUCCESS, settings.getLanguage()))
                                    .setEphemeral(true)
                                    .queue();
                        }, errorHandler);
                    }, errorHandler);

                } else {
                    errorHandler.accept(new Exception("Category creation failed"));
                }
            } else {
                errorHandler.accept(new Exception("Public role does not exist!"));
            }
        } else {
            // Se arriviamo qua significa che deve essere aggiornata solo la maxInactivity
            // quindi facciamo partire un check forzato per rendere effettivi i cambiamenti da subito
            Bot.getInstance().getCore().getActivityManager().forceCheck(guild);

            hook.sendMessage(">>> " + Translations.string(Messages.COMMAND_SETUP_SUCCESS, settings.getLanguage()))
                    .setEphemeral(true)
                    .queue();
        }
    }

    @Override
    public boolean inMenu() {
        return false;
    }

    @Override
    public boolean needsVC() {
        return false;
    }

    @Override
    public String getName() {
        return "setup";
    }

    @Override
    public String getDescription(String lang) {
        return Translations.string(Messages.COMMAND_SETUP_DESCRIPTION, lang);
    }
}
