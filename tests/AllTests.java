/**
 * Запуск всех тестов проекта.
 */
public class AllTests {

    /**
     * Точка входа: запустить все тесты и завершиться с кодом результата.
     *
     * @param args аргументы командной строки (не используются)
     */
    public static void main(String[] args) {
        TestRunner runner = new TestRunner();
        MainTest.addTo(runner);
        PyPiTest.addTo(runner);
        System.exit(runner.finish());
    }
}
