package dev.starless.maggiordomo.commands.slash;

import dev.starless.maggiordomo.Bot;
import dev.starless.maggiordomo.commands.types.Slash;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.localization.Translations;
import dev.starless.maggiordomo.localization.Messages;
import dev.starless.maggiordomo.utils.discord.Embeds;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.awt.*;

public class MenuCommand implements Slash {

    @Override
    public void execute(Settings settings, SlashCommandInteractionEvent e) {
        if (e.getChannelType().isMessage() && e.getMessageChannel() instanceof TextChannel channel) {
            e.deferReply(true).queue();

            MessageCreateData data = Bot.getInstance().getCore().createMenu(channel.getGuild().getId());
            if (data != null) {
                channel.sendMessage(data).queue(message -> {
                    settings.setMenuID(message.getId());
                    Bot.getInstance().getCore().getSettingsMapper().update(settings);

                    e.getInteraction().getHook().sendMessageEmbeds(new EmbedBuilder()
                                    .setDescription(Translations.string(Messages.COMMAND_MENU_SUCCESS, settings.getLanguage()))
                                    .setColor(Color.decode("#65A25F"))
                                    .build())
                            .setEphemeral(true)
                            .queue();
                });
            } else {
                e.getInteraction().getHook().sendMessageEmbeds(new EmbedBuilder()
                                .setDescription(Translations.string(Messages.COMMAND_MENU_FAIL, settings.getLanguage()))
                                .setColor(Color.red)
                                .build())
                        .setEphemeral(true)
                        .queue();
            }
        } else {
            e.replyEmbeds(Embeds.errorEmbed())
                    .setEphemeral(true)
                    .queue();
        }
    }

    @Override
    public String getName() {
        return "setupmenu";
    }

    @Override
    public String getDescription(String lang) {
        return Translations.string(Messages.COMMAND_MENU_DESCRIPTION, lang);
    }
}
