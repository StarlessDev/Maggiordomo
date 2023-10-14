package dev.starless.maggiordomo.utils;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import net.dv8tion.jda.api.utils.TimeFormat;
import net.dv8tion.jda.api.utils.Timestamp;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@UtilityClass
public class DateUtils {
    
    @Getter private final DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    @Getter private final DateTimeFormatter fullFormatter = DateTimeFormatter.ofPattern("dd-MM HH:mm:ss");

    private final ZoneOffset OFFSET = ZoneOffset.ofHours(1);

    public OffsetDateTime getTime() {
        return Instant.now(Clock.systemUTC()).atOffset(OFFSET);
    }

    public String now(DateTimeFormatter formatter) {
        return getTime().format(formatter);
    }

}
