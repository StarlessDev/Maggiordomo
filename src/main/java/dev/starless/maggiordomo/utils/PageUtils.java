package dev.starless.maggiordomo.utils;

import dev.starless.maggiordomo.localization.Messages;
import dev.starless.maggiordomo.localization.Translations;
import lombok.experimental.UtilityClass;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class PageUtils {

    public int getPageFromId(@NotNull String id) {
        String[] parts = id.split(":");
        int page = 0; // numero della pagina di default
        if (parts.length >= 2) {
            try {
                page = Integer.parseInt(parts[1]);
            } catch (NumberFormatException ex) {
                return -1;
            }
        }

        return page;
    }

    public Button getBackButton(String name, int page, String language) {
        Button button = Button.secondary(name + ":" + (page - 1), Translations.string(Messages.PREV_BUTTON, language));

        return page <= 0 ? button.asDisabled() : button;
    }

    public Button getNextButton(String name, int maxPages, int page, String language) {
        Button button = Button.primary(name + ":" + (page + 1), Translations.string(Messages.NEXT_BUTTON, language));

        return page + 1 >= maxPages ? button.asDisabled() : button;
    }
}
