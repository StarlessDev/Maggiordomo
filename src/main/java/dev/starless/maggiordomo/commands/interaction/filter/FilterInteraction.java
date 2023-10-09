package dev.starless.maggiordomo.commands.interaction.filter;

import dev.starless.maggiordomo.Bot;
import dev.starless.maggiordomo.commands.types.Interaction;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.filter.FilterType;
import dev.starless.maggiordomo.data.user.VC;
import dev.starless.maggiordomo.localization.Messages;
import dev.starless.maggiordomo.localization.Translations;
import dev.starless.maggiordomo.utils.PageUtils;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.api.utils.messages.MessageEditData;

import java.util.Set;

@RequiredArgsConstructor
public abstract class FilterInteraction implements Interaction {

    private final FilterType type;

    @Override
    public VC onButtonInteraction(VC vc, Settings settings, String id, ButtonInteractionEvent e) {
        int page = PageUtils.getPageFromId(id);
        if (page == -1) { // If the button is not a valid "go back" or "go forward" button
            TextInputStyle textStyle = type.equals(FilterType.BASIC) ? TextInputStyle.PARAGRAPH : TextInputStyle.SHORT;
            Messages modalValueMessage = type.equals(FilterType.BASIC) ? Messages.COMMAND_FILTERS_BASIC_INPUT : Messages.COMMAND_FILTERS_PATTERN_INPUT;
            e.replyModal(Modal.create(getName(), Translations.string(Messages.FILTER_MENU_TITLE, settings.getLanguage()))
                            .addActionRow(TextInput.create("input", "input", textStyle)
                                    .setValue(Translations.string(modalValueMessage, settings.getLanguage()))
                                    .setRequiredRange(1, 256)
                                    .build())
                            .build())
                    .queue();
        } else {
            e.deferReply().setEphemeral(true).queue(nothing -> e.getInteraction().getHook().deleteOriginal().queue());
            e.getMessage().editMessage(MessageEditData.fromCreateData(createMenu(settings, page)))
                    .setReplace(true)
                    .queue();
        }

        return null;
    }

    @Override
    public VC onModalInteraction(VC vc, Settings settings, String id, ModalInteractionEvent e) {
        onInputReceived(settings, e);

        e.getMessage().editMessage(MessageEditData.fromCreateData(createMenu(settings, 0)))
                .setReplace(true)
                .queue();

        // If the event is acknowledged, an exception was thrown
        // and an error data was already displayed
        if (!e.isAcknowledged()) {
            String listType = Translations.string(type.equals(FilterType.BASIC) ? Messages.FILTER_BASIC : Messages.FILTER_PATTERN, settings.getLanguage()).toLowerCase();
            e.reply(Translations.string(Messages.COMMAND_FILTERS_UPDATED, settings.getLanguage(), listType))
                    .setEphemeral(true)
                    .queue();
        }

        return null;
    }

    @Override
    public VC onStringSelected(VC vc, Settings settings, String id, StringSelectInteractionEvent e) {
        if (!e.getSelectedOptions().isEmpty()) {
            e.getSelectedOptions().forEach(option -> settings.modifyFilters(type, set -> set.remove(option.getValue())));

            Bot.getInstance().getCore().getSettingsMapper().update(settings);

            e.getMessage().editMessage(MessageEditData.fromCreateData(createMenu(settings, 0)))
                    .setReplace(true)
                    .queue();

            String listType = Translations.string(type.equals(FilterType.BASIC) ? Messages.FILTER_BASIC : Messages.FILTER_PATTERN, settings.getLanguage()).toLowerCase();
            e.reply(Translations.string(Messages.COMMAND_FILTERS_UPDATED, settings.getLanguage(), listType))
                    .setEphemeral(true)
                    .queue();
        }


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

        String filterName = Translations.string(type.equals(FilterType.BASIC) ? Messages.FILTER_BASIC : Messages.FILTER_PATTERN, settings.getLanguage());
        String content = Translations.string(Messages.COMMAND_FILTERS_EXPLANATION, settings.getLanguage(), filterName);

        MessageCreateBuilder builder = new MessageCreateBuilder().setContent(content);

        if (!words.isEmpty()) {
            StringSelectMenu.Builder menu = StringSelectMenu.create(getName()).setRequiredRange(1, 10);
            words.stream()
                    .skip(10L * page)
                    .limit(10)
                    .forEach(word -> menu.addOption(word, word));
            builder.addActionRow(menu.build());
        }

        return builder.addActionRow(PageUtils.getShortBackButton("filters", settings.getLanguage()),
                        Button.secondary(getName() + ":add", Translations.string(Messages.COMMAND_FILTERS_ADD_BUTTON, settings.getLanguage())))
                .addActionRow(PageUtils.getBackButton(getName(), page, settings.getLanguage()),
                        PageUtils.getNextButton(getName(), maxPages, page, settings.getLanguage()))
                .build();
    }
}
