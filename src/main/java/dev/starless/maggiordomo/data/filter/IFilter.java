package dev.starless.maggiordomo.data.filter;

public interface IFilter {

    FilterResult apply(String input, String value);

    FilterType type();
}
