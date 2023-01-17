package hexlet.code;

import hexlet.code.controllers.RootController;
import hexlet.code.controllers.UrlCheckController;
import hexlet.code.controllers.UrlController;
import io.javalin.Javalin;
import io.javalin.plugin.rendering.template.JavalinThymeleaf;
import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import static io.javalin.apibuilder.ApiBuilder.path;
import static io.javalin.apibuilder.ApiBuilder.post;
import static io.javalin.apibuilder.ApiBuilder.get;


public class App {

    private static int getPort() {
        String port = System.getenv().getOrDefault("PORT", "5000");

        return Integer.parseInt(port);
    }

    private static String getAppEnv() {
        return System.getenv().getOrDefault("APP_ENV", "development");
    }

    private static void addRoutes(Javalin app) {
        app.get("/", RootController.index);
        app.routes(() -> {
            path("/urls", () -> {
                get(UrlController.listUrls);
                post(UrlController.addUrl);
                path("{id}", () -> {
                    get(UrlController.showUrl);
                    path("/checks", () -> {
                        post(UrlCheckController.urlChecks);
                    });
                });
            });
        });
    }

    private static ClassLoaderTemplateResolver getTemplateResolver() {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setPrefix("/templates/");
        templateResolver.setSuffix(".html");

        return templateResolver;
    }

    private static TemplateEngine getTemplateEngine() {
        TemplateEngine templateEngine = new TemplateEngine();
        templateEngine.addDialect(new LayoutDialect());
        templateEngine.addDialect(new Java8TimeDialect());
        templateEngine.addTemplateResolver(getTemplateResolver());

        return templateEngine;
    }

    public static Javalin getApp() {
        Javalin app = Javalin.create(config -> {
            if (getAppEnv().equals("development")) {
                config.enableDevLogging();
            }

            config.enableWebjars();
            JavalinThymeleaf.configure(getTemplateEngine());
        });

        addRoutes(app);
        return app;
    }

    public static void main(String[] args) {
        Javalin app = getApp();
        app.start(getPort());
    }

}
