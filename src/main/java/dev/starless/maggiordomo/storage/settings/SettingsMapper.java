package dev.starless.maggiordomo.storage.settings;

import dev.starless.maggiordomo.data.Settings;
import dev.starless.mongo.MongoStorage;
import dev.starless.mongo.api.Query;
import dev.starless.mongo.api.QueryBuilder;
import dev.starless.mongo.api.mapper.IMapper;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class SettingsMapper implements IMapper<Settings> {

    private final SettingsGateway gateway;
    @Getter private final Map<String, Settings> settings;

    public SettingsMapper(MongoStorage storage) {
        gateway = new SettingsGateway(storage);
        settings = new ConcurrentHashMap<>();

        gateway.lazyLoad(QueryBuilder.empty()).forEach(storedSettings -> settings.put(storedSettings.getGuild(), storedSettings));
    }

    @Override
    public boolean insert(Settings value) {
        // Guarda nellla cache se esiste già un valore
        Optional<Settings> cached = search(QueryBuilder.init().add("guild", value.getGuild()).create());
        if (cached.isPresent()) return false; // Altrimenti si deve usare update()

        boolean success = gateway.insert(value);
        if(success) {
            settings.put(value.getGuild(), value);
        }

        return success;
    }

    @Override
    public void update(Settings value) {
        delete(value);
        insert(value);
    }

    @Override
    public int delete(Settings value) {
        int removed = gateway.remove(value);
        settings.remove(value.getGuild());
        return removed;
    }

    @Override
    public Optional<Settings> search(Query query) {
        String guild = query.get("guild");
        if (guild == null) return Optional.empty();

        return Optional.ofNullable(settings.getOrDefault(guild, null));
    }

    @Override
    public List<Settings> bulkSearch(Query query) {
        return gateway.lazyLoad(query);
    }
}
