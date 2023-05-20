package gg.discord.dorado.commands.interaction;

import gg.discord.dorado.commands.CommandInfo;
import gg.discord.dorado.commands.types.Interaction;
import gg.discord.dorado.data.Settings;
import gg.discord.dorado.data.user.VC;
import gg.discord.dorado.utils.discord.Embeds;
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

@CommandInfo(name = "rename", description = "Cambia il nome della tua stanza")
public class TitleInteraction implements Interaction {

    @Override
    public VC execute(VC vc, Settings guild, String id, ModalInteractionEvent e) {
        ModalMapping mapping = e.getValue("vc:title");
        if (mapping == null) {
            e.replyEmbeds(Embeds.errorEmbed())
                    .setEphemeral(true)
                    .queue();
        } else {
            String newTitle = mapping.getAsString();
            vc.setTitle(newTitle);

            VoiceChannel channel = e.getGuild().getVoiceChannelById(vc.getChannel());
            if(channel != null) {
                channel.getManager().setName(newTitle).queue();
            }

            e.replyEmbeds(new EmbedBuilder()
                            .setDescription("Titolo cambiato! :pencil:")
                            .setColor(new Color(100, 160, 94))
                            .build())
                    .setEphemeral(true)
                    .queue();
        }

        return vc;
    }

    @Override
    public VC execute(VC vc, Settings settings, String fullID, ButtonInteractionEvent e) {
        e.replyModal(Modal.create(getName(), "Inserisci")
                        .addActionRow(TextInput.create("vc:title", "Titolo", TextInputStyle.SHORT)
                                .setRequired(true)
                                .setRequiredRange(1, 99)
                                .setValue(vc.getTitle())
                                .setPlaceholder("Stanza di " + e.getUser().getName())
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
}
