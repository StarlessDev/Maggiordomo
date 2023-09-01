package dev.starless.maggiordomo.commands.interaction;

import dev.starless.maggiordomo.commands.types.Interaction;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.enums.RecordType;
import dev.starless.maggiordomo.data.user.VC;
import dev.starless.maggiordomo.localization.Translations;
import dev.starless.maggiordomo.localization.Messages;
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
            e.replyEmbeds(Embeds.defaultErrorEmbed(settings.getLanguage()))
                    .setEphemeral(true)
                    .queue();
        } else {
            Optional<Member> optionalMember = Matcher.getMemberFromInput(e.getGuild(), mapping.getAsString());
            if (optionalMember.isEmpty()) {
                e.replyEmbeds(Embeds.errorEmbed(Translations.string(Messages.MEMBER_MODAL_INPUT_ERROR, settings.getLanguage())))
                        .setEphemeral(true)
                        .queue();

                return null;
            }

            Member member = optionalMember.get();
            if (vc.getUser().equals(member.getId())) {
                e.replyEmbeds(Embeds.errorEmbed(Translations.string(Messages.INTERACTION_BAN_SELF_ERROR, settings.getLanguage())))
                        .setEphemeral(true)
                        .queue();
            } else if (vc.hasPlayerRecord(RecordType.BAN, member.getId())) {
                e.replyEmbeds(Embeds.errorEmbed(Translations.string(Messages.INTERACTION_BAN_ALREADY_BANNED, settings.getLanguage())))
                        .setEphemeral(true)
                        .queue();
            } else if (vc.hasPlayerRecord(RecordType.TRUST, member.getId())) {
                e.replyEmbeds(Embeds.errorEmbed(Translations.string(Messages.INTERACTION_BAN_TRUSTED_ERROR, settings.getLanguage())))
                        .setEphemeral(true)
                        .queue();
            } else if (settings.hasNoAccess(member)) {
                e.replyEmbeds(Embeds.errorEmbed(Translations.string(Messages.NO_PUBLIC_ROLE, settings.getLanguage())))
                        .setEphemeral(true)
                        .queue();
            } else if (member.hasPermission(Permission.ADMINISTRATOR)) {
                e.replyEmbeds(Embeds.errorEmbed(Translations.string(Messages.INTERACTION_BAN_ADMIN_ERROR, settings.getLanguage())))
                        .setEphemeral(true)
                        .queue();
            } else {
                VoiceChannel channel = e.getGuild().getVoiceChannelById(vc.getChannel());
                boolean isChannelCreated = channel != null;
                vc.addPlayerRecord(RecordType.BAN, member.getId());

                // Rispondi alla richiesta
                e.replyEmbeds(new EmbedBuilder()
                                .setDescription(Translations.string(Messages.INTERACTION_BAN_SUCCESS, settings.getLanguage(), member.getEffectiveName()))
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
                                                .setTitle(Translations.string(Messages.INTERACTION_BAN_NOTIFICATION_TITLE, settings.getLanguage()))
                                                .setColor(new Color(239, 210, 95))
                                                .setDescription(Translations.stringFormatted(Messages.INTERACTION_BAN_NOTIFICATION_DESC, settings.getLanguage(),
                                                        "issuer", e.getUser().getAsMention(),
                                                        "target", vc.getTitle()))
                                                .build())
                                        .queue(RestUtils.emptyConsumer(), RestUtils.emptyConsumer()),
                                throwable -> e.replyEmbeds(Embeds.errorEmbed(Translations.string(Messages.GENERIC_ERROR, settings.getLanguage())))
                                        .setEphemeral(true)
                                        .queue());

                return vc;
            }
        }

        return null;
    }

    @Override
    public VC onButtonInteraction(VC vc, Settings settings, String fullID, ButtonInteractionEvent e) {
        e.replyModal(Modal.create( getName(), Translations.string(Messages.MEMBER_MODAL_TITLE, settings.getLanguage()))
                        .addActionRow(TextInput.create("ban:id", "user", TextInputStyle.SHORT)
                                .setValue(Translations.string(Messages.MEMBER_MODAL_INPUT_VALUE, settings.getLanguage()))
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
