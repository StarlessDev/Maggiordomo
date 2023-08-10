package dev.starless.maggiordomo.data.filter.impl;

import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.filter.FilterResult;
import dev.starless.maggiordomo.data.filter.FilterType;
import dev.starless.maggiordomo.data.filter.IFilter;
import dev.starless.maggiordomo.localization.Translations;
import dev.starless.maggiordomo.localization.Messages;

public class ContainsFilter implements IFilter {

    @Override
    public FilterResult apply(Settings settings, String input, String value) {
        boolean normalFlag = input.toLowerCase().contains(value.toLowerCase());
        if (normalFlag) {
            return new FilterResult(true, Translations.string(Messages.FILTER_FLAG_CONTAINS, settings.getLanguage(), value));
        } else {
            boolean leetFlag = input.toLowerCase()
                    .replaceAll("1", "i")
                    .replaceAll("2", "l")
                    .replaceAll("3", "e")
                    .replaceAll("4", "a")
                    .replaceAll("8", "b")
                    .replaceAll("0", "o")
                    .contains(value.toLowerCase());

            String message = Translations.string(Messages.FILTER_FLAG_CONTAINS, settings.getLanguage(), value) + " (leet speak)";
            return leetFlag ? new FilterResult(true, message) : new FilterResult();
        }
    }

    @Override
    public FilterType type() {
        return FilterType.CONTAINS;
    }
}
