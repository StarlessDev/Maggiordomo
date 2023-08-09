package dev.starless.maggiordomo.commands.slash;

import dev.starless.maggiordomo.Bot;
import dev.starless.maggiordomo.commands.Parameter;
import dev.starless.maggiordomo.commands.types.Slash;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.enums.RecordType;
import dev.starless.maggiordomo.data.enums.VCStatus;
import dev.starless.maggiordomo.localization.Translations;
import dev.starless.maggiordomo.localization.Messages;
import dev.starless.maggiordomo.storage.vc.LocalVCMapper;
import dev.starless.maggiordomo.utils.discord.Embeds;
import dev.starless.maggiordomo.data.user.VC;
import dev.starless.maggiordomo.utils.discord.Perms;
import dev.starless.mongo.objects.QueryBuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.awt.*;
import java.util.*;
import java.util.List;

public class RecoverCommand implements Slash {

    @Override
    public void execute(Settings settings, SlashCommandInteractionEvent e) {
        OptionMapping voiceMapping = e.getOption("voice");
        OptionMapping boolMapping = e.getOption("pinned");
        if (voiceMapping == null || boolMapping == null) return;

        if (voiceMapping.getChannelType().equals(ChannelType.VOICE)) {
            String publicRole = settings.getPublicRole();
            if (publicRole == null) {
                e.replyEmbeds(Embeds.errorEmbed(Translations.get(Messages.INVALID_PUB_ROLE, settings.getLanguage())))
                        .setEphemeral(true)
                        .queue();
                return;
            }

            VoiceChannel voiceChannel = voiceMapping.getAsChannel().asVoiceChannel();
            Category category = Objects.requireNonNullElse(voiceChannel.getParentCategory(), settings.getAvailableCategory(e.getGuild()));

            String guild = e.getGuild().getId();
            LocalVCMapper localMapper = Bot.getInstance().getCore()
                    .getChannelMapper()
                    .getMapper(guild);

            boolean notFound = localMapper.search(QueryBuilder.init()
                            .add("guild", guild)
                            .add("channel", voiceChannel.getId())
                            .create())
                    .isEmpty();

            if (notFound) {
                String user = null;
                int limit = voiceChannel.getUserLimit();
                List<String> trusted = new ArrayList<>();
                List<String> banned = new ArrayList<>();

                for (PermissionOverride perm : voiceChannel.getMemberPermissionOverrides()) {
                    Member member = perm.getMember();
                    if (member == null || member.getUser().isBot()) continue;

                    boolean isOwner = matches(Perms.voiceOwnerPerms, perm.getAllowed());
                    if (user == null && isOwner) {
                        user = member.getId();
                        continue;
                    }

                    boolean isTrusted = matches(new Permission[]{Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT}, perm.getAllowed());
                    if (isTrusted) {
                        trusted.add(member.getId());
                        continue;
                    }

                    boolean isBanned = matches(new Permission[]{Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT}, perm.getDenied());
                    if (isBanned) {
                        banned.add(member.getId());
                    }
                }

                if (user == null) {
                    e.replyEmbeds(Embeds.errorEmbed(Translations.get(Messages.COMMAND_RECOVER_MISSING_OWNER, settings.getLanguage())))
                            .setEphemeral(true)
                            .queue();
                    return;
                }

                boolean isLocked = voiceChannel.getRolePermissionOverrides()
                        .stream()
                        .filter(perm -> perm.getRole().getId().endsWith(publicRole))
                        .map(perm -> perm.getDenied().contains(Permission.VOICE_CONNECT))
                        .findFirst()
                        .orElse(false);

                VC vc = new VC(guild,
                        user,
                        voiceChannel.getId(),
                        category.getId(),
                        voiceChannel.getName(),
                        limit,
                        isLocked ? VCStatus.LOCKED : VCStatus.OPEN,
                        boolMapping.getAsBoolean());

                trusted.forEach(string -> vc.addRecordPlayer(RecordType.TRUST, string));
                banned.forEach(string -> vc.addRecordPlayer(RecordType.BAN, string));

                localMapper.search(QueryBuilder.init()
                                .add("guild", guild)
                                .add("user", user)
                                .create())
                        .ifPresent(localMapper::delete);
                localMapper.insert(vc);

                e.replyEmbeds(new EmbedBuilder()
                                .setDescription(Translations.get(Messages.COMMAND_RECOVER_SUCCESS, settings.getLanguage(), vc.getTitle()))
                                .setColor(new Color(101, 162, 95))
                                .build())
                        .setEphemeral(true)
                        .queue();
            } else {
                e.replyEmbeds(Embeds.errorEmbed(Translations.get(Messages.COMMAND_RECOVER_NOT_CORRUPTED, settings.getLanguage())))
                        .setEphemeral(true)
                        .queue();
            }
        } else {
            e.replyEmbeds(Embeds.errorEmbed(Translations.get(Messages.COMMAND_RECOVER_NOT_A_VC, settings.getLanguage())))
                    .setEphemeral(true)
                    .queue();
        }
    }

    @Override
    public Parameter[] getParameters(String lang) {

        return new Parameter[]{
                new Parameter(OptionType.CHANNEL, "voice", Translations.get(Messages.COMMAND_RECOVER_PARAMETER_CHANNEL, lang), true),
                new Parameter(OptionType.BOOLEAN, "pinned", Translations.get(Messages.COMMAND_RECOVER_PARAMETER_PINNED, lang), true)};
    }

    private boolean matches(Permission[] data, EnumSet<Permission> toCheck) {
        if (toCheck.size() != data.length) return false;

        Set<Permission> cache = new HashSet<>(toCheck);
        cache.removeIf(perm -> {
            for (Permission datum : data) {
                if (datum.equals(perm)) return true;
            }
            return false;
        });

        return cache.isEmpty();
    }

    @Override
    public String getName() {
        return "recover";
    }

    @Override
    public String getDescription(String lang) {
        return Translations.get(Messages.COMMAND_RECOVER_DESCRIPTION, lang);
    }
}
