package dev.starless.maggiordomo.commands.slash;

import dev.starless.maggiordomo.Bot;
import dev.starless.maggiordomo.commands.types.Slash;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.enums.RecordType;
import dev.starless.maggiordomo.data.user.UserRecord;
import dev.starless.maggiordomo.storage.vc.LocalVCMapper;
import dev.starless.maggiordomo.utils.discord.Perms;
import dev.starless.mongo.objects.QueryBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.managers.channel.concrete.VoiceChannelManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ReloadPermsCommand implements Slash {

    @Override
    public void execute(Settings settings, SlashCommandInteractionEvent e) {
        AtomicInteger count = new AtomicInteger(0);

        LocalVCMapper localMapper = Bot.getInstance().getCore()
                .getChannelMapper()
                .getMapper(e.getGuild());

        e.reply("Sto mettendo in coda gli aggiornamenti necessari... ⏳")
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
                                        Arrays.asList(Perms.voiceOwnerPerms),
                                        Collections.emptyList());
                            } else {
                                localMapper.scheduleForDeletion(vc, channel);
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
    }

    @Override
    public String getName() {
        return "refreshperms";
    }

    @Override
    public String getDescription(String lang) {
        return "Aggiorna i permessi di tutte le vocali";
    }
}
