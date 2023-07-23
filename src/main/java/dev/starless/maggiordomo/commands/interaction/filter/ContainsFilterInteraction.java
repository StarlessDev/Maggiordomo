package dev.starless.maggiordomo.commands.interaction.filter;

import dev.starless.maggiordomo.Bot;
import dev.starless.maggiordomo.commands.CommandInfo;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.filter.FilterType;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

import java.util.Arrays;

@CommandInfo(name = "contains")
public class ContainsFilterInteraction extends FilterInteraction {

    public ContainsFilterInteraction() {
        super(FilterType.CONTAINS,
                """
                        # Filtri Base
                        Qua sotto sono elencate le parole che hai inserito già nella lista.
                        Clicca su un'opzione per rimuoverla dalla lista.""",
                Modal.create("contains", "Inserisci")
                        .addActionRow(TextInput.create("input", "parole", TextInputStyle.PARAGRAPH)
                                .setValue("""
                                        Inserisci le parole da inserire una sotto all'altra.
                                        in
                                        questo
                                        modo""")
                                .setRequiredRange(1, 256)
                                .build())
                        .build());
    }

    @Override
    protected void onInputReceived(Settings settings, ModalInteractionEvent e) {
        ModalMapping mapping = e.getValue("input");
        if (mapping != null) {
            String[] words = mapping.getAsString().split("\n");
            settings.modifyFilters(FilterType.CONTAINS, set -> set.addAll(Arrays.asList(words)));
            Bot.getInstance().getCore().getSettingsMapper().update(settings);
        }
    }
}
