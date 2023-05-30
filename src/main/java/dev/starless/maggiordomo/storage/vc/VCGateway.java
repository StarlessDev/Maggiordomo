package dev.starless.maggiordomo.storage.vc;

import com.mongodb.client.model.Filters;
import dev.starless.maggiordomo.data.user.VC;
import it.ayyjava.storage.MongoStorage;
import it.ayyjava.storage.structures.Query;
import it.ayyjava.storage.structures.gateway.GatewayImpl;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class VCGateway extends GatewayImpl<VC> {

    public VCGateway(MongoStorage storage) {
        super(storage);
    }

    @Override
    public Optional<VC> load(Query query) {
        String guild = query.get("guild");
        String user = query.get("user");
        if (guild == null || user == null) return Optional.empty();

        return storage().findFirst(VC.class, Filters.and(
                Filters.eq("guild", guild),
                Filters.eq("user", user)));
    }

    public Optional<VC> loadByChannel(Query query) {
        String guild = query.get("guild");
        String channel = query.get("channel");
        if (guild == null || channel == null) return Optional.empty();

        return storage().findFirst(VC.class, Filters.and(
                Filters.eq("guild", guild),
                Filters.eq("channel", channel)));
    }

    @Override
    public List<VC> lazyLoad(Query query) {
        // Tutte le VC verranno caricate
        // in modo dinamico al bisogno
        String guild = query.get("guild");
        if(guild == null) return Collections.emptyList();

        return storage().find(VC.class, Filters.eq("guild", guild));
    }
}
