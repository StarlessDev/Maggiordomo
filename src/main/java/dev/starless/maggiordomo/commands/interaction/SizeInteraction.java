package dev.starless.maggiordomo.commands.interaction;

import dev.starless.maggiordomo.Core;
import dev.starless.maggiordomo.commands.types.Interaction;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.VC;
import dev.starless.maggiordomo.localization.Translations;
import dev.starless.maggiordomo.localization.Messages;
import dev.starless.maggiordomo.utils.discord.Embeds;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

import java.awt.*;

public class SizeInteraction implements Interaction {

    @Override
    public VC onModalInteraction(Core core, VC vc, Settings settings, String id, ModalInteractionEvent e) {
        ModalMapping mapping = e.getValue("vc:size");
        if (mapping == null) {
            e.replyEmbeds(Embeds.defaultErrorEmbed(settings.getLanguage()))
                    .setEphemeral(true)
                    .queue();
        } else {
            int size;
            try {
                size = Integer.parseInt(mapping.getAsString());
                if (size < 0 || size > 99) throw new NumberFormatException();

            } catch (NumberFormatException ex) {
                e.replyEmbeds(Embeds.errorEmbed(Translations.string(Messages.INTERACTION_SIZE_FORMAT_ERROR, settings.getLanguage())))
                        .setEphemeral(true)
                        .queue();

                return null;
            }

            vc.setSize(size);
            VoiceChannel voiceChannel = e.getGuild().getVoiceChannelById(vc.getChannel());
            if (voiceChannel != null) {
                voiceChannel.getManager().setUserLimit(size).queue();
            }

            e.replyEmbeds(new EmbedBuilder()
                            .setDescription(Translations.string(Messages.INTERACTION_SIZE_SUCCESS, settings.getLanguage(), size != 0 ? size : "∞"))
                            .setColor(new Color(100, 160, 94))
                            .build())
                    .setEphemeral(true)
                    .queue();

            return vc;
        }

        return null;
    }

    @Override
    public VC onButtonInteraction(Core core, VC vc, Settings settings, String id, ButtonInteractionEvent e) {
        e.replyModal(Modal.create(getName(), Translations.string(Messages.FILTER_MENU_TITLE, settings.getLanguage()))
                        .addActionRow(TextInput.create("vc:size", "Number", TextInputStyle.SHORT)
                                .setRequiredRange(1, 2)
                                .setPlaceholder("1-99")
                                .build())
                        .build())
                .queue();

        return null;
    }

    @Override
    public Emoji emoji() {
        return Emoji.fromUnicode("👥");
    }

    @Override
    public String getName() {
        return "size";
    }
}
