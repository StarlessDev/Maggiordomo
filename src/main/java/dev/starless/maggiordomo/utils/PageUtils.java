package dev.starless.maggiordomo.utils;

import dev.starless.maggiordomo.localization.Messages;
import dev.starless.maggiordomo.localization.Translations;
import lombok.experimental.UtilityClass;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class PageUtils {

    public final int DROPDOWN_MAX_ENTRIES = 25;

    public int getPageFromId(@NotNull String id) {
        return getPageFromId(id.split(":"));
    }

    public int getPageFromId(String[] parts) {
        int page = 0; // numero della pagina di default
        if (parts.length >= 2) {
            page = getPageFromInt(parts[1]);
        }

        return page;
    }

    public int getPageFromInt(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    public Button getBackButton(String name, int page, String language) {
        Button button = Button.secondary(name + ":" + (page - 1), Translations.string(Messages.PREV_BUTTON, language));

        return page <= 0 ? button.asDisabled() : button;
    }

    public Button getShortBackButton(String name, String language) {
        return Button.secondary(name, Translations.string(Messages.SHORT_PREV_BUTTON, language));
    }

    public Button getNextButton(String name, int maxPages, int page, String language) {
        Button button = Button.primary(name + ":" + (page + 1), Translations.string(Messages.NEXT_BUTTON, language));

        return page + 1 >= maxPages ? button.asDisabled() : button;
    }

    public int getMaxPages(int total) {
        return (int) Math.ceil(total / (double) PageUtils.DROPDOWN_MAX_ENTRIES);
    }
}
