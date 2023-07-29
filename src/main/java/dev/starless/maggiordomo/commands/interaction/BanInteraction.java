package dev.starless.maggiordomo.commands.interaction;

import dev.starless.maggiordomo.commands.types.Interaction;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.enums.RecordType;
import dev.starless.maggiordomo.data.user.VC;
import dev.starless.maggiordomo.utils.Matcher;
import dev.starless.maggiordomo.utils.discord.Embeds;
import dev.starless.maggiordomo.utils.discord.Perms;
import dev.starless.maggiordomo.utils.discord.RestUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

import java.awt.*;
import java.util.Optional;

public class BanInteraction implements Interaction {

    @Override
    public VC onModalInteraction(VC vc, Settings settings, String id, ModalInteractionEvent e) {
        ModalMapping mapping = e.getValue("ban:id");
        if (mapping == null) {
            e.replyEmbeds(Embeds.errorEmbed())
                    .setEphemeral(true)
                    .queue();
        } else {
            Optional<Member> optionalMember = Matcher.getMemberFromInput(e.getGuild(), mapping.getAsString());
            if (optionalMember.isEmpty()) {
                e.replyEmbeds(Embeds.errorEmbed("Errore! Devi inserire un username#tag, @username o ID valido."))
                        .setEphemeral(true)
                        .queue();

                return null;
            }

            Member member = optionalMember.get();
            if (vc.getUser().equals(member.getId())) {
                e.replyEmbeds(Embeds.errorEmbed("Non puoi bannarti dalla tua stessa stanza!"))
                        .setEphemeral(true)
                        .queue();
            } else if (vc.hasRecordPlayer(RecordType.BAN, member.getId())) {
                e.replyEmbeds(Embeds.errorEmbed("Questo giocatore è già bannato"))
                        .setEphemeral(true)
                        .queue();
            } else if (vc.hasRecordPlayer(RecordType.TRUST, member.getId())) {
                e.replyEmbeds(Embeds.errorEmbed("Non puoi bannare un utente trustato!"))
                        .setEphemeral(true)
                        .queue();
            } else if (member.getRoles().stream().noneMatch(role -> settings.getPublicRole().equals(role.getId()))) {
                e.replyEmbeds(Embeds.errorEmbed("Quell'utente non ha accesso alle stanze private.\nNon serve bannarlo!"))
                        .setEphemeral(true)
                        .queue();
            } else if (member.hasPermission(Permission.ADMINISTRATOR)) {
                e.replyEmbeds(Embeds.errorEmbed("Non puoi bannare un amministratore dalla stanza!"))
                        .setEphemeral(true)
                        .queue();
            } else {
                VoiceChannel channel = e.getGuild().getVoiceChannelById(vc.getChannel());
                boolean isChannelCreated = channel != null;
                vc.addRecordPlayer(RecordType.BAN, member.getId());

                // Rispondi alla richiesta
                e.replyEmbeds(new EmbedBuilder()
                                .setDescription(member.getEffectiveName() + " è stato bannato dalla stanza.")
                                .setColor(new Color(239, 210, 95))
                                .build())
                        .setEphemeral(true)
                        .queue();

                if (isChannelCreated) {
                    Perms.ban(member, channel.getManager()).queue();

                    channel.getMembers()
                            .stream()
                            .filter(connectedMember -> connectedMember.getId().equals(member.getId()))
                            .findFirst()
                            .ifPresent(bannedMember -> e.getGuild().kickVoiceMember(bannedMember).queue());
                }

                // Avvisa l'utente bannato in dm
                member.getUser().openPrivateChannel()
                        .queue(dm -> dm.sendMessageEmbeds(new EmbedBuilder()
                                                .setTitle("Sei stato bannato! :no_entry_sign:")
                                                .setColor(new Color(239, 210, 95))
                                                .setDescription(String.format("""
                                                        %s, il proprietario della stanza `%s`,
                                                        ti ha bannato da essa!
                                                                                                                
                                                        **Non potrai più rientrare, nè vedere la stanza vocale!**
                                                        """, e.getUser().getAsMention(), vc.getTitle()
                                                ))
                                                .build())
                                        .queue(RestUtils.emptyConsumer(), RestUtils.emptyConsumer()),
                                throwable -> e.replyEmbeds(Embeds.errorEmbed("Qualcosa è andato storto durante l'interazione, riprova!"))
                                        .setEphemeral(true)
                                        .queue());

                return vc;
            }
        }

        return null;
    }

    @Override
    public VC onButtonInteraction(VC vc, Settings settings, String fullID, ButtonInteractionEvent e) {
        e.replyModal(Modal.create( getName(), "Inserisci")
                        .addActionRow(TextInput.create("ban:id", "utente", TextInputStyle.SHORT)
                                .setValue("username#0001, @username o un id")
                                .build())
                        .build())
                .queue();

        return null;
    }

    @Override
    public Emoji emoji() {
        return Emoji.fromUnicode("U+1F6AB");
    }

    @Override
    public String getName() {
        return "ban";
    }
}
