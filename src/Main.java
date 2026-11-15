import java.io.PrintStream;
import java.util.List;

/**
 * Инструмент визуализации графа зависимостей. Этап 2.
 *
 * <p>Программа читает параметры из опций командной строки, проверяет
 * их, получает из репозитория PyPI описание пакета pip и выводит
 * на экран все его прямые зависимости.</p>
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
        List<String> dependencies;
        try {
            config = Config.parse(args);
            dependencies = getDependencies(config);
        } catch (IllegalArgumentException error) {
            err.println("Ошибка: " + error.getMessage());
            return EXIT_ERROR;
        }
        printDependencies(config.getPackageName(), dependencies, out);
        return EXIT_OK;
    }

    /**
     * Получить прямые зависимости пакета.
     *
     * @param config параметры программы
     * @return список прямых зависимостей
     * @throws IllegalArgumentException если режим test
     *         или данные не получены
     */
    private static List<String> getDependencies(Config config) {
        if (Config.MODE_TEST.equals(config.getMode())) {
            throw new IllegalArgumentException(
                    "режим test пока не поддерживается");
        }
        return PyPi.getDirectDependencies(config.getRepo(),
                config.getPackageName());
    }

    /**
     * Вывести прямые зависимости пакета.
     *
     * @param packageName имя пакета
     * @param dependencies список зависимостей
     * @param out поток для вывода
     */
    private static void printDependencies(String packageName,
                                          List<String> dependencies,
                                          PrintStream out) {
        out.println("Прямые зависимости пакета " + packageName + ":");
        if (dependencies.isEmpty()) {
            out.println("нет зависимостей");
        }
        for (String dependency : dependencies) {
            out.println(dependency);
        }
    }
}
