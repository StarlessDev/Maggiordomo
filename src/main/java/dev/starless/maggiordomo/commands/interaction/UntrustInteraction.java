package dev.starless.maggiordomo.commands.interaction;

import dev.starless.maggiordomo.commands.types.Interaction;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.enums.RecordType;
import dev.starless.maggiordomo.data.user.UserRecord;
import dev.starless.maggiordomo.data.user.VC;
import dev.starless.maggiordomo.localization.Translations;
import dev.starless.maggiordomo.localization.Messages;
import dev.starless.maggiordomo.utils.PageUtils;
import dev.starless.maggiordomo.utils.discord.Embeds;
import dev.starless.maggiordomo.utils.discord.Perms;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

import java.awt.*;
import java.util.Set;

public class UntrustInteraction implements Interaction {

    @Override
    @SuppressWarnings("DuplicatedCode")
    public VC onButtonInteraction(VC vc, Settings settings, String id, ButtonInteractionEvent e) {
        int page = PageUtils.getPageFromId(id);
        if(page == -1) {
            e.replyEmbeds(Embeds.errorEmbed(Translations.string(Messages.GENERIC_ERROR, settings.getLanguage())))
                    .setEphemeral(true)
                    .queue();

            return null;
        }

        Set<UserRecord> records = vc.getTotalRecords();
        int recordsNumber = records.size();

        MessageCreateBuilder builder = new MessageCreateBuilder();
        if (recordsNumber == 0) {
            builder.setContent(Translations.string(Messages.INTERACTION_UNTRUST_EMPTY, settings.getLanguage()));
        } else {
            StringSelectMenu.Builder menuBuilder = StringSelectMenu.create(getName())
                    .setPlaceholder(Translations.string(Messages.USER_SELECTION_PLACEHOLDER, settings.getLanguage()));

            records.stream()
                    .filter(record -> record.type().equals(RecordType.TRUST))
                    .skip(10L * page)
                    .limit(10)
                    .forEach(record -> {
                        Member member = e.getGuild().getMemberById(record.user());
                        if (member == null) return;

                        menuBuilder.addOption(member.getEffectiveName(), member.getId());
                    });

            // Credo che a volte, se gli utenti sono bannati ed escono dal server,
            // i membri non vengono trovati e JDA lancia una exception
            if (menuBuilder.getOptions().isEmpty()) {
                builder.setContent(Translations.string(Messages.INTERACTION_UNTRUST_EMPTY, settings.getLanguage()));
            } else {
                int maxPages = (int) Math.ceil(recordsNumber / 10D);
                Button backButton = PageUtils.getBackButton(getName(), page, settings.getLanguage());
                Button nextButton = PageUtils.getNextButton(getName(), maxPages, page, settings.getLanguage());

                builder.setContent(Translations.string(Messages.USER_SELECTION_MESSAGE_CONTENT, settings.getLanguage()))
                        .addComponents(ActionRow.of(menuBuilder.build()))
                        .addComponents(ActionRow.of(backButton, nextButton));
            }
        }

        e.reply(builder.build())
                .setEphemeral(true)
                .queue();

        return null;
    }

    @Override
    public VC onStringSelected(VC vc, Settings settings, String id, StringSelectInteractionEvent e) {
        if (!e.getValues().isEmpty()) {
            String memberId = e.getValues().get(0);
            Member member = e.getGuild().getMemberById(memberId);
            if (member == null) {
                vc.removeRecordPlayer(RecordType.TRUST, member.getId());

                e.replyEmbeds(Embeds.errorEmbed(Translations.string(Messages.MEMBER_NOT_FOUND, settings.getLanguage())))
                        .setEphemeral(true)
                        .queue();

                return vc;
            }

            vc.removeRecordPlayer(RecordType.TRUST, member.getId());

            // Rispondi alla richiesta
            e.replyEmbeds(new EmbedBuilder()
                            .setDescription(Translations.string(Messages.INTERACTION_UNTRUST_SUCCESS, settings.getLanguage(), member.getEffectiveName()))
                            .setColor(new Color(100, 160, 94))
                            .build())
                    .setEphemeral(true)
                    .queue();

            VoiceChannel channel = e.getGuild().getVoiceChannelById(vc.getChannel());
            if (channel != null) Perms.reset(member, channel.getManager());

            return vc;
        }

        return null;
    }

    @Override
    public Emoji emoji() {
        return Emoji.fromUnicode("U+1F595");
    }

    @Override
    public String getName() {
        return "untrust";
    }
}
