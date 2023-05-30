package dev.starless.maggiordomo.storage.settings;

import dev.starless.maggiordomo.data.Settings;
import it.ayyjava.storage.MongoStorage;
import it.ayyjava.storage.structures.Query;
import it.ayyjava.storage.structures.gateway.GatewayImpl;

import java.util.List;
import java.util.Optional;

public class SettingsGateway extends GatewayImpl<Settings> {

    public SettingsGateway(MongoStorage storage) {
        super(storage);
    }

    @Override
    public Optional<Settings> load(Query query) {
        // Tutte le categorie verranno caricate
        // all'avvio del bot esattamente come le gilde.
        return Optional.empty();
    }

    @Override
    public List<Settings> lazyLoad(Query query) {
        return storage().find(Settings.class);
    }
}
