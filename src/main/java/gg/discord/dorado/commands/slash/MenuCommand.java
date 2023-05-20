package gg.discord.dorado.commands.slash;

import gg.discord.dorado.Bot;
import gg.discord.dorado.commands.CommandInfo;
import gg.discord.dorado.commands.types.Slash;
import gg.discord.dorado.data.Settings;
import gg.discord.dorado.utils.discord.Embeds;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.awt.*;

@CommandInfo(name = "setupmenu", description = "Manda il menu per controllare le proprie stanze")
public class MenuCommand implements Slash {

    @Override
    public void execute(Settings settings, SlashCommandInteractionEvent e) {
        if (e.getChannelType().isMessage() && e.getMessageChannel() instanceof TextChannel channel) {
            e.deferReply(true).queue();

            Bot.getInstance().getCore().sendMenu(channel);

            e.getInteraction().getHook().sendMessageEmbeds(new EmbedBuilder()
                            .setDescription("Menu creato!")
                            .setColor(Color.decode("#65A25F"))
                            .build())
                    .setEphemeral(true)
                    .queue();
        } else {
            e.replyEmbeds(Embeds.errorEmbed())
                    .setEphemeral(true)
                    .queue();
        }
    }
}
