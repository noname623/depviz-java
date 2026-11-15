import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Построение графа зависимостей алгоритмом BFS (обход в ширину).
 *
 * <p>Обход сделан без рекурсии: пакеты, которые ещё нужно обработать,
 * хранятся в очереди. Каждый пакет обрабатывается только один раз,
 * поэтому циклические зависимости не приводят к зацикливанию.</p>
 */
public class Graph {

    /**
     * Источник прямых зависимостей: репозиторий PyPI или тестовый файл.
     */
    public interface DependencySource {

        /**
         * Получить прямые зависимости пакета.
         *
         * @param packageName имя пакета
         * @return список имён прямых зависимостей
         */
        List<String> getDependencies(String packageName);
    }

    /**
     * Закрытый конструктор: класс содержит только статические методы.
     */
    private Graph() {
    }

    /**
     * Построить граф зависимостей пакета с учётом транзитивности.
     *
     * @param start имя анализируемого пакета
     * @param source источник прямых зависимостей
     * @return словарь: пакет -> список его прямых зависимостей;
     *         пакеты идут в том порядке, в котором их обошёл BFS
     */
    public static Map<String, List<String>> build(String start,
                                                  DependencySource source) {
        Map<String, List<String>> graph = new LinkedHashMap<>();
        Queue<String> queue = new ArrayDeque<>();
        queue.add(start);
        while (!queue.isEmpty()) {
            String packageName = queue.poll();
            if (graph.containsKey(packageName)) {
                continue;
            }
            List<String> dependencies =
                    new ArrayList<>(source.getDependencies(packageName));
            graph.put(packageName, dependencies);
            queue.addAll(dependencies);
        }
        return graph;
    }
}
