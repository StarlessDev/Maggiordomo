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
    protected void onInputReceived(Settings settings, ModalInteractionEvent e) {
        ModalMapping mapping = e.getValue("input");
        if (mapping != null) {
            String[] words = mapping.getAsString().split("\n");
            settings.modifyFilters(FilterType.BASIC, set -> set.addAll(Arrays.asList(words)));
            Bot.getInstance().getCore().getSettingsMapper().update(settings);
        }
    }

    @Override
    public String getName() {
        return "contains";
    }
}
