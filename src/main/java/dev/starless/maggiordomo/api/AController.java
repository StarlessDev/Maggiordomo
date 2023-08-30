package dev.starless.maggiordomo.api;

import io.javalin.http.Context;

public interface AController {

    default void get(Context ctx) {
    }

    default void post(Context ctx) {
    }

    default void delete(Context ctx) {
    }
}
