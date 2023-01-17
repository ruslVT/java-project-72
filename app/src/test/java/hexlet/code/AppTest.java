package hexlet.code;

import hexlet.code.domain.Url;
import hexlet.code.domain.UrlCheck;
import hexlet.code.domain.query.QUrl;
import hexlet.code.domain.query.QUrlCheck;
import io.ebean.DB;
import io.ebean.Database;
import io.javalin.Javalin;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


public class AppTest {

    @Test
    void testInit() {
        assertThat(true).isEqualTo(true);
    }

    private static Javalin app;
    private static String baseUrl;
    private static Database database;
    private static MockWebServer webServer;

    @BeforeAll
    public static void beforeAll() {
        app = App.getApp();
        app.start(0);
        int port = app.port();
        baseUrl = "http://localhost:" + port;
        database = DB.getDefault();
        webServer = new MockWebServer();
    }

    @AfterAll
    public static void afterAll() {
        app.stop();
    }

    @BeforeEach
    void beforeEach() {
        database.script().run("/truncate.sql");
        database.script().run("/seed-test-db.sql");
    }

    @Nested
    class RootControllerTest {
        @Test
        void testIndex() {
            HttpResponse<String> response = Unirest.get(baseUrl).asString();
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getBody()).contains("Анализатор страниц");
        }
    }

    @Nested
    class UrlControllerTest {
        @Test
        void testIndex() {
            HttpResponse<String> response = Unirest.get(baseUrl + "/urls").asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(body).contains("https://www.yandex.ru");
            assertThat(body).contains("https://www.rambler.ru");
        }

        @Test
        void testAddUrl() {
            String inputName = "https://www.google.com";

            // check addUrl
            HttpResponse<String> responsePost = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", inputName)
                    .asString();

            assertThat(responsePost.getStatus()).isEqualTo(302);
            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/urls");

            // check is available url in table on /urls page
            HttpResponse<String> response = Unirest.get(baseUrl + "/urls").asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(body).contains(inputName);
            assertThat(body).contains("Страница успешно добавлена");

            // check url in base
            Url actualUrl = new QUrl()
                    .name.equalTo(inputName)
                    .findOne();

            assertThat(actualUrl).isNotNull();
            assertThat(actualUrl.getName()).isEqualTo(inputName);
        }

        @Test
        void testShowUrl() {
            HttpResponse<String> response = Unirest.get(baseUrl + "/urls/1").asString();
            String body = response.getBody();

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(body).contains("https://www.yandex.ru");
            assertThat(body).contains("01/01/2022 13:57");

        }
    }

    @Nested
    class UrlCheckControllerTest {

        @Test
        void testUrlCheck() throws IOException {
            MockResponse response = new MockResponse();
            response.setBody("<title>Название</title><h1>Заголовок</h1>"
                    + "<meta name=\"description\" content=\"Описание\">");
            webServer.enqueue(response);
            webServer.start(5001);
            String serverUrl = webServer.url("/").toString();

            assertThat(serverUrl).isEqualTo("http://localhost:5001/");

            // check post request on /urls/{id}/checks
            String id = "3";
            HttpResponse<String> postResponseCheckUrl = Unirest.post(baseUrl + "/urls/3/checks")
                    .field("id", id)
                    .asString();

            assertThat(postResponseCheckUrl.getStatus()).isEqualTo(302);

            // check create entity urlCheck in base
            UrlCheck urlCheck = new QUrlCheck()
                    .id.equalTo(1)
                    .findOne();

            assertThat(urlCheck).isNotNull();
            assertThat(urlCheck.getStatusCode()).isNotNull().isEqualTo(200);
            assertThat(urlCheck.getH1()).isNotNull().isEqualTo("Заголовок");
            assertThat(urlCheck.getTitle()).isNotNull().isEqualTo("Название");
            assertThat(urlCheck.getDescription()).isNotNull().isEqualTo("Описание");
            assertThat(urlCheck.getCreatedAt()).isNotNull();

            // check table urlCheck on /urls/{id} page after urlChecks
            HttpResponse<String> getResponseShow = Unirest.get(baseUrl + "/urls/3").asString();
            String body = getResponseShow.getBody();

            assertThat(getResponseShow.getStatus()).isEqualTo(200);
            assertThat(body).contains("Страница успешно проверена");
            assertThat(body).contains("http://localhost:5001/");
            assertThat(body).contains("Название");
            assertThat(body).contains("Заголовок");
            assertThat(body).contains("Описание");

            webServer.shutdown();

        }

    }
}
