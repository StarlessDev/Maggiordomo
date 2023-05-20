package gg.discord.dorado.commands.types;

import gg.discord.dorado.commands.Command;
import gg.discord.dorado.commands.Parameter;
import gg.discord.dorado.data.Settings;
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
