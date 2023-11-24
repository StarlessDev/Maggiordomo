package dev.starless.maggiordomo.commands.interaction.management;

import dev.starless.maggiordomo.commands.types.Interaction;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.user.VC;
import dev.starless.maggiordomo.utils.discord.Perms;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.util.Arrays;
import java.util.Collections;

public abstract class AManagementInteraction implements Interaction {

    @Override
    public VC onButtonInteraction(VC vc, Settings settings, String id, ButtonInteractionEvent e) {
        String[] parts = id.split(":");
        MessageEditData edit = handle(e, settings, parts.length > 1 ? Arrays.copyOfRange(parts, 1, parts.length) : new String[0]);
        if (edit != null) {
            e.editMessage(edit)
                    .setAllowedMentions(Collections.emptyList())
                    .setReplace(true)
                    .queue();
        }

        return null;
    }

    protected abstract MessageEditData handle(ButtonInteractionEvent e, Settings settings, String[] parts);

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
