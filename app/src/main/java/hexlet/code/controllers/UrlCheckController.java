package hexlet.code.controllers;

import hexlet.code.domain.Url;
import hexlet.code.domain.UrlCheck;
import hexlet.code.domain.query.QUrl;
import io.javalin.http.Handler;

import io.javalin.http.NotFoundResponse;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class UrlCheckController {

    public static Handler urlChecks = ctx -> {

        String urlId = ctx.formParam("id");

        if (urlId == null) {
            throw new NotFoundResponse();
        }

        Url url = new QUrl()
                .id.equalTo(Integer.parseInt(urlId))
                .findOne();

        try {
            HttpResponse<String> response = Unirest.get(url.getName()).asString();

            Integer statusCode = response.getStatus();
            String body = response.getBody();
            String title = "";
            String h1 = "";
            String description = "";

            // parse html
            Element titleElement = Jsoup.parse(body).selectFirst("title");
            Element h1Element = Jsoup.parse(body).selectFirst("h1");
            Elements metas = Jsoup.parse(body).select("meta");

            if (titleElement != null) {
                title = titleElement.text();
            }

            if (h1Element != null) {
                h1 = h1Element.text();
            }

            for (Element e : metas) {
                if (e.attr("name").equals("description")) {
                    description = e.attr("content");
                }
            }

            UrlCheck urlCheck = new UrlCheck(statusCode, title, h1, description, url);
            urlCheck.save();

            ctx.sessionAttribute("flash", "Страница успешно проверена");
            ctx.sessionAttribute("flashType", "success");
            ctx.redirect("/urls/" + urlId);

        } catch (UnirestException e) {
            ctx.sessionAttribute("flash", "Некорректный адрес");
            ctx.sessionAttribute("flashType", "danger");
            ctx.redirect("/urls/" + urlId);
        }
    };

}
