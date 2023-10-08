package dev.starless.maggiordomo.commands.slash;

import dev.starless.maggiordomo.commands.types.Interaction;
import dev.starless.maggiordomo.commands.types.Slash;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.user.VC;
import dev.starless.maggiordomo.utils.discord.Perms;
import dev.starless.maggiordomo.utils.discord.References;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.util.Collections;

public class ManagementCommand implements Slash, Interaction {

    @Override
    public void execute(Settings settings, SlashCommandInteractionEvent e) {
        e.reply(getMenu(e.getGuild(), settings)).queue();
    }

    @Override
    public VC onButtonInteraction(VC vc, Settings settings, String id, ButtonInteractionEvent e) {
        e.getMessage().editMessage(MessageEditData.fromCreateData(getMenu(e.getGuild(), settings)))
                .setReplace(true)
                .queue();

        e.deferReply().queue(hook -> hook.deleteOriginal().queue());
        return null;
    }

    private MessageCreateData getMenu(Guild guild, Settings settings) {
        return new MessageCreateBuilder()
                .setContent("""
                        # Admin Dashboard 🛠
                        Manage everything easily from a single place.
                        
                        Your current setup settings are:
                        - Public role: %s
                        - Inactivity: maximum of %s day(s) for pinned rooms"""
                        .formatted(References.role(guild, settings.getPublicRole()),
                                settings.getMaxInactivity() == -1 ? "∞" : settings.getMaxInactivity()
                        ))
                .addActionRow(
                        Button.primary("premium", "💎 Ruoli Premium"),
                        Button.primary("blacklist", "❌ Ruoli Bannati"),
                        Button.primary("filters", "📜 Filtri")
                )
                .addActionRow(
                        Button.danger("refreshperms", "🔁 Aggiorna i permessi"),
                        Button.secondary("manage", "💼 Gestisci le stanze")
                )
                .setAllowedMentions(Collections.emptyList())
                .build();
    }

    @Override
    public String getName() {
        return "admin";
    }

    @Override
    public String getDescription(String lang) {
        return "All your server's settings in one place";
    }

    @Override
    public boolean hasPermission(Member member, Settings settings) {
        return Perms.isAdmin(member);
    }
}