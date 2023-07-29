package dev.starless.maggiordomo.commands.interaction;

import dev.starless.maggiordomo.commands.types.Interaction;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.user.VC;
import dev.starless.maggiordomo.utils.PageUtils;
import dev.starless.maggiordomo.utils.discord.Embeds;
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
import java.util.List;

public class KickInteraction implements Interaction {

    @Override
    public VC onButtonInteraction(VC vc, Settings settings, String id, ButtonInteractionEvent e) {
        VoiceChannel voiceChannel = e.getGuild().getVoiceChannelById(vc.getChannel());
        if (voiceChannel != null) {
            List<Member> joinedMembers = voiceChannel.getMembers();
            if (joinedMembers.isEmpty()) {
                e.replyEmbeds(Embeds.errorEmbed("Non c'è nessuno nella tua stanza!"))
                        .setEphemeral(true)
                        .queue();
            } else {
                int page = PageUtils.getPageFromId(id);
                if (page == -1) {
                    e.replyEmbeds(Embeds.errorEmbed("An internal error has occurred.\nThis should never happen!"))
                            .setEphemeral(true)
                            .queue();

                    return null;
                }
                StringSelectMenu.Builder builder = StringSelectMenu.create(getName());
                final int offset = page * 25;
                final int limit = Math.min(offset + 25, joinedMembers.size());
                for (int i = offset; i < limit; i++) {
                    Member member = joinedMembers.get(i);
                    if (member == null) continue;

                    String trimmedUsername = member.getEffectiveName().length() > 100
                            ? member.getEffectiveName().substring(0, 100)
                            : member.getEffectiveName();

                    builder = builder.addOption(trimmedUsername, member.getId());
                }

                int maxPages = (int) Math.ceil(joinedMembers.size() / 25D);
                String content = """
                        Seleziona un utente.
                        *Pagina (%d/%d)*"""
                        .formatted(page + 1, maxPages);

                Button backButton = PageUtils.getBackButton(getName(), page);
                Button nextButton = PageUtils.getNextButton(getName(), maxPages, page);

                e.reply(new MessageCreateBuilder()
                                .setContent(content)
                                .addComponents(ActionRow.of(builder.build()))
                                .addComponents(ActionRow.of(backButton, nextButton))
                                .build())
                        .setEphemeral(true)
                        .queue();
            }

            return vc;
        } else {
            e.replyEmbeds(Embeds.errorEmbed("Non c'è nessuno nella tua stanza!"))
                    .setEphemeral(true)
                    .queue();
        }

        return null;
    }

    @Override
    public VC onStringSelected(VC vc, Settings settings, String id, StringSelectInteractionEvent e) {
        String label = e.getValues().get(0);
        if (label != null) {
            VoiceChannel channel = e.getGuild().getVoiceChannelById(vc.getChannel());
            if (channel != null) {
                channel.getMembers().stream()
                        .filter(member -> member.getId().equals(label))
                        .findFirst()
                        .ifPresent(member -> e.getGuild().kickVoiceMember(member).queue(unused ->
                                e.replyEmbeds(new EmbedBuilder()
                                                .setDescription(String.format("L'utente %s è stato kickato dalla stanza! :dash:", member.getEffectiveName()))
                                                .setColor(new Color(239, 210, 95))
                                                .build())
                                        .setEphemeral(true)
                                        .queue()));
            } else {
                e.replyEmbeds(Embeds.errorEmbed("Stanza non trovata!"))
                        .setEphemeral(true)
                        .queue();
            }
        } else {
            e.replyEmbeds(Embeds.errorEmbed("Non hai selezionato niente? :face_with_spiral_eyes:"))
                    .setEphemeral(true)
                    .queue();
        }

        return null;
    }

    @Override
    public Emoji emoji() {
        return Emoji.fromUnicode("👢");
    }

    @Override
    public String getName() {
        return "kick";
    }
}
