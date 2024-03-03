package dev.starless.maggiordomo.commands.slash;

import dev.starless.maggiordomo.Bot;
import dev.starless.maggiordomo.Core;
import dev.starless.maggiordomo.commands.types.Interaction;
import dev.starless.maggiordomo.commands.types.Slash;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.VC;
import dev.starless.maggiordomo.localization.Messages;
import dev.starless.maggiordomo.localization.Translations;
import dev.starless.maggiordomo.utils.discord.Perms;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

public class ManagementCommand implements Slash, Interaction {

    @Override
    public void execute(Core core, Settings settings, SlashCommandInteractionEvent e) {
        e.reply(core.getManagementMenu(e.getGuild(), settings)).queue();
    }

    @Override
    public VC onButtonInteraction(Core core, VC vc, Settings settings, String id, ButtonInteractionEvent e) {
        e.editMessage(MessageEditData.fromCreateData(core.getManagementMenu(e.getGuild(), settings)))
                .setReplace(true)
                .queue();

        return null;
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