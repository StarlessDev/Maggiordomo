package dev.starless.maggiordomo.commands;

import net.dv8tion.jda.api.interactions.commands.OptionType;

public record Parameter(OptionType type, String name, String description, boolean required, boolean autocomplete) {

    public Parameter(OptionType type, String name, String description, boolean required) {
        this(type, name, description, required, false);
    }
}
