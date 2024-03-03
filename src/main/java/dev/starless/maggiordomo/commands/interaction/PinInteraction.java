package dev.starless.maggiordomo.commands.interaction;

import dev.starless.maggiordomo.Core;
import dev.starless.maggiordomo.commands.types.Interaction;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.VC;
import dev.starless.maggiordomo.localization.Messages;
import dev.starless.maggiordomo.localization.Translations;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;

import java.awt.*;

public class PinInteraction implements Interaction {

    @Override
    public VC onButtonInteraction(Core core, VC vc, Settings settings, String id, ButtonInteractionEvent e) {
        core.getChannelMapper().getMapper(e.getGuild()).togglePinStatus(e.getGuild(), settings, vc);

        Messages message = vc.isPinned() ? Messages.INTERACTION_PIN_PINNED : Messages.INTERACTION_PIN_UNPINNED;
        e.replyEmbeds(new EmbedBuilder()
                        .setColor(new Color(123, 0, 212))
                        .setDescription(Translations.string(message, settings.getLanguage()))
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
        /*
        This command is executable by users who:
        - have the administrator permission
        - are boosting the server (if the option in the settings is enabled)
        - have a "premium role"
         */
        return member.hasPermission(Permission.ADMINISTRATOR) ||
                (settings.isBoosterPremium() && member.isBoosting()) ||
                settings.getPremiumRoles()
                        .stream()
                        .anyMatch(id -> member.getRoles()
                                .stream()
                                .anyMatch(role -> role.getId().equals(id)));
    }
}
