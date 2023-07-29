package dev.starless.maggiordomo.commands.interaction.filter;

import dev.starless.maggiordomo.Bot;
import dev.starless.maggiordomo.commands.types.Interaction;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.filter.FilterType;
import dev.starless.maggiordomo.data.user.VC;
import dev.starless.maggiordomo.utils.PageUtils;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.util.Set;

@RequiredArgsConstructor
public abstract class FilterInteraction implements Interaction {

    private final FilterType type;
    private final String messageContent;
    private final Modal inputModal;

    @Override
    public VC onButtonInteraction(VC vc, Settings settings, String id, ButtonInteractionEvent e) {
        int page = PageUtils.getPageFromId(id);
        if (page != -1) {
            e.reply(createMenu(settings, page)).queue();
        } else {
            e.replyModal(inputModal).queue();
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

        MessageCreateBuilder builder = new MessageCreateBuilder().setContent(messageContent);
        if (!words.isEmpty()) {
            StringSelectMenu.Builder menu = StringSelectMenu.create(getName()).setPlaceholder("Valori inseriti");
            words.stream()
                    .skip(10L * page)
                    .limit(10)
                    .forEach(word -> menu.addOption(word, word));
            builder.addActionRow(menu.build());
        }

        return builder.addActionRow(
                        PageUtils.getBackButton(getName(), page),
                        Button.secondary(getName() + ":add", "📖 Aggiungi"),
                        PageUtils.getNextButton(getName(), maxPages, page))
                .build();
    }
}
