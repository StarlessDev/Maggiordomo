package dev.starless.maggiordomo.commands.slash;

import dev.starless.maggiordomo.Bot;
import dev.starless.maggiordomo.commands.types.Slash;
import dev.starless.maggiordomo.commands.CommandInfo;
import dev.starless.maggiordomo.commands.Parameter;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.logging.BotLogger;
import dev.starless.maggiordomo.utils.discord.Embeds;
import dev.starless.maggiordomo.utils.discord.Perms;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.managers.channel.concrete.CategoryManager;
import net.dv8tion.jda.api.managers.channel.concrete.TextChannelManager;
import net.dv8tion.jda.api.managers.channel.concrete.VoiceChannelManager;
import net.dv8tion.jda.internal.utils.PermissionUtil;

import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@CommandInfo(name = "setup", description = "Crea la categoria dedicata alle stanze")
public class SetupCommand implements Slash {

    @Override
    public void execute(Settings settings, SlashCommandInteractionEvent e) {
        e.deferReply(true).queue();

        Role usersRole = e.getOption("role").getAsRole();
        if (!PermissionUtil.canInteract(e.getGuild().getMember(e.getJDA().getSelfUser()), usersRole)) {
            e.getInteraction().getHook()
                    .sendMessageEmbeds(Embeds.errorEmbed("Non posso interagire con questo ruolo!"))
                    .setEphemeral(true)
                    .queue();
            return;
        }

        e.getGuild().createCategory("private").queue(category -> {
            if (category != null) {
                settings.setCategoryID(category.getId());
                settings.setPublicRole(usersRole.getId());

                // Vieta la visione a @everyone se necessario
                Role everyone = e.getGuild().getPublicRole();
                boolean isRoleNotDefault = !everyone.getId().equals(usersRole.getId());

                CategoryManager categoryManager = category.getManager();
                if (isRoleNotDefault) {
                    categoryManager = categoryManager.putRolePermissionOverride(everyone.getIdLong(),
                            Collections.emptyList(),
                            List.of(Permission.VIEW_CHANNEL,
                                    Permission.VOICE_CONNECT,
                                    Permission.MESSAGE_SEND,
                                    Permission.MESSAGE_SEND_IN_THREADS));
                }

                // Permessi per il ruolo scelto
                categoryManager = categoryManager.putRolePermissionOverride(usersRole.getIdLong(),
                        Collections.singletonList(Permission.VIEW_CHANNEL),
                        List.of(Permission.CREATE_PUBLIC_THREADS,
                                Permission.CREATE_PRIVATE_THREADS,
                                Permission.MESSAGE_SEND_IN_THREADS));

                categoryManager.queue(success -> {
                    // Crea il canale textuale
                    category.createTextChannel("〢🔮・pannello").queue(textChannel -> {
                        if (textChannel != null) {
                            TextChannelManager textChannelManager = textChannel.getManager();

                            // Aggiungi i permessi necessari
                            textChannelManager = textChannelManager.putRolePermissionOverride(usersRole.getIdLong(),
                                    Collections.singletonList(Permission.VIEW_CHANNEL),
                                    List.of(Permission.MESSAGE_SEND,
                                            Permission.MESSAGE_SEND_IN_THREADS,
                                            Permission.CREATE_PUBLIC_THREADS,
                                            Permission.CREATE_PRIVATE_THREADS,
                                            Permission.MESSAGE_SEND_IN_THREADS,
                                            Permission.USE_APPLICATION_COMMANDS));

                            if (isRoleNotDefault) {
                                textChannelManager = textChannelManager.putRolePermissionOverride(everyone.getIdLong(),
                                        0,
                                        Permission.ALL_PERMISSIONS);
                            }

                            textChannelManager.queue(success2 -> {
                                // Manda il messaggio con il menu dentro
                                Bot.getInstance().getCore().sendMenu(textChannel);

                                // Imposta l'ID del canale del messaggio
                                settings.setChannelID(textChannel.getId());

                                String createVoiceName = Emoji.fromUnicode("U+1F509").getAsReactionCode() + " | Crea";
                                category.createVoiceChannel(createVoiceName).queue(voiceChannel -> {
                                    if (voiceChannel != null) { // Se è stata creata correttamente
                                        VoiceChannelManager voiceChannelManager = voiceChannel.getManager();

                                        // Aggiungi i permessi per il bot
                                        //noinspection ConstantConditions
                                        voiceChannelManager = voiceChannelManager.putMemberPermissionOverride(
                                                voiceChannel.getJDA().getSelfUser().getIdLong(),
                                                Arrays.asList(Perms.selfPerms),
                                                Collections.emptyList());

                                        // Aggiungi i permessi per gli utenti
                                        voiceChannelManager = voiceChannelManager.putRolePermissionOverride(usersRole.getIdLong(),
                                                List.of(Permission.VIEW_CHANNEL,
                                                        Permission.VOICE_CONNECT,
                                                        Permission.VOICE_MOVE_OTHERS),
                                                List.of(Permission.MESSAGE_SEND,
                                                        Permission.MESSAGE_SEND_IN_THREADS,
                                                        Permission.CREATE_PUBLIC_THREADS,
                                                        Permission.CREATE_PRIVATE_THREADS,
                                                        Permission.MESSAGE_SEND_IN_THREADS,
                                                        Permission.VOICE_SPEAK,
                                                        Permission.VOICE_STREAM,
                                                        Permission.USE_APPLICATION_COMMANDS));

                                        if (isRoleNotDefault) {
                                            // Se togliamo a @everyone il permesso di parlare e usare il VAD
                                            // nessuno potrà parlare nella vc
                                            voiceChannelManager = voiceChannelManager.putRolePermissionOverride(everyone.getIdLong(),
                                                    List.of(Permission.VOICE_SPEAK, Permission.VOICE_USE_VAD),
                                                    Collections.singletonList(Permission.VIEW_CHANNEL));
                                        }

                                        voiceChannelManager.queue();

                                        settings.setVoiceID(voiceChannel.getId());
                                        Bot.getInstance().getCore().getSettingsMapper().update(settings); // Aggiorna la cache

                                        e.getInteraction().getHook().sendMessageEmbeds(new EmbedBuilder()
                                                        .setDescription("Creazione effettuata!")
                                                        .setColor(Color.decode("#65A25F"))
                                                        .build())
                                                .setEphemeral(true)
                                                .queue();
                                    } else {
                                        BotLogger.warn("Creazione canale vocale fallita. Non ho abbastanza permessi?");
                                        e.getInteraction().getHook()
                                                .sendMessageEmbeds(Embeds.errorEmbed("Impossibile creare la stanza vocale!"))
                                                .setEphemeral(true)
                                                .queue();
                                    }
                                });
                            }, throwable -> e.getInteraction().getHook()
                                    .sendMessageEmbeds(Embeds.errorEmbed("Impossibile creare completare il setup! (TextPerms)"))
                                    .setEphemeral(true)
                                    .queue());
                        } else {
                            BotLogger.warn("Creazione canale testuale fallita. Non ho abbastanza permessi?");
                            e.getInteraction().getHook()
                                    .sendMessageEmbeds(Embeds.errorEmbed("Impossibile creare il canale testuale!"))
                                    .setEphemeral(true)
                                    .queue();
                        }
                    });
                }, throwable -> e.getInteraction().getHook()
                        .sendMessageEmbeds(Embeds.errorEmbed("Impossibile creare completare il setup! (CatPerms)"))
                        .setEphemeral(true)
                        .queue());
            } else {
                BotLogger.warn("Creazione categoria fallita. Non ho abbastanza permessi?");
                e.getInteraction().getHook()
                        .sendMessageEmbeds(Embeds.errorEmbed("Impossibile creare la categoria!"))
                        .setEphemeral(true)
                        .queue();
            }
        });
    }

    @Override
    public Parameter[] getParameters() {
        return new Parameter[]{new Parameter(OptionType.ROLE,
                "role",
                "Ruolo dedicato a chi può usare le stanze",
                true)};
    }
}
