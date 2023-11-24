package dev.starless.maggiordomo.commands.interaction.filter;

import dev.starless.maggiordomo.Bot;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.filter.FilterType;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

import java.util.Arrays;

public class ContainsFilterInteraction extends FilterInteraction {

    public ContainsFilterInteraction() {
        super(FilterType.BASIC);
    }

    @Override
    protected boolean onInputReceived(Settings settings, ModalInteractionEvent e) {
        ModalMapping mapping = e.getValue("input");
        if (mapping != null) {
            String[] words = mapping.getAsString().split("\n");
            for (String word : words) {
                String trimmed = word.trim();
                if (trimmed.isBlank()) continue;

                settings.modifyFilters(FilterType.BASIC, set -> set.add(trimmed));
            }

            Bot.getInstance().getCore().getSettingsMapper().update(settings);

            return true;
        }

        return false;
    }

    @Override
    public String getName() {
        return "contains";
    }
}
