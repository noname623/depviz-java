# depviz — визуализация графа зависимостей

## Общее описание

Учебный инструмент визуализации графа зависимостей для менеджера пакетов
pip (вариант №10). Программа написана на Java и не использует сторонних
библиотек и менеджеров пакетов.

Выполненные этапы:

- **Этап 1.** Параметры задаются опциями командной строки и
  проверяются на ошибки.
- **Этап 2.** Сбор данных: программа получает из репозитория PyPI
  описание пакета pip и выводит на экран все его прямые зависимости.

## Структура проекта

```
depviz/
├── src/Config.java              разбор и проверка параметров
├── src/Main.java                точка входа программы
├── src/PyPi.java                загрузка описания пакета из PyPI
├── src/RequiresDistParser.java  чтение поля requires_dist из JSON
├── tests/AllTests.java          запуск всех тестов
├── tests/MainTest.java          тесты параметров и вывода
├── tests/PyPiTest.java          тесты загрузки и чтения JSON
├── tests/FakeRepo.java          поддельный репозиторий для тестов
├── tests/TestRunner.java        простой запуск тестов и проверки
├── run.sh                       сборка и запуск в Linux / macOS
├── run.bat                      сборка и запуск в Windows
├── Makefile                     команды сборки, запуска и тестов
├── README.md
└── .gitignore
```

## Функции и настройки

### Параметры командной строки

| Опция       | Описание                                         | Обязательна |
|-------------|--------------------------------------------------|-------------|
| `--package` | Имя пакета pip, например `requests`              | да          |
| `--repo`    | URL репозитория или путь к файлу тестового репозитория | да    |
| `--mode`    | Режим работы с репозиторием: `real` или `test`   | нет (`real`) |
| `--output`  | Имя файла с изображением графа (`.svg`)          | да          |

Каждая опция пишется вместе со значением: `--package requests`.
Параметр `--output` пока только проверяется, файл изображения будет
создаваться на этапе 5. Режим `test` будет реализован на этапе 3.

### Как программа получает зависимости

1. Из URL репозитория и имени пакета составляется адрес JSON-описания:
   `<репозиторий>/<пакет>/json`. Например, для `requests` и
   `https://pypi.org/pypi` это `https://pypi.org/pypi/requests/json`.
2. Описание скачивается классом `HttpURLConnection` из стандартной
   библиотеки Java.
3. Прямые зависимости пакета записаны в поле `info.requires_dist` —
   это список строк, например `"idna<4,>=2.5"`. Поле читается своим
   маленьким разборщиком JSON (`RequiresDistParser`), потому что
   готовые JSON-библиотеки сторонние.
4. Если поле равно `null` или список пустой, у пакета нет зависимостей.
5. Каждая зависимость выводится так, как записана в описании пакета:
   с ограничениями версии и условиями (например, `; extra == "socks"`).

### Обработка ошибок

При ошибке программа выводит сообщение `Ошибка: ...` и завершается
с кодом 1.

| Что проверяется | Когда выводится ошибка |
|-----------------|------------------------|
| опции           | опция неизвестна, указана дважды или у неё нет значения |
| `--package`     | имя не задано, пустое или содержит пробелы; в режиме `real` содержит символы, кроме латинских букв, цифр и `. - _`, или начинается/заканчивается не буквой и не цифрой |
| `--mode`        | значение не `real` и не `test`; режим `test` пока не поддерживается |
| `--repo`        | не задан; в режиме `real` не начинается с `http://` или `https://`; в режиме `test` файл не существует |
| `--output`      | имя не задано, пустое или не оканчивается на `.svg` |
| загрузка        | нет сети, пакет не найден (код ответа 404), неверный адрес |
| ответ           | в JSON нет поля `requires_dist` или JSON испорчен |

### Классы и методы

`src/Config.java` — параметры программы:

| Метод | Описание |
|-------|----------|
| `parse(args)` | разбирает аргументы и проверяет все параметры |
| `readOptions(args)` | читает пары «опция значение» в словарь |
| `isKnownOption(option)` | проверяет, что опция известна |
| `isBlank(text)` | проверяет строку на пустоту |
| `check()` | вызывает все проверки по очереди |
| `checkMode()` | проверяет режим работы |
| `checkPackage()` | проверяет имя пакета |
| `checkRepo()` | проверяет URL или путь к тестовому файлу |
| `checkOutput()` | проверяет имя файла изображения |
| `getPackageName()`, `getRepo()`, `getMode()`, `getOutput()` | возвращают значения параметров |

`src/PyPi.java` — работа с репозиторием PyPI:

| Метод | Описание |
|-------|----------|
| `makeJsonUrl(repo, packageName)` | составляет адрес JSON-описания пакета |
| `makeUrl(address)` | превращает текст адреса в объект URL |
| `downloadText(address)` | скачивает текст по адресу |
| `getDirectDependencies(repo, packageName)` | скачивает описание и возвращает зависимости |

`src/RequiresDistParser.java` — чтение поля `requires_dist`:

| Метод | Описание |
|-------|----------|
| `parse()` | возвращает список строк из поля `requires_dist` |
| `findKey()` | находит ключ `"requires_dist"` вне других строк |
| `readArray()` | читает массив строк `[...]` |
| `readString()` | читает строку в кавычках |
| `readEscape()`, `readUnicode()` | читают записи вида `\"`, `\n`, `A` |
| `skipSpaces()`, `current()`, `expect(symbol)` | вспомогательные методы чтения |

`src/Main.java` — точка входа:

| Метод | Описание |
|-------|----------|
| `main(args)` | запускает программу и передаёт код выхода системе |
| `run(args, out, err)` | проверяет параметры, получает и выводит зависимости |
| `getDependencies(config)` | получает прямые зависимости пакета |
| `printDependencies(packageName, dependencies, out)` | выводит зависимости |

## Сборка и запуск тестов

Нужен JDK 11 или новее (команды `javac` и `java`). Сторонние библиотеки
не нужны. Скомпилированные классы складываются в папку `build/`.

Сборка:

```
make build
```

Запуск программы (скрипты сами собирают проект):

```
./run.sh --package <имя> --repo <url> --output <файл.svg>
run.bat --package <имя> --repo <url> --output <файл.svg>
make run ARGS="--package <имя> --repo <url> --output <файл.svg>"
```

Запуск тестов (интернет не нужен: тесты запускают на компьютере
поддельный репозиторий):

```
make test
```

или без make:

```
javac -encoding UTF-8 -d build src/*.java tests/*.java
java -cp build AllTests
```

Удаление собранных файлов:

```
make clean
```

## Примеры использования

Прямые зависимости пакета `requests` (вывод зависит от текущей версии
пакета в PyPI):

```
$ ./run.sh --package requests --repo https://pypi.org/pypi --output graph.svg
Прямые зависимости пакета requests:
charset_normalizer<4,>=2
idna<4,>=2.5
urllib3<3,>=1.21.1
certifi>=2017.4.17
PySocks!=1.5.7,>=1.5.6; extra == "socks"
chardet<6,>=3.0.2; extra == "use-chardet-on-py3"
```

Пакет без зависимостей:

```
$ ./run.sh --package idna --repo https://pypi.org/pypi --output graph.svg
Прямые зависимости пакета idna:
нет зависимостей
```

Примеры ошибок:

```
$ ./run.sh --package no-such-package-xyz --repo https://pypi.org/pypi --output graph.svg
Ошибка: не удалось загрузить https://pypi.org/pypi/no-such-package-xyz/json (код ответа 404)

$ ./run.sh --package -requests --repo https://pypi.org/pypi --output graph.svg
Ошибка: имя пакета pip может содержать только латинские буквы, цифры и знаки . - _ и должно начинаться и заканчиваться буквой или цифрой

$ ./run.sh --package A --repo README.md --mode test --output graph.svg
Ошибка: режим test пока не поддерживается

$ ./run.sh --repo https://pypi.org/pypi --output graph.svg
Ошибка: не задано имя пакета (--package)

$ ./run.sh --package requests --repo ftp://example.org --output graph.svg
Ошибка: URL репозитория должен начинаться с http:// или https://

$ ./run.sh --package requests --repo https://pypi.org/pypi --mode abc --output graph.svg
Ошибка: режим (--mode) должен быть real или test

$ ./run.sh --package requests --repo https://pypi.org/pypi --output graph.png
Ошибка: файл изображения должен иметь расширение .svg

$ ./run.sh --package requests --repo https://pypi.org/pypi --output graph.svg --color red
Ошибка: неизвестная опция: --color
```

## Тестовые команды этапа 2

Юнит-тесты (интернет не нужен, тесты запускают поддельный репозиторий):

```
make test
```

Прямые зависимости пакетов из PyPI (нужен интернет):

```
./run.sh --package requests --repo https://pypi.org/pypi --output graph.svg
./run.sh --package flask --repo https://pypi.org/pypi --output graph.svg
```

Проверка обработки ошибок:

```
./run.sh --package no-such-package-xyz --repo https://pypi.org/pypi --output graph.svg
./run.sh --package -requests --repo https://pypi.org/pypi --output graph.svg
./run.sh --package requests --repo ftp://example.org --output graph.svg
./run.sh --package requests --repo https://pypi.org/pypi --output graph.png
```
