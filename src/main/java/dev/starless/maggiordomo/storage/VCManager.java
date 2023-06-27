package dev.starless.maggiordomo.storage;

import dev.starless.maggiordomo.storage.vc.LocalVCMapper;
import dev.starless.mongo.MongoStorage;
import net.dv8tion.jda.api.entities.Guild;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VCManager {

    private final Map<String, LocalVCMapper> mappers;
    private final MongoStorage storage;

    public VCManager(MongoStorage storage) {
        this.mappers = new ConcurrentHashMap<>();
        this.storage = storage;
    }

    public LocalVCMapper getMapper(Guild guild) {
        return getMapper(guild.getId());
    }

    public LocalVCMapper getMapper(String id) {
        return mappers.compute(id, (key, mapper) -> {
            if(mapper == null) mapper = new LocalVCMapper(storage);

            return mapper;
        });
    }
}
