package dev.starless.maggiordomo.commands.interaction;

import dev.starless.maggiordomo.Bot;
import dev.starless.maggiordomo.commands.types.Interaction;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.filter.FilterResult;
import dev.starless.maggiordomo.data.VC;
import dev.starless.maggiordomo.localization.Messages;
import dev.starless.maggiordomo.localization.Translations;
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

public class TitleInteraction implements Interaction {

    @Override
    public VC onModalInteraction(VC vc, Settings settings, String id, ModalInteractionEvent e) {
        ModalMapping mapping = e.getValue("vc:title");
        if (mapping == null) {
            e.replyEmbeds(Embeds.defaultErrorEmbed(settings.getLanguage()))
                    .setEphemeral(true)
                    .queue();
        } else {
            String input = mapping.getAsString();
            FilterResult result = Bot.getInstance().getCore().getFilters().check(settings, input);
            if (result.flagged()) {
                e.reply(Translations.string(Messages.FILTER_FLAG_PREFIX, settings.getLanguage()) + " " + result.data())
                        .setEphemeral(true)
                        .queue();

                return null;
            }

            vc.setTitle(result.data());

            VoiceChannel channel = e.getGuild().getVoiceChannelById(vc.getChannel());
            if (channel != null) {
                channel.getManager().setName(input).queue();
            }

            e.replyEmbeds(new EmbedBuilder()
                            .setDescription(Translations.string(Messages.INTERACTION_TITLE_SUCCESS, settings.getLanguage()))
                            .setColor(new Color(100, 160, 94))
                            .build())
                    .setEphemeral(true)
                    .queue();
        }

        return vc;
    }

    @Override
    public VC onButtonInteraction(VC vc, Settings settings, String fullID, ButtonInteractionEvent e) {
        e.replyModal(Modal.create(getName(), Translations.string(Messages.INTERACTION_TITLE_MODAL_TITLE, settings.getLanguage()))
                        .addActionRow(TextInput.create("vc:title", Translations.string(Messages.INTERACTION_TITLE_MODAL_INPUT_LABEL, settings.getLanguage()), TextInputStyle.SHORT)
                                .setRequired(true)
                                .setRequiredRange(1, 99)
                                .setValue(vc.getTitle())
                                .setPlaceholder(Translations.string(Messages.INTERACTION_TITLE_MODAL_INPUT_PLACEHOLDER, settings.getLanguage(), e.getUser().getName()))
                                .build())
                        .build())
                .queue();

        return null;
    }

    @Override
    public Emoji emoji() {
        return Emoji.fromUnicode("📝");
    }

    @Override
    public long timeout() {
        return 30;
    }

    @Override
    public String getName() {
        return "rename";
    }
}
