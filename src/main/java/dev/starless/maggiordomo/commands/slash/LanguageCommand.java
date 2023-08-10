package dev.starless.maggiordomo.commands.slash;

import dev.starless.maggiordomo.Bot;
import dev.starless.maggiordomo.commands.Parameter;
import dev.starless.maggiordomo.commands.types.Slash;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.localization.DefaultLanguages;
import dev.starless.maggiordomo.localization.Messages;
import dev.starless.maggiordomo.localization.Translations;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import java.util.List;

public class LanguageCommand implements Slash {

    @Override
    public void execute(Settings settings, SlashCommandInteractionEvent e) {
        OptionMapping mapping = e.getOption("lang"); // this should never be null

        String code = mapping.getAsString();
        boolean success = Bot.getInstance().getCore().updateLanguage(e.getGuild(), settings, code);
        if (success) {
            String langName = DefaultLanguages.fromCode(code).map(DefaultLanguages::getName).orElse(code);
            e.reply(Translations.string(Messages.COMMAND_LANGUAGE_SUCCESS, settings.getLanguage(), langName))
                    .setEphemeral(true)
                    .queue();
        } else {
            e.reply(Translations.string(Messages.COMMAND_LANGUAGE_FAIL, settings.getLanguage(), code))
                    .setEphemeral(true)
                    .queue();
        }
    }

    @Override
    public void autocomplete(Settings settings, CommandAutoCompleteInteractionEvent e) {
        if (e.getFocusedOption().getName().equals("lang")) {
            List<Command.Choice> options = Translations.getLanguageCodes()
                    .stream()
                    .filter(code -> code.toLowerCase().startsWith(e.getFocusedOption().getValue().toLowerCase()))
                    .map(code -> DefaultLanguages.fromCode(code)
                            .map(defaultLanguages -> new Command.Choice(defaultLanguages.getName(), code))
                            .orElseGet(() -> new Command.Choice(code, code)))
                    .toList();

            e.replyChoices(options).queue();
        }
    }

    @Override
    public Parameter[] getParameters(String lang) {
        return new Parameter[]{new Parameter(
                OptionType.STRING,
                "lang",
                Translations.string(Messages.COMMAND_LANGUAGE_PARAMETER_LANG, lang),
                true,
                true)
        };
    }

    @Override
    public String getName() {
        return "language";
    }

    @Override
    public String getDescription(String lang) {
        return Translations.string(Messages.COMMAND_LANGUAGE_DESCRIPTION, lang);
    }
}
