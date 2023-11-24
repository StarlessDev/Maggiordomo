package dev.starless.maggiordomo.commands.slash;

import dev.starless.maggiordomo.commands.types.Interaction;
import dev.starless.maggiordomo.commands.types.Slash;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.user.VC;
import dev.starless.maggiordomo.localization.Messages;
import dev.starless.maggiordomo.localization.Translations;
import dev.starless.maggiordomo.utils.discord.Perms;
import dev.starless.maggiordomo.utils.discord.References;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.util.Collections;

public class ManagementCommand implements Slash, Interaction {

    @Override
    public void execute(Settings settings, SlashCommandInteractionEvent e) {
        e.reply(getMenu(e.getGuild(), settings)).queue();
    }

    @Override
    public VC onButtonInteraction(VC vc, Settings settings, String id, ButtonInteractionEvent e) {
        e.editMessage(MessageEditData.fromCreateData(getMenu(e.getGuild(), settings)))
                .setReplace(true)
                .queue();

        return null;
    }

    private MessageCreateData getMenu(Guild guild, Settings settings) {
        String content = Translations.stringFormatted(Messages.COMMAND_MANAGEMENT_MENU_CONTENT, settings.getLanguage(),
                "publicRole", References.role(guild, settings.getPublicRole()),
                "days", settings.getMaxInactivity() == -1 ? "∞" : settings.getMaxInactivity());
        return new MessageCreateBuilder()
                .setContent(content)
                .addActionRow(
                        Button.primary("premium", Translations.string(Messages.COMMAND_MANAGEMENT_MENU_PREMIUM_ROLES_BUTTON, settings.getLanguage())),
                        Button.primary("blacklist", Translations.string(Messages.COMMAND_MANAGEMENT_MENU_BANNED_ROLES_BUTTON, settings.getLanguage())),
                        Button.primary("filters", Translations.string(Messages.COMMAND_MANAGEMENT_MENU_FILTERS_BUTTON, settings.getLanguage()))
                )
                .addActionRow(
                        Button.danger("refreshperms", Translations.string(Messages.COMMAND_MANAGEMENT_MENU_REFRESH_PERMS_BUTTON, settings.getLanguage())),
                        Button.secondary("manage", Translations.string(Messages.COMMAND_MANAGEMENT_MENU_MANAGE_ROOMS_BUTTON, settings.getLanguage()))
                )
                .setAllowedMentions(Collections.emptyList())
                .build();
    }

    @Override
    public String getName() {
        return "admin";
    }

    @Override
    public String getDescription(String lang) {
        return Translations.string(Messages.COMMAND_MANAGEMENT_DESCRIPTION, lang);
    }

    @Override
    public boolean hasPermission(Member member, Settings settings) {
        return Perms.isAdmin(member);
    }

    @Override
    public boolean needsVC() {
        return false;
    }

    @Override
    public boolean inMenu() {
        return false;
    }
}