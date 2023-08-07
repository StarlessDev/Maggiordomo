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

public class BannedCommand implements Slash {

    @Override
    public void execute(Settings settings, SlashCommandInteractionEvent e) {
        Role role = e.getOption("role").getAsRole();
        String roleID = role.getId();
        // Decidi cosa fare ina base alla presenza in lista dell'oggetto o meno
        boolean toBeRemoved = settings.getBannedRoles().contains(roleID);
        boolean success = toBeRemoved ? settings.getBannedRoles().remove(roleID) : settings.getBannedRoles().add(roleID);
        Messages message = toBeRemoved ? Messages.COMMAND_BAN_ROLE_SUCCESS_REMOVED : Messages.COMMAND_BAN_ROLE_SUCCESS_ADDED;

        // Aggiorna il database
        Bot.getInstance().getCore().getSettingsMapper().update(settings);

        if (success) {
            e.replyEmbeds(new EmbedBuilder()
                            // String.format(isAdded ? "Il ruolo %s è stato rimosso dalla blacklist" : "Ora il ruolo %s è stato aggiunto alla blacklist", role.getAsMention())
                            .setDescription(MessageProvider.getMessage(message, settings.getLanguage()))
                            .setColor(toBeRemoved ? new Color(239, 210, 95) : new Color(100, 160, 94))
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
                MessageProvider.getMessage(Messages.COMMAND_BAN_ROLE_DESCRIPTION, lang),
                true)};
    }

    @Override
    public String getName() {
        return "blacklist";
    }

    @Override
    public String getDescription() {
        return "Aggiungi/Togli ruoli dalla blacklist";
    }
}
