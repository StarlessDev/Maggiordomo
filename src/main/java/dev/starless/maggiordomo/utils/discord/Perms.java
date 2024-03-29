package dev.starless.maggiordomo.utils.discord;

import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.enums.VCState;
import lombok.experimental.UtilityClass;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.managers.channel.concrete.TextChannelManager;
import net.dv8tion.jda.api.managers.channel.concrete.VoiceChannelManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@UtilityClass
public class Perms {

    // Perms for the users' rooms
    public final List<Permission> voiceSelfPerms = List.of(
            Permission.VIEW_CHANNEL,
            Permission.VOICE_CONNECT,
            Permission.VOICE_MOVE_OTHERS,
            Permission.MANAGE_PERMISSIONS,
            Permission.MANAGE_CHANNEL
    );

    public final List<Permission> voiceOwnerPerms = List.of(
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
    );

    private final List<Permission> voicePublicAllowedPerms = List.of(
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

    private final List<Permission> voicePublicDeniedPerms = Arrays.asList(
            Permission.CREATE_PUBLIC_THREADS,
            Permission.CREATE_PRIVATE_THREADS);

    // Perms granted/denied to trusted/banned users
    public final List<Permission> specialPerms = List.of(
            Permission.VIEW_CHANNEL,
            Permission.VOICE_SPEAK,
            Permission.VOICE_CONNECT,
            Permission.VOICE_STREAM,
            Permission.MESSAGE_SEND
    );

    // Perms for the dashboard channel
    public final List<Permission> dashboardAllowedPerms = Collections.singletonList(Permission.VIEW_CHANNEL);

    public final List<Permission> dashboardDeniedPerms = List.of(
            Permission.MESSAGE_SEND,
            Permission.MESSAGE_SEND_IN_THREADS,
            Permission.CREATE_PUBLIC_THREADS,
            Permission.CREATE_PRIVATE_THREADS,
            Permission.MESSAGE_SEND_IN_THREADS,
            Permission.USE_APPLICATION_COMMANDS
    );

    // Perms for the voice channel generator
    public final List<Permission> createAllowedPerms = List.of(
            Permission.VIEW_CHANNEL,
            Permission.VOICE_CONNECT,
            Permission.VOICE_MOVE_OTHERS
    );

    public final List<Permission> createDeniedPerms = List.of(
            Permission.MESSAGE_SEND,
            Permission.MESSAGE_SEND_IN_THREADS,
            Permission.CREATE_PUBLIC_THREADS,
            Permission.CREATE_PRIVATE_THREADS,
            Permission.MESSAGE_SEND_IN_THREADS,
            Permission.VOICE_SPEAK,
            Permission.VOICE_STREAM,
            Permission.USE_APPLICATION_COMMANDS
    );

    public void updatePublicPerms(Guild guild, Settings settings, Role oldRole, Role newRole) {
        if (oldRole == null || newRole == null) return;

        Role everyone = guild.getPublicRole();
        boolean newRoleNotEveryone = everyone.getIdLong() != newRole.getIdLong();

        // In pratica imposta tutti i permessi del vecchio ruolo
        // sul nuovo ruolo
        settings.getCategories().forEach(categoryID -> {
            Category category = guild.getCategoryById(categoryID);
            if (category != null) {
                // Setta nuovamente i permessi delle vocali
                category.getVoiceChannels().forEach(voiceChannel -> {
                    if (voiceChannel.getId().equals(settings.getVoiceGeneratorID())) return;

                    VoiceChannelManager manager = voiceChannel.getManager();
                    PermissionOverride oldPerms = voiceChannel.getPermissionOverride(oldRole);
                    if (oldPerms != null) { // Non dovrebbe mai succedere
                        long allowed = oldPerms.getAllowedRaw();
                        long denied = oldPerms.getDeniedRaw();

                        manager.putRolePermissionOverride(newRole.getIdLong(), allowed, denied);

                        if (oldRole.getIdLong() == everyone.getIdLong()) {
                            manager.putRolePermissionOverride(oldRole.getIdLong(),
                                    Collections.emptyList(),
                                    List.of(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT, Permission.MESSAGE_SEND));
                        } else {
                            manager.removePermissionOverride(oldRole);
                        }

                        manager.queue();
                    }
                });
            }
        });

        // Reimposta i permessi dei canali
        TextChannel dashboardChannel = guild.getTextChannelById(settings.getMenuChannelID());
        if (dashboardChannel != null) {
            TextChannelManager manager = dashboardChannel.getManager()
                    .removePermissionOverride(oldRole)
                    .putRolePermissionOverride(newRole.getIdLong(), dashboardAllowedPerms, dashboardDeniedPerms);

            if (newRoleNotEveryone) {
                manager.putRolePermissionOverride(everyone.getIdLong(),
                        0,
                        Permission.ALL_PERMISSIONS);
            }

            manager.queue();
        }

        VoiceChannel createChannel = guild.getVoiceChannelById(settings.getVoiceGeneratorID());
        if (createChannel != null) {
            VoiceChannelManager manager = createChannel.getManager()
                    .removePermissionOverride(oldRole)
                    .putRolePermissionOverride(newRole.getIdLong(), Perms.createAllowedPerms, Perms.createDeniedPerms);

            if (newRoleNotEveryone) {
                manager.putRolePermissionOverride(everyone.getIdLong(),
                        List.of(Permission.VOICE_SPEAK, Permission.VOICE_USE_VAD),
                        Collections.singletonList(Permission.VIEW_CHANNEL));
            }

            manager.queue();
        }
    }

    public VoiceChannelManager setPublicPerms(VoiceChannelManager manager, VCState status, Role publicRole, boolean visible) {
        if (manager == null || publicRole == null) return manager;

        Role everyone = publicRole.getGuild().getPublicRole();
        if (publicRole.getIdLong() != everyone.getIdLong()) {
            manager = manager.putRolePermissionOverride(everyone.getIdLong(),
                    Collections.emptyList(),
                    List.of(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT, Permission.MESSAGE_SEND));
        }

        List<Permission> allowed = new ArrayList<>(voicePublicAllowedPerms);
        List<Permission> denied = new ArrayList<>(voicePublicDeniedPerms);

        // Status della stanza
        if (status.equals(VCState.LOCKED)) {
            denied.add(Permission.VOICE_CONNECT);
        }

        if (visible) {
            allowed.add(Permission.VIEW_CHANNEL);
        } else {
            denied.add(Permission.VIEW_CHANNEL);
        }

        return manager.putRolePermissionOverride(publicRole.getIdLong(), allowed, denied);
    }

    public VoiceChannelManager ban(IPermissionHolder holder, VoiceChannelManager manager) {
        if (manager == null) return null;

        return manager.putPermissionOverride(holder, Collections.emptyList(), specialPerms);
    }

    public VoiceChannelManager trust(IPermissionHolder holder, VoiceChannelManager manager) {
        if (manager == null) return null;

        return manager.putPermissionOverride(holder, specialPerms, Collections.emptyList());
    }


    public void reset(IPermissionHolder holder, VoiceChannelManager manager) {
        if (manager == null) return;

        manager.removePermissionOverride(holder).queue();
    }

    public boolean isAdmin(Member member) {
        return member.hasPermission(Permission.MANAGE_SERVER);
    }
}
