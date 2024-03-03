package dev.starless.maggiordomo.commands.interaction.management;

import dev.starless.maggiordomo.Core;
import dev.starless.maggiordomo.commands.types.Interaction;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.VC;
import dev.starless.maggiordomo.utils.discord.Perms;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;

import java.util.Arrays;
import java.util.Collections;

public abstract class AManagementInteraction implements Interaction {

    @Override
    public VC onButtonInteraction(Core core, VC vc, Settings settings, String id, ButtonInteractionEvent e) {
        String[] parts = id.split(":");
        MessageEditBuilder edit = handle(core, settings, parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : new String[0], e);
        if (edit != null) {
            edit.setAllowedMentions(Collections.emptyList()).setReplace(true);

            if (!e.isAcknowledged()) {
                e.editMessage(edit.build()).queue();
            } else {
                e.getMessage().editMessage(edit.build()).queue();
            }
        }

        return null;
    }

    protected abstract MessageEditBuilder handle(Core core, Settings settings, String[] parts, ButtonInteractionEvent e);

    protected String getFullId(String[] parts) {
        StringBuilder sb = new StringBuilder(getName());
        for (String part : parts) {
            sb.append(":").append(part);
        }

        return sb.toString();
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
