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

@CommandInfo(name = "size", description = "Imposta il numero massimo di utenti nella tua stanza")
public class SizeInteraction implements Interaction {

    @Override
    public VC execute(VC vc, Settings settings, String id, ModalInteractionEvent e) {
        ModalMapping mapping = e.getValue("vc:size");
        if (mapping == null) {
            e.replyEmbeds(Embeds.errorEmbed())
                    .setEphemeral(true)
                    .queue();
        } else {
            int size;
            try {
                size = Integer.parseInt(mapping.getAsString());
                if (size < 0 || size > 99) throw new NumberFormatException();

            } catch (NumberFormatException ex) {
                e.replyEmbeds(Embeds.errorEmbed("Devi inserire un numero valido! :x:"))
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
                            .setDescription(String.format("Ora la stanza può ospitare %s utenti! :eyes:", size != 0 ? size : "∞"))
                            .setColor(new Color(100, 160, 94))
                            .build())
                    .setEphemeral(true)
                    .queue();

            return vc;
        }

        return null;
    }

    @Override
    public VC execute(VC vc, Settings guild, String id, ButtonInteractionEvent e) {
        e.replyModal(Modal.create(getName(), "Inserisci")
                        .addActionRow(TextInput.create("vc:size", "Numero", TextInputStyle.SHORT)
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
}
