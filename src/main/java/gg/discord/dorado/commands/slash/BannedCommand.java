package gg.discord.dorado.commands.slash;

import gg.discord.dorado.Bot;
import gg.discord.dorado.commands.CommandInfo;
import gg.discord.dorado.commands.Parameter;
import gg.discord.dorado.commands.types.Slash;
import gg.discord.dorado.data.Settings;
import gg.discord.dorado.utils.discord.Embeds;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.awt.*;

@CommandInfo(name = "blacklist", description = "Aggiungi/Togli ruoli dalla blacklist")
public class BannedCommand implements Slash {

    @Override
    public void execute(Settings settings, SlashCommandInteractionEvent e) {
        Role role = e.getOption("role").getAsRole();
        String roleID = role.getId();
        // Decidi cosa fare ina base alla presenza in lista dell'oggetto o meno
        boolean isAdded = settings.getBannedRoles().contains(roleID);
        boolean success = isAdded ? settings.getBannedRoles().remove(roleID) : settings.getBannedRoles().add(roleID);

        // Aggiorna il database
        Bot.getInstance().getCore().getSettingsMapper().update(settings);

        if (success) {
            e.replyEmbeds(new EmbedBuilder()
                            .setDescription(String.format(isAdded ? "Il ruolo %s è stato rimosso dalla blacklist" : "Ora il ruolo %s è stato aggiunto alla blacklist", role.getAsMention()))
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
        return new Parameter[]{new Parameter(OptionType.ROLE, "role", "Ruolo che verrà messo nella lista ban", true)};
    }
}
