package dev.starless.maggiordomo.commands.interaction;

import dev.starless.maggiordomo.commands.CommandInfo;
import dev.starless.maggiordomo.commands.types.Interaction;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.enums.RecordType;
import dev.starless.maggiordomo.data.user.UserRecord;
import dev.starless.maggiordomo.data.user.VC;
import dev.starless.maggiordomo.utils.discord.Embeds;
import dev.starless.maggiordomo.utils.PageUtils;
import dev.starless.maggiordomo.utils.discord.Perms;
import dev.starless.maggiordomo.utils.discord.RestUtils;
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

@CommandInfo(name = "unban", description = "Rimuovi un ban di un utente")
public class UnbanInteraction implements Interaction {

    @Override
    @SuppressWarnings("DuplicatedCode")
    public VC execute(VC vc, Settings settings, String id, ButtonInteractionEvent e) {
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
            builder.setContent("*Non ci sono utenti bannati* :rainbow:");
        } else {
            StringSelectMenu.Builder menuBuilder = StringSelectMenu.create(getName())
                    .setPlaceholder("Utente")
                    .setMaxValues(1);

            records.stream()
                    .filter(record -> record.type().equals(RecordType.BAN))
                    .skip(10L * page)
                    .limit(10)
                    .forEach(record -> {
                        Member member = e.getGuild().getMemberById(record.user());
                        if (member == null) return;

                        menuBuilder.addOption(member.getEffectiveName(), member.getId());
                    });

            int maxPages = (int) Math.ceil(recordsNumber / 10D);
            Button backButton = PageUtils.getBackButton(getName(), page);
            Button nextButton = PageUtils.getNextButton(getName(), maxPages, page);

            builder.setContent("Scegli un utente :point_down:")
                    .addComponents(ActionRow.of(menuBuilder.build()))
                    .addComponents(ActionRow.of(backButton, nextButton));
        }

        e.reply(builder.build())
                .setEphemeral(true)
                .queue();

        return null;
    }

    @Override
    public VC execute(VC vc, Settings guild, String id, StringSelectInteractionEvent e) {
        String memberId = e.getValues().get(0);
        if (memberId != null) {
            Member member = e.getGuild().getMemberById(memberId);
            if (member == null) {
                e.replyEmbeds(Embeds.errorEmbed("Questo utente non è più nel server"))
                        .setEphemeral(true)
                        .queue();

                return vc;
            }

            VoiceChannel channel = e.getGuild().getVoiceChannelById(vc.getChannel());
            if (!vc.hasRecordPlayer(RecordType.BAN, member.getId())) {
                e.replyEmbeds(Embeds.errorEmbed("Questo utente non è bannato"))
                        .setEphemeral(true)
                        .queue();

                return null;
            }

            boolean isChannelCreated = channel != null;
            vc.removeRecordPlayer(RecordType.BAN, member.getId());

            // Rispondi alla richiesta
            e.replyEmbeds(new EmbedBuilder()
                            .setDescription(member.getEffectiveName() + " è stato sbannato dalla stanza.")
                            .setColor(new Color(239, 210, 95))
                            .build())
                    .setEphemeral(true)
                    .queue();

            if (isChannelCreated) Perms.reset(member, channel.getManager());

            // Manda un messaggio all'utente sbannato
            member.getUser().openPrivateChannel()
                    .queue(dm -> dm.sendMessageEmbeds(new EmbedBuilder()
                                            .setTitle("Sei stato sbannato! :tada:")
                                            .setColor(new Color(239, 210, 95))
                                            .setDescription(String.format("""
                                                    %s ti ha rimosso dalla lista dei bannati!
                                                                                                        
                                                    Ora puoi vedere e rientrare nella sua stanza `%s`.
                                                    """, e.getUser().getAsMention(), vc.getTitle()
                                            ))
                                            .build())
                                    .queue(RestUtils.emptyConsumer(), RestUtils.emptyConsumer()),
                            throwable -> RestUtils.emptyConsumer());

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
        return Emoji.fromUnicode("U+2B55");
    }
}
