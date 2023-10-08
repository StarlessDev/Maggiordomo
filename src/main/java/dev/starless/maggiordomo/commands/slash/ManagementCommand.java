package dev.starless.maggiordomo.commands.slash;

import dev.starless.maggiordomo.commands.types.Interaction;
import dev.starless.maggiordomo.commands.types.Slash;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.user.VC;
import dev.starless.maggiordomo.utils.discord.Perms;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

public class ManagementCommand implements Slash, Interaction {

    @Override
    public void execute(Settings settings, SlashCommandInteractionEvent e) {
        e.reply(getMenu()).queue();
    }

    @Override
    public VC onButtonInteraction(VC vc, Settings settings, String id, ButtonInteractionEvent e) {
        e.getMessage().editMessage(MessageEditData.fromCreateData(getMenu()))
                .setReplace(true)
                .queue();

        e.deferReply().queue(hook -> hook.deleteOriginal().queue());
        return null;
    }

    private MessageCreateData getMenu() {
        return new MessageCreateBuilder()
                .setContent("""
                        # Admin Dashboard 🛠
                        Manage everything easily from a single place.""")
                .addActionRow(
                        Button.primary("premium", "💎 Ruoli Premium"),
                        Button.primary("blacklist", "❌ Ruoli Bannati"),
                        Button.primary("filters", "📜 Filtri")
                )
                .build();
    }

    @Override
    public String getName() {
        return "admin";
    }

    @Override
    public String getDescription(String lang) {
        return "Balls";
    }

    @Override
    public boolean hasPermission(Member member, Settings settings) {
        return Perms.isAdmin(member);
    }
}