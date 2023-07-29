package dev.starless.maggiordomo.commands.slash;

import dev.starless.maggiordomo.Bot;
import dev.starless.maggiordomo.commands.types.Slash;
import dev.starless.maggiordomo.commands.Parameter;
import dev.starless.maggiordomo.data.Settings;
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

        if (success) {
            e.replyEmbeds(new EmbedBuilder()
                            .setDescription(String.format(isAdded ? "Al ruolo %s sono stati rimossi i permessi premium)" : "Ora il ruolo %s ha i permessi 'premium'", role.getAsMention()))
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
    public Parameter[] getParameters() {
        return new Parameter[]{new Parameter(OptionType.ROLE, "role", "Ruolo a cui verranno dati permessi in più", true)};
    }

    @Override
    public String getName() {
        return "premium";
    }

    @Override
    public String getDescription() {
        return "Aggiungi/Rimuovi un ruolo dalla lista premium";
    }
}
