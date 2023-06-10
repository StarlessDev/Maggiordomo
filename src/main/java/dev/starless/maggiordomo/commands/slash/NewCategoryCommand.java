package dev.starless.maggiordomo.commands.slash;

import dev.starless.maggiordomo.commands.CommandInfo;
import dev.starless.maggiordomo.commands.types.Slash;
import dev.starless.maggiordomo.data.Settings;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

@CommandInfo(name = "newcategory", description = "Crea una nuova categoria")
public class NewCategoryCommand implements Slash {

    @Override
    public void execute(Settings settings, SlashCommandInteractionEvent e) {
        Category category = settings.createCategory(e.getGuild());

        e.reply("Creata una nuova categoria: " + category.getAsMention())
                .setEphemeral(true)
                .queue();
    }
}
