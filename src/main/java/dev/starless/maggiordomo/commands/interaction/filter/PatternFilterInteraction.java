package dev.starless.maggiordomo.commands.interaction.filter;

import dev.starless.maggiordomo.Bot;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.filter.FilterType;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class PatternFilterInteraction extends FilterInteraction {

    public PatternFilterInteraction() {
        super(FilterType.REGEX,
                """
                        # Regex
                        Qua sotto sono elencate le espressioni regolari che hai già inserito nella lista.
                        Clicca su un'opzione per rimuoverla dalla lista.""",
                Modal.create("pattern", "Inserisci")
                        .addActionRow(TextInput.create("input", "pattern", TextInputStyle.SHORT)
                                .setValue("Inserisci qua l'espressione")
                                .setRequiredRange(1, 512)
                                .build())
                        .build());
    }

    @Override
    protected void onInputReceived(Settings settings, ModalInteractionEvent e) {
        ModalMapping mapping = e.getValue("input");
        if (mapping != null) {
            try {
                Pattern pattern = Pattern.compile(mapping.getAsString());
                settings.modifyFilters(FilterType.REGEX, set -> set.add(pattern.pattern()));
                Bot.getInstance().getCore().getSettingsMapper().update(settings);

                e.getChannel().sendMessage(String.format("Aggiunto nuovo pattern `%s`", pattern.pattern())).queue();
            } catch (PatternSyntaxException ex) {
                e.reply(">>> :x: Errore di sintassi! \n" + ex.getMessage())
                        .setEphemeral(true)
                        .queue();
            }
        }
    }

    @Override
    public String getName() {
        return "pattern";
    }
}
