import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Тесты главного класса: параметры и вывод графа.
 */
public class MainTest {

    private static final String REPO_URL = "https://pypi.org/pypi";
    private static final String SIMPLE = RepoFileTest.DATA + "simple.txt";
    private static final String CYCLE = RepoFileTest.DATA + "cycle.txt";
    private static final String BAD = RepoFileTest.DATA + "bad.txt";

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
     * Запустить программу в режиме test.
     *
     * @param path путь к файлу тестового репозитория
     * @param packageName имя пакета
     * @return результат запуска
     */
    static Result runTest(String path, String packageName) {
        return runMain("--package", packageName, "--repo", path,
                "--mode", "test", "--output", "graph.svg");
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
     * Добавить тесты этого класса в общий запуск.
     *
     * @param runner объект для запуска тестов
     */
    public static void addTo(TestRunner runner) {
        runner.run("граф из тестового файла", MainTest::testSimpleGraph);
        runner.run("граф с циклами", MainTest::testCycleGraph);
        runner.run("граф от середины", MainTest::testMiddle);
        runner.run("пакета нет в файле", MainTest::testNotInFile);
        runner.run("ошибка в тестовом файле", MainTest::testBadFile);
        runner.run("имя в режиме test", MainTest::testBadTestName);
        runner.run("граф из PyPI", MainTest::testRealGraph);
        runner.run("обратные зависимости D", MainTest::testReverseShared);
        runner.run("обратные зависимости F", MainTest::testReverseDeep);
        runner.run("от пакета ничего не зависит", MainTest::testReverseTop);
        runner.run("обратные зависимости в цикле", MainTest::testReverseCycle);
        runner.run("--reverse в режиме real", MainTest::testReverseReal);
        runner.run("--reverse дважды", MainTest::testReverseTwice);
        runner.run("пакета нет в PyPI", MainTest::testNotFound);
        addErrorTests(runner);
    }

    /**
     * Добавить тесты ошибок в параметрах.
     *
     * @param runner объект для запуска тестов
     */
    static void addErrorTests(TestRunner runner) {
        runner.run("нет имени пакета", MainTest::testNoPackage);
        runner.run("пробел в имени пакета", MainTest::testPackageSpace);
        runner.run("неверное имя пакета pip", MainTest::testBadPipName);
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

    /** Выводится весь граф с транзитивными зависимостями. */
    static void testSimpleGraph() {
        Result result = runTest(SIMPLE, "A");
        TestRunner.assertEquals(Main.EXIT_OK, result.code);
        String expected = String.join(System.lineSeparator(),
                "Граф зависимостей пакета A:", "A -> B, C", "B -> D",
                "C -> D, E", "D -> нет зависимостей", "E -> F",
                "F -> нет зависимостей", "");
        TestRunner.assertEquals(expected, result.out);
    }

    /** Граф с циклами выводится без зацикливания. */
    static void testCycleGraph() {
        Result result = runTest(CYCLE, "A");
        TestRunner.assertEquals(Main.EXIT_OK, result.code);
        TestRunner.assertContains("C -> A, D", result.out);
        TestRunner.assertContains("D -> D", result.out);
    }

    /** Граф строится от заданного пакета. */
    static void testMiddle() {
        Result result = runTest(SIMPLE, "E");
        TestRunner.assertContains("E -> F", result.out);
        TestRunner.assertEquals(false, result.out.contains("A ->"));
    }

    /** Пакета нет в файле тестового репозитория. */
    static void testNotInFile() {
        checkError("пакет Z не найден", "--package", "Z", "--repo", SIMPLE,
                "--mode", "test", "--output", "g.svg");
    }

    /** Ошибка в файле тестового репозитория. */
    static void testBadFile() {
        checkError("строка 2", "--package", "A", "--repo", BAD,
                "--mode", "test", "--output", "g.svg");
    }

    /** В режиме test имя пакета - большие латинские буквы. */
    static void testBadTestName() {
        checkError("больших латинских букв", "--package", "a",
                "--repo", SIMPLE, "--mode", "test", "--output", "g.svg");
    }

    /**
     * Запустить программу в режиме test с опцией --reverse.
     *
     * @param path путь к файлу тестового репозитория
     * @param packageName имя пакета
     * @return результат запуска
     */
    static Result runReverse(String path, String packageName) {
        return runMain("--package", packageName, "--repo", path,
                "--mode", "test", "--output", "graph.svg", "--reverse");
    }

    /** От D зависят B и C, а от них - A. */
    static void testReverseShared() {
        Result result = runReverse(SIMPLE, "D");
        TestRunner.assertEquals(Main.EXIT_OK, result.code);
        String expected = String.join(System.lineSeparator(),
                "Обратные зависимости пакета D "
                + "(пакеты, которые от него зависят):", "D <- B, C",
                "B <- A", "C <- A", "A <- от него ничего не зависит", "");
        TestRunner.assertEquals(expected, result.out);
    }

    /** Обратные зависимости F идут по цепочке E, C, A. */
    static void testReverseDeep() {
        Result result = runReverse(SIMPLE, "F");
        TestRunner.assertContains("F <- E", result.out);
        TestRunner.assertContains("E <- C", result.out);
        TestRunner.assertContains("C <- A", result.out);
        TestRunner.assertEquals(false, result.out.contains("B <-"));
    }

    /** От верхнего пакета ничего не зависит. */
    static void testReverseTop() {
        Result result = runReverse(SIMPLE, "A");
        TestRunner.assertContains("A <- от него ничего не зависит",
                result.out);
    }

    /** Обратные зависимости в графе с циклами. */
    static void testReverseCycle() {
        Result result = runReverse(CYCLE, "D");
        TestRunner.assertEquals(Main.EXIT_OK, result.code);
        TestRunner.assertContains("D <- C, D", result.out);
        TestRunner.assertContains("C <- B", result.out);
        TestRunner.assertContains("B <- A", result.out);
        TestRunner.assertContains("A <- C", result.out);
    }

    /** Обратные зависимости выводятся только в режиме test. */
    static void testReverseReal() {
        checkError("только в режиме test", "--package", "requests",
                "--repo", REPO_URL, "--output", "g.svg", "--reverse");
    }

    /** Опция --reverse указана дважды. */
    static void testReverseTwice() {
        checkError("опция указана дважды: --reverse", "--package", "A",
                "--repo", SIMPLE, "--mode", "test", "--output", "g.svg",
                "--reverse", "--reverse");
    }

    /**
     * Граф строится по описаниям пакетов из репозитория.
     *
     * @throws IOException если сервер не запустился
     */
    static void testRealGraph() throws IOException {
        Map<String, String> pages = new HashMap<>();
        pages.put("/pypi/requests/json", PyPiTest.REQUESTS_JSON);
        pages.put("/pypi/idna/json", PyPiTest.NO_DEPS_JSON);
        pages.put("/pypi/certifi/json", PyPiTest.NO_DEPS_JSON);
        pages.put("/pypi/urllib3/json", PyPiTest.NO_DEPS_JSON);
        pages.put("/pypi/charset-normalizer/json", PyPiTest.NO_DEPS_JSON);
        FakeRepo repo = new FakeRepo(pages);
        try {
            Result result = runMain("--package", "Requests", "--repo",
                    repo.url(), "--output", "graph.svg");
            TestRunner.assertEquals(Main.EXIT_OK, result.code);
            TestRunner.assertContains("requests -> charset-normalizer, "
                    + "idna, urllib3, certifi", result.out);
            TestRunner.assertContains("idna -> нет зависимостей",
                    result.out);
        } finally {
            repo.stop();
        }
    }

    /**
     * Пакета нет в репозитории: ошибка 404.
     *
     * @throws IOException если сервер не запустился
     */
    static void testNotFound() throws IOException {
        FakeRepo repo = new FakeRepo(new HashMap<>());
        try {
            checkError("код ответа 404", "--package", "nothing",
                    "--repo", repo.url(), "--output", "graph.svg");
        } finally {
            repo.stop();
        }
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

    /** Имя пакета не подходит для pip. */
    static void testBadPipName() {
        checkError("имя пакета pip", "--package", "-requests",
                "--repo", REPO_URL, "--output", "g.svg");
        checkError("имя пакета pip", "--package", "a/b",
                "--repo", REPO_URL, "--output", "g.svg");
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
