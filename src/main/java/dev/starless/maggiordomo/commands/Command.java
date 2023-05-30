package dev.starless.maggiordomo.commands;

import dev.starless.maggiordomo.data.Settings;
import net.dv8tion.jda.api.entities.Member;

public interface Command {

    default CommandInfo getCommandInfo() {
        return this.getClass().getAnnotation(CommandInfo.class);
    }

    default String getName() {
        return getCommandInfo().name();
    }

    default String getDescription() {
        return getCommandInfo().description();
    }

    default boolean hasPermission(Member member, Settings settings) {
        return true;
    }
}
