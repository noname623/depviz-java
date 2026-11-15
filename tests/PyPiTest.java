import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Тесты получения прямых зависимостей из репозитория PyPI.
 */
public class PyPiTest {

    /** Пример ответа PyPI для пакета с зависимостями. */
    static final String REQUESTS_JSON = "{\"info\": {\"name\": \"requests\","
            + " \"requires_dist\": [\"charset_normalizer<4,>=2\","
            + " \"idna<4,>=2.5\", \"urllib3<3,>=1.21.1\","
            + " \"certifi>=2017.4.17\","
            + " \"PySocks!=1.5.7,>=1.5.6; extra == \\\"socks\\\"\"],"
            + " \"version\": \"2.32.3\"}}";

    /** Пример ответа PyPI для пакета без зависимостей. */
    static final String NO_DEPS_JSON =
            "{\"info\": {\"name\": \"idna\", \"requires_dist\": null}}";

    /**
     * Добавить тесты этого класса в общий запуск.
     *
     * @param runner объект для запуска тестов
     */
    public static void addTo(TestRunner runner) {
        runner.run("адрес JSON-описания", PyPiTest::testJsonUrl);
        runner.run("адрес со слэшем в конце", PyPiTest::testJsonUrlSlash);
        runner.run("чтение requires_dist", PyPiTest::testParseList);
        runner.run("requires_dist = null", PyPiTest::testParseNull);
        runner.run("пустой список", PyPiTest::testParseEmpty);
        runner.run("экранированные символы", PyPiTest::testParseEscapes);
        runner.run("ключ внутри строки", PyPiTest::testKeyInsideString);
        runner.run("нет поля requires_dist", PyPiTest::testNoField);
        runner.run("испорченный JSON", PyPiTest::testBrokenJson);
        runner.run("загрузка с сервера", PyPiTest::testDownload);
        runner.run("ошибка 404", PyPiTest::testNotFound);
        runner.run("имя из требования", PyPiTest::testReadName);
        runner.run("зависимость extra", PyPiTest::testIsExtra);
        runner.run("единый вид имени", PyPiTest::testNormalizeName);
        runner.run("имена для графа", PyPiTest::testDependencyNames);
    }

    /** Адрес составляется из репозитория и имени пакета. */
    static void testJsonUrl() {
        TestRunner.assertEquals("https://pypi.org/pypi/requests/json",
                PyPi.makeJsonUrl("https://pypi.org/pypi", "requests"));
    }

    /** Слэш в конце адреса репозитория убирается. */
    static void testJsonUrlSlash() {
        TestRunner.assertEquals("https://pypi.org/pypi/requests/json",
                PyPi.makeJsonUrl("https://pypi.org/pypi/", "requests"));
    }

    /** Из ответа берутся все строки requires_dist. */
    static void testParseList() {
        List<String> result = new RequiresDistParser(REQUESTS_JSON).parse();
        TestRunner.assertEquals(Arrays.asList("charset_normalizer<4,>=2",
                "idna<4,>=2.5", "urllib3<3,>=1.21.1", "certifi>=2017.4.17",
                "PySocks!=1.5.7,>=1.5.6; extra == \"socks\""), result);
    }

    /** Значение null означает, что зависимостей нет. */
    static void testParseNull() {
        TestRunner.assertEquals(Collections.emptyList(),
                new RequiresDistParser(NO_DEPS_JSON).parse());
    }

    /** Пустой массив означает, что зависимостей нет. */
    static void testParseEmpty() {
        TestRunner.assertEquals(Collections.emptyList(),
                new RequiresDistParser("{\"requires_dist\": []}").parse());
    }

    /** Записи \\" и \\u0041 превращаются в символы. */
    static void testParseEscapes() {
        String json = "{\"requires_dist\": [\"a\\\"b\", \"\\u0041\"]}";
        TestRunner.assertEquals(Arrays.asList("a\"b", "A"),
                new RequiresDistParser(json).parse());
    }

    /** Текст "requires_dist" внутри другой строки не считается ключом. */
    static void testKeyInsideString() {
        String json = "{\"description\": \"see \\\"requires_dist\\\"\","
                + " \"requires_dist\": [\"idna\"]}";
        TestRunner.assertEquals(Arrays.asList("idna"),
                new RequiresDistParser(json).parse());
    }

    /** Если поля нет, выводится ошибка. */
    static void testNoField() {
        checkParseError("{\"info\": {}}", "нет поля requires_dist");
    }

    /** Незакрытый массив вызывает ошибку. */
    static void testBrokenJson() {
        checkParseError("{\"requires_dist\": [\"idna\"", "ошибки JSON");
    }

    /**
     * Проверить, что разбор JSON заканчивается ошибкой.
     *
     * @param json текст JSON
     * @param text ожидаемая часть сообщения
     */
    static void checkParseError(String json, String text) {
        try {
            new RequiresDistParser(json).parse();
        } catch (IllegalArgumentException error) {
            TestRunner.assertContains(text, error.getMessage());
            return;
        }
        throw new AssertionError("ошибка не возникла");
    }

    /**
     * Зависимости скачиваются с сервера по адресу пакета.
     *
     * @throws IOException если сервер не запустился
     */
    static void testDownload() throws IOException {
        FakeRepo repo = new FakeRepo(
                Map.of("/pypi/requests/json", REQUESTS_JSON));
        try {
            List<String> result =
                    PyPi.getDirectDependencies(repo.url(), "requests");
            TestRunner.assertEquals("idna<4,>=2.5", result.get(1));
        } finally {
            repo.stop();
        }
    }

    /**
     * Ответ 404 превращается в понятную ошибку.
     *
     * @throws IOException если сервер не запустился
     */
    static void testNotFound() throws IOException {
        FakeRepo repo = new FakeRepo(Map.of());
        try {
            PyPi.downloadText(repo.url() + "/nothing/json");
        } catch (IllegalArgumentException error) {
            TestRunner.assertContains("код ответа 404", error.getMessage());
            return;
        } finally {
            repo.stop();
        }
        throw new AssertionError("ошибка не возникла");
    }

    /** Имя пакета берётся из начала строки требования. */
    static void testReadName() {
        TestRunner.assertEquals("idna", PyPi.readName("idna<4,>=2.5"));
        TestRunner.assertEquals("PySocks",
                PyPi.readName("PySocks!=1.5.7; extra == \"socks\""));
        TestRunner.assertEquals("zope.interface",
                PyPi.readName("zope.interface (>=5)"));
        TestRunner.assertEquals("urllib3", PyPi.readName("urllib3[socks]"));
    }

    /** Зависимости с условием extra распознаются. */
    static void testIsExtra() {
        TestRunner.assertEquals(true,
                PyPi.isExtra("PySocks>=1.5.6; extra == \"socks\""));
        TestRunner.assertEquals(false, PyPi.isExtra("idna<4,>=2.5"));
        TestRunner.assertEquals(false,
                PyPi.isExtra("tomli; python_version < \"3.11\""));
    }

    /** Имя приводится к маленьким буквам и дефисам. */
    static void testNormalizeName() {
        TestRunner.assertEquals("charset-normalizer",
                PyPi.normalizeName("Charset_Normalizer"));
        TestRunner.assertEquals("zope-interface",
                PyPi.normalizeName("zope.interface"));
    }

    /**
     * Для графа берутся имена без extra, без повторов.
     *
     * @throws IOException если сервер не запустился
     */
    static void testDependencyNames() throws IOException {
        FakeRepo repo = new FakeRepo(
                Map.of("/pypi/requests/json", REQUESTS_JSON));
        try {
            TestRunner.assertEquals(Arrays.asList("charset-normalizer",
                    "idna", "urllib3", "certifi"),
                    PyPi.getDependencyNames(repo.url(), "requests"));
        } finally {
            repo.stop();
        }
    }
}
