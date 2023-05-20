package gg.discord.dorado.utils.discord;

import gg.discord.dorado.data.enums.VCStatus;
import lombok.experimental.UtilityClass;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.managers.channel.concrete.VoiceChannelManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@UtilityClass
public class Perms {

    public final Permission[] selfPerms = {
            Permission.VIEW_CHANNEL,
            Permission.VOICE_CONNECT,
            Permission.VOICE_MOVE_OTHERS,
            Permission.MANAGE_PERMISSIONS,
            Permission.MANAGE_CHANNEL
    };

    public final Permission[] ownerPerms = {
            Permission.VIEW_CHANNEL,
            Permission.VOICE_CONNECT,
            Permission.VOICE_SPEAK,
            Permission.VOICE_USE_VAD,
            Permission.VOICE_STREAM,
            Permission.VOICE_MOVE_OTHERS,
            Permission.VOICE_USE_SOUNDBOARD,
            Permission.VOICE_USE_EXTERNAL_SOUNDS,
            Permission.MESSAGE_SEND,
            Permission.MESSAGE_ADD_REACTION,
            Permission.MESSAGE_EXT_STICKER,
            Permission.MESSAGE_EXT_EMOJI,
            Permission.MESSAGE_MANAGE
    };

    private final List<Permission> publicAllowedPerms = Arrays.asList(
            Permission.MESSAGE_SEND,
            Permission.VOICE_SPEAK,
            Permission.VOICE_USE_VAD,
            Permission.VOICE_STREAM,
            Permission.VOICE_USE_SOUNDBOARD,
            Permission.VOICE_USE_EXTERNAL_SOUNDS,
            Permission.MESSAGE_ADD_REACTION,
            Permission.MESSAGE_EXT_STICKER,
            Permission.MESSAGE_EXT_EMOJI
    );

    private final List<Permission> publicDeniedPerms = Arrays.asList(
            Permission.CREATE_PUBLIC_THREADS,
            Permission.CREATE_PRIVATE_THREADS);

    public VoiceChannelManager setPublicPerms(VoiceChannelManager manager, VCStatus status, Role publicRole, boolean visible) {
        if (manager == null || publicRole == null) return manager;

        List<Permission> allowed = new ArrayList<>(publicAllowedPerms);
        List<Permission> denied = new ArrayList<>(publicDeniedPerms);

        // Status della stanza
        if (status.equals(VCStatus.LOCKED)) {
            denied.add(Permission.VOICE_CONNECT);
        }

        if (visible) {
            allowed.add(Permission.VIEW_CHANNEL);
        } else {
            denied.add(Permission.VIEW_CHANNEL);
        }

        return manager.putRolePermissionOverride(publicRole.getIdLong(), allowed, denied);
    }

    public VoiceChannelManager ban(Member member, VoiceChannelManager manager) {
        if (manager == null) return null;

        return manager.putMemberPermissionOverride(member.getIdLong(),
                Collections.emptyList(),
                List.of(Permission.VIEW_CHANNEL,
                        Permission.VOICE_SPEAK,
                        Permission.VOICE_CONNECT,
                        Permission.MESSAGE_SEND));
    }

    public VoiceChannelManager trust(Member member, VoiceChannelManager manager) {
        if (manager == null) return null;

        return manager.putMemberPermissionOverride(member.getIdLong(),
                List.of(Permission.VIEW_CHANNEL,
                        Permission.VOICE_SPEAK,
                        Permission.VOICE_CONNECT,
                        Permission.MESSAGE_SEND),
                Collections.emptyList());
    }


    public void reset(Member member, VoiceChannelManager manager) {
        if (manager == null) return;

        manager.removePermissionOverride(member).queue();
    }
}
