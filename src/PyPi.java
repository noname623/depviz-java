import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Получение прямых зависимостей пакета pip из репозитория PyPI.
 *
 * <p>Репозиторий PyPI отдаёт описание пакета в формате JSON по адресу
 * {@code <репозиторий>/<имя пакета>/json}, например
 * https://pypi.org/pypi/requests/json. Прямые зависимости записаны
 * в поле info.requires_dist.</p>
 *
 * <p>Каждая зависимость записана как требование, например
 * {@code idna<4,>=2.5} или {@code PySocks>=1.5.6; extra == "socks"}.
 * В начале стоит имя пакета, затем ограничения версии, а после точки
 * с запятой - условие (маркер).</p>
 */
public class PyPi {

    private static final int TIMEOUT_MS = 30000;
    private static final String MARKER_SEPARATOR = ";";
    private static final String EXTRA_MARKER = "extra";
    private static final String NAME_SEPARATORS = "[-_.]+";
    private static final String NAME_DASH = "-";

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

    /**
     * Проверить, что зависимость нужна только для дополнительной
     * возможности пакета (extra).
     *
     * <p>Такие зависимости pip ставит, только если пользователь сам
     * попросил эту возможность, например {@code requests[socks]}.</p>
     *
     * @param requirement строка требования
     * @return true, если в условии после ";" есть extra
     */
    public static boolean isExtra(String requirement) {
        if (!requirement.contains(MARKER_SEPARATOR)) {
            return false;
        }
        String marker = requirement.substring(
                requirement.indexOf(MARKER_SEPARATOR));
        return marker.contains(EXTRA_MARKER);
    }

    /**
     * Прочитать имя пакета из начала строки требования.
     *
     * <p>Имя заканчивается на первом символе, который не может быть
     * частью имени: пробел, знак сравнения, скобка или точка с запятой.</p>
     *
     * @param requirement строка требования, например idna<4,>=2.5
     * @return имя пакета, например idna
     */
    public static String readName(String requirement) {
        StringBuilder name = new StringBuilder();
        for (char symbol : requirement.trim().toCharArray()) {
            if (!isNameChar(symbol)) {
                break;
            }
            name.append(symbol);
        }
        return name.toString();
    }

    /**
     * Проверить, может ли символ быть частью имени пакета pip.
     *
     * @param symbol символ
     * @return true для латинских букв, цифр и знаков . - _
     */
    private static boolean isNameChar(char symbol) {
        boolean latinLetter = (symbol >= 'a' && symbol <= 'z')
                || (symbol >= 'A' && symbol <= 'Z');
        boolean digit = symbol >= '0' && symbol <= '9';
        return latinLetter || digit || symbol == '.' || symbol == '-'
                || symbol == '_';
    }

    /**
     * Привести имя пакета к единому виду, как это делает pip.
     *
     * <p>Буквы становятся маленькими, а знаки . - _ заменяются
     * на один дефис. Так Charset_Normalizer и charset-normalizer
     * считаются одним пакетом.</p>
     *
     * @param name имя пакета
     * @return имя в едином виде
     */
    public static String normalizeName(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.replaceAll(NAME_SEPARATORS, NAME_DASH);
    }

    /**
     * Получить имена прямых зависимостей пакета для построения графа.
     *
     * <p>Зависимости extra не учитываются. Имена приводятся
     * к единому виду.</p>
     *
     * @param repo URL репозитория
     * @param packageName имя пакета
     * @return список имён прямых зависимостей
     * @throws IllegalArgumentException если данные не получены
     */
    public static List<String> getDependencyNames(String repo,
                                                  String packageName) {
        List<String> names = new ArrayList<>();
        for (String requirement : getDirectDependencies(repo, packageName)) {
            String name = normalizeName(readName(requirement));
            if (!isExtra(requirement) && !names.contains(name)) {
                names.add(name);
            }
        }
        return names;
    }
}
