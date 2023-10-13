package dev.starless.maggiordomo.commands.slash;

import dev.starless.maggiordomo.Bot;
import dev.starless.maggiordomo.commands.Parameter;
import dev.starless.maggiordomo.commands.types.Slash;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.localization.Translations;
import dev.starless.maggiordomo.localization.Messages;
import dev.starless.maggiordomo.utils.discord.Embeds;
import dev.starless.maggiordomo.utils.discord.Perms;
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

        // Setta i permessi
        settings.forEachCategory(e.getGuild(), guild -> guild.getVoiceChannels().forEach(channel -> {
            if (toBeRemoved) {
                Perms.reset(role, channel.getManager());
            } else {
                Perms.ban(role, channel.getManager()).queue();
            }
        }));

        // Aggiorna il database
        Bot.getInstance().getCore().getSettingsMapper().update(settings);

        if (success) {
            e.replyEmbeds(new EmbedBuilder()
                            .setDescription(Translations.string(message, settings.getLanguage(), role.getAsMention()))
                            .setColor(toBeRemoved ? new Color(239, 210, 95) : new Color(100, 160, 94))
                            .build())
                    .setEphemeral(true)
                    .queue();
        } else {
            e.replyEmbeds(Embeds.defaultErrorEmbed(settings.getLanguage()))
                    .setEphemeral(true)
                    .queue();
        }
    }

    @Override
    public Parameter[] getParameters(String lang) {
        return new Parameter[]{new Parameter(OptionType.ROLE,
                "role",
                Translations.string(Messages.COMMAND_BAN_ROLE_DESCRIPTION, lang),
                true)};
    }

    @Override
    public String getName() {
        return "blacklist";
    }

    @Override
    public String getDescription(String lang) {
        return Translations.string(Messages.COMMAND_BAN_ROLE_DESCRIPTION, lang);
    }
}
