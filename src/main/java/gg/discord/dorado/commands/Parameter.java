package gg.discord.dorado.commands;

import net.dv8tion.jda.api.interactions.commands.OptionType;

public record Parameter(OptionType type, String name, String description, boolean required) {

}
