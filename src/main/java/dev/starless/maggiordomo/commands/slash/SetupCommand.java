package dev.starless.maggiordomo.commands.slash;

import dev.starless.maggiordomo.Bot;
import dev.starless.maggiordomo.commands.CommandInfo;
import dev.starless.maggiordomo.commands.types.Interaction;
import dev.starless.maggiordomo.commands.types.Slash;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.user.VC;
import dev.starless.maggiordomo.utils.discord.Perms;
import dev.starless.maggiordomo.utils.discord.References;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
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
import java.util.concurrent.atomic.AtomicBoolean;

@CommandInfo(name = "setup", description = "Crea la categoria dedicata alle stanze")
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
    public VC execute(VC vc, Settings settings, String id, ButtonInteractionEvent e) {
        String[] args = id.split(":");
        if (args.length >= 2) {
            String element = args[1];
            String content;

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

                    if (!settings.getPublicRole().equals("-1")) {
                        content += "\nIl ruolo pubblico attuale è " + References.role(e.getGuild(), settings.getPublicRole());
                        rows.add(ActionRow.of(Button.secondary("setup:embed", "Continua ➡")));
                    }

                    rows.add(ActionRow.of(roleSelector));
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

                    rows.add(ActionRow.of(menu.build()));
                }
                default -> {
                    return null;
                }
            }

            e.deferReply().queue(success -> e.getInteraction().getHook().deleteOriginal().queue());
            e.getMessage().editMessage(builder.setContent(content)
                            .setComponents(rows)
                            .build())
                    .setReplace(true)
                    .queue();
        }

        return null;
    }

    @Override
    public VC execute(VC vc, Settings settings, String id, ModalInteractionEvent e) {
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

                Bot.getInstance().getCore().getSettingsMapper().update(settings);
            }
        }

        return null;
    }

    @Override
    public VC execute(VC vc, Settings settings, String id, StringSelectInteractionEvent e) {
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

                String result = setupChannels(e.getGuild(), settings)
                        ? "Setup completato! :white_check_mark:"
                        : "Qualcosa è andato storto! Riprova. :x:";

                e.getHook().sendMessage(result)
                        .setEphemeral(true)
                        .queue();
            }
        }

        return null;
    }

    @Override
    public VC execute(VC vc, Settings settings, String id, EntitySelectInteractionEvent e) {
        String[] args = id.split(":");
        if (args.length >= 2) {
            String element = args[1];
            if (element.equals("role")) {
                List<Role> roles = e.getMentions().getRoles();
                if (roles.isEmpty()) return null;

                Message message = e.getMessage();
                Role role = roles.get(0);
                settings.setPublicRole(role.getId());
                Bot.getInstance().getCore().getSettingsMapper().update(settings);

                e.reply("Ruolo aggiornato :white_check_mark:")
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

    @Override
    public boolean needsVC() {
        return false;
    }

    private boolean setupChannels(Guild guild, Settings settings) {
        AtomicBoolean bool = new AtomicBoolean(false);
        if (settings.getCategories().isEmpty()) {
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
                            .addRolePermissionOverride(usersRole.getIdLong(),
                                    Collections.singletonList(Permission.VIEW_CHANNEL),
                                    List.of(Permission.MESSAGE_SEND,
                                            Permission.MESSAGE_SEND_IN_THREADS,
                                            Permission.CREATE_PUBLIC_THREADS,
                                            Permission.CREATE_PRIVATE_THREADS,
                                            Permission.MESSAGE_SEND_IN_THREADS,
                                            Permission.USE_APPLICATION_COMMANDS));

                    // Room generator voicechannel
                    ChannelAction<VoiceChannel> createGenerator = category.createVoiceChannel("create")
                            // Bot's permissions
                            .addMemberPermissionOverride(selfID,
                                    Arrays.asList(Perms.selfPerms),
                                    Collections.emptyList())
                            // Users' permissions
                            .addRolePermissionOverride(usersRole.getIdLong(),
                                    List.of(Permission.VIEW_CHANNEL,
                                            Permission.VOICE_CONNECT,
                                            Permission.VOICE_MOVE_OTHERS),
                                    List.of(Permission.MESSAGE_SEND,
                                            Permission.MESSAGE_SEND_IN_THREADS,
                                            Permission.CREATE_PUBLIC_THREADS,
                                            Permission.CREATE_PRIVATE_THREADS,
                                            Permission.MESSAGE_SEND_IN_THREADS,
                                            Permission.VOICE_SPEAK,
                                            Permission.VOICE_STREAM,
                                            Permission.USE_APPLICATION_COMMANDS));

                    if (excludeEveryone) {
                        createDashboard.addRolePermissionOverride(everyone.getIdLong(),
                                0,
                                Permission.ALL_PERMISSIONS);

                       createGenerator.addRolePermissionOverride(everyone.getIdLong(),
                                List.of(Permission.VOICE_SPEAK, Permission.VOICE_USE_VAD),
                                Collections.singletonList(Permission.VIEW_CHANNEL));
                    }

                    createDashboard.submit()
                            .thenCompose(textChannel -> {
                                Bot.getInstance().getCore().sendMenu(textChannel);
                                settings.setChannelID(textChannel.getId());

                                return createGenerator.submit();
                            })
                            .whenComplete((voiceChannel, throwable) -> {
                                if(throwable == null) {
                                    settings.setVoiceID(voiceChannel.getId());
                                    bool.set(true);

                                    // Update the document in the db if everything goes smoothly
                                    Bot.getInstance().getCore().getSettingsMapper().update(settings);
                                }
                            });
                }
            }
        }

        return bool.get();
    }
}
