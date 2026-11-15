import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Поддельный репозиторий для тестов без интернета.
 *
 * <p>Запускает на компьютере маленький HTTP-сервер из стандартной
 * библиотеки JDK. Сервер отдаёт заранее заданные ответы по путям,
 * а на остальные пути отвечает кодом 404.</p>
 */
public class FakeRepo {

    private static final String HOST = "127.0.0.1";
    private static final int ANY_FREE_PORT = 0;
    private static final int NOT_FOUND = 404;
    private static final int OK = 200;
    private static final int NO_DELAY = 0;

    private final HttpServer server;

    /**
     * Запустить сервер.
     *
     * @param pages словарь: путь (например /pypi/a/json) -> текст ответа
     * @throws IOException если сервер не удалось запустить
     */
    public FakeRepo(Map<String, String> pages) throws IOException {
        server = HttpServer.create(
                new InetSocketAddress(HOST, ANY_FREE_PORT), NO_DELAY);
        server.createContext("/", exchange -> answer(exchange, pages));
        server.start();
    }

    /**
     * Ответить на один запрос.
     *
     * @param exchange запрос и ответ
     * @param pages заранее заданные ответы
     * @throws IOException если ответ не удалось отправить
     */
    private static void answer(HttpExchange exchange,
                               Map<String, String> pages)
            throws IOException {
        String body = pages.get(exchange.getRequestURI().getPath());
        int code = OK;
        if (body == null) {
            body = "not found";
            code = NOT_FOUND;
        }
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    /**
     * Получить адрес репозитория для параметра --repo.
     *
     * @return адрес вида http://127.0.0.1:порт/pypi
     */
    public String url() {
        return "http://" + HOST + ":" + server.getAddress().getPort()
                + "/pypi";
    }

    /**
     * Остановить сервер.
     */
    public void stop() {
        server.stop(NO_DELAY);
    }
}
