package dev.starless.maggiordomo.commands.slash;

import dev.starless.maggiordomo.Bot;
import dev.starless.maggiordomo.commands.Parameter;
import dev.starless.maggiordomo.commands.types.Slash;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.localization.MessageProvider;
import dev.starless.maggiordomo.localization.Messages;
import dev.starless.maggiordomo.utils.discord.Embeds;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.awt.*;

public class PremiumCommand implements Slash {

    @Override
    public void execute(Settings settings, SlashCommandInteractionEvent e) {
        Role role = e.getOption("role").getAsRole();
        String roleID = role.getId();
        // Decidi cosa fare ina base alla presenza in lista dell'oggetto o meno
        boolean isAdded = settings.getPremiumRoles().contains(roleID);
        boolean success = isAdded ? settings.getPremiumRoles().remove(roleID) : settings.getPremiumRoles().add(roleID);

        // Aggiorna il database
        Bot.getInstance().getCore().getSettingsMapper().update(settings);

        Messages messages = isAdded ? Messages.COMMAND_PREMIUM_ROLE_SUCCESS_REMOVED : Messages.COMMAND_PREMIUM_ROLE_SUCCESS_ADDED;
        String desc = MessageProvider.getMessage(messages, settings.getLanguage(), role.getAsMention());

        if (success) {
            e.replyEmbeds(new EmbedBuilder()
                            .setDescription(desc)
                            .setColor(isAdded ? new Color(239, 210, 95) : new Color(100, 160, 94))
                            .build())
                    .setEphemeral(true)
                    .queue();
        } else {
            e.replyEmbeds(Embeds.errorEmbed())
                    .setEphemeral(true)
                    .queue();
        }
    }

    @Override
    public Parameter[] getParameters(String lang) {
        return new Parameter[]{new Parameter(OptionType.ROLE,
                "role",
                MessageProvider.getMessage(Messages.COMMAND_PREMIUM_ROLE_PARAMETERS_ROLE, lang),
                true)};
    }

    @Override
    public String getName() {
        return "premium";
    }

    @Override
    public String getDescription(String lang) {
        return MessageProvider.getMessage(Messages.COMMAND_PREMIUM_ROLE_DESCRIPTION, lang);
    }
}
