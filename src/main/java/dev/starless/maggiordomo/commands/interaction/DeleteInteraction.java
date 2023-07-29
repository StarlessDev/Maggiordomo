package dev.starless.maggiordomo.commands.interaction;

import dev.starless.maggiordomo.Bot;
import dev.starless.maggiordomo.commands.types.Interaction;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.user.VC;
import dev.starless.maggiordomo.storage.vc.LocalVCMapper;
import dev.starless.maggiordomo.utils.discord.Embeds;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

public class DeleteInteraction implements Interaction {

    @Override
    public VC onButtonInteraction(VC vc, Settings settings, String id, ButtonInteractionEvent e) {
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
    public VC onModalInteraction(VC vc, Settings settings, String id, ModalInteractionEvent e) {
        ModalMapping mapping = e.getValue("vc:confirmation");
        if (mapping == null) {
            e.replyEmbeds(Embeds.errorEmbed())
                    .setEphemeral(true)
                    .queue();
        } else if (mapping.getAsString().equalsIgnoreCase("si")) {
            LocalVCMapper localMapper = Bot.getInstance().getCore()
                    .getChannelMapper()
                    .getMapper(e.getGuild());

            localMapper.delete(vc);

            VoiceChannel channel = e.getGuild().getVoiceChannelById(vc.getChannel());
            if (channel != null) { // Risparmiamo query importanti in questo modo...
                localMapper.scheduleForDeletion(vc, channel).queue();
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

    @Override
    public String getName() {
        return "delete";
    }
}
