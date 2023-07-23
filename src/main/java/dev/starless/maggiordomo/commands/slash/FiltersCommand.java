package dev.starless.maggiordomo.commands.slash;

import dev.starless.maggiordomo.commands.CommandInfo;
import dev.starless.maggiordomo.commands.types.Slash;
import dev.starless.maggiordomo.data.Settings;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

@CommandInfo(name = "filters", description = "Gestisci le parole vietate nei nomi delle stanze")
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
}
