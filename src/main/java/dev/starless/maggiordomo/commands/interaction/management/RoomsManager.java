package dev.starless.maggiordomo.commands.interaction.management;

import dev.starless.maggiordomo.Bot;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.VC;
import dev.starless.maggiordomo.localization.Messages;
import dev.starless.maggiordomo.localization.Translations;
import dev.starless.maggiordomo.utils.PageUtils;
import dev.starless.maggiordomo.utils.discord.References;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;

import java.util.ArrayList;
import java.util.List;

public class RoomsManager extends AManagementInteraction {

    @Override
    protected MessageEditBuilder handle(ButtonInteractionEvent e, Settings settings, String[] parts) {
        List<VC> vcs = Bot.getInstance().getCore().getChannelMapper()
                .getMapper(e.getGuild())
                .getCreatedVCs();

        int totalVCs = vcs.size();
        int page = PageUtils.getPageFromInt(parts.length > 0 ? parts[0] : "0");
        int from = page * PageUtils.DROPDOWN_MAX_ENTRIES;
        int to = Math.min(totalVCs, from + PageUtils.DROPDOWN_MAX_ENTRIES);

        StringSelectMenu.Builder dropdown = StringSelectMenu.create("inspect");
        int active = 0;
        for (VC vc : vcs.subList(page, to)) {
            VoiceChannel channel = e.getGuild().getVoiceChannelById(vc.getChannel());
            if (channel != null) {
                dropdown.addOption(vc.getTitle(), vc.getChannel(), Translations.string(Messages.COMMAND_MANAGEMENT_ROOMS_OWNED_BY, settings.getLanguage()) + " " + References.user(vc.getUser()));

                if (!channel.getMembers().isEmpty()) active++;
            }
        }

        List<ActionRow> rows = new ArrayList<>();
        if (!dropdown.getOptions().isEmpty()) {
            rows.add(ActionRow.of(dropdown.build()));
        }
        rows.add(ActionRow.of(PageUtils.getShortBackButton("admin", settings.getLanguage())));
        rows.add(ActionRow.of(
                PageUtils.getBackButton(getName(), page, settings.getLanguage()),
                PageUtils.getNextButton(getName(), PageUtils.getMaxPages(totalVCs), page, settings.getLanguage()))
        );

        return new MessageEditBuilder()
                .setContent(Translations.stringFormatted(Messages.COMMAND_MANAGEMENT_ROOMS_MAIN_MENU, settings.getLanguage(),
                        "total", totalVCs,
                        "active", active))
                .setComponents(rows);
    }

    @Override
    public String getName() {
        return "manage";
    }
}
