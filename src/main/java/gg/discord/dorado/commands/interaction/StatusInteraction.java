package gg.discord.dorado.commands.interaction;

import gg.discord.dorado.commands.CommandInfo;
import gg.discord.dorado.commands.types.Interaction;
import gg.discord.dorado.data.Settings;
import gg.discord.dorado.data.enums.VCStatus;
import gg.discord.dorado.data.user.VC;
import gg.discord.dorado.utils.discord.Embeds;
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

@CommandInfo(name = "status", description = "Imposta se tutti possono entrare nella tua stanza oppure solo quelli trustati")
public class StatusInteraction implements Interaction {

    @Override
    public VC execute(VC vc, Settings guild, String id, ButtonInteractionEvent e) {
        String content = String.format("Al momento la stanza è **%s**",
                vc.getStatus().equals(VCStatus.OPEN) ? "aperta" : "chiusa");

        e.reply(new MessageCreateBuilder()
                        .setContent(content)
                        .addComponents(ActionRow.of(StringSelectMenu.create(getName())
                                .setMaxValues(1)
                                .setPlaceholder("Come vuoi la tua stanza?")
                                .addOption("Aperta", VCStatus.OPEN.name())
                                .addOption("Chiusa", VCStatus.LOCKED.name())
                                .build()))
                        .build())
                .setEphemeral(true)
                .queue();

        return null;
    }

    @Override
    public VC execute(VC vc, Settings settings, String id, StringSelectInteractionEvent e) {
        String label = e.getValues().get(0);
        if (label != null) {
            // Controlla se il ruolo esiste
            Role usersRole = e.getGuild().getRoleById(settings.getPublicRole());
            if (usersRole == null) {
                e.replyEmbeds(Embeds.errorEmbed("Il ruolo degli utenti è invalido!"))
                        .setEphemeral(true)
                        .queue();

                return null;
            }

            try {
                VCStatus status = VCStatus.valueOf(label);
                vc.setStatus(status);

                // Cambia i permessi della stanza, se presente
                VoiceChannel channel = e.getGuild().getVoiceChannelById(vc.getChannel());
                if(channel != null) {
                    PermissionOverrideAction everyonePerms = channel.upsertPermissionOverride(usersRole);

                    if(vc.getStatus().equals(VCStatus.LOCKED)) {
                        everyonePerms = everyonePerms.setDenied(Permission.VOICE_CONNECT);
                    } else {
                        everyonePerms = everyonePerms.setAllowed(Permission.VOICE_CONNECT);
                    }

                    if(channel.getMembers().size() > 0) {
                        everyonePerms = everyonePerms.grant(Permission.VIEW_CHANNEL);
                    } else {
                        everyonePerms = everyonePerms.deny(Permission.VIEW_CHANNEL);
                    }

                    everyonePerms.queue();
                }

                // Setta il messaggio di risposta
                String statusDescription = status.equals(VCStatus.OPEN) ? "aperta a tutti" : "aperta solo agli utenti trustati";
                Color embedColor = status.equals(VCStatus.OPEN) ? new Color(239, 210, 95) : new Color(100, 160, 94);
                e.replyEmbeds(new EmbedBuilder()
                                .setDescription(String.format("Ora la tua stanza è %s.", statusDescription))
                                .setColor(embedColor)
                                .build())
                        .setEphemeral(true)
                        .queue();

                return vc;
            } catch (IllegalArgumentException ex) {
                ex.printStackTrace();
                e.replyEmbeds(Embeds.errorEmbed())
                        .setEphemeral(true)
                        .queue();
            }
        } else {
            e.replyEmbeds(Embeds.errorEmbed("Non hai selezionato niente? :face_with_spiral_eyes:"))
                    .setEphemeral(true)
                    .queue();
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
}
