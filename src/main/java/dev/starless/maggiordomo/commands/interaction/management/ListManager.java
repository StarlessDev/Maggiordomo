package dev.starless.maggiordomo.commands.interaction.management;

import dev.starless.maggiordomo.Bot;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.user.VC;
import dev.starless.maggiordomo.utils.PageUtils;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.RoleIcon;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.utils.messages.MessageEditBuilder;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ListManager extends AManagementInteraction {

    private final String name;
    private final String title;
    private final SetSupplier supplier;

    @Override
    protected MessageEditData handle(ButtonInteractionEvent e, Settings settings, String[] parts) {
        String id = parts.length > 0 ? parts[0] : "";
        MessageEditBuilder edit;

        switch (id) {
            case "add" -> edit = getAddMenu();
            case "remove" -> edit = getRemoveMenu(e.getGuild(), settings, parts);
            default -> edit = getMainMenu(settings);
        }

        return edit.build();
    }

    @Override
    public VC onEntitySelected(VC vc, Settings settings, String id, EntitySelectInteractionEvent e) {
        List<Role> roles = e.getMentions().getRoles();
        if (roles.isEmpty()) {
            e.reply("Non hai selezionato nessun ruolo!").setEphemeral(true).queue();
        } else {
            supplier.get(settings).addAll(roles.stream().map(Role::getId).toList());
            Bot.getInstance().getCore().getSettingsMapper().update(settings);

            e.getMessage().editMessage(getMainMenu(settings)
                            .setAllowedMentions(Collections.emptyList())
                            .setReplace(true)
                            .build())
                    .queue(success -> e.reply("I ruoli selezionati sono stati aggiunti!").setEphemeral(true).queue());
        }

        return null;
    }

    @Override
    public VC onStringSelected(VC vc, Settings settings, String id, StringSelectInteractionEvent e) {
        List<String> ids = e.getValues();
        if (ids.isEmpty()) {
            e.reply("Non hai selezionato nessun ruolo!").setEphemeral(true).queue();
        } else {
            ids.forEach(supplier.get(settings)::remove);
            Bot.getInstance().getCore().getSettingsMapper().update(settings);

            e.getMessage().editMessage(getMainMenu(settings)
                            .setAllowedMentions(Collections.emptyList())
                            .setReplace(true)
                            .build())
                    .queue(success -> e.reply("I ruoli selezionati sono stati rimossi!").setEphemeral(true).queue());
        }

        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    private MessageEditBuilder getMainMenu(Settings settings) {
        Set<String> ids = supplier.get(settings);
        String content = title + "\n";
        if (ids.isEmpty()) {
            content += "Non c'è nessun ruolo qui!.";
        } else {
            String list = ids.stream().map(str -> "- <@&" + str + ">").collect(Collectors.joining("\n"));
            content += "Ruoli attualmente inseriti:\n" + list;
        }

        return new MessageEditBuilder()
                .setContent(content)
                .setComponents(ActionRow.of(
                        PageUtils.getShortBackButton("admin", settings.getLanguage()),
                        Button.primary(getName() + ":add", "Aggiungi"),
                        Button.danger(getName() + ":remove:0", "Rimuovi")
                ));
    }

    private MessageEditBuilder getAddMenu() {
        return new MessageEditBuilder()
                .setContent("Scegli un ruolo da aggiungere")
                .setActionRow(EntitySelectMenu.create(getName(), EntitySelectMenu.SelectTarget.ROLE)
                        .setRequiredRange(1, 10)
                        .build());
    }

    private MessageEditBuilder getRemoveMenu(Guild guild, Settings settings, String[] parts) {
        MessageEditBuilder edit = new MessageEditBuilder();
        int page = PageUtils.getPageFromId(parts);
        int base = page * PageUtils.DROPDOWN_MAX_ENTRIES;
        int limit = base + PageUtils.DROPDOWN_MAX_ENTRIES;

        List<String> ids = new ArrayList<>(supplier.get(settings));
        StringSelectMenu.Builder builder = StringSelectMenu.create(getName()).setRequiredRange(1, ids.size());

        ids.subList(base, Math.min(ids.size(), limit)).forEach(roleID -> {
            Role role = guild.getRoleById(roleID);
            if (role == null) return;

            Emoji emoji = null;
            RoleIcon icon = role.getIcon();
            if (icon != null && icon.isEmoji()) {
                emoji = Emoji.fromUnicode(icon.getEmoji());
            }

            builder.addOption(role.getName(), roleID, "", emoji);
        });

        int maxPages = (int) Math.ceil(supplier.get(settings).size() / (double) PageUtils.DROPDOWN_MAX_ENTRIES);
        edit = edit.setContent("Scegli un ruolo da rimuovere")
                .setComponents(List.of(
                        ActionRow.of(builder.build()),
                        ActionRow.of(
                                PageUtils.getBackButton(getFullId(parts), page, settings.getLanguage()),
                                PageUtils.getNextButton(getFullId(parts), maxPages, page, settings.getLanguage())
                        )
                ));

        return edit;
    }

    public interface SetSupplier {

        Set<String> get(Settings settings);
    }
}
