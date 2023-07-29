package dev.starless.maggiordomo.utils.discord;

import lombok.experimental.UtilityClass;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.*;

@UtilityClass
public class Embeds {

    public MessageEmbed errorEmbed(String message) {
        return new EmbedBuilder()
                .setColor(Color.RED)
                .setDescription(message.isBlank() ? "Something went wrong during this interaction." : message)
                .build();
    }

    public MessageEmbed errorEmbed() {
        return errorEmbed("");
    }
}