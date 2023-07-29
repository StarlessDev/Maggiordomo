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
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

import java.awt.*;
import java.util.List;
import java.util.Optional;

public class TrustInteraction implements Interaction {

    @Override
    public VC onModalInteraction(VC vc, Settings settings, String id, ModalInteractionEvent e) {
        ModalMapping mapping = e.getValue("trust:id");
        if (mapping == null) {
            e.replyEmbeds(Embeds.errorEmbed())
                    .setEphemeral(true)
                    .queue();
        } else {
            Optional<Member> optionalMember = Matcher.getMemberFromInput(e.getGuild(), mapping.getAsString());
            if (optionalMember.isEmpty()) {
                e.replyEmbeds(Embeds.errorEmbed("Errore! Devi inserire un username#tag, @username o ID valido!"))
                        .setEphemeral(true)
                        .queue();

                return null;
            }

            Member member = optionalMember.get();
            if (vc.getUser().equals(member.getId())) {
                e.replyEmbeds(Embeds.errorEmbed("Questa è già la tua stanza!"))
                        .setEphemeral(true)
                        .queue();
            } else if (vc.hasRecordPlayer(RecordType.TRUST, member.getId())) {
                e.replyEmbeds(Embeds.errorEmbed("Questo giocatore è già trustato"))
                        .setEphemeral(true)
                        .queue();
            } else if (vc.hasRecordPlayer(RecordType.BAN, member.getId())) {
                e.replyEmbeds(Embeds.errorEmbed("Non puoi trustare un utente bannato!"))
                        .setEphemeral(true)
                        .queue();
            } else {
                List<String> memberRoles = member.getRoles().stream()
                        .map(Role::getId)
                        .toList();

                if (!memberRoles.contains(settings.getPublicRole())) {
                    e.replyEmbeds(Embeds.errorEmbed("Quell'utente non ha il ruolo necessario!"))
                            .setEphemeral(true)
                            .queue();
                } else if (memberRoles.stream().anyMatch(role -> settings.getBannedRoles().contains(role))) {
                    e.replyEmbeds(Embeds.errorEmbed("Questo utente è bannato dall'utilizzo di questo bot!"))
                            .setEphemeral(true)
                            .queue();
                } else {
                    VoiceChannel channel = e.getGuild().getVoiceChannelById(vc.getChannel());
                    boolean isChannelCreated = channel != null;
                    vc.addRecordPlayer(RecordType.TRUST, member.getId());

                    // Rispondi alla richiesta
                    e.replyEmbeds(new EmbedBuilder()
                                    .setDescription("Hai scelto di fidarti di " + member.getEffectiveName() + " :ok_hand:")
                                    .setColor(new Color(100, 160, 94))
                                    .build())
                            .setEphemeral(true)
                            .queue();

                    // Dai i permessi se possibile subito
                    if (isChannelCreated) Perms.trust(member, channel.getManager()).queue();

                    // Avvisa l'utente trustato in dm
                    member.getUser().openPrivateChannel()
                            .queue(dm -> dm.sendMessageEmbeds(new EmbedBuilder()
                                                    .setTitle("Sei stato trustato! :innocent:")
                                                    .setColor(new Color(100, 160, 94))
                                                    .setDescription(String.format("""
                                                        %s, il proprietario della stanza `%s`,
                                                        ha deciso di fidarsi di te.
                                                                                                                
                                                        **Ora puoi entrare nella sua stanza a tuo piacimento!**
                                                        *(Anche se chiusa al resto del server)*
                                                        :point_right: Ricorda di non abusarne!
                                                        """, e.getUser().getAsMention(), vc.getTitle()
                                                    ))
                                                    .build())
                                            .queue(RestUtils.emptyConsumer(), RestUtils.emptyConsumer()),
                                    throwable -> RestUtils.emptyConsumer());

                    return vc;
                }
            }
        }

        return null;
    }

    @Override
    public VC onButtonInteraction(VC vc, Settings guild, String id, ButtonInteractionEvent e) {
        e.replyModal(Modal.create(getName(), "Inserisci")
                        .addActionRow(TextInput.create("trust:id", "utente", TextInputStyle.SHORT)
                                .setValue("username#0001, @username o un id")
                                .build())
                        .build())
                .queue();

        return vc;
    }

    @Override
    public Emoji emoji() {
        return Emoji.fromUnicode("🙏");
    }

    @Override
    public String getName() {
        return "trust";
    }
}
