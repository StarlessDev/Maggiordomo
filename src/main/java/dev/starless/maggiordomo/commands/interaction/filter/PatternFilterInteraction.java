package dev.starless.maggiordomo.commands.interaction.filter;

import dev.starless.maggiordomo.Bot;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.filter.FilterType;
import dev.starless.maggiordomo.localization.Messages;
import dev.starless.maggiordomo.localization.Translations;
import dev.starless.maggiordomo.storage.settings.SettingsMapper;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class PatternFilterInteraction extends FilterInteraction {

    public PatternFilterInteraction() {
        super(FilterType.REGEX);
    }

    @Override
    protected boolean onInputReceived(SettingsMapper settingsMapper, Settings settings, ModalInteractionEvent e) {
        ModalMapping mapping = e.getValue("input");
        if (mapping != null) {
            try {
                Pattern pattern = Pattern.compile(mapping.getAsString());
                settings.modifyFilters(FilterType.REGEX, set -> set.add(pattern.pattern()));
                settingsMapper.update(settings);

                return true;
            } catch (PatternSyntaxException ex) {
                e.reply(Translations.string(Messages.FILTER_PATTERN_ERROR, settings.getLanguage())
                                + "\n"
                                + ex.getMessage())
                        .setEphemeral(true)
                        .queue();
            }
        }

        return false;
    }

    @Override
    public String getName() {
        return "pattern";
    }
}
