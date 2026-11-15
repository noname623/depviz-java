import java.io.PrintStream;

/**
 * Инструмент визуализации графа зависимостей. Этап 1.
 *
 * <p>Программа читает параметры из опций командной строки,
 * проверяет их и выводит на экран в формате ключ-значение.</p>
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
        try {
            config = Config.parse(args);
        } catch (IllegalArgumentException error) {
            err.println("Ошибка: " + error.getMessage());
            return EXIT_ERROR;
        }
        printConfig(config, out);
        return EXIT_OK;
    }

    /**
     * Вывести параметры в формате ключ-значение.
     *
     * @param config параметры программы
     * @param out поток для вывода
     */
    private static void printConfig(Config config, PrintStream out) {
        out.println("package = " + config.getPackageName());
        out.println("repo = " + config.getRepo());
        out.println("mode = " + config.getMode());
        out.println("output = " + config.getOutput());
    }
}
