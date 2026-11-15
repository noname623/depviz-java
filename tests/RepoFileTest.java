import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Тесты чтения файла тестового репозитория.
 */
public class RepoFileTest {

    /** Путь к папке с файлами тестовых репозиториев. */
    static final String DATA = "tests/data/";

    /**
     * Добавить тесты этого класса в общий запуск.
     *
     * @param runner объект для запуска тестов
     */
    public static void addTo(TestRunner runner) {
        runner.run("разбор строк файла", RepoFileTest::testParse);
        runner.run("строка без двоеточия", RepoFileTest::testNoColon);
        runner.run("маленькие буквы", RepoFileTest::testSmallLetters);
        runner.run("цифра в имени", RepoFileTest::testDigit);
        runner.run("проверка имён", RepoFileTest::testIsTestName);
        runner.run("пакет без своей строки", RepoFileTest::testMissing);
        runner.run("чтение файла", RepoFileTest::testLoad);
        runner.run("файла нет", RepoFileTest::testNoFile);
    }

    /** Строки разбираются в словарь, пустые строки пропускаются. */
    static void testParse() {
        Map<String, List<String>> repo = RepoFile.parse(
                Arrays.asList("A: B C", "", "B: C", "C:"));
        TestRunner.assertEquals(Arrays.asList("B", "C"), repo.get("A"));
        TestRunner.assertEquals(new ArrayList<>(), repo.get("C"));
        TestRunner.assertEquals(Arrays.asList("A", "B", "C"),
                new ArrayList<>(repo.keySet()));
    }

    /**
     * Проверить, что разбор строк заканчивается ошибкой.
     *
     * @param text ожидаемая часть сообщения
     * @param lines строки файла
     */
    static void checkError(String text, String... lines) {
        try {
            RepoFile.parse(Arrays.asList(lines));
        } catch (IllegalArgumentException error) {
            TestRunner.assertContains(text, error.getMessage());
            return;
        }
        throw new AssertionError("ошибка не возникла");
    }

    /** Строка без двоеточия вызывает ошибку с номером строки. */
    static void testNoColon() {
        checkError("строка 2: нет двоеточия", "A: B", "B C");
    }

    /** Имя пакета из маленьких букв вызывает ошибку. */
    static void testSmallLetters() {
        checkError("'a'", "a: B");
    }

    /** Цифра в имени зависимости вызывает ошибку. */
    static void testDigit() {
        checkError("'B1'", "A: B1");
    }

    /** Подходят только большие латинские буквы. */
    static void testIsTestName() {
        TestRunner.assertEquals(true, RepoFile.isTestName("A"));
        TestRunner.assertEquals(true, RepoFile.isTestName("AB"));
        TestRunner.assertEquals(false, RepoFile.isTestName("a"));
        TestRunner.assertEquals(false, RepoFile.isTestName("А"));
        TestRunner.assertEquals(false, RepoFile.isTestName("A1"));
    }

    /** У пакета, которого нет в файле, нет зависимостей. */
    static void testMissing() {
        Map<String, List<String>> repo = RepoFile.parse(
                Arrays.asList("A: B"));
        TestRunner.assertEquals(new ArrayList<>(),
                RepoFile.getDependencies(repo, "B"));
    }

    /** Файл из папки data читается правильно. */
    static void testLoad() {
        Map<String, List<String>> repo = RepoFile.load(DATA + "cycle.txt");
        TestRunner.assertEquals(Arrays.asList("A", "D"), repo.get("C"));
    }

    /** Отсутствующий файл вызывает ошибку. */
    static void testNoFile() {
        try {
            RepoFile.load("no_such_file.txt");
        } catch (IllegalArgumentException error) {
            TestRunner.assertContains("не удалось прочитать",
                    error.getMessage());
            return;
        }
        throw new AssertionError("ошибка не возникла");
    }
}
