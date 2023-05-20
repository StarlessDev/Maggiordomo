package gg.discord.dorado.commands.interaction;

import gg.discord.dorado.Bot;
import gg.discord.dorado.commands.CommandInfo;
import gg.discord.dorado.commands.types.Interaction;
import gg.discord.dorado.data.Settings;
import gg.discord.dorado.data.user.VC;
import gg.discord.dorado.utils.discord.Embeds;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

@CommandInfo(name = "delete", description = "Cancella la tua stanza completamente (perderai i tuoi dati)")
public class DeleteInteraction implements Interaction {

    @Override
    public VC execute(VC vc, Settings settings, String id, ButtonInteractionEvent e) {
        e.replyModal(Modal.create(getName(), "Cancellazione")
                        .addActionRow(TextInput.create("vc:confirmation", "Risposta", TextInputStyle.SHORT)
                                .setMaxLength(31)
                                .setValue("Scrivi qua \"Si\" se sei sicuro")
                                .build())
                        .build())
                .queue();

        return vc;
    }

    @Override
    public VC execute(VC vc, Settings settings, String id, ModalInteractionEvent e) {
        ModalMapping mapping = e.getValue("vc:confirmation");
        if (mapping == null) {
            e.replyEmbeds(Embeds.errorEmbed())
                    .setEphemeral(true)
                    .queue();
        } else if (mapping.getAsString().equalsIgnoreCase("si")) {
            Bot.getInstance().getCore().getChannelMapper().delete(vc);

            VoiceChannel channel = e.getGuild().getVoiceChannelById(vc.getChannel());
            if (channel != null) { // Risparmiamo query importanti in questo modo...
                Bot.getInstance().getCore().getChannelMapper().scheduleForDeletion(vc, channel);
            }

            e.reply("La tua stanza è stata cancellata con successo. :white_check_mark:")
                    .setEphemeral(true)
                    .queue();
        }

        return null;
    }

    @Override
    public long timeout() {
        return 3600;
    }

    @Override
    public Emoji emoji() {
        return Emoji.fromUnicode("🧨");
    }
}
