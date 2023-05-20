package gg.discord.dorado.commands.types;

import gg.discord.dorado.commands.Command;
import gg.discord.dorado.data.Settings;
import gg.discord.dorado.data.user.VC;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;

public interface Interaction extends Command {

    default VC execute(VC vc, Settings settings, String id, ModalInteractionEvent e) {
        return vc;
    }

    default VC execute(VC vc, Settings settings, String id, ButtonInteractionEvent e) {
        return vc;
    }

    default VC execute(VC vc, Settings settings, String id, StringSelectInteractionEvent e) {
        return vc;
    }

    default Emoji emoji() {
        return Emoji.fromUnicode("U+2753");
    }

    // Timeout in secondi
    default long timeout() {
        return -1L;
    }
}
