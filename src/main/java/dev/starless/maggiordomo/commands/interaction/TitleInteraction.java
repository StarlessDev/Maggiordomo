package dev.starless.maggiordomo.commands.interaction;

import cz.jirutka.unidecode.Unidecode;
import dev.starless.maggiordomo.commands.types.Interaction;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.filter.FilterResult;
import dev.starless.maggiordomo.data.filter.FilterType;
import dev.starless.maggiordomo.data.filter.IFilter;
import dev.starless.maggiordomo.data.filter.impl.ContainsFilter;
import dev.starless.maggiordomo.data.filter.impl.PatternFilter;
import dev.starless.maggiordomo.data.user.VC;
import dev.starless.maggiordomo.localization.Translations;
import dev.starless.maggiordomo.localization.Messages;
import dev.starless.maggiordomo.utils.discord.Embeds;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;

import java.awt.*;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.Map;

public class TitleInteraction implements Interaction {

    private final Unidecode unidecode = Unidecode.toAscii();
    private final Map<Character, Character> leetMap = Map.of(
            '1', 'i',
            '2', 'l',
            '3', 'e',
            '4', 'a',
            '6', 'g',
            '7', 't',
            '8', 'b',
            '0', 'o'
    );

    private final ContainsFilter containsFilter = new ContainsFilter();
    private final PatternFilter patternFilter = new PatternFilter();

    @Override
    public VC onModalInteraction(VC vc, Settings settings, String id, ModalInteractionEvent e) {
        ModalMapping mapping = e.getValue("vc:title");
        if (mapping == null) {
            e.replyEmbeds(Embeds.defaultErrorEmbed(settings.getLanguage()))
                    .setEphemeral(true)
                    .queue();
        } else {
            String input = mapping.getAsString();
            String newTitle = normalize(input);
            for (FilterType type : FilterType.values()) {
                IFilter filter = switch (type) {
                    case BASIC -> containsFilter;
                    case REGEX -> patternFilter;
                };

                for (String string : settings.getFilterStrings().getOrDefault(type, new HashSet<>())) {
                    FilterResult result = filter.apply(settings, newTitle, string);
                    if (result.flagged()) {
                        e.reply(Translations.string(Messages.FILTER_FLAG_PREFIX, settings.getLanguage()) + " " + result.message())
                                .setEphemeral(true)
                                .queue();
                        return null;
                    }
                }
            }

            vc.setTitle(input);

            VoiceChannel channel = e.getGuild().getVoiceChannelById(vc.getChannel());
            if (channel != null) {
                channel.getManager().setName(input).queue();
            }

            e.replyEmbeds(new EmbedBuilder()
                            .setDescription(Translations.string(Messages.INTERACTION_TITLE_SUCCESS, settings.getLanguage()))
                            .setColor(new Color(100, 160, 94))
                            .build())
                    .setEphemeral(true)
                    .queue();
        }

        return vc;
    }

    @Override
    public VC onButtonInteraction(VC vc, Settings settings, String fullID, ButtonInteractionEvent e) {
        e.replyModal(Modal.create(getName(), Translations.string(Messages.INTERACTION_TITLE_MODAL_TITLE, settings.getLanguage()))
                        .addActionRow(TextInput.create("vc:title", Translations.string(Messages.INTERACTION_TITLE_MODAL_INPUT_LABEL, settings.getLanguage()), TextInputStyle.SHORT)
                                .setRequired(true)
                                .setRequiredRange(1, 99)
                                .setValue(vc.getTitle())
                                .setPlaceholder(Translations.string(Messages.INTERACTION_TITLE_MODAL_INPUT_PLACEHOLDER, settings.getLanguage(), e.getUser().getName()))
                                .build())
                        .build())
                .queue();

        return null;
    }

    private String normalize(String input) {
        if (!Normalizer.isNormalized(input, Normalizer.Form.NFKC)) {
            input = Normalizer.normalize(input, Normalizer.Form.NFKC);
        }

        String ascii = unidecode.decode(input.replaceAll("\\p{M}", "")).replaceAll("\\.|,|-|_|'|`|", "");
        StringBuilder normalized = new StringBuilder();
        for (int i = 0; i < ascii.length(); i++) {
            char c = ascii.charAt(i);
            normalized.append(Character.toLowerCase(leetMap.getOrDefault(c, c)));
        }

        return normalized.toString();
    }

    @Override
    public Emoji emoji() {
        return Emoji.fromUnicode("📝");
    }

    @Override
    public long timeout() {
        return 30;
    }

    @Override
    public String getName() {
        return "rename";
    }
}
