package dev.starless.maggiordomo.commands.slash;

import dev.starless.maggiordomo.Bot;
import dev.starless.maggiordomo.commands.Parameter;
import dev.starless.maggiordomo.commands.types.Interaction;
import dev.starless.maggiordomo.commands.types.Slash;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.user.VC;
import dev.starless.maggiordomo.localization.DefaultLanguages;
import dev.starless.maggiordomo.logging.BotLogger;
import dev.starless.maggiordomo.tasks.ActivityChecker;
import dev.starless.maggiordomo.utils.discord.Perms;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
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
import java.util.stream.Stream;

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
                .setContent("""
                        ## Setup
                        Cosa andrai a personalizzare tra poco:
                        ・ Public role
                        ・ Dettagli della guida: titolo e descrizione dell'embed
                        ・ Giorni di inattività massimi delle stanze fissate
                                                
                        Una volta terminato un passaggio clicca "Continua" per passare al prossimo.""")
                .setActionRow(Button.primary("setup:role", "Inizia 📖"))
                .build();

        e.reply(message).queue();
    }

    @Override
    public VC onButtonInteraction(VC vc, Settings settings, String id, ButtonInteractionEvent e) {
        String[] args = id.split(":");
        if (args.length >= 2) {
            String element = args[1];
            String content = null;

            MessageEditBuilder builder = new MessageEditBuilder();
            List<ActionRow> rows = new ArrayList<>();

            switch (element) {
                case "role" -> {
                    content = """
                            Seleziona il ruolo che tutti gli utenti devono avere per usare il bot (anche everyone è supportato).
                            Se hai già impostato il ruolo, puoi cliccare sul pulsante 'Continua'.""";

                    EntitySelectMenu roleSelector = EntitySelectMenu.create("setup:role", EntitySelectMenu.SelectTarget.ROLE)
                            .setPlaceholder("Seleziona un ruolo")
                            .build();

                    rows.add(ActionRow.of(roleSelector));
                    rows.add(ActionRow.of(Button.secondary("setup:embed", "Continua ➡")));
                    // to be removed: content += "\nIl ruolo pubblico attuale è " + References.role(e.getGuild(), settings.getPublicRole());
                }
                case "embed" -> {
                    content = """
                            Clicca sul pulsante 'Modifica' per cambiare l'embed che viene mostrato.
                            Al momento è disponibile solo un placeholder per la descrizione dell'embed: {CHANNEL} che viene rimpiazzato con la menzione del canale vocale dedicato alla creazione delle stanze.
                            Quando hai fatto, clicca continua per passare al prossimo passaggio.""";

                    rows.add(ActionRow.of(
                            Button.success("setup:embed_preview", "Anteprima 👀"),
                            Button.primary("setup:embed_impl", "Modifica"),
                            Button.secondary("setup:inactivity", "Continua ➡")
                    ));
                }
                case "embed_impl" -> {
                    e.replyModal(Modal.create("setup:embed_impl", "Inserisci")
                                    .addActionRow(TextInput.create("title", "Titolo", TextInputStyle.SHORT)
                                            .setValue(settings.getTitle())
                                            .setMaxLength(128)
                                            .build())
                                    .addActionRow(TextInput.create("desc", "Descrizione", TextInputStyle.PARAGRAPH)
                                            .setValue(settings.getDescriptionRaw())
                                            .setMaxLength(1024)
                                            .build())
                                    .build())
                            .queue();

                    return null;
                }
                case "embed_preview" -> {
                    e.reply("""
                                    Ecco la tua preview:
                                                                
                                    # %s

                                    %s
                                    """.formatted(settings.getTitle(), settings.getDescription()))
                            .setEphemeral(true)
                            .queue();

                    return null;
                }
                case "inactivity" -> {
                    content = """
                            **SOLAMENTE le stanze fissate** hanno una "data di scadenza" per evitare che le categorie si riempiano di stanze inutilizzate.
                            Di default numero di giorni dopo i quali la stanza viene cancellata è -1, cioè la funzione è disabilitata. Puoi scegliere tra 3 a 7 giorni.""";

                    StringSelectMenu.Builder menu = StringSelectMenu.create("setup:inactivity")
                            .setPlaceholder("Cancella la stanza dopo...")
                            .addOption("Disabilita", "-1", Emoji.fromUnicode("❌"));

                    for (int i = 3; i <= 7; i++) {
                        menu.addOption(i + " giorni", String.valueOf(i), daysEmojis.getOrDefault(i, Emoji.fromUnicode("❓")));
                    }

                    String defaultOption = String.valueOf(settings.getMaxInactivity());
                    rows.add(ActionRow.of(menu.setDefaultValues(defaultOption).build()));
                    rows.add(ActionRow.of(Button.secondary("setup:inactivity_impl", "Continua ➡")));
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
            } else {
                e.deferReply().queue(success -> e.getInteraction().getHook().deleteOriginal().queue());
            }

            if(content != null) {
                e.getMessage().editMessage(builder.setContent(content)
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
                    updateMenu(e.getGuild(), settings);
                }

                Bot.getInstance().getCore().getSettingsMapper().update(settings);

                e.reply(">>> Messaggio aggiornato! :white_check_mark:")
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

                Message message = e.getMessage();
                Role oldRole = e.getGuild().getRoleById(settings.getPublicRole());
                Role newRole = roles.get(0);

                Perms.updatePublicPerms(e.getGuild(), settings, oldRole, newRole);

                settings.setPublicRole(newRole.getId());
                Bot.getInstance().getCore().getSettingsMapper().update(settings);

                e.reply(">>> Ruolo aggiornato :white_check_mark:")
                        .setEphemeral(true)
                        .queue(success -> {
                            if (message.getButtons().stream().noneMatch(button -> button.getId() != null && button.getId().endsWith("embed"))) {
                                List<ActionRow> rows = new ArrayList<>(message.getActionRows());
                                rows.add(ActionRow.of(Button.secondary("setup:embed", "Continua ➡")));

                                message.editMessage(MessageEditBuilder.fromMessage(message)
                                                .setComponents(rows)
                                                .build())
                                        .queue();
                            }
                        });
            }
        }

        return null;
    }

    private void updateMenu(Guild guild, Settings settings) {
        TextChannel channel = guild.getTextChannelById(settings.getChannelID());
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
            }, throwable -> BotLogger.warn("Could not update the menu of the guild (no message): " + guild.getName()));
        } else {
            BotLogger.warn("Could not update the menu of the guild (no channel): " + guild.getName());
        }
    }

    private void completeSetup(Guild guild, InteractionHook hook, Settings settings) {
        Consumer<Throwable> errorHandler = throwable -> {
            BotLogger.error("Something went wrong (%s) while setting up the guild '%s'",
                    throwable.getMessage(),
                    guild.getName());

            hook.sendMessage(">>> Qualcosa è andato storto. Riprova! :x:")
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
                            .addMemberPermissionOverride(selfID, Arrays.asList(Perms.voiceSelfPerms), Collections.emptyList())
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
                        settings.setChannelID(textChannel.getId());

                        createGenerator.queue(voiceChannel -> {
                            settings.setVoiceID(voiceChannel.getId());

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
                                                .setContent("""
                                                        Impossibile creare il menu! :x:
                                                        Usa il comando `/maggiordomo setupMenu` in questo canale per riprovare.""")
                                                .build())
                                        .queue();
                            }

                            // Manda il feedback all'utente
                            hook.sendMessage(">>> Setup completato! :white_check_mark:")
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
            new Thread(new ActivityChecker(guild.getId())).start();

            hook.sendMessage(">>> Setup completato! :white_check_mark:")
                    .setEphemeral(true)
                    .queue();
        }
    }

    @Override
    public void autocomplete(Settings settings, CommandAutoCompleteInteractionEvent e) {
        if (e.getFocusedOption().getName().equals("lang")) {
            List<Command.Choice> options = Stream.of(DefaultLanguages.values())
                    .filter(lang -> lang.getName().toLowerCase().startsWith(e.getFocusedOption().getValue().toLowerCase()))
                    .map(lang -> new Command.Choice(lang.getName(), lang.getCode()))
                    .toList();

            e.replyChoices(options).queue();
        }
    }

    @Override
    public Parameter[] getParameters(String lang) {
        return new Parameter[]{ new Parameter(OptionType.STRING, "lang", "Language of the server (default is English)", false)};
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
    public String getDescription() {
        return "Crea la categoria dedicata alle stanze";
    }
}
