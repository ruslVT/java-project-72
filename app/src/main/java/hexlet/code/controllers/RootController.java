package hexlet.code.controllers;

import io.javalin.http.Handler;

public final class RootController {

    public static Handler index = ctx -> {
        ctx.render("/layouts/layout.html");
    };

}
