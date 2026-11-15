import java.io.PrintStream;
import java.util.List;
import java.util.Map;

/**
 * Инструмент визуализации графа зависимостей. Этап 3.
 *
 * <p>Программа читает параметры из опций командной строки, проверяет
 * их, строит граф зависимостей пакета (с учётом транзитивности)
 * и выводит его на экран. Зависимости берутся из репозитория PyPI
 * или из файла тестового репозитория.</p>
 */
public class Main {

    /** Код выхода при успешной работе. */
    public static final int EXIT_OK = 0;

    /** Код выхода при ошибке. */
    public static final int EXIT_ERROR = 1;

    /**
     * Точка входа программы.
     *
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    /**
     * Выполнить программу.
     *
     * @param args аргументы командной строки
     * @param out поток для вывода результата
     * @param err поток для вывода ошибок
     * @return код выхода: 0 при успехе, 1 при ошибке
     */
    public static int run(String[] args, PrintStream out, PrintStream err) {
        Config config;
        String start;
        Map<String, List<String>> graph;
        try {
            config = Config.parse(args);
            start = getStartName(config);
            graph = Graph.build(start, makeSource(config));
        } catch (IllegalArgumentException error) {
            err.println("Ошибка: " + error.getMessage());
            return EXIT_ERROR;
        }
        printGraph(start, graph, out);
        return EXIT_OK;
    }

    /**
     * Получить имя пакета, с которого начинается обход.
     *
     * <p>В режиме real имя приводится к единому виду, как это делает
     * pip, чтобы совпадать с именами зависимостей.</p>
     *
     * @param config параметры программы
     * @return имя начального пакета
     */
    private static String getStartName(Config config) {
        if (Config.MODE_TEST.equals(config.getMode())) {
            return config.getPackageName();
        }
        return PyPi.normalizeName(config.getPackageName());
    }

    /**
     * Выбрать источник зависимостей по режиму работы.
     *
     * @param config параметры программы
     * @return источник прямых зависимостей
     * @throws IllegalArgumentException если тестовый файл с ошибками
     *         или пакета в нём нет
     */
    private static Graph.DependencySource makeSource(Config config) {
        if (Config.MODE_TEST.equals(config.getMode())) {
            return makeTestSource(config.getRepo(), config.getPackageName());
        }
        String repo = config.getRepo();
        return name -> PyPi.getDependencyNames(repo, name);
    }

    /**
     * Создать источник зависимостей из тестового файла.
     *
     * @param path путь к файлу тестового репозитория
     * @param packageName имя анализируемого пакета
     * @return источник прямых зависимостей
     * @throws IllegalArgumentException если файл с ошибками
     *         или пакета в нём нет
     */
    private static Graph.DependencySource makeTestSource(String path,
                                                         String packageName) {
        Map<String, List<String>> repo = RepoFile.load(path);
        if (!repo.containsKey(packageName)) {
            throw new IllegalArgumentException("пакет " + packageName
                    + " не найден в тестовом репозитории");
        }
        return name -> RepoFile.getDependencies(repo, name);
    }

    /**
     * Вывести граф зависимостей на экран.
     *
     * <p>Каждая строка: пакет -> его прямые зависимости.</p>
     *
     * @param start имя анализируемого пакета
     * @param graph граф зависимостей
     * @param out поток для вывода
     */
    private static void printGraph(String start,
                                   Map<String, List<String>> graph,
                                   PrintStream out) {
        out.println("Граф зависимостей пакета " + start + ":");
        for (Map.Entry<String, List<String>> entry : graph.entrySet()) {
            String line = entry.getKey() + " -> ";
            if (entry.getValue().isEmpty()) {
                line += "нет зависимостей";
            } else {
                line += String.join(", ", entry.getValue());
            }
            out.println(line);
        }
    }
}
