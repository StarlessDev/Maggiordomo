package dev.starless.maggiordomo.commands;

import dev.starless.maggiordomo.commands.types.Interaction;
import dev.starless.maggiordomo.commands.types.Slash;
import dev.starless.maggiordomo.data.Cooldown;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class CommandManager {

    private String commandName;

    private final List<Slash> commands;
    private final List<Interaction> interactions;

    private final Map<String, Cooldown> cooldowns;

    public CommandManager() {
        commands = new ArrayList<>();
        interactions = new ArrayList<>();

        cooldowns = new ConcurrentHashMap<>();
    }

    /**
     * Azione terminale: crea il comando principale con tutti
     * i sottocomandi registrati in precedenza
     *
     * @param jda Istanza di JDA
     */
    public void createMainCommand(JDA jda) {
        if (commandName == null) {
            throw new RuntimeException("Non hai inserito il nome del comando!");
        }

        List<SubcommandData> mappedData = commands.stream()
                .map(command -> {
                    SubcommandData data = new SubcommandData(command.getName(), command.getDescription());
                    List<OptionData> optionData = data.getOptions();
                    // Fix per i cambi di parametri possibili
                    if(!optionData.isEmpty()) {
                        optionData.forEach(option -> data.removeOptionByName(option.getName()));
                    }

                    for (Parameter param : command.getParameters()) {
                        data.addOption(param.type(), param.name(), param.description(), param.required());
                    }
                    return data;
                })
                .toList();


        jda.updateCommands()
                .addCommands(Commands.slash(commandName.toLowerCase(), "Comando principale del bot")
                        .setGuildOnly(true)
                        .addSubcommands(mappedData))
                .queue();
    }

    public CommandManager name(String name) {
        commandName = name;
        return this;
    }

    public CommandManager command(Slash slash) {
        if (isCommandNotRegistered(slash)) {
            commands.add(slash);
        }

        return this;
    }

    public CommandManager interaction(Interaction interaction) {
        if (isInteractionNotRegistered(interaction)) {
            interactions.add(interaction);
        }

        return this;
    }

    public CommandManager both(Command command) {
        if (command instanceof Slash slash && isCommandNotRegistered(slash)) {
            commands.add(slash);
        }

        if (command instanceof Interaction interaction && isInteractionNotRegistered(interaction)) {
            interactions.add(interaction);
        }

        return this;
    }

    // This returns an immutable list
    public List<Interaction> getMenuInteractions() {
        return interactions.stream().filter(Interaction::inMenu).toList();
    }

    public void handleCooldown(Interaction interaction, String user) {
        if (interaction.timeout() == -1L) return;

        cooldowns.compute(user, (id, usage) -> {
            if (usage == null) usage = new Cooldown(user);

            usage.apply(interaction);
            return usage;
        });
    }

    public Cooldown.Result isOnCooldown(Interaction interaction, String user) {
        if (interaction.timeout() == -1L) return new Cooldown.Result(false);

        Cooldown usage = cooldowns.getOrDefault(user, null);
        if (usage != null) {
            return usage.check(interaction);
        }

        return new Cooldown.Result(false);
    }

    private boolean isCommandNotRegistered(Slash slash) {
        return commands.stream().noneMatch(otherCommand -> isTheSame(slash, otherCommand));
    }

    private boolean isInteractionNotRegistered(Interaction interaction) {
        return interactions.stream().noneMatch(otherCommand -> isTheSame(interaction, otherCommand));
    }

    private boolean isTheSame(Command command, Command otherCommand) {
        return command.getClass() == otherCommand.getClass() &&
                command.getName().equalsIgnoreCase(command.getName());
    }
}
