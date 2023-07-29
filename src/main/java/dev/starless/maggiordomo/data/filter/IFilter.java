package dev.starless.maggiordomo.data.filter;

import dev.starless.maggiordomo.data.Settings;

public interface IFilter {

    FilterResult apply(Settings settings, String input, String value);

    FilterType type();
}
