package dev.starless.maggiordomo.commands.interaction.management;

import dev.starless.maggiordomo.Bot;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.user.VC;
import dev.starless.maggiordomo.localization.Messages;
import dev.starless.maggiordomo.localization.Translations;
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

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ListManager extends AManagementInteraction {

    private final String name;
    private final Function<String, String> titleSupplier;
    private final SetSupplier supplier;
    private final SuccessAction successAction;

    public ListManager(String name, Function<String, String> titleSupplier, SetSupplier setSupplier) {
        this.name = name;
        this.titleSupplier = titleSupplier;
        this.supplier = setSupplier;
        this.successAction = null;
    }

    @Override
    protected MessageEditData handle(ButtonInteractionEvent e, Settings settings, String[] parts) {
        String id = parts.length > 0 ? parts[0] : "";
        MessageEditBuilder edit;

        switch (id) {
            case "add" -> edit = getAddMenu(settings.getLanguage());
            case "remove" -> edit = getRemoveMenu(e.getGuild(), settings, parts);
            default -> edit = getMainMenu(settings);
        }

        return edit.build();
    }

    @Override
    public VC onEntitySelected(VC vc, Settings settings, String id, EntitySelectInteractionEvent e) {
        List<Role> roles = e.getMentions().getRoles();
        if (roles.isEmpty()) {
            e.reply(Translations.string(Messages.NO_SELECTION, settings.getLanguage())).setEphemeral(true).queue();
        } else {
            supplier.get(settings).addAll(roles.stream().map(Role::getId).toList());
            Bot.getInstance().getCore().getSettingsMapper().update(settings);

            e.editMessage(getMainMenu(settings)
                            .setAllowedMentions(Collections.emptyList())
                            .setReplace(true)
                            .build())
                    .queue(success -> e.reply(Translations.string(Messages.COMMAND_MANAGEMENT_LISTS_ROLE_ADDED, settings.getLanguage())).setEphemeral(true).queue());

            if (successAction != null) {
                successAction.accept(roles, false);
            }
        }

        return null;
    }

    @Override
    public VC onStringSelected(VC vc, Settings settings, String id, StringSelectInteractionEvent e) {
        List<String> ids = e.getValues();
        if (ids.isEmpty()) {
            e.reply(Translations.string(Messages.NO_SELECTION, settings.getLanguage())).setEphemeral(true).queue();
        } else {
            ids.forEach(supplier.get(settings)::remove);
            Bot.getInstance().getCore().getSettingsMapper().update(settings);

            e.editMessage(getMainMenu(settings)
                            .setAllowedMentions(Collections.emptyList())
                            .setReplace(true)
                            .build())
                    .queue(success -> e.reply(Translations.string(Messages.COMMAND_MANAGEMENT_LISTS_ROLE_REMOVED, settings.getLanguage())).setEphemeral(true).queue());

            if (successAction != null) {
                successAction.accept(ids.stream().map(e.getGuild()::getRoleById)
                        .filter(Objects::nonNull)
                        .toList(), true);
            }
        }

        return null;
    }

    @Override
    public String getName() {
        return name;
    }

    private MessageEditBuilder getMainMenu(Settings settings) {
        Set<String> ids = supplier.get(settings);
        String content = "## " + titleSupplier.apply(settings.getLanguage()) + "\n";
        if (ids.isEmpty()) {
            content += Translations.string(Messages.COMMAND_MANAGEMENT_LISTS_NO_ROLES, settings.getLanguage());
        } else {
            String list = ids.stream().map(str -> "- <@&" + str + ">").collect(Collectors.joining("\n"));
            content += Translations.string(Messages.COMMAND_MANAGEMENT_LISTS_ROLES_LIST, settings.getLanguage()) + "\n" + list;
        }

        return new MessageEditBuilder()
                .setContent(content)
                .setComponents(ActionRow.of(
                        PageUtils.getShortBackButton("admin", settings.getLanguage()),
                        Button.primary(getName() + ":add", Translations.string(Messages.COMMAND_MANAGEMENT_LISTS_ADD_BUTTON_LABEL, settings.getLanguage())),
                        Button.danger(getName() + ":remove:0", Translations.string(Messages.COMMAND_MANAGEMENT_LISTS_REMOVE_BUTTON_LABEL, settings.getLanguage()))
                ));
    }

    private MessageEditBuilder getAddMenu(String lang) {
        return new MessageEditBuilder()
                .setContent(Translations.string(Messages.COMMAND_MANAGEMENT_LISTS_ADD_SELECTION_PLACEHOLDER, lang))
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
        edit = edit.setContent(Translations.string(Messages.COMMAND_MANAGEMENT_LISTS_REMOVE_PLACEHOLDER, settings.getLanguage()))
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

    public interface SuccessAction {

        void accept(List<Role> roles, boolean removed);
    }
}
