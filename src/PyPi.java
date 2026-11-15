import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Получение прямых зависимостей пакета pip из репозитория PyPI.
 *
 * <p>Репозиторий PyPI отдаёт описание пакета в формате JSON по адресу
 * {@code <репозиторий>/<имя пакета>/json}, например
 * https://pypi.org/pypi/requests/json. Прямые зависимости записаны
 * в поле info.requires_dist.</p>
 */
public class PyPi {

    private static final int TIMEOUT_MS = 30000;

    /**
     * Закрытый конструктор: класс содержит только статические методы.
     */
    private PyPi() {
    }

    /**
     * Составить адрес JSON-описания пакета.
     *
     * @param repo URL репозитория, например https://pypi.org/pypi
     * @param packageName имя пакета
     * @return адрес JSON-описания
     */
    public static String makeJsonUrl(String repo, String packageName) {
        String base = repo;
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/" + packageName + "/json";
    }

    /**
     * Превратить текст адреса в объект URL.
     *
     * @param address адрес файла
     * @return объект URL
     * @throws IllegalArgumentException если адрес записан неверно
     */
    private static URL makeUrl(String address) {
        try {
            return URI.create(address).toURL();
        } catch (IllegalArgumentException | MalformedURLException error) {
            throw new IllegalArgumentException(
                    "неверный адрес репозитория: " + address);
        }
    }

    /**
     * Скачать текст по адресу.
     *
     * @param address адрес файла
     * @return содержимое ответа
     * @throws IllegalArgumentException если скачать не удалось
     */
    public static String downloadText(String address) {
        try {
            HttpURLConnection connection =
                    (HttpURLConnection) makeUrl(address).openConnection();
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                throw new IllegalArgumentException("не удалось загрузить "
                        + address + " (код ответа " + code + ")");
            }
            try (InputStream input = connection.getInputStream()) {
                return new String(input.readAllBytes(),
                        StandardCharsets.UTF_8);
            }
        } catch (IOException error) {
            throw new IllegalArgumentException("не удалось загрузить "
                    + address + " (" + error.getMessage() + ")");
        }
    }

    /**
     * Получить прямые зависимости пакета из репозитория.
     *
     * @param repo URL репозитория
     * @param packageName имя пакета
     * @return строки зависимостей из поля requires_dist
     * @throws IllegalArgumentException если данные не получены
     */
    public static List<String> getDirectDependencies(String repo,
                                                     String packageName) {
        String json = downloadText(makeJsonUrl(repo, packageName));
        return new RequiresDistParser(json).parse();
    }
}
