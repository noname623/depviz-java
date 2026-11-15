import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Параметры программы, заданные опциями командной строки.
 *
 * <p>Класс разбирает аргументы командной строки и проверяет
 * каждый параметр. При ошибке выбрасывается
 * {@link IllegalArgumentException} с понятным сообщением.</p>
 */
public class Config {

    /** Режим работы с настоящим репозиторием. */
    public static final String MODE_REAL = "real";

    /** Режим работы с тестовым репозиторием. */
    public static final String MODE_TEST = "test";

    /** Опция имени анализируемого пакета. */
    public static final String OPTION_PACKAGE = "--package";

    /** Опция URL репозитория или пути к тестовому файлу. */
    public static final String OPTION_REPO = "--repo";

    /** Опция режима работы с репозиторием. */
    public static final String OPTION_MODE = "--mode";

    /** Опция имени файла с изображением графа. */
    public static final String OPTION_OUTPUT = "--output";

    private static final String[] OPTIONS = {
        OPTION_PACKAGE, OPTION_REPO, OPTION_MODE, OPTION_OUTPUT
    };
    private static final String HTTP_PREFIX = "http://";
    private static final String HTTPS_PREFIX = "https://";
    private static final String IMAGE_EXTENSION = ".svg";
    private static final String PIP_NAME_PATTERN =
            "[A-Za-z0-9]([A-Za-z0-9._-]*[A-Za-z0-9])?";

    private final String packageName;
    private final String repo;
    private final String mode;
    private final String output;

    /**
     * Создать набор параметров.
     *
     * @param packageName имя анализируемого пакета
     * @param repo URL репозитория или путь к тестовому файлу
     * @param mode режим работы с репозиторием
     * @param output имя файла с изображением графа
     */
    public Config(String packageName, String repo, String mode,
                  String output) {
        this.packageName = packageName;
        this.repo = repo;
        this.mode = mode;
        this.output = output;
    }

    /**
     * Разобрать аргументы командной строки и проверить параметры.
     *
     * @param args аргументы командной строки
     * @return проверенные параметры программы
     * @throws IllegalArgumentException если параметр задан неверно
     */
    public static Config parse(String[] args) {
        Map<String, String> values = readOptions(args);
        String mode = values.getOrDefault(OPTION_MODE, MODE_REAL);
        Config config = new Config(values.get(OPTION_PACKAGE),
                values.get(OPTION_REPO), mode, values.get(OPTION_OUTPUT));
        config.check();
        return config;
    }

    /**
     * Прочитать пары "опция значение" из аргументов.
     *
     * @param args аргументы командной строки
     * @return словарь: опция -> значение
     * @throws IllegalArgumentException если опция неизвестна,
     *         повторяется или у неё нет значения
     */
    private static Map<String, String> readOptions(String[] args) {
        Map<String, String> values = new HashMap<>();
        for (int i = 0; i < args.length; i += 2) {
            String option = args[i];
            if (!isKnownOption(option)) {
                throw new IllegalArgumentException(
                        "неизвестная опция: " + option);
            }
            if (values.containsKey(option)) {
                throw new IllegalArgumentException(
                        "опция указана дважды: " + option);
            }
            if (i + 1 >= args.length) {
                throw new IllegalArgumentException(
                        "не задано значение опции " + option);
            }
            values.put(option, args[i + 1]);
        }
        return values;
    }

    /**
     * Проверить, что опция есть среди известных.
     *
     * @param option текст опции
     * @return true, если опция известна
     */
    private static boolean isKnownOption(String option) {
        for (String known : OPTIONS) {
            if (known.equals(option)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Проверить строку на пустоту.
     *
     * @param text строка или null
     * @return true, если строка не задана или состоит из пробелов
     */
    private static boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    /**
     * Проверить все параметры по очереди.
     *
     * @throws IllegalArgumentException если параметр задан неверно
     */
    private void check() {
        checkMode();
        checkPackage();
        checkRepo();
        checkOutput();
    }

    /**
     * Проверить режим работы с репозиторием.
     *
     * @throws IllegalArgumentException если режим не real и не test
     */
    private void checkMode() {
        if (!MODE_REAL.equals(mode) && !MODE_TEST.equals(mode)) {
            throw new IllegalArgumentException(
                    "режим (--mode) должен быть real или test");
        }
    }

    /**
     * Проверить имя анализируемого пакета.
     *
     * <p>В режиме real имя должно быть именем пакета pip:
     * латинские буквы, цифры и знаки . - _ внутри.</p>
     *
     * @throws IllegalArgumentException если имя пустое, с пробелами
     *         или не подходит для pip
     */
    private void checkPackage() {
        if (isBlank(packageName)) {
            throw new IllegalArgumentException(
                    "не задано имя пакета (--package)");
        }
        if (packageName.contains(" ")) {
            throw new IllegalArgumentException(
                    "имя пакета не должно содержать пробелы");
        }
        if (MODE_REAL.equals(mode)
                && !packageName.matches(PIP_NAME_PATTERN)) {
            throw new IllegalArgumentException(
                    "имя пакета pip может содержать только латинские "
                    + "буквы, цифры и знаки . - _ и должно начинаться "
                    + "и заканчиваться буквой или цифрой");
        }
    }

    /**
     * Проверить URL репозитория или путь к тестовому файлу.
     *
     * @throws IllegalArgumentException если адрес или путь неверный
     */
    private void checkRepo() {
        if (isBlank(repo)) {
            throw new IllegalArgumentException(
                    "не задан репозиторий (--repo)");
        }
        boolean isUrl = repo.startsWith(HTTP_PREFIX)
                || repo.startsWith(HTTPS_PREFIX);
        if (MODE_REAL.equals(mode) && !isUrl) {
            throw new IllegalArgumentException(
                    "URL репозитория должен начинаться с http:// "
                    + "или https://");
        }
        if (MODE_TEST.equals(mode) && !new File(repo).isFile()) {
            throw new IllegalArgumentException(
                    "файл тестового репозитория не найден: " + repo);
        }
    }

    /**
     * Проверить имя файла с изображением графа.
     *
     * @throws IllegalArgumentException если имя пустое или не .svg
     */
    private void checkOutput() {
        if (isBlank(output)) {
            throw new IllegalArgumentException(
                    "не задано имя файла изображения (--output)");
        }
        if (!output.endsWith(IMAGE_EXTENSION)) {
            throw new IllegalArgumentException(
                    "файл изображения должен иметь расширение .svg");
        }
    }

    /**
     * Получить имя анализируемого пакета.
     *
     * @return имя пакета
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * Получить URL репозитория или путь к тестовому файлу.
     *
     * @return URL или путь
     */
    public String getRepo() {
        return repo;
    }

    /**
     * Получить режим работы с репозиторием.
     *
     * @return real или test
     */
    public String getMode() {
        return mode;
    }

    /**
     * Получить имя файла с изображением графа.
     *
     * @return имя файла
     */
    public String getOutput() {
        return output;
    }
}
