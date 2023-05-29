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

@CommandInfo(name = "resetdata", description = "Resetta tutti i trustati e bannati della stanza. La stanza verrà cancellata temporaneamente, ma potrai ricrearla.")
public class ResetDataInteraction implements Interaction {

    @Override
    public VC execute(VC vc, Settings settings, String id, ButtonInteractionEvent e) {
        e.replyModal(Modal.create(getName(), "Sei sicuro?")
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
            VoiceChannel channel = e.getGuild().getVoiceChannelById(vc.getChannel());
            if (channel != null) { // Risparmiamo query importanti in questo modo...
                Bot.getInstance().getCore()
                        .getChannelMapper()
                        .getMapper(e.getGuild())
                        .scheduleForDeletion(vc, channel);
            }

            vc.getTrusted().clear();
            vc.getBanned().clear();

            e.reply("I tuoi dati sono stati resettati con successo :white_check_mark:")
                    .setEphemeral(true)
                    .queue();

            return vc;
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
}
