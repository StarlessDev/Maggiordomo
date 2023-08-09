package dev.starless.maggiordomo.localization;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.stream.Stream;

@Getter
@RequiredArgsConstructor
public enum DefaultLanguages {

    ENGLISH("en", "English"),
    ITALIAN("it", "Italiano");

    private final String code;
    private final String name;

    public static Optional<DefaultLanguages> fromCode(String code) {
        return Stream.of(DefaultLanguages.values())
                .filter(lang -> lang.getCode().equals(code))
                .findFirst();
    }
}
