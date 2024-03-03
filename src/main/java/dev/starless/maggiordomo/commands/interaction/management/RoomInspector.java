package dev.starless.maggiordomo.commands.interaction.management;

import dev.starless.maggiordomo.Bot;
import dev.starless.maggiordomo.Core;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.enums.UserState;
import dev.starless.maggiordomo.data.filter.FilterResult;
import dev.starless.maggiordomo.data.VC;
import dev.starless.maggiordomo.localization.Messages;
import dev.starless.maggiordomo.localization.Translations;
import dev.starless.maggiordomo.storage.vc.LocalVCMapper;
import dev.starless.maggiordomo.utils.PageUtils;
import dev.starless.maggiordomo.utils.discord.Perms;
import dev.starless.mongo.api.QueryBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
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

import java.util.*;

public class RoomInspector extends AManagementInteraction {

    @Override
    protected MessageEditBuilder handle(Core core, Settings settings, String[] parts, ButtonInteractionEvent e) {
        if (parts.length < 2) return null;

        String id = parts[0];
        String action = parts[1];

        LocalVCMapper mapper = core.getChannelMapper().getMapper(e.getGuild().getId());
        Optional<VC> op = mapper.searchByID(QueryBuilder.init()
                .add("guild", e.getGuild().getId())
                .add("channel", id)
                .create());
        if (op.isEmpty()) {
            return new MessageEditBuilder()
                    .setContent(Translations.string(Messages.COMMAND_MANAGEMENT_ROOMS_NOT_AVAILABLE, settings.getLanguage()))
                    .setActionRow(PageUtils.getShortBackButton("manage", settings.getLanguage()));
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

                Messages message = vc.isPinned() ? Messages.COMMAND_MANAGEMENT_ROOMS_FEEDBACK_PIN : Messages.COMMAND_MANAGEMENT_ROOMS_FEEDBACK_UNPIN;
                e.reply(Translations.string(message, settings.getLanguage()))
                        .setEphemeral(true)
                        .queue();
            }
            case "delete" -> {
                VoiceChannel channel = e.getGuild().getVoiceChannelById(vc.getChannel());
                if (channel != null) {
                    mapper.scheduleForDeletion(vc, channel).queue(success -> mapper.delete(vc));
                    e.reply(Translations.string(Messages.COMMAND_MANAGEMENT_ROOMS_FEEDBACK_DELETE, settings.getLanguage()))
                            .setEphemeral(true)
                            .queue();
                }
            }
        }

        return getInspectMenu(e.getGuild(), id, settings.getLanguage());
    }

    @Override
    public VC onStringSelected(Core core, VC vc, Settings settings, String id, StringSelectInteractionEvent e) {
        List<String> values = e.getValues();
        if (!values.isEmpty()) {
            if (id.contains(":")) {
                String[] parts = id.split(":");
                if (parts.length >= 3) {
                    String channel = parts[1];
                    String action = parts[2];

                    LocalVCMapper mapper = core.getChannelMapper().getMapper(e.getGuild().getId());
                    Optional<VC> op = mapper.searchByID(QueryBuilder.init()
                            .add("guild", e.getGuild().getId())
                            .add("channel", channel)
                            .create());

                    if (op.isPresent()) {
                        VC targetVC = op.get();
                        String memberTargetID = values.get(0);
                        boolean modifyTrusted = action.equals("trust");

                        targetVC.removePlayerRecord(modifyTrusted ? UserState.TRUST : UserState.BAN, memberTargetID);
                        if (targetVC.hasChannel()) {
                            Member targetMember = e.getGuild().getMemberById(memberTargetID);
                            if (targetMember != null) {
                                VoiceChannel voiceChannel = e.getGuild().getVoiceChannelById(targetVC.getChannel());
                                if (voiceChannel != null) {
                                    Perms.reset(targetMember, voiceChannel.getManager());
                                }
                            }
                        }
                        mapper.update(targetVC);

                        e.getMessage().editMessage(getInspectMenu(e.getGuild(), targetVC.getChannel(), settings.getLanguage()).build())
                                .setAllowedMentions(Collections.emptyList())
                                .setReplace(true)
                                .queue();

                        e.reply(Translations.string(Messages.COMMAND_MANAGEMENT_LISTS_USER_REMOVED, settings.getLanguage()))
                                .setEphemeral(true)
                                .queue();

                        return null;
                    }
                }
            }
        }

        e.editMessage(getInspectMenu(e.getGuild(), values.get(0), settings.getLanguage()).build())
                .setAllowedMentions(Collections.emptyList())
                .setReplace(true)
                .queue();

        return null;
    }

    @Override
    public VC onModalInteraction(Core core, VC vc, Settings settings, String id, ModalInteractionEvent e) {
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

            VoiceChannel targetVoiceChannel = e.getGuild().getVoiceChannelById(targetVC.getChannel());
            if (targetVoiceChannel != null) {
                targetVoiceChannel.getManager().setName(result.data()).queue();
            }
            targetVC.setTitle(result.data());

            e.getMessage().editMessage(getInspectMenu(e.getGuild(), targetVC.getChannel(), settings.getLanguage()).build())
                    .setAllowedMentions(Collections.emptyList())
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

    private MessageEditBuilder getInspectMenu(Guild guild, String channel, String language) {
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
            content = Translations.stringFormatted(Messages.COMMAND_MANAGEMENT_ROOMS_INSPECTION_MENU, language,
                    "name", vc.getTitle(),
                    "owner", vc.getUser(),
                    "type", vc.isPinned() ? "📌" : "⏳",
                    "trusted", vc.getTrusted().size(),
                    "banned", vc.getBanned().size(),
                    "lastJoin", TimeFormat.fromStyle("D").atInstant(vc.getLastJoin()));

            rows.add(ActionRow.of(PageUtils.getShortBackButton("manage", language)));

            String bttLabel = Translations.string(vc.isPinned() ? Messages.COMMAND_MANAGEMENT_ROOMS_BUTTONS_UNPIN_LABEL : Messages.COMMAND_MANAGEMENT_ROOMS_BUTTONS_PIN_LABEL, language);
            Button togglePinButton = Button.secondary("inspector:" + vc.getChannel() + ":togglepin", bttLabel);
            rows.add(ActionRow.of(
                    Button.primary("inspector:" + vc.getChannel() + ":title", Translations.string(Messages.COMMAND_MANAGEMENT_ROOMS_BUTTONS_TITLE_LABEL, language)),
                    togglePinButton,
                    Button.danger("inspector:" + vc.getChannel() + ":delete", Translations.string(Messages.COMMAND_MANAGEMENT_ROOMS_BUTTONS_DELETE_LABEL, language))
            ));

            List<ItemComponent> lastRow = new ArrayList<>();
            Set<String> trusted = vc.getTrusted();
            Set<String> banned = vc.getBanned();
            if (!trusted.isEmpty()) {
                lastRow.add(createUserMenu("trust", vc.getChannel(),
                        Translations.string(Messages.COMMAND_MANAGEMENT_ROOMS_DROPDOWNS_TRUSTED_PLACEHOLDER, language),
                        language, guild, trusted));
            }
            if (!banned.isEmpty()) {
                lastRow.add(createUserMenu("ban", vc.getChannel(),
                        Translations.string(Messages.COMMAND_MANAGEMENT_ROOMS_DROPDOWNS_BANNED_PLACEHOLDER, language),
                        language, guild, banned));
            }

            if (!lastRow.isEmpty()) {
                rows.add(ActionRow.of(lastRow));
            }

        } else {
            content = Translations.string(Messages.COMMAND_MANAGEMENT_ROOMS_NOT_AVAILABLE, language);
            rows.add(ActionRow.of(PageUtils.getShortBackButton("manage", language)));
        }

        return edit.setComponents(rows).setContent(content);
    }

    private StringSelectMenu createUserMenu(String name, String id, String placeholder, String language,
                                            Guild guild, Set<String> records) {
        StringSelectMenu.Builder builder = StringSelectMenu.create("inspector:" + id + ":" + name);
        records.forEach(record -> {
            User user = guild.getJDA().getUserById(record);
            String username;
            String nickname;

            if (user != null) {
                username = user.getName();

                Member member = guild.getMember(user);
                if (member != null) {
                    nickname = member.getEffectiveName();
                } else {
                    nickname = Translations.string(Messages.COMMAND_MANAGEMENT_ROOMS_DEFAULT_NICKNAME, language);
                }
            } else {
                username = Translations.string(Messages.COMMAND_MANAGEMENT_ROOMS_DEFAULT_USERNAME, language);
                nickname = Translations.string(Messages.COMMAND_MANAGEMENT_ROOMS_DEFAULT_NICKNAME, language);
            }

            builder.addOption(nickname, record, Translations.string(Messages.COMMAND_MANAGEMENT_ROOMS_AKA, language) + " @" + username);
        });

        return builder.setPlaceholder(placeholder).build();
    }
}
