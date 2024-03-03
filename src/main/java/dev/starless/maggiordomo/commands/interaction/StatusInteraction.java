package dev.starless.maggiordomo.commands.interaction;

import dev.starless.maggiordomo.Core;
import dev.starless.maggiordomo.commands.types.Interaction;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.enums.VCState;
import dev.starless.maggiordomo.data.VC;
import dev.starless.maggiordomo.localization.Translations;
import dev.starless.maggiordomo.localization.Messages;
import dev.starless.maggiordomo.utils.discord.Embeds;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.requests.restaction.PermissionOverrideAction;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

import java.awt.*;

public class StatusInteraction implements Interaction {

    @Override
    public VC onButtonInteraction(Core core, VC vc, Settings settings, String id, ButtonInteractionEvent e) {
        String open = Translations.string(Messages.VC_OPEN_STATUS, settings.getLanguage());
        String locked = Translations.string(Messages.VC_LOCKED_STATUS, settings.getLanguage());
        String content = Translations.string(Messages.INTERACTION_STATUS_CURRENT, settings.getLanguage(), (vc.getState().equals(VCState.OPEN) ? open : locked).toLowerCase());

        e.reply(new MessageCreateBuilder()
                        .setContent(content)
                        .addComponents(ActionRow.of(StringSelectMenu.create(getName())
                                .setMaxValues(1)
                                .setPlaceholder(Translations.string(Messages.INTERACTION_STATUS_SELECTION_TITLE, settings.getLanguage()))
                                .addOption(open, VCState.OPEN.name())
                                .addOption(locked, VCState.LOCKED.name())
                                .build()))
                        .build())
                .setEphemeral(true)
                .queue();

        return null;
    }

    @Override
    public VC onStringSelected(Core core, VC vc, Settings settings, String id, StringSelectInteractionEvent e) {
        if (!e.getValues().isEmpty()) {
            String label = e.getValues().get(0);
            // Controlla se il ruolo esiste
            Role usersRole = e.getGuild().getRoleById(settings.getPublicRole());
            if (usersRole == null) {
                e.replyEmbeds(Embeds.errorEmbed(Translations.string(Messages.INVALID_PUB_ROLE, settings.getLanguage())))
                        .setEphemeral(true)
                        .queue();

                return null;
            }

            try {
                VCState status = VCState.valueOf(label);
                vc.setState(status);

                // Cambia i permessi della stanza, se presente
                VoiceChannel channel = e.getGuild().getVoiceChannelById(vc.getChannel());
                if(channel != null) {
                    PermissionOverrideAction everyonePerms = channel.upsertPermissionOverride(usersRole);

                    if(vc.getState().equals(VCState.LOCKED)) {
                        everyonePerms = everyonePerms.setDenied(Permission.VOICE_CONNECT);
                    } else {
                        everyonePerms = everyonePerms.setAllowed(Permission.VOICE_CONNECT);
                    }

                    if(!channel.getMembers().isEmpty()) {
                        everyonePerms = everyonePerms.grant(Permission.VIEW_CHANNEL);
                    } else {
                        everyonePerms = everyonePerms.deny(Permission.VIEW_CHANNEL);
                    }

                    everyonePerms.queue();
                }

                // Setta il messaggio di risposta
                Messages successMessage = status.equals(VCState.OPEN) ? Messages.INTERACTION_STATUS_SUCCESS_OPEN : Messages.INTERACTION_STATUS_SUCCESS_LOCKED;
                Color embedColor = status.equals(VCState.OPEN) ? new Color(239, 210, 95) : new Color(100, 160, 94);
                e.replyEmbeds(new EmbedBuilder()
                                .setDescription(Translations.string(successMessage, settings.getLanguage()))
                                .setColor(embedColor)
                                .build())
                        .setEphemeral(true)
                        .queue();

                return vc;
            } catch (IllegalArgumentException ex) {
                e.replyEmbeds(Embeds.errorEmbed(Translations.string(Messages.GENERIC_ERROR, settings.getLanguage())))
                        .setEphemeral(true)
                        .queue();
            }
        }

        return null;
    }

    @Override
    public Emoji emoji() {
        return Emoji.fromUnicode("🔐");
    }

    @Override
    public long timeout() {
        return 30;
    }

    @Override
    public String getName() {
        return "status";
    }
}
