package dev.starless.maggiordomo.commands.interaction;

import com.anyascii.AnyAscii;
import dev.starless.maggiordomo.commands.types.Interaction;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.filter.FilterResult;
import dev.starless.maggiordomo.data.filter.FilterType;
import dev.starless.maggiordomo.data.filter.IFilter;
import dev.starless.maggiordomo.data.filter.impl.ContainsFilter;
import dev.starless.maggiordomo.data.filter.impl.PatternFilter;
import dev.starless.maggiordomo.data.user.VC;
import dev.starless.maggiordomo.localization.Messages;
import dev.starless.maggiordomo.localization.Translations;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TitleInteraction implements Interaction {

    private final Pattern emojiPattern = Pattern.compile(":\\w{1,32}:");
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
        // Remove characters that could interfere with a MongoDB query
        // and get the best translation to an ascii string of the input
        String transliteration = AnyAscii.transliterate(input.trim().replaceAll("\\.|,|-|_|'|`|", ""));

        // The emojis will get translated to something like this: :rofl:.
        // Since there are emojis which represent letters, we try to remove the double quotes
        // and keep only the letters to avoid bypasses
        StringBuilder normalized = new StringBuilder(transliteration);
        Matcher emojiMatches = emojiPattern.matcher(transliteration);

        // It's important to filter from the start to the end of the string for the code to work
        emojiMatches.results().forEachOrdered(new Consumer<>() {
            int offset = 0;

            @Override
            public void accept(MatchResult match) {
                // The offset accounts for previously deleted chars.
                // I substracted 2 from match.end() since it's exclusive
                // and to account for the char just deleted
                normalized.deleteCharAt(match.start() - offset).deleteCharAt(match.end() - offset - 2);
                offset += 2;
            }
        });

        // Account for leet speak
        for (int i = 0; i < normalized.length(); i++) {
            char c = leetMap.getOrDefault(normalized.charAt(i), Character.MIN_VALUE);
            if (c != Character.MIN_VALUE) {
                normalized.setCharAt(i, Character.toLowerCase(c));
            }
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
