package dev.starless.maggiordomo.data;

import dev.starless.maggiordomo.commands.types.Interaction;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Accessors(fluent = true)
@EqualsAndHashCode(of = "user")
public class Cooldown {

    @Getter private final String user;
    private final Map<String, Instant> restrictions = new HashMap<>();

    public void apply(Interaction interaction) {
        restrictions.put(interaction.getName(), Instant.now().plusSeconds(interaction.timeout()));
    }

    public Result check(Interaction interaction) {
        // cioè se l'ora nell'hashmap è dopo l'istante attuale
        Instant lastExecution = restrictions.getOrDefault(interaction.getName(), Instant.EPOCH);
        Instant now = Instant.now();

        boolean active = lastExecution.isAfter(now);
        Duration nextExecution = active ? Duration.between(now, lastExecution) : Duration.ZERO;

        return new Result(active, nextExecution);
    }

    public record Result(boolean active, Duration nextExecutionInstant) {

        public Result(boolean active) {
            this(active, Duration.ZERO);
        }
    }
}
