package dev.starless.maggiordomo.commands.slash;

import dev.starless.maggiordomo.commands.types.Slash;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.localization.MessageProvider;
import dev.starless.maggiordomo.localization.Messages;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

public class FiltersCommand implements Slash {

    @Override
    public void execute(Settings settings, SlashCommandInteractionEvent e) {
        e.reply("""
                        # Filtri 🚧
                        Ci sono due tipi di filtri: **base** e **regex**.
                        I filtri base controllano semplicemente se una parola è presente all'interno del nome della stanza.
                        Il secondo tipo invece usa le espressioni regolari (Regex) e controlla se ci sono una o più corripondenze.""")
                .addActionRow(Button.primary("contains", "Filtri base"), Button.secondary("pattern", "Filtri regex"))
                .queue();
    }

    @Override
    public String getName() {
        return "filters";
    }

    @Override
    public String getDescription(String lang) {
        return MessageProvider.getMessage(Messages.COMMAND_FILTERS_DESCRIPTION, lang);
    }
}
