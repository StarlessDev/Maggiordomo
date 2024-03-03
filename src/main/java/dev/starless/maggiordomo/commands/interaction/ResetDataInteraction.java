package dev.starless.maggiordomo.commands.interaction;

import dev.starless.maggiordomo.Core;
import dev.starless.maggiordomo.commands.types.Interaction;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.VC;
import dev.starless.maggiordomo.localization.Messages;
import dev.starless.maggiordomo.localization.Translations;
import dev.starless.maggiordomo.utils.discord.Embeds;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

public class ResetDataInteraction implements Interaction {

    @Override
    public VC onButtonInteraction(Core core, VC vc, Settings settings, String id, ButtonInteractionEvent e) {
        e.replyModal(Modal.create(getName(), Translations.string(Messages.CONFIRMATION_MODAL_TITLE, settings.getLanguage()))
                        .addActionRow(TextInput.create("vc:confirmation", Translations.string(Messages.CONFIRMATION_MODAL_INPUT_LABEL, settings.getLanguage()), TextInputStyle.SHORT)
                                .setMaxLength(31)
                                .setValue(Translations.string(Messages.CONFIRMATION_MODAL_INPUT_VALUE, settings.getLanguage()))
                                .build())
                        .build())
                .queue();

        return vc;
    }

    @Override
    public VC onModalInteraction(Core core, VC vc, Settings settings, String id, ModalInteractionEvent e) {
        ModalMapping mapping = e.getValue("vc:confirmation");
        if (mapping == null) {
            e.replyEmbeds(Embeds.defaultErrorEmbed(settings.getLanguage()))
                    .setEphemeral(true)
                    .queue();
        } else if (mapping.getAsString().equalsIgnoreCase(Translations.string(Messages.CONFIRMATION_VALUE, settings.getLanguage()))) {
            VoiceChannel channel = e.getGuild().getVoiceChannelById(vc.getChannel());
            if (channel != null) { // Risparmiamo query importanti in questo modo...
                core.getChannelMapper()
                        .getMapper(e.getGuild())
                        .scheduleForDeletion(vc, channel)
                        .complete();
            }

            vc.getTrusted().clear();
            vc.getBanned().clear();

            e.reply(Translations.string(Messages.INTERACTION_RESET_SUCCESS, settings.getLanguage()))
                    .setEphemeral(true)
                    .queue();

            return vc;
        } else {
            e.reply(Translations.string(Messages.CONFIRMATION_MODAL_NOT_CONFIRMED, settings.getLanguage()))
                    .setEphemeral(true)
                    .queue();
        }

        return null;
    }

    @Override
    public Emoji emoji() {
        return Emoji.fromUnicode("🔄");
    }

    @Override
    public long timeout() {
        return 600;
    }

    @Override
    public String getName() {
        return "resetdata";
    }
}
