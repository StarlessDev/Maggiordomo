package dev.starless.maggiordomo.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.starless.maggiordomo.Bot;
import dev.starless.maggiordomo.utils.GsonUtil;
import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.util.Objects;

public class ResponseBuilder {

    private HttpStatus code;
    private JsonElement output;

    public static ResponseBuilder init() {
        return new ResponseBuilder();
    }

    private ResponseBuilder() {
        this.code = HttpStatus.OK;
    }

    public ResponseBuilder code(HttpStatus code) {
        this.code = code;
        return this;
    }

    public ResponseBuilder json(Object o) {
        this.output = GsonUtil.gson.toJsonTree(o);
        return this;
    }

    public void send(Context ctx) {
        Objects.requireNonNull(code);

        ctx.status(code).contentType(ContentType.APPLICATION_JSON);
        if (output != null) {
            ctx.result(output.toString());
        }
    }
}
