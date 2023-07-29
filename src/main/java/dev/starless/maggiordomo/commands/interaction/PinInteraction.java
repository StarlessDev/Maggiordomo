package dev.starless.maggiordomo.commands.interaction;

import dev.starless.maggiordomo.Bot;
import dev.starless.maggiordomo.commands.types.Interaction;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.user.VC;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.awt.*;

public class PinInteraction implements Interaction {

    @Override
    public VC onButtonInteraction(VC vc, Settings settings, String id, ButtonInteractionEvent e) {
        Bot.getInstance().getCore()
                .getChannelMapper()
                .getMapper(e.getGuild())
                .togglePinStatus(e.getGuild(), settings, vc);

        String content = String.format("Ora la tua stanza%s è bloccata :thumbsup:", vc.isPinned() ? "" : " non");
        e.replyEmbeds(new EmbedBuilder()
                        .setColor(new Color(123, 0, 212))
                        .setDescription(content)
                        .build())
                .setEphemeral(true)
                .queue();

        return null;
    }

    @Override
    public Emoji emoji() {
        return Emoji.fromUnicode("📌");
    }

    @Override
    public long timeout() {
        return 30;
    }

    @Override
    public String getName() {
        return "pin";
    }

    @Override
    public boolean hasPermission(Member member, Settings settings) {
        return member.hasPermission(Permission.ADMINISTRATOR) || // Se ha il permesso di amministratore
                settings.getPremiumRoles() // oppure se ha un altro dei ruoli settati
                        .stream()
                        .anyMatch(id -> member.getRoles()
                                .stream()
                                .anyMatch(role -> role.getId().equals(id)));
    }
}
