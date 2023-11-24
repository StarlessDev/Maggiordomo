package dev.starless.maggiordomo.commands.interaction.management;

import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.localization.Messages;
import dev.starless.maggiordomo.localization.Translations;
import dev.starless.maggiordomo.utils.PageUtils;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;

public class FiltersManager extends AManagementInteraction {

    @Override
    protected MessageEditBuilder handle(ButtonInteractionEvent e, Settings settings, String[] parts) {
        return new MessageEditBuilder()
                .setContent(Translations.string(Messages.COMMAND_FILTERS_MESSAGE_CONTENT, settings.getLanguage()))
                .setActionRow(
                        PageUtils.getShortBackButton("admin", settings.getLanguage()),
                        Button.primary("contains:0", Translations.string(Messages.FILTER_BASIC, settings.getLanguage())),
                        Button.primary("pattern:0", Translations.string(Messages.FILTER_PATTERN, settings.getLanguage()))
                );
    }

    @Override
    public String getName() {
        return "filters";
    }
}
