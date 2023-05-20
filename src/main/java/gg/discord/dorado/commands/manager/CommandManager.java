package gg.discord.dorado.commands.manager;

import gg.discord.dorado.commands.Command;
import gg.discord.dorado.commands.Parameter;
import gg.discord.dorado.commands.types.Interaction;
import gg.discord.dorado.commands.types.Slash;
import gg.discord.dorado.data.Cooldown;
import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class CommandManager {

    private String commandName;

    private final List<Slash> slashCommands;
    private final List<Interaction> menuCommands;

    private final Map<String, Cooldown> cooldowns;

    public CommandManager() {
        slashCommands = new ArrayList<>();
        menuCommands = new ArrayList<>();

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

        List<SubcommandData> mappedData = slashCommands.stream()
                .map(command -> {
                    SubcommandData data = new SubcommandData(command.getName(), command.getDescription());
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

    public CommandManager command(Slash action) {
        if (isNotRegistered(action)) {
            slashCommands.add(action);
        }

        return this;
    }

    public CommandManager interaction(Interaction action) {
        if (isNotRegistered(action)) {
            menuCommands.add(action);
        }

        return this;
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

    private boolean isNotRegistered(Command command) {
        return (command instanceof Slash ? slashCommands : menuCommands)
                .stream()
                .noneMatch(otherCommand -> isTheSame(command, otherCommand));
    }

    private boolean isTheSame(Command command, Command otherCommand) {
        return command.getClass() == otherCommand.getClass() &&
                command.getName().equalsIgnoreCase(command.getName());
    }
}
