package dev.starless.maggiordomo.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.starless.mongo.adapters.DurationAdapter;
import dev.starless.mongo.adapters.InstantAdapter;
import dev.starless.mongo.adapters.OffsetDateTimeAdapter;
import lombok.experimental.UtilityClass;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;

@UtilityClass
public class GsonUtil {

    public Gson gson = new GsonBuilder()
            .registerTypeAdapter(Instant.class, new InstantAdapter())
            .registerTypeAdapter(Duration.class, new DurationAdapter())
            .registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeAdapter())
            .serializeNulls()
            .create();
}
