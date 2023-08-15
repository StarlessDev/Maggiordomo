package dev.starless.maggiordomo.utils.discord;

import dev.starless.maggiordomo.localization.Messages;
import dev.starless.maggiordomo.localization.Translations;
import lombok.experimental.UtilityClass;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.*;

@UtilityClass
public class Embeds {

    public MessageEmbed errorEmbed(String message) {
        return new EmbedBuilder()
                .setColor(Color.RED)
                .setDescription(message)
                .build();
    }

    public MessageEmbed defaultErrorEmbed(String language) {
        return errorEmbed(Translations.string(Messages.GENERIC_ERROR, language));
    }
}