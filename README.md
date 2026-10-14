# Практическая работа №5

Строки, массивы, циклы и методы обработки данных.
Обработка строки табеля одного сотрудника за рабочую неделю (`TimesheetAnalyzer`), вариант 13.

Пакет называется `ru.edu.pr02`.

## Сборка и запуск

Нужна Java SE 21 или выше.

```bash
mkdir -p out
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -cp out ru.edu.pr02.SelfCheck
```

Windows: `run_selfcheck.bat`.

Интерактивный режим (ввод строк табеля, выход по `exit` или пустой строке):

```bash
java -cp out ru.edu.pr02.SelfCheck --interactive
```

Если кириллица в консоли Windows отображается неправильно: `chcp 65001`
и `java -Dstdout.encoding=UTF-8 ...`.

## Состав

- `src/ru/edu/pr02` исходный код
- `data/sample.csv` контрольные значения (data-driven режим), формат `вход|ожидание|комментарий`
- `diagrams/` Activity-диаграмма и диаграмма классов (.dot и .png)
- `DECISIONS.md` решения и ответы на вопросы
