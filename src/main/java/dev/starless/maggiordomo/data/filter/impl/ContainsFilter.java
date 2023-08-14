package dev.starless.maggiordomo.data.filter.impl;

import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.filter.FilterResult;
import dev.starless.maggiordomo.data.filter.FilterType;
import dev.starless.maggiordomo.data.filter.IFilter;
import dev.starless.maggiordomo.localization.Messages;
import dev.starless.maggiordomo.localization.Translations;

public class ContainsFilter implements IFilter {

    @Override
    public FilterResult apply(Settings settings, String input, String value) {
        boolean normalFlag = input.toLowerCase().contains(value.toLowerCase());
        if (normalFlag) {
            return new FilterResult(true, Translations.string(Messages.FILTER_FLAG_CONTAINS, settings.getLanguage(), value));
        } else {
            String message = Translations.string(Messages.FILTER_FLAG_CONTAINS, settings.getLanguage(), value) + " (leet speak)";
            return input.contains(value.toLowerCase()) ? new FilterResult(true, message) : new FilterResult();
        }
    }

    @Override
    public FilterType type() {
        return FilterType.BASIC;
    }
}
