import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Чтение тестового репозитория из текстового файла.
 *
 * <p>Каждая строка файла описывает один пакет и его прямые
 * зависимости. Имя пакета и зависимости разделяются двоеточием,
 * зависимости - пробелами. Пустые строки пропускаются. Пример:</p>
 *
 * <pre>
 * A: B C
 * B: D
 * C: D
 * D:
 * </pre>
 *
 * <p>Имена пакетов состоят из больших латинских букв.</p>
 */
public class RepoFile {

    private static final String SEPARATOR = ":";
    private static final String SPACES = "\\s+";
    private static final String TEST_NAME_PATTERN = "[A-Z]+";

    /**
     * Закрытый конструктор: класс содержит только статические методы.
     */
    private RepoFile() {
    }

    /**
     * Проверить, что имя состоит только из больших латинских букв.
     *
     * @param name имя пакета
     * @return true, если имя подходит для тестового репозитория
     */
    public static boolean isTestName(String name) {
        return name.matches(TEST_NAME_PATTERN);
    }

    /**
     * Проверить имя пакета из строки файла.
     *
     * @param name имя пакета
     * @param lineNumber номер строки в файле
     * @throws IllegalArgumentException если имя не из больших
     *         латинских букв
     */
    private static void checkTestName(String name, int lineNumber) {
        if (!isTestName(name)) {
            throw new IllegalArgumentException("строка " + lineNumber
                    + ": имя пакета '" + name + "' должно состоять "
                    + "из больших латинских букв");
        }
    }

    /**
     * Разобрать одну строку вида "A: B C" и добавить её в репозиторий.
     *
     * @param line строка файла
     * @param lineNumber номер строки в файле
     * @param repo репозиторий, в который добавляется пакет
     * @throws IllegalArgumentException если в строке нет двоеточия
     *         или неверные имена
     */
    private static void parseLine(String line, int lineNumber,
                                  Map<String, List<String>> repo) {
        if (!line.contains(SEPARATOR)) {
            throw new IllegalArgumentException("строка " + lineNumber
                    + ": нет двоеточия после имени пакета");
        }
        int index = line.indexOf(SEPARATOR);
        String name = line.substring(0, index).trim();
        String rest = line.substring(index + SEPARATOR.length()).trim();
        List<String> dependencies = new ArrayList<>();
        if (!rest.isEmpty()) {
            dependencies.addAll(Arrays.asList(rest.split(SPACES)));
        }
        checkTestName(name, lineNumber);
        for (String dependency : dependencies) {
            checkTestName(dependency, lineNumber);
        }
        repo.put(name, dependencies);
    }

    /**
     * Разобрать строки файла тестового репозитория.
     *
     * @param lines строки файла
     * @return словарь: имя пакета -> список его прямых зависимостей
     * @throws IllegalArgumentException если в файле есть ошибки
     */
    public static Map<String, List<String>> parse(List<String> lines) {
        Map<String, List<String>> repo = new LinkedHashMap<>();
        int lineNumber = 0;
        for (String line : lines) {
            lineNumber++;
            if (!line.trim().isEmpty()) {
                parseLine(line, lineNumber, repo);
            }
        }
        return repo;
    }

    /**
     * Прочитать файл тестового репозитория.
     *
     * @param path путь к файлу
     * @return словарь: имя пакета -> список его прямых зависимостей
     * @throws IllegalArgumentException если файл не удалось прочитать
     *         или в нём ошибки
     */
    public static Map<String, List<String>> load(String path) {
        List<String> lines;
        try {
            lines = Files.readAllLines(Paths.get(path),
                    StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new IllegalArgumentException(
                    "не удалось прочитать файл " + path);
        }
        return parse(lines);
    }

    /**
     * Получить прямые зависимости пакета из тестового репозитория.
     *
     * <p>Если пакет не описан в файле, у него нет зависимостей.</p>
     *
     * @param repo тестовый репозиторий
     * @param name имя пакета
     * @return список прямых зависимостей
     */
    public static List<String> getDependencies(
            Map<String, List<String>> repo, String name) {
        return repo.getOrDefault(name, new ArrayList<>());
    }

    /**
     * Проверить, упоминается ли пакет в тестовом репозитории.
     *
     * @param repo тестовый репозиторий
     * @param name имя пакета
     * @return true, если пакет описан своей строкой или указан
     *         как зависимость другого пакета
     */
    public static boolean contains(Map<String, List<String>> repo,
                                   String name) {
        if (repo.containsKey(name)) {
            return true;
        }
        for (List<String> dependencies : repo.values()) {
            if (dependencies.contains(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Построить обратный репозиторий: все стрелки направлены наоборот.
     *
     * <p>Если в файле A зависит от B, то в обратном репозитории
     * у B будет запись A: от B зависит A.</p>
     *
     * @param repo тестовый репозиторий
     * @return словарь: пакет -> пакеты, которые напрямую от него зависят
     */
    public static Map<String, List<String>> reverse(
            Map<String, List<String>> repo) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : repo.entrySet()) {
            for (String dependency : entry.getValue()) {
                result.putIfAbsent(dependency, new ArrayList<>());
                result.get(dependency).add(entry.getKey());
            }
        }
        return result;
    }
}
