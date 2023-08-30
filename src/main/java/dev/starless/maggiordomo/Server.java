package dev.starless.maggiordomo;

import dev.starless.maggiordomo.api.ResponseBuilder;
import dev.starless.maggiordomo.api.controllers.VCController;
import dev.starless.maggiordomo.config.Config;
import dev.starless.maggiordomo.config.ConfigEntry;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.interfaces.Service;
import dev.starless.mongo.objects.QueryBuilder;
import io.javalin.Javalin;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
import io.javalin.http.util.NaiveRateLimit;
import lombok.RequiredArgsConstructor;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static io.javalin.apibuilder.ApiBuilder.*;

@RequiredArgsConstructor
public class Server implements Service {

    private Javalin server;
    private final Config config;

    @Override
    public void start() {
        if (server != null) return;

        // Check what features are enabled
        boolean uptimeSupport = config.getBoolean(ConfigEntry.UPTIME_ENABLED);
        boolean enableApi = config.getBoolean(ConfigEntry.API_ENABLED);
        if(!uptimeSupport && !enableApi) return; // If none are enabled, just do nothing

        // Create a basic Javalin server
        server = Javalin.create();

        // Enable UptimeRobot endpoint to keep track of the bot's uptime
        if (uptimeSupport) {
            server = server.head(config.getString(ConfigEntry.UPTIME_ENDPOINT), ctx -> ctx.status(HttpStatus.OK));
        }

        // Enable the http api
        if (enableApi) {
            VCController vcController = new VCController(); // Handles VC data requests
            // This api key is used to authenticate requests.
            // Has to be used has a Bearer token.
            String apiKey = config.getString(ConfigEntry.API_KEY);

            server = server.updateConfig(config -> {
                        config.contextResolver.ip = context -> {
                            // Check for headers containing the ip of the user
                            final String cloudflareHeader = context.header("CF-Connecting-IP");
                            if (cloudflareHeader != null) {
                                return cloudflareHeader;
                            }

                            final String forwardedFor = context.header("X-Forwarded-For");
                            if (forwardedFor != null) {
                                return forwardedFor.split(",")[0];
                            }

                            return context.req().getRemoteAddr(); // If none are found, use the default Context#ip value
                        };

                        config.accessManager((handler, ctx, routeRoles) -> {
                            if (ctx.method().equals(HandlerType.HEAD)) {
                                handler.handle(ctx);
                                return;
                            }

                            String authorization = ctx.header("Authorization");
                            if (authorization != null) {
                                String[] spl = authorization.split(" ");
                                if (spl.length >= 2) {
                                    String bearer = spl[0];
                                    if (bearer.equals("Bearer")) {
                                        String token = authorization.substring(spl[0].length() + 1);
                                        if (token.equals(apiKey)) {
                                            handler.handle(ctx);
                                            return;
                                        }
                                    }

                                    ctx.status(HttpStatus.UNAUTHORIZED);
                                    return;
                                }
                            }

                            ctx.status(HttpStatus.BAD_REQUEST);
                        });
                    })
                    .error(HttpStatus.NOT_FOUND, ctx -> ResponseBuilder.init().code(HttpStatus.NOT_FOUND).send(ctx))
                    .routes(() -> path("api", () -> {
                        before(ctx -> NaiveRateLimit.requestPerTimeUnit(ctx, 10, TimeUnit.SECONDS));

                        path("{guild}", () -> {
                            get("/", ctx -> {
                                Optional<Settings> op = Bot.getInstance().getCore()
                                        .getSettingsMapper()
                                        .search(QueryBuilder.init()
                                                .add("guild", ctx.pathParam("guild"))
                                                .create());
                                if (op.isPresent()) {
                                    ResponseBuilder.init()
                                            .json(op.get())
                                            .send(ctx);
                                } else {
                                    ResponseBuilder.init()
                                            .code(HttpStatus.NOT_FOUND)
                                            .send(ctx);
                                }
                            });

                            path("vcs", () -> {
                                get("/", ctx -> ResponseBuilder.init()
                                        .json(Bot.getInstance().getCore()
                                                .getChannelMapper()
                                                .getMapper(ctx.pathParam("guild"))
                                                .bulkSearch(QueryBuilder.init()
                                                        .add("guild", ctx.pathParam("guild"))
                                                        .create()))
                                        .send(ctx));

                                EndpointGroup group = () -> {
                                    get("{id}", vcController::get);
                                    delete("{id}", vcController::delete);
                                };
                                path("channel", group);
                                path("user", group);
                            });
                        });
                    }));
        }

        server.start(config.getInt(ConfigEntry.SERVER_PORT));
    }

    @Override
    public void stop() {
        if (server == null) return;

        server.stop();
    }
}
