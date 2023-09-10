package dev.starless.maggiordomo;

import com.google.gson.JsonObject;
import dev.starless.maggiordomo.api.ResponseBuilder;
import dev.starless.maggiordomo.api.controllers.VCController;
import dev.starless.maggiordomo.config.Config;
import dev.starless.maggiordomo.config.ConfigEntry;
import dev.starless.maggiordomo.data.Settings;
import dev.starless.maggiordomo.interfaces.Service;
import dev.starless.maggiordomo.utils.GsonUtil;
import dev.starless.mongo.api.QueryBuilder;
import io.javalin.Javalin;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.HandlerType;
import io.javalin.http.HttpStatus;
import io.javalin.http.util.NaiveRateLimit;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Guild;

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

            // Max requests per second
            // set to a very low value rn because this has still no use case.
            // Maybe a web gui coming soon?
            int maxRequests = config.getInt(ConfigEntry.API_RATE_LIMIT);

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
                        before(ctx -> NaiveRateLimit.requestPerTimeUnit(ctx, maxRequests, TimeUnit.SECONDS));

                        get("stats", ctx -> ResponseBuilder.init().json(Statistics.getInstance().toJsonObject()).send(ctx));

                        path("{guild}", () -> {
                            get("/", ctx -> {
                                Guild guild = Bot.getInstance().getJda().getGuildById(ctx.pathParam("guild"));
                                if(guild != null) {
                                    Optional<Settings> op = Bot.getInstance().getCore()
                                            .getSettingsMapper()
                                            .search(QueryBuilder.init()
                                                    .add("guild", guild.getId())
                                                    .create());
                                    if (op.isPresent()) {
                                        JsonObject json = GsonUtil.toJson(op.get());
                                        json.addProperty("name", guild.getName());
                                        json.addProperty("icon", guild.getIconUrl());

                                        ResponseBuilder.init()
                                                .json(json)
                                                .send(ctx);

                                        return;
                                    }
                                }

                                ResponseBuilder.init()
                                        .code(HttpStatus.NOT_FOUND)
                                        .send(ctx);
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
