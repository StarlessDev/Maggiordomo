package dev.starless.maggiordomo.commands.interaction.management;

import dev.starless.maggiordomo.Bot;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.enums.RecordType;
import dev.starless.maggiordomo.data.user.UserRecord;
import dev.starless.maggiordomo.localization.Messages;
import dev.starless.maggiordomo.localization.Translations;
import dev.starless.maggiordomo.storage.vc.LocalVCMapper;
import dev.starless.maggiordomo.utils.discord.Perms;
import dev.starless.mongo.api.QueryBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.managers.channel.concrete.VoiceChannelManager;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class RefreshPerms extends AManagementInteraction {

    @Override
    protected MessageEditData handle(ButtonInteractionEvent e, Settings settings, String[] parts) {
        AtomicInteger count = new AtomicInteger(0);

        LocalVCMapper localMapper = Bot.getInstance().getCore()
                .getChannelMapper()
                .getMapper(e.getGuild());

        e.reply(Translations.string(Messages.COMMAND_RELOAD_PERMS_WAITING, settings.getLanguage()))
                .setEphemeral(true)
                .queue();

        localMapper.bulkSearch(QueryBuilder.init()
                        .add("guild", e.getGuild().getId())
                        .create())
                .forEach(vc -> Optional.ofNullable(e.getGuild().getVoiceChannelById(vc.getChannel()))
                        .ifPresent(channel -> {
                            VoiceChannelManager manager = channel.getManager();

                            // Owner
                            Member owner = e.getGuild().getMemberById(vc.getUser());
                            if (owner != null) {
                                manager = manager.putMemberPermissionOverride(owner.getIdLong(),
                                        Perms.voiceOwnerPerms,
                                        Collections.emptyList());
                            } else {
                                localMapper.scheduleForDeletion(vc, channel).complete();
                                localMapper.delete(vc);
                                return;
                            }

                            // Trusted & untrusted
                            for (UserRecord record : vc.getTotalRecords()) {
                                Member targetRecord = e.getGuild().getMemberById(record.user());
                                if (targetRecord != null) {
                                    if (record.type().equals(RecordType.TRUST)) {
                                        manager = manager.putMemberPermissionOverride(targetRecord.getIdLong(),
                                                List.of(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT),
                                                channel.upsertPermissionOverride(targetRecord).getDeniedPermissions());
                                    } else {
                                        manager = manager.putMemberPermissionOverride(targetRecord.getIdLong(),
                                                channel.upsertPermissionOverride(targetRecord).getAllowedPermissions(),
                                                List.of(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT));
                                    }
                                }
                            }

                            // PublicRole
                            Role role = e.getGuild().getRoleById(settings.getPublicRole());
                            if (role != null) {
                                manager = Perms.setPublicPerms(manager, vc.getStatus(), role, !channel.getMembers().isEmpty());
                            }

                            manager.queueAfter(count.incrementAndGet() * 250L, TimeUnit.MILLISECONDS);
                        }));

        return null;
    }

    @Override
    public String getName() {
        return "refreshperms";
    }
}
