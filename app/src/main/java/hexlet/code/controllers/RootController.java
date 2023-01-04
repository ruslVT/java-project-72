package hexlet.code.controllers;

import hexlet.code.domain.Url;
import io.javalin.http.Handler;

public final class RootController {

    public static Handler index = ctx -> {
        Url url = new Url();
        ctx.attribute("url", url);
        ctx.render("index.html");
    };

}
