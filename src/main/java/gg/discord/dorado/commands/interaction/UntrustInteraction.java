package gg.discord.dorado.commands.interaction;

import gg.discord.dorado.commands.CommandInfo;
import gg.discord.dorado.commands.types.Interaction;
import gg.discord.dorado.data.Settings;
import gg.discord.dorado.data.enums.RecordType;
import gg.discord.dorado.data.user.PlayerRecord;
import gg.discord.dorado.data.user.VC;
import gg.discord.dorado.utils.PageUtils;
import gg.discord.dorado.utils.discord.Embeds;
import gg.discord.dorado.utils.discord.Perms;
import gg.discord.dorado.utils.discord.RestUtils;
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

@CommandInfo(name = "untrust", description = "Rimuovi il trust ad un utente")
public class UntrustInteraction implements Interaction {

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

        Set<PlayerRecord> records = vc.getTotalRecords();
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
                vc.removeRecordPlayer(RecordType.TRUST, member.getId());

                e.replyEmbeds(Embeds.errorEmbed("Questo utente non è più nel server"))
                        .setEphemeral(true)
                        .queue();

                return vc;
            }

            VoiceChannel channel = e.getGuild().getVoiceChannelById(vc.getChannel());
            if (!vc.hasRecordPlayer(RecordType.TRUST, member.getId())) {
                e.replyEmbeds(Embeds.errorEmbed("Questo utente non è trustato."))
                        .setEphemeral(true)
                        .queue();

                return null;
            }

            boolean isChannelCreated = channel != null;
            vc.removeRecordPlayer(RecordType.TRUST, member.getId());

            // Rispondi alla richiesta
            e.replyEmbeds(new EmbedBuilder()
                            .setDescription(String.format("Hai smesso di fidarti di %s e gli hai tolto i permessi.", member.getEffectiveName()))
                            .setColor(new Color(100, 160, 94))
                            .build())
                    .setEphemeral(true)
                    .queue();

            if (isChannelCreated) Perms.reset(member, channel.getManager());

            // Rimosso messaggio perché era brutto

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
}
