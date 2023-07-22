package dev.starless.maggiordomo.data.filter.impl;

import dev.starless.maggiordomo.data.filter.FilterResult;
import dev.starless.maggiordomo.data.filter.FilterType;
import dev.starless.maggiordomo.data.filter.IFilter;

import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class PatternFilter implements IFilter {

    @Override
    public FilterResult apply(String input, String value) {
        Pattern pattern;
        try {
            pattern = Pattern.compile(value);
        } catch (PatternSyntaxException ex) {
            return new FilterResult();
        }

        List<MatchResult> results = pattern.matcher(input).results().toList();
        if (results.isEmpty()) {
            return new FilterResult();
        } else {
            StringBuilder sb = new StringBuilder("le seguenti parti del nome violano le regole:\n");
            results.forEach(result -> sb.append("・ `").append(result.group()).append("`\n"));

            return new FilterResult(true, sb.toString());
        }
    }

    @Override
    public FilterType type() {
        return FilterType.REGEX;
    }
}
