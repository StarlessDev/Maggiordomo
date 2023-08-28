package dev.starless.maggiordomo.commands.slash;

import dev.starless.maggiordomo.commands.types.Slash;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.localization.Translations;
import dev.starless.maggiordomo.localization.Messages;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

public class FiltersCommand implements Slash {

    @Override
    public void execute(Settings settings, SlashCommandInteractionEvent e) {
        e.reply(Translations.string(Messages.COMMAND_FILTERS_MESSAGE_CONTENT, settings.getLanguage()))
                .addActionRow(
                        Button.primary("contains:0", Translations.string(Messages.FILTER_BASIC, settings.getLanguage())),
                        Button.secondary("pattern:0", Translations.string(Messages.FILTER_PATTERN, settings.getLanguage()))
                )
                .queue();
    }

    @Override
    public String getName() {
        return "filters";
    }

    @Override
    public String getDescription(String lang) {
        return Translations.string(Messages.COMMAND_FILTERS_DESCRIPTION, lang);
    }
}
