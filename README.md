# depviz — визуализация графа зависимостей

## Общее описание

Учебный инструмент визуализации графа зависимостей для менеджера пакетов
(вариант №10). Программа написана на Java и не использует сторонних
библиотек.

Сейчас выполнен **этап 1** — минимальный прототип с конфигурацией.
Программа принимает параметры из опций командной строки, проверяет их и
выводит на экран в формате ключ-значение. При ошибке в параметре
программа выводит сообщение `Ошибка: ...` и завершается с кодом 1.

## Структура проекта

```
depviz/
├── src/Config.java         разбор и проверка параметров
├── src/Main.java           точка входа программы
├── tests/AllTests.java     запуск всех тестов
├── tests/MainTest.java     тесты параметров
├── tests/TestRunner.java   простой запуск тестов и проверки
├── run.sh                  сборка и запуск в Linux / macOS
├── run.bat                 сборка и запуск в Windows
├── Makefile                команды сборки, запуска и тестов
├── README.md
└── .gitignore
```

## Функции и настройки

### Параметры командной строки

| Опция       | Описание                                         | Обязательна |
|-------------|--------------------------------------------------|-------------|
| `--package` | Имя анализируемого пакета                        | да          |
| `--repo`    | URL репозитория или путь к файлу тестового репозитория | да    |
| `--mode`    | Режим работы с репозиторием: `real` или `test`   | нет (`real`) |
| `--output`  | Имя файла с изображением графа (`.svg`)          | да          |

Каждая опция пишется вместе со значением: `--package requests`.

### Проверка параметров (обработка ошибок)

| Что проверяется | Когда выводится ошибка |
|-----------------|------------------------|
| опции           | опция неизвестна, указана дважды или у неё нет значения |
| `--package`     | имя не задано, пустое или содержит пробелы |
| `--mode`        | значение не `real` и не `test` |
| `--repo`        | не задан; в режиме `real` не начинается с `http://` или `https://`; в режиме `test` файл не существует |
| `--output`      | имя не задано, пустое или не оканчивается на `.svg` |

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

`src/Main.java` — точка входа:

| Метод | Описание |
|-------|----------|
| `main(args)` | запускает программу и передаёт код выхода системе |
| `run(args, out, err)` | разбирает параметры и выводит их или сообщение об ошибке |
| `printConfig(config, out)` | выводит параметры в формате ключ-значение |

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

Запуск тестов:

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

Обычный режим:

```
$ ./run.sh --package requests --repo https://pypi.org/pypi --output graph.svg
package = requests
repo = https://pypi.org/pypi
mode = real
output = graph.svg
```

Тестовый режим (файл должен существовать):

```
$ ./run.sh --package A --repo README.md --mode test --output graph.svg
package = A
repo = README.md
mode = test
output = graph.svg
```

Примеры ошибок:

```
$ ./run.sh --repo https://pypi.org/pypi --output graph.svg
Ошибка: не задано имя пакета (--package)

$ ./run.sh --package "a b" --repo https://pypi.org/pypi --output graph.svg
Ошибка: имя пакета не должно содержать пробелы

$ ./run.sh --package requests --output graph.svg
Ошибка: не задан репозиторий (--repo)

$ ./run.sh --package requests --repo ftp://example.org --output graph.svg
Ошибка: URL репозитория должен начинаться с http:// или https://

$ ./run.sh --package A --repo nofile.txt --mode test --output graph.svg
Ошибка: файл тестового репозитория не найден: nofile.txt

$ ./run.sh --package requests --repo https://pypi.org/pypi --mode abc --output graph.svg
Ошибка: режим (--mode) должен быть real или test

$ ./run.sh --package requests --repo https://pypi.org/pypi
Ошибка: не задано имя файла изображения (--output)

$ ./run.sh --package requests --repo https://pypi.org/pypi --output graph.png
Ошибка: файл изображения должен иметь расширение .svg

$ ./run.sh --package requests --repo https://pypi.org/pypi --output graph.svg --color red
Ошибка: неизвестная опция: --color

$ ./run.sh --package requests --repo https://pypi.org/pypi --output
Ошибка: не задано значение опции --output

$ ./run.sh --package a --package b --repo https://pypi.org/pypi --output graph.svg
Ошибка: опция указана дважды: --package
```

## Тестовые команды этапа 1

Юнит-тесты (интернет не нужен):

```
make test
```

Параметры выводятся в формате ключ-значение:

```
./run.sh --package requests --repo https://pypi.org/pypi --output graph.svg
# package = requests
# repo = https://pypi.org/pypi
# mode = real
# output = graph.svg
```

Проверка обработки ошибок (каждая команда завершается сообщением
`Ошибка: ...` и кодом 1):

```
./run.sh --package requests --repo https://pypi.org/pypi --output graph.png
./run.sh --package requests --repo https://pypi.org/pypi --mode foo --output graph.svg
./run.sh --package requests --repo ftp://example.org --output graph.svg
./run.sh --package requests --output graph.svg
./run.sh --package --repo https://pypi.org/pypi --output graph.svg
./run.sh --package requests --package pip --repo https://pypi.org/pypi --output graph.svg
./run.sh --unknown x --package requests --repo https://pypi.org/pypi --output graph.svg
./run.sh --package requests --repo no-such-file.txt --mode test --output graph.svg
```
