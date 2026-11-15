import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Тесты построения графа алгоритмом BFS.
 */
public class GraphTest {

    /**
     * Создать репозиторий из строк вида "A: B C".
     *
     * @param lines строки описания
     * @return словарь: пакет -> зависимости
     */
    static Map<String, List<String>> repo(String... lines) {
        return RepoFile.parse(Arrays.asList(lines));
    }

    /**
     * Построить граф по репозиторию.
     *
     * @param repo репозиторий
     * @param start начальный пакет
     * @return граф зависимостей
     */
    static Map<String, List<String>> build(Map<String, List<String>> repo,
                                           String start) {
        return Graph.build(start, name -> RepoFile.getDependencies(repo,
                name));
    }

    /**
     * Добавить тесты этого класса в общий запуск.
     *
     * @param runner объект для запуска тестов
     */
    public static void addTo(TestRunner runner) {
        runner.run("транзитивные зависимости", GraphTest::testTransitive);
        runner.run("порядок обхода в ширину", GraphTest::testBfsOrder);
        runner.run("граф от середины", GraphTest::testPartOfGraph);
        runner.run("цикл A -> B -> C -> A", GraphTest::testCycle);
        runner.run("пакет зависит сам от себя", GraphTest::testSelfLoop);
        runner.run("общая зависимость один раз", GraphTest::testShared);
        runner.run("BFS по обратным стрелкам", GraphTest::testReverseBfs);
    }

    /** В граф попадают и прямые, и транзитивные зависимости. */
    static void testTransitive() {
        Map<String, List<String>> repo = repo("A: B", "B: C", "C:");
        Map<String, List<String>> expected = new LinkedHashMap<>();
        expected.put("A", Arrays.asList("B"));
        expected.put("B", Arrays.asList("C"));
        expected.put("C", new ArrayList<>());
        TestRunner.assertEquals(expected, build(repo, "A"));
    }

    /** Сначала обходятся ближние пакеты, потом дальние. */
    static void testBfsOrder() {
        Map<String, List<String>> repo = repo("A: B C", "B: D", "C: E",
                "D: F", "E:", "F:");
        TestRunner.assertEquals(Arrays.asList("A", "B", "C", "D", "E", "F"),
                new ArrayList<>(build(repo, "A").keySet()));
    }

    /** Граф строится только от заданного пакета. */
    static void testPartOfGraph() {
        Map<String, List<String>> repo = repo("A: B", "B: C", "C:");
        TestRunner.assertEquals(Arrays.asList("B", "C"),
                new ArrayList<>(build(repo, "B").keySet()));
    }

    /** Цикл не приводит к зацикливанию. */
    static void testCycle() {
        Map<String, List<String>> repo = repo("A: B", "B: C", "C: A");
        Map<String, List<String>> graph = build(repo, "A");
        TestRunner.assertEquals(Arrays.asList("A", "B", "C"),
                new ArrayList<>(graph.keySet()));
        TestRunner.assertEquals(Arrays.asList("A"), graph.get("C"));
    }

    /** Пакет, зависящий сам от себя, обрабатывается один раз. */
    static void testSelfLoop() {
        Map<String, List<String>> graph = build(repo("A: A"), "A");
        TestRunner.assertEquals(Arrays.asList("A"), graph.get("A"));
        TestRunner.assertEquals(1, graph.size());
    }

    /** Общая зависимость двух пакетов попадает в граф один раз. */
    static void testShared() {
        Map<String, List<String>> repo = repo("A: B C", "B: D", "C: D",
                "D:");
        TestRunner.assertEquals(Arrays.asList("A", "B", "C", "D"),
                new ArrayList<>(build(repo, "A").keySet()));
    }

    /** Тот же BFS по обратному репозиторию находит все зависящие пакеты. */
    static void testReverseBfs() {
        Map<String, List<String>> repo = repo("A: B", "B: C", "D: C", "C:");
        Map<String, List<String>> graph = build(RepoFile.reverse(repo), "C");
        TestRunner.assertEquals(Arrays.asList("C", "B", "D", "A"),
                new ArrayList<>(graph.keySet()));
    }
}
