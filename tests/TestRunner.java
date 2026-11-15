/**
 * Простой запуск тестов без сторонних библиотек.
 *
 * <p>Каждый тест - это метод без аргументов. Если проверка внутри
 * теста не прошла, тест выбрасывает {@link AssertionError}.</p>
 */
public class TestRunner {

    private int passed;
    private int failed;

    /**
     * Описание одного теста.
     */
    public interface Test {

        /**
         * Выполнить тест.
         *
         * @throws Exception если тест завершился с ошибкой
         */
        void run() throws Exception;
    }

    /**
     * Запустить один тест и запомнить результат.
     *
     * @param name название теста
     * @param test код теста
     */
    public void run(String name, Test test) {
        try {
            test.run();
            passed++;
            System.out.println("OK    " + name);
        } catch (Exception | AssertionError error) {
            failed++;
            System.out.println("FAIL  " + name + ": " + error.getMessage());
        }
    }

    /**
     * Вывести итог и вернуть код выхода.
     *
     * @return 0, если все тесты прошли, иначе 1
     */
    public int finish() {
        System.out.println();
        System.out.println("Пройдено: " + passed + ", ошибок: " + failed);
        if (failed > 0) {
            return 1;
        }
        return 0;
    }

    /**
     * Проверить, что два значения равны.
     *
     * @param expected ожидаемое значение
     * @param actual полученное значение
     */
    public static void assertEquals(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError(
                    "ожидалось <" + expected + ">, получено <" + actual + ">");
        }
    }

    /**
     * Проверить, что текст содержит подстроку.
     *
     * @param part ожидаемая подстрока
     * @param text проверяемый текст
     */
    public static void assertContains(String part, String text) {
        if (!text.contains(part)) {
            throw new AssertionError(
                    "в тексте <" + text + "> нет <" + part + ">");
        }
    }
}
