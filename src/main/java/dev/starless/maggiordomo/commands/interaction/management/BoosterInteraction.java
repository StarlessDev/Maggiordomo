package dev.starless.maggiordomo.commands.interaction.management;

import dev.starless.maggiordomo.Bot;
import dev.starless.maggiordomo.Core;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.localization.Messages;
import dev.starless.maggiordomo.localization.Translations;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

public class BoosterInteraction extends AManagementInteraction {

    @Override
    protected MessageEditBuilder handle(Core core, Settings settings, String[] parts, ButtonInteractionEvent e) {
        boolean newValue = !settings.isBoosterPremium();
        settings.setBoosterPremium(newValue);
        core.getSettingsMapper().update(settings);

        String state = Translations.string(newValue ? Messages.COMMAND_MANAGEMENT_BOOSTERS_PREMIUM_STATE : Messages.COMMAND_MANAGEMENT_BOOSTERS_NORMAL_STATE, settings.getLanguage());
        String message = Translations.stringFormatted(Messages.COMMAND_MANAGEMENT_BOOSTERS_MESSAGE, settings.getLanguage()
                , "state", state);
        e.editMessage(MessageEditData.fromCreateData(core.getManagementMenu(e.getGuild(), settings)))
                .setReplace(true)
                .queue(success -> e.getHook().sendMessage(message).setEphemeral(true).queue());

        return null;
    }

    @Override
    public String getName() {
        return "boosters";
    }
}
