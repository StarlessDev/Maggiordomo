package dev.starless.maggiordomo.commands;

import dev.starless.maggiordomo.data.Settings;
import net.dv8tion.jda.api.entities.Member;

public interface Command {

    String getName();

    default String getDescription(String lang) {
        return "";
    }

    default boolean hasPermission(Member member, Settings settings) {
        return true;
    }
}
