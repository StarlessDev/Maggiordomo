package dev.starless.maggiordomo.commands.types;

import dev.starless.maggiordomo.Core;
import dev.starless.maggiordomo.commands.Command;
import dev.starless.maggiordomo.commands.Parameter;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.utils.discord.Perms;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public interface Slash extends Command {

    void execute(Core core, Settings settings, SlashCommandInteractionEvent e);

    default void autocomplete(Settings settings, CommandAutoCompleteInteractionEvent e) {

    }

    default Parameter[] getParameters(String lang) {
        return new Parameter[0];
    }

    @Override
    default boolean hasPermission(Member member, Settings settings) {
        return Perms.isAdmin(member);
    }
}
