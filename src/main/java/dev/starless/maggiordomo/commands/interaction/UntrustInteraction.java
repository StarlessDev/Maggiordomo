package dev.starless.maggiordomo.commands.interaction;

import dev.starless.maggiordomo.commands.types.Interaction;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.enums.RecordType;
import dev.starless.maggiordomo.data.user.UserRecord;
import dev.starless.maggiordomo.data.user.VC;
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
            e.replyEmbeds(Embeds.errorEmbed("An internal error has occurred.\nThis should never happen!"))
                    .setEphemeral(true)
                    .queue();

            return null;
        }

        Set<UserRecord> records = vc.getTotalRecords();
        int recordsNumber = records.size();

        MessageCreateBuilder builder = new MessageCreateBuilder();
        if (recordsNumber == 0) {
            builder.setContent("*Non ci sono utenti con i permessi* :sob:");
        } else {
            StringSelectMenu.Builder menuBuilder = StringSelectMenu.create(getName())
                    .setPlaceholder("Utente")
                    .setMaxValues(1);

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
            if (menuBuilder.getOptions().size() == 0) {
                builder.setContent("*Non ci sono utenti trustati* :rainbow:");
            } else {
                int maxPages = (int) Math.ceil(recordsNumber / 10D);
                Button backButton = PageUtils.getBackButton(getName(), page);
                Button nextButton = PageUtils.getNextButton(getName(), maxPages, page);

                builder.setContent("Scegli un utente :point_down:")
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
    public VC onStringSelected(VC vc, Settings guild, String id, StringSelectInteractionEvent e) {
        String memberId = e.getValues().get(0);
        if (memberId != null) {
            Member member = e.getGuild().getMemberById(memberId);
            if (member == null) {
                vc.removeRecordPlayer(RecordType.TRUST, member.getId());

                e.replyEmbeds(Embeds.errorEmbed("Questo utente non è più nel server"))
                        .setEphemeral(true)
                        .queue();

                return vc;
            }

            vc.removeRecordPlayer(RecordType.TRUST, member.getId());

            // Rispondi alla richiesta
            e.replyEmbeds(new EmbedBuilder()
                            .setDescription(String.format("Hai smesso di fidarti di %s e gli hai tolto i permessi.", member.getEffectiveName()))
                            .setColor(new Color(100, 160, 94))
                            .build())
                    .setEphemeral(true)
                    .queue();

            VoiceChannel channel = e.getGuild().getVoiceChannelById(vc.getChannel());
            if (channel != null) Perms.reset(member, channel.getManager());

            return vc;
        } else {
            e.replyEmbeds(Embeds.errorEmbed())
                    .setEphemeral(true)
                    .queue();
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
