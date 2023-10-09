package dev.starless.maggiordomo.commands.interaction.management;

import dev.starless.maggiordomo.Bot;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.enums.RecordType;
import dev.starless.maggiordomo.data.filter.FilterResult;
import dev.starless.maggiordomo.data.user.UserRecord;
import dev.starless.maggiordomo.data.user.VC;
import dev.starless.maggiordomo.localization.Messages;
import dev.starless.maggiordomo.localization.Translations;
import dev.starless.maggiordomo.storage.vc.LocalVCMapper;
import dev.starless.maggiordomo.utils.PageUtils;
import dev.starless.maggiordomo.utils.discord.Perms;
import dev.starless.mongo.api.QueryBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.dv8tion.jda.api.utils.TimeFormat;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.util.*;

public class RoomInspector extends AManagementInteraction {

    @Override
    protected MessageEditData handle(ButtonInteractionEvent e, Settings settings, String[] parts) {
        if (parts.length < 2) return null;

        String id = parts[0];
        String action = parts[1];

        LocalVCMapper mapper = Bot.getInstance().getCore().getChannelMapper().getMapper(e.getGuild().getId());
        Optional<VC> op = mapper.searchByID(QueryBuilder.init()
                .add("guild", e.getGuild().getId())
                .add("channel", id)
                .create());
        if (op.isEmpty()) {
            return new MessageEditBuilder()
                    .setContent(">>> Questa stanza non è più disponibile.\nProva a sceglierne un' altra.")
                    .setActionRow(PageUtils.getShortBackButton("manage", settings.getLanguage()))
                    .build();
        }

        VC vc = op.get();
        switch (action) {
            case "title" ->
                    e.replyModal(Modal.create("inspector:" + vc.getChannel(), Translations.string(Messages.INTERACTION_TITLE_MODAL_TITLE, settings.getLanguage()))
                                    .addActionRow(TextInput.create("title", Translations.string(Messages.INTERACTION_TITLE_MODAL_INPUT_LABEL, settings.getLanguage()), TextInputStyle.SHORT)
                                            .setRequired(true)
                                            .setRequiredRange(1, 99)
                                            .setValue(vc.getTitle())
                                            .setPlaceholder(Translations.string(Messages.INTERACTION_TITLE_MODAL_INPUT_PLACEHOLDER, settings.getLanguage(), e.getUser().getName()))
                                            .build())
                                    .build())
                            .queue();
            case "togglepin" -> {
                mapper.togglePinStatus(e.getGuild(), settings, vc);
                e.reply("La stanza ora è " + (vc.isPinned() ? "fissata" : "temporanea"))
                        .setEphemeral(true)
                        .queue();
            }
            case "delete" -> {
                VoiceChannel channel = e.getGuild().getVoiceChannelById(vc.getChannel());
                if (channel != null) {
                    mapper.scheduleForDeletion(vc, channel).queue(success -> mapper.delete(vc));
                    e.reply("La stanza è stata cancellata con successo.")
                            .setEphemeral(true)
                            .queue();
                }
            }
        }

        return getInspectMenu(e.getGuild(), id, settings.getLanguage());
    }

    @Override
    public VC onStringSelected(VC vc, Settings settings, String id, StringSelectInteractionEvent e) {
        e.deferReply().queue(hook -> hook.deleteOriginal().queue());

        List<String> values = e.getValues();
        if (!values.isEmpty()) {
            if (id.contains(":")) {
                String[] parts = id.split(":");
                if (parts.length >= 3) {
                    String channel = parts[1];
                    String action = parts[2];

                    LocalVCMapper mapper = Bot.getInstance().getCore().getChannelMapper().getMapper(e.getGuild().getId());
                    Optional<VC> op = mapper.searchByID(QueryBuilder.init()
                            .add("guild", e.getGuild().getId())
                            .add("channel", channel)
                            .create());

                    if (op.isPresent()) {
                        VC targetVC = op.get();
                        String memberTargetID = values.get(0);
                        boolean modifyTrusted = action.equals("trust");

                        vc.removePlayerRecord(modifyTrusted ? RecordType.TRUST : RecordType.BAN, memberTargetID);
                        if (vc.hasChannel()) {
                            Member targetMember = e.getGuild().getMemberById(memberTargetID);
                            if (targetMember != null) {
                                VoiceChannel voiceChannel = e.getGuild().getVoiceChannelById(vc.getChannel());
                                if (voiceChannel != null) {
                                    Perms.reset(targetMember, voiceChannel.getManager());
                                }
                            }
                        }
                        mapper.update(targetVC);

                        e.getMessage().editMessage(getInspectMenu(e.getGuild(), targetVC.getChannel(), settings.getLanguage()))
                                .setAllowedMentions(Collections.emptyList())
                                .setReplace(true)
                                .queue();

                        e.reply("The user is now neutral towards that room.")
                                .setEphemeral(true)
                                .queue();
                    }
                }
            } else {
                e.getMessage().editMessage(getInspectMenu(e.getGuild(), values.get(0), settings.getLanguage()))
                        .setReplace(true)
                        .queue();
            }
        }

        return null;
    }

    @Override
    public VC onModalInteraction(VC vc, Settings settings, String id, ModalInteractionEvent e) {
        ModalMapping mapping = e.getValue("title");
        if (mapping == null) return null;

        String[] parts = id.split(":");
        if (parts.length < 2) return null;

        String channel = parts[1];
        Optional<VC> op = Bot.getInstance().getCore()
                .getChannelMapper()
                .getMapper(e.getGuild().getId())
                .searchByID(QueryBuilder.init()
                        .add("guild", e.getGuild().getId())
                        .add("channel", channel)
                        .create());

        if (op.isPresent()) {
            VC targetVC = op.get();

            FilterResult result = Bot.getInstance().getCore().getFilters().check(settings, mapping.getAsString());
            if (result.flagged()) {
                e.reply(Translations.string(Messages.FILTER_FLAG_PREFIX, settings.getLanguage()) + " " + result.data())
                        .setEphemeral(true)
                        .queue();

                return null;
            }

            VoiceChannel targetVoiceChannel = e.getGuild().getVoiceChannelById(vc.getChannel());
            if (targetVoiceChannel != null) {
                targetVoiceChannel.getManager().setName(result.data()).queue();
            }
            targetVC.setTitle(result.data());

            e.getMessage().editMessage(getInspectMenu(e.getGuild(), targetVC.getChannel(), settings.getLanguage()))
                    .setReplace(true)
                    .queue();
            e.reply(Translations.string(Messages.INTERACTION_TITLE_SUCCESS, settings.getLanguage()))
                    .setEphemeral(true)
                    .queue();

            return targetVC;
        }

        return null;
    }

    @Override
    public String getName() {
        return "inspect";
    }

    private MessageEditData getInspectMenu(Guild guild, String channel, String language) {
        MessageEditBuilder edit = new MessageEditBuilder();
        Optional<VC> op = Bot.getInstance().getCore().getChannelMapper()
                .getMapper(guild.getId())
                .searchByID(QueryBuilder.init()
                        .add("guild", guild.getId())
                        .add("channel", channel)
                        .create());

        List<ActionRow> rows = new ArrayList<>();
        String content;
        if (op.isPresent()) {
            VC vc = op.get();
            String type = vc.isPinned() ? "📌" : "⏳";
            content = """
                    # Inspection 🔎
                    Usa i pulsanti per cambiare le caratteristiche della stanza.
                    Seleziona gli utenti trustati o bannati da rimuovere usando i dropdown.
                    **Attenzione**: cancellando una stanza tutti i dati della stanza andranno persi!
                                        
                    **Proprietà**:
                    - Nome della stanza: `%s`
                    - Proprietario: <@%s>
                    - Tipologia: %s
                    - Utenti trustati: `%d`
                    - Utenti bannati: `%d`
                    - Ultimo join in data: %s"""
                    .formatted(vc.getTitle(), vc.getUser(), type,
                            vc.getTrusted().size(), vc.getBanned().size(), TimeFormat.fromStyle("D").atInstant(vc.getLastJoin()));

            rows.add(ActionRow.of(PageUtils.getShortBackButton("manage", language)));

            String bttLabel = vc.isPinned() ? "Rendi Temporanea" : "Rendi Fissata";
            Button togglePinButton = Button.secondary("inspector:" + vc.getChannel() + ":togglepin", bttLabel);
            rows.add(ActionRow.of(
                    Button.primary("inspector:" + vc.getChannel() + ":title", "Cambia titolo 📝"),
                    togglePinButton,
                    Button.danger("inspector:" + vc.getChannel() + ":delete", "Cancella 🗑")
            ));

            List<ItemComponent> lastRow = new ArrayList<>();
            Set<UserRecord> trusted = vc.getTrusted();
            Set<UserRecord> banned = vc.getBanned();
            if (!trusted.isEmpty()) {
                lastRow.add(createUserMenu("trust", vc.getChannel(), "Utenti trustati", guild, trusted));
            }
            if (!banned.isEmpty()) {
                lastRow.add(createUserMenu("ban", vc.getChannel(), "Utenti bannati", guild, banned));
            }

            if (!lastRow.isEmpty()) {
                rows.add(ActionRow.of(lastRow));
            }

        } else {
            content = ">>> Questa stanza non è più disponibile.\nProva a sceglierne un' altra.";
            rows.add(ActionRow.of(PageUtils.getShortBackButton("manage", language)));
        }

        return edit.setComponents(rows)
                .setContent(content)
                .build();
    }

    private StringSelectMenu createUserMenu(String name, String id, String placeholder, Guild
            guild, Set<UserRecord> records) {
        StringSelectMenu.Builder builder = StringSelectMenu.create("inspector:" + id + ":" + name);
        records.forEach(record -> {
            String nickname = "Somebody";
            String username = "@unknown";

            Member member = guild.getMemberById(record.user());
            if (member != null) {
                username = member.getUser().getName();
                nickname = Objects.requireNonNullElse(member.getNickname(), username);
            }

            builder.addOption(nickname, record.user(), "aka @" + username);
        });

        return builder.setPlaceholder(placeholder).build();
    }
}
