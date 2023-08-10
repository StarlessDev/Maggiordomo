package dev.starless.maggiordomo.commands.interaction.filter;

import dev.starless.maggiordomo.Bot;
import dev.starless.maggiordomo.commands.types.Interaction;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.filter.FilterType;
import dev.starless.maggiordomo.data.user.VC;
import dev.starless.maggiordomo.localization.Translations;
import dev.starless.maggiordomo.localization.Messages;
import dev.starless.maggiordomo.utils.PageUtils;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.util.Set;

@RequiredArgsConstructor
public abstract class FilterInteraction implements Interaction {

    private final FilterType type;

    @Override
    public VC onButtonInteraction(VC vc, Settings settings, String id, ButtonInteractionEvent e) {
        int page = PageUtils.getPageFromId(id);
        if (page != -1) {
            e.reply(createMenu(settings, page)).queue();
        } else {
            TextInputStyle textStyle = type.equals(FilterType.CONTAINS) ? TextInputStyle.PARAGRAPH : TextInputStyle.SHORT;

            e.replyModal(Modal.create(getName(), Translations.string(Messages.FILTER_MENU_TITLE, settings.getLanguage()))
                            .addActionRow(TextInput.create("input", "input", textStyle)
                                    .setValue(Translations.string(Messages.FILTER_EXPLANATION, settings.getLanguage()))
                                    .setRequiredRange(1, 256)
                                    .build())
                            .build())
                    .queue();
        }

        return null;
    }

    @Override
    public VC onModalInteraction(VC vc, Settings settings, String id, ModalInteractionEvent e) {
        onInputReceived(settings, e);

        e.getMessage().delete().queue();
        e.reply(createMenu(settings, 0)).queue();

        return null;
    }

    @Override
    public VC onStringSelected(VC vc, Settings settings, String id, StringSelectInteractionEvent e) {
        e.getSelectedOptions().stream()
                .map(SelectOption::getValue)
                .forEach(selection -> settings.modifyFilters(type, set -> set.remove(selection)));
        Bot.getInstance().getCore().getSettingsMapper().update(settings);

        e.getMessage().delete().queue();
        e.reply(createMenu(settings, 0)).queue();

        return null;
    }

    @Override
    public boolean needsVC() {
        return false;
    }

    @Override
    public boolean inMenu() {
        return false;
    }

    protected abstract void onInputReceived(Settings settings, ModalInteractionEvent e);

    protected MessageCreateData createMenu(Settings settings, int page) {
        Set<String> words = settings.getFilterWords(type);
        int maxPages = (int) Math.ceil(words.size() / 10D);

        String filterName = Translations.string(type.equals(FilterType.CONTAINS) ? Messages.FILTER_BASIC : Messages.FILTER_PATTERN, settings.getLanguage());
        String content = Translations.string(Messages.COMMAND_SETUP_EXPLANATION, settings.getLanguage(), filterName);

        MessageCreateBuilder builder = new MessageCreateBuilder().setContent(content);

        if (!words.isEmpty()) {
            StringSelectMenu.Builder menu = StringSelectMenu.create(getName());
            words.stream()
                    .skip(10L * page)
                    .limit(10)
                    .forEach(word -> menu.addOption(word, word));
            builder.addActionRow(menu.build());
        }

        return builder.addActionRow(
                        PageUtils.getBackButton(getName(), page),
                        Button.secondary(getName() + ":add", Translations.string(Messages.COMMAND_FILTERS_ADD_BUTTON, settings.getLanguage())),
                        PageUtils.getNextButton(getName(), maxPages, page))
                .build();
    }
}
