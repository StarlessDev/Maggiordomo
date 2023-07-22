package dev.starless.maggiordomo.data.filter.impl;

import dev.starless.maggiordomo.data.filter.FilterResult;
import dev.starless.maggiordomo.data.filter.FilterType;
import dev.starless.maggiordomo.data.filter.IFilter;

public class ContainsFilter implements IFilter {

    @Override
    public FilterResult apply(String input, String value) {
        boolean normalFlag = input.toLowerCase().contains(value.toLowerCase());
        if (normalFlag) {
            return new FilterResult(true, String.format("Contains the word `%s`", value));
        } else {
            boolean leetFlag = input.toLowerCase()
                    .replaceAll("1", "i")
                    .replaceAll("2", "l")
                    .replaceAll("3", "e")
                    .replaceAll("4", "a")
                    .replaceAll("8", "b")
                    .replaceAll("0", "o")
                    .contains(value.toLowerCase());

            return leetFlag ? new FilterResult(true, String.format("parola non consentita `%s` (leet speak)", value)) : new FilterResult();
        }
    }

    @Override
    public FilterType type() {
        return FilterType.CONTAINS;
    }
}
