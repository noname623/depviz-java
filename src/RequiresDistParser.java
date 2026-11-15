import java.util.ArrayList;
import java.util.List;

/**
 * Чтение поля requires_dist из JSON-описания пакета PyPI.
 *
 * <p>Поле requires_dist содержит список прямых зависимостей пакета,
 * например {@code ["idna<4,>=2.5", "certifi>=2017.4.17"]},
 * или значение null, если зависимостей нет. Класс читает только
 * это поле и не разбирает остальной JSON.</p>
 */
public class RequiresDistParser {

    private static final String KEY = "\"requires_dist\"";
    private static final String NULL_VALUE = "null";
    private static final int HEX_RADIX = 16;
    private static final int UNICODE_DIGITS = 4;

    private final String text;
    private int pos;

    /**
     * Создать разборщик для текста JSON.
     *
     * @param text содержимое JSON-ответа репозитория
     */
    public RequiresDistParser(String text) {
        this.text = text;
        this.pos = 0;
    }

    /**
     * Прочитать список прямых зависимостей.
     *
     * @return строки зависимостей из поля requires_dist
     * @throws IllegalArgumentException если поля нет или JSON испорчен
     */
    public List<String> parse() {
        pos = findKey();
        skipSpaces();
        expect(':');
        skipSpaces();
        if (text.startsWith(NULL_VALUE, pos)) {
            return new ArrayList<>();
        }
        return readArray();
    }

    /**
     * Найти ключ requires_dist, который не находится внутри строки.
     *
     * @return позиция сразу после ключа
     * @throws IllegalArgumentException если ключ не найден
     */
    private int findKey() {
        int index = text.indexOf(KEY);
        while (index > 0 && text.charAt(index - 1) == '\\') {
            index = text.indexOf(KEY, index + 1);
        }
        if (index < 0) {
            throw new IllegalArgumentException(
                    "в ответе репозитория нет поля requires_dist");
        }
        return index + KEY.length();
    }

    /**
     * Прочитать массив строк вида ["a", "b"].
     *
     * @return список строк массива
     */
    private List<String> readArray() {
        List<String> result = new ArrayList<>();
        expect('[');
        skipSpaces();
        if (current() == ']') {
            pos++;
            return result;
        }
        while (true) {
            skipSpaces();
            result.add(readString());
            skipSpaces();
            if (current() == ']') {
                pos++;
                return result;
            }
            expect(',');
        }
    }

    /**
     * Прочитать строку в двойных кавычках.
     *
     * @return текст строки без кавычек
     */
    private String readString() {
        expect('"');
        StringBuilder result = new StringBuilder();
        while (current() != '"') {
            char symbol = current();
            pos++;
            if (symbol == '\\') {
                result.append(readEscape());
            } else {
                result.append(symbol);
            }
        }
        pos++;
        return result.toString();
    }

    /**
     * Прочитать символ после обратной косой черты, например \" или \n.
     *
     * @return символ, который обозначает эта запись
     */
    private char readEscape() {
        char symbol = current();
        pos++;
        switch (symbol) {
            case 'n':
                return '\n';
            case 't':
                return '\t';
            case 'r':
                return '\r';
            case 'b':
                return '\b';
            case 'f':
                return '\f';
            case 'u':
                return readUnicode();
            default:
                return symbol;
        }
    }

    /**
     * Прочитать символ, записанный как \\uXXXX.
     *
     * @return символ с этим кодом
     */
    private char readUnicode() {
        if (pos + UNICODE_DIGITS > text.length()) {
            throw new IllegalArgumentException(
                    "ответ репозитория содержит ошибки JSON");
        }
        String hex = text.substring(pos, pos + UNICODE_DIGITS);
        pos += UNICODE_DIGITS;
        try {
            return (char) Integer.parseInt(hex, HEX_RADIX);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException(
                    "ответ репозитория содержит ошибки JSON");
        }
    }

    /**
     * Пропустить пробелы, табуляции и переводы строк.
     */
    private void skipSpaces() {
        while (pos < text.length()
                && Character.isWhitespace(text.charAt(pos))) {
            pos++;
        }
    }

    /**
     * Получить текущий символ.
     *
     * @return символ в текущей позиции
     * @throws IllegalArgumentException если текст закончился
     */
    private char current() {
        if (pos >= text.length()) {
            throw new IllegalArgumentException(
                    "ответ репозитория содержит ошибки JSON");
        }
        return text.charAt(pos);
    }

    /**
     * Проверить, что в текущей позиции стоит нужный символ, и пропустить его.
     *
     * @param symbol ожидаемый символ
     * @throws IllegalArgumentException если символ другой
     */
    private void expect(char symbol) {
        if (current() != symbol) {
            throw new IllegalArgumentException(
                    "ответ репозитория содержит ошибки JSON");
        }
        pos++;
    }
}
