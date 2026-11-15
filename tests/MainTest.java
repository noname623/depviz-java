import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Тесты главного класса: параметры и вывод зависимостей.
 */
public class MainTest {

    private static final String REPO_URL = "https://pypi.org/pypi";

    /**
     * Результат запуска программы в тесте.
     */
    static class Result {

        /** Код выхода. */
        final int code;

        /** Текст обычного вывода. */
        final String out;

        /** Текст вывода ошибок. */
        final String err;

        /**
         * Сохранить результат запуска.
         *
         * @param code код выхода
         * @param out текст обычного вывода
         * @param err текст вывода ошибок
         */
        Result(int code, String out, String err) {
            this.code = code;
            this.out = out;
            this.err = err;
        }
    }

    /**
     * Запустить программу и перехватить её вывод.
     *
     * @param args аргументы командной строки
     * @return код выхода и выведенный текст
     */
    static Result runMain(String... args) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        int code = Main.run(args,
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
        return new Result(code, out.toString(StandardCharsets.UTF_8),
                err.toString(StandardCharsets.UTF_8));
    }

    /**
     * Проверить, что программа завершилась с ошибкой.
     *
     * @param text ожидаемая часть сообщения об ошибке
     * @param args аргументы командной строки
     */
    static void checkError(String text, String... args) {
        Result result = runMain(args);
        TestRunner.assertEquals(Main.EXIT_ERROR, result.code);
        TestRunner.assertEquals("", result.out);
        TestRunner.assertContains(text, result.err);
    }

    /**
     * Создать временный файл, который удалится после тестов.
     *
     * @return путь к файлу
     * @throws IOException если файл не удалось создать
     */
    static String makeTempFile() throws IOException {
        File file = File.createTempFile("repo", ".txt");
        file.deleteOnExit();
        return file.getPath();
    }

    /**
     * Добавить тесты этого класса в общий запуск.
     *
     * @param runner объект для запуска тестов
     */
    public static void addTo(TestRunner runner) {
        runner.run("вывод прямых зависимостей", MainTest::testPrintDeps);
        runner.run("пакет без зависимостей", MainTest::testNoDeps);
        runner.run("пакета нет в репозитории", MainTest::testNotFound);
        runner.run("режим test пока не работает", MainTest::testTestMode);
        runner.run("неверное имя пакета pip", MainTest::testBadPipName);
        runner.run("нет имени пакета", MainTest::testNoPackage);
        runner.run("пробел в имени пакета", MainTest::testPackageSpace);
        runner.run("нет репозитория", MainTest::testNoRepo);
        runner.run("неверный URL", MainTest::testBadUrl);
        runner.run("нет тестового файла", MainTest::testNoTestFile);
        runner.run("неверный режим", MainTest::testBadMode);
        runner.run("нет имени изображения", MainTest::testNoOutput);
        runner.run("изображение не .svg", MainTest::testBadOutput);
        runner.run("неизвестная опция", MainTest::testUnknownOption);
        runner.run("опция без значения", MainTest::testNoValue);
        runner.run("опция дважды", MainTest::testTwice);
    }

    /**
     * Запустить программу на поддельном репозитории с одним пакетом.
     *
     * @param name имя пакета в репозитории
     * @param json JSON-описание пакета
     * @param asked имя пакета, которое передаётся программе
     * @return результат запуска
     * @throws IOException если сервер не запустился
     */
    static Result runWithRepo(String name, String json, String asked)
            throws IOException {
        FakeRepo repo = new FakeRepo(Map.of("/pypi/" + name + "/json", json));
        try {
            return runMain("--package", asked, "--repo", repo.url(),
                    "--output", "graph.svg");
        } finally {
            repo.stop();
        }
    }

    /**
     * Выводятся все прямые зависимости пакета.
     *
     * @throws IOException если сервер не запустился
     */
    static void testPrintDeps() throws IOException {
        Result result = runWithRepo("requests", PyPiTest.REQUESTS_JSON,
                "requests");
        TestRunner.assertEquals(Main.EXIT_OK, result.code);
        TestRunner.assertContains("Прямые зависимости пакета requests:",
                result.out);
        TestRunner.assertContains("idna<4,>=2.5", result.out);
        TestRunner.assertContains("extra == \"socks\"", result.out);
    }

    /**
     * Сообщение, если у пакета нет зависимостей.
     *
     * @throws IOException если сервер не запустился
     */
    static void testNoDeps() throws IOException {
        Result result = runWithRepo("idna", PyPiTest.NO_DEPS_JSON, "idna");
        TestRunner.assertEquals(Main.EXIT_OK, result.code);
        TestRunner.assertContains("нет зависимостей", result.out);
    }

    /**
     * Пакета нет в репозитории: ошибка 404.
     *
     * @throws IOException если сервер не запустился
     */
    static void testNotFound() throws IOException {
        Result result = runWithRepo("idna", PyPiTest.NO_DEPS_JSON, "nothing");
        TestRunner.assertEquals(Main.EXIT_ERROR, result.code);
        TestRunner.assertContains("код ответа 404", result.err);
    }

    /**
     * Режим test пока не поддерживается.
     *
     * @throws IOException если не удалось создать временный файл
     */
    static void testTestMode() throws IOException {
        checkError("пока не поддерживается", "--package", "A",
                "--repo", makeTempFile(), "--mode", "test",
                "--output", "graph.svg");
    }

    /** Имя пакета не подходит для pip. */
    static void testBadPipName() {
        checkError("имя пакета pip", "--package", "-requests",
                "--repo", REPO_URL, "--output", "g.svg");
        checkError("имя пакета pip", "--package", "a/b",
                "--repo", REPO_URL, "--output", "g.svg");
    }

    /** Не задано имя пакета. */
    static void testNoPackage() {
        checkError("не задано имя пакета",
                "--repo", REPO_URL, "--output", "g.svg");
    }

    /** Имя пакета содержит пробел. */
    static void testPackageSpace() {
        checkError("пробелы", "--package", "a b", "--repo", REPO_URL,
                "--output", "g.svg");
    }

    /** Не задан репозиторий. */
    static void testNoRepo() {
        checkError("не задан репозиторий",
                "--package", "requests", "--output", "g.svg");
    }

    /** URL не начинается с http:// или https://. */
    static void testBadUrl() {
        checkError("URL репозитория", "--package", "requests",
                "--repo", "ftp://x.org", "--output", "g.svg");
    }

    /** Файл тестового репозитория не существует. */
    static void testNoTestFile() {
        checkError("не найден", "--package", "A", "--repo", "no_file.txt",
                "--mode", "test", "--output", "g.svg");
    }

    /** Неизвестный режим работы. */
    static void testBadMode() {
        checkError("режим", "--package", "requests", "--repo", REPO_URL,
                "--mode", "abc", "--output", "g.svg");
    }

    /** Не задано имя файла изображения. */
    static void testNoOutput() {
        checkError("не задано имя файла",
                "--package", "requests", "--repo", REPO_URL);
    }

    /** Имя файла изображения без расширения .svg. */
    static void testBadOutput() {
        checkError(".svg", "--package", "requests", "--repo", REPO_URL,
                "--output", "g.png");
    }

    /** Неизвестная опция. */
    static void testUnknownOption() {
        checkError("неизвестная опция", "--package", "requests",
                "--repo", REPO_URL, "--output", "g.svg", "--color", "red");
    }

    /** У последней опции нет значения. */
    static void testNoValue() {
        checkError("не задано значение опции --output",
                "--package", "requests", "--repo", REPO_URL, "--output");
    }

    /** Опция указана дважды. */
    static void testTwice() {
        checkError("опция указана дважды", "--package", "a",
                "--package", "b", "--repo", REPO_URL, "--output", "g.svg");
    }
}
