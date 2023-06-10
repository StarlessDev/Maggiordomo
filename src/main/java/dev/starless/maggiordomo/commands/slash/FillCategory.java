package dev.starless.maggiordomo.commands.slash;

import dev.starless.maggiordomo.commands.CommandInfo;
import dev.starless.maggiordomo.commands.Parameter;
import dev.starless.maggiordomo.commands.types.Slash;
import dev.starless.maggiordomo.data.Settings;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;

@CommandInfo(name = "fillcat", description = "test")
public class FillCategory implements Slash {

    @Override
    public void execute(Settings settings, SlashCommandInteractionEvent e) {
        GuildChannelUnion channel = e.getOption("cat").getAsChannel();
        Category category = channel.asCategory();

        for (int i = 0; i < 48; i++) {
            category.createVoiceChannel("test " + i).queue();
        }
    }

    @Override
    public Parameter[] getParameters() {
        return new Parameter[]{
                new Parameter(OptionType.CHANNEL, "cat", "sas", true)
        };
    }
}
