package dev.starless.maggiordomo.localization;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DefaultLanguages {

    ENGLISH("en", "English"),
    ITALIAN("it", "Italiano");

    private final String code;
    private final String name;
}
