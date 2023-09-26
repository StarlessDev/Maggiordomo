package dev.starless.maggiordomo;

import dev.starless.maggiordomo.interfaces.Service;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UptimeServer implements Service {

    private Javalin server;
    private final String endpoint;
    private final int port;

    @Override
    public void start() {
        if (server != null) return;

        server = Javalin.create()
                .head(endpoint, ctx -> ctx.status(HttpStatus.OK))
                .start(port);
    }

    @Override
    public void stop() {
        if (server == null) return;

        server.stop();
    }
}
