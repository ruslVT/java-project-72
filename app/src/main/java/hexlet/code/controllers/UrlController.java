package hexlet.code.controllers;


import hexlet.code.domain.Url;
import hexlet.code.domain.UrlCheck;
import hexlet.code.domain.query.QUrl;
import hexlet.code.domain.query.QUrlCheck;
import io.ebean.PagedList;
import io.javalin.http.Handler;
import io.javalin.http.NotFoundResponse;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class UrlController {

    public static Handler listUrls = ctx -> {

        String term = ctx.queryParamAsClass("term", String.class).getOrDefault("");
        int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1) - 1;
        int rowsPerPage = 10;

        PagedList<Url> pagedUrls = new QUrl()
                .name.icontains(term)
                        .setFirstRow(page * rowsPerPage)
                                .setMaxRows(rowsPerPage)
                                        .orderBy()
                                                .id.asc()
                        .findPagedList();

        List<Url> urls = pagedUrls.getList();

        int lastPage = pagedUrls.getTotalPageCount() + 1;
        int currentPage = pagedUrls.getPageIndex() + 1;
        List<Integer> pages = IntStream
                .range(1, lastPage)
                        .boxed()
                                .collect(Collectors.toList());

        ctx.attribute("urls", urls);
        ctx.attribute("term", term);
        ctx.attribute("pages", pages);
        ctx.attribute("currentPage", currentPage);
        ctx.render("urls/index.html");
    };

    public static Handler addUrl = ctx -> {
        String inputName = ctx.formParam("url");
        String normUrl = "";

        // checking the url for validity
        try {

            if (inputName == null) {  // if input form no property required=""
                throw new MalformedURLException();
            }

            URL requiredUrl = new URL(inputName);
            normUrl = requiredUrl.toString().replace(requiredUrl.getPath(), ""); // get url without path
        } catch (MalformedURLException e) {
            ctx.sessionAttribute("flash", "Некорректный URL");
            ctx.sessionAttribute("flashType", "danger");
            ctx.redirect("/");
            return;
        }

        // search duplicate url
        Url duplicateUrl = new QUrl()
                .name.equalTo(normUrl)
                        .findOne();

        if (duplicateUrl != null) {
            ctx.sessionAttribute("flash", "Страница уже существует");
            ctx.sessionAttribute("flashType", "success");
            ctx.redirect("/urls");
            return;
        }

        Url url = new Url(normUrl);
        url.save();

        ctx.sessionAttribute("flash", "Страница успешно добавлена");
        ctx.sessionAttribute("flashType", "success");
        ctx.redirect("/urls");
    };

    public static Handler showUrl = ctx -> {
        int id = ctx.pathParamAsClass("id", Integer.class).getOrDefault(null);

        Url url = new QUrl()
                .id.equalTo(id)
                .findOne();

        if (url == null) {
            throw new NotFoundResponse();
        }

        List<UrlCheck> urlChecks = new QUrlCheck()
                .url.equalTo(url)
                .orderBy().id
                .desc()
                .findList();

        ctx.attribute("url", url);
        ctx.attribute("urlChecks", urlChecks);
        ctx.render("urls/show.html");
    };

    public static Handler checkUrl = ctx -> {

        Integer urlId = ctx.pathParamAsClass("id", Integer.class).getOrDefault(null);

        Url url = new QUrl()
                .id.equalTo(urlId)
                .findOne();

        if (url == null) {
            throw new NotFoundResponse();
        }


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
