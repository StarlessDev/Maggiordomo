package dev.starless.maggiordomo.commands.interaction;

import dev.starless.maggiordomo.commands.CommandInfo;
import dev.starless.maggiordomo.commands.types.Interaction;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.data.filter.FilterResult;
import dev.starless.maggiordomo.data.filter.FilterType;
import dev.starless.maggiordomo.data.filter.IFilter;
import dev.starless.maggiordomo.data.filter.impl.ContainsFilter;
import dev.starless.maggiordomo.data.filter.impl.PatternFilter;
import dev.starless.maggiordomo.data.user.VC;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@CommandInfo(name = "rename", description = "Cambia il nome della tua stanza")
public class TitleInteraction implements Interaction {

    Set<IFilter> filters = new HashSet<>();
    private final ContainsFilter containsFilter = new ContainsFilter();
    private final PatternFilter patternFilter = new PatternFilter();

    @Override
    public VC execute(VC vc, Settings settings, String id, ModalInteractionEvent e) {
        ModalMapping mapping = e.getValue("vc:title");
        if (mapping == null) {
            e.replyEmbeds(Embeds.errorEmbed())
                    .setEphemeral(true)
                    .queue();
        } else {
            String newTitle = mapping.getAsString();
            for (FilterType type : FilterType.values()) {
                IFilter filter = switch (type) {
                    case CONTAINS -> containsFilter;
                    case REGEX -> patternFilter;
                };

                for (String string : settings.getFilterStrings().getOrDefault(type, new HashSet<>())) {
                    FilterResult result = filter.apply(newTitle, string);
                    if (result.flagged()) {
                        e.reply("Questo nome non è permesso perchè " + result.message())
                                .setEphemeral(true)
                                .queue();
                        return null;
                    }
                }
            }

            vc.setTitle(newTitle);

            VoiceChannel channel = e.getGuild().getVoiceChannelById(vc.getChannel());
            if (channel != null) {
                channel.getManager().setName(newTitle).queue();
            }

            e.replyEmbeds(new EmbedBuilder()
                            .setDescription("Titolo cambiato! :pencil:")
                            .setColor(new Color(100, 160, 94))
                            .build())
                    .setEphemeral(true)
                    .queue();
        }

        return vc;
    }

    @Override
    public VC execute(VC vc, Settings settings, String fullID, ButtonInteractionEvent e) {
        e.replyModal(Modal.create(getName(), "Inserisci")
                        .addActionRow(TextInput.create("vc:title", "Titolo", TextInputStyle.SHORT)
                                .setRequired(true)
                                .setRequiredRange(1, 99)
                                .setValue(vc.getTitle())
                                .setPlaceholder("Stanza di " + e.getUser().getName())
                                .build())
                        .build())
                .queue();

        return null;
    }

    @Override
    public Emoji emoji() {
        return Emoji.fromUnicode("📝");
    }

    @Override
    public long timeout() {
        return 30;
    }
}
