package dev.starless.maggiordomo.commands.types;

import dev.starless.maggiordomo.commands.Command;
import dev.starless.maggiordomo.commands.Parameter;
import dev.starless.maggiordomo.data.Settings;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public interface Slash extends Command {

    void execute(Settings settings, SlashCommandInteractionEvent e);

    default Parameter[] getParameters() {
        return new Parameter[0];
    }

    @Override
    default boolean hasPermission(Member member, Settings settings) {
        return member.hasPermission(Permission.ADMINISTRATOR);
    }
}
