package dev.starless.maggiordomo.api.controllers;

import dev.starless.maggiordomo.Bot;
import dev.starless.maggiordomo.api.AController;
import dev.starless.maggiordomo.api.ResponseBuilder;
import dev.starless.maggiordomo.data.user.VC;
import dev.starless.maggiordomo.storage.vc.LocalVCMapper;
import dev.starless.mongo.api.QueryBuilder;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.util.Optional;

public class VCController implements AController {

    @Override
    public void get(Context ctx) {
        getVCWithContext(ctx).ifPresentOrElse(
                vc -> ResponseBuilder.init().json(vc).send(ctx),
                () -> ResponseBuilder.init().code(HttpStatus.NOT_FOUND).send(ctx));
    }

    @Override
    public void delete(Context ctx) {
        getVCWithContext(ctx).ifPresentOrElse(
                vc -> {
                    Bot.getInstance().getCore()
                            .getChannelMapper()
                            .getMapper(ctx.pathParam("guild"))
                            .delete(vc);

                    ResponseBuilder.init().code(HttpStatus.OK).send(ctx);
                },
                () -> ResponseBuilder.init().code(HttpStatus.NOT_FOUND).send(ctx));
    }

    private Optional<VC> getVCWithContext(Context ctx) {
        String guild = ctx.pathParam("guild");
        LocalVCMapper vcMapper = Bot.getInstance().getCore()
                .getChannelMapper()
                .getNullableMapper(guild);

        if (vcMapper == null) return Optional.empty();

        String id = ctx.pathParam("id");
        String path = ctx.path();
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        String method = path.substring(0, path.lastIndexOf('/'));
        if (method.endsWith("user")) {
            return vcMapper.getGateway().load(QueryBuilder.init()
                    .add("guild", guild)
                    .add("user", id)
                    .create());
        } else {
            return vcMapper.getGateway().loadByChannel(QueryBuilder.init()
                    .add("guild", guild)
                    .add("channel", id)
                    .create());
        }
    }
}
