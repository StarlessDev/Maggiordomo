package dev.starless.maggiordomo;

import com.anyascii.AnyAscii;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.filter.FilterResult;
import dev.starless.maggiordomo.data.filter.FilterType;
import dev.starless.maggiordomo.data.filter.IFilter;
import dev.starless.maggiordomo.data.filter.impl.ContainsFilter;
import dev.starless.maggiordomo.data.filter.impl.PatternFilter;

import java.util.HashSet;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Filters {

    private final Pattern emojiPattern = Pattern.compile(":\\w{1,32}:");
    private final Map<Character, Character> leetMap = Map.of(
            '1', 'i',
            '2', 'l',
            '3', 'e',
            '4', 'a',
            '6', 'g',
            '7', 't',
            '8', 'b',
            '0', 'o'
    );

    private final ContainsFilter containsFilter = new ContainsFilter();
    private final PatternFilter patternFilter = new PatternFilter();

    public FilterResult check(Settings settings, String input) {
        String trimmed = input.trim();
        String normalized = normalize(trimmed);
        for (FilterType type : FilterType.values()) {
            IFilter filter = switch (type) {
                case BASIC -> containsFilter;
                case REGEX -> patternFilter;
            };

            for (String str : settings.getFilterStrings().getOrDefault(type, new HashSet<>())) {
                FilterResult result = filter.apply(settings, normalized, str);
                if (result.flagged()) {
                    return result;
                }
            }
        }

        return new FilterResult(false, trimmed);
    }

    private String normalize(String input) {
        // Try to switch unicode characters with their best ascii representation.
        // For example: 🅾 will be translated to 'O'
        String transliteration = AnyAscii.transliterate(input);

        // The emojis will get translated to something like this: :rofl:.
        // Since there are emojis which represent letters, we try to remove the double quotes
        // and keep only the letters to avoid bypasses
        StringBuilder normalized = new StringBuilder(transliteration);
        Matcher emojiMatches = emojiPattern.matcher(transliteration);

        // It's important to filter from the start to the end of the string for the code to work
        emojiMatches.results().forEachOrdered(new Consumer<>() {
            int offset = 0;

            @Override
            public void accept(MatchResult match) {
                // The offset accounts for previously deleted chars.
                // I subtracted 2 from match.end() since it's exclusive
                // and to account for the char just deleted
                normalized.deleteCharAt(match.start() - offset).deleteCharAt(match.end() - offset - 2);
                offset += 2;
            }
        });

        // Account for leet speak
        for (int i = 0; i < normalized.length(); i++) {
            char c = leetMap.getOrDefault(normalized.charAt(i), Character.MIN_VALUE);
            if (c != Character.MIN_VALUE) {
                normalized.setCharAt(i, Character.toLowerCase(c));
            }
        }

        return normalized.toString();
    }
}
