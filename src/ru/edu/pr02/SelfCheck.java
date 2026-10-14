package ru.edu.pr02;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/**
 * Единственная точка входа и тестовый раннер для TimesheetAnalyzer.
 *
 * Режим 1: unit-проверки в коде. Режим 2: data-driven из data/sample.csv.
 * Режим 3 (ключ --interactive): ввод строк табеля с консоли, как на защите.
 */
public class SelfCheck {
    private static final String SAMPLE_FILE = "data/sample.csv";
    private static final double EPS = 1e-9;

    private static int total = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        runUnitChecks();
        runDataDriven();
        System.out.println("Итого: " + (total - failed) + "/" + total + " проверок пройдено");
        if (args.length > 0 && args[0].equals("--interactive")) {
            runInteractive();
        }
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ---------- режим 1: unit-проверки ----------

    private static void runUnitChecks() {
        checkParseHours();
        checkTotal();
        checkAverage();
        checkOvertimeDays();
        checkWeeklyOvertime();
        checkGuards();
        checkVariantPartA();
        checkVariantPartB();
    }

    // parseHours: структура, формат, диапазон
    private static void checkParseHours() {
        expectArray("unit/1 parseHours корректная строка", "8;9;10;8;8", new int[]{8, 9, 10, 8, 8});
        expectError("unit/2 parseHours пустая строка", "", "Ошибка: структура");
        expectError("unit/3 parseHours 17 вне диапазона", "8;8;17;8;8", "Ошибка: позиция 3 ");
        expectError("unit/4 parseHours не число", "8;abc;8;8;8", "Ошибка: позиция 2 ");
        expectError("unit/5 parseHours 4 значения", "8;8;8;8", "Ошибка: структура");
        expectError("unit/6 parseHours шесть значений", "8;8;8;8;8;8", "Ошибка: структура");
        expectError("unit/7 parseHours ведущий ноль", "08;8;8;8;8", "Ошибка: позиция 1 ");
        expectError("unit/7b parseHours пробел после ;", "8; 9;10;8;8", "Ошибка: позиция 2 ");
        expectError("unit/7c parseHours дробное", "8;8.5;8;8;8", "Ошибка: позиция 2 ");
        expectError("unit/7d parseHours отрицательное", "8;8;-3;8;8", "Ошибка: позиция 3 ");
        expectError("unit/7e parseHours переполнение int", "8;8;99999999999;8;8", "Ошибка: позиция 3 ");
        expectError("unit/7f parseHours null", null, "Ошибка: структура");
    }

    // total: типовое значение и границы диапазона
    private static void checkTotal() {
        expectInt("unit/8 total типовое", 43, TimesheetAnalyzer.total(new int[]{8, 9, 10, 8, 8}));
        expectInt("unit/9 total нижняя граница", 0, TimesheetAnalyzer.total(new int[]{0, 0, 0, 0, 0}));
        expectInt("unit/10 total верхняя граница", 80, TimesheetAnalyzer.total(new int[]{16, 16, 16, 16, 16}));
    }

    // average: округление до двух знаков
    private static void checkAverage() {
        expectDouble("unit/11 average типовое", 8.6, TimesheetAnalyzer.average(new int[]{8, 9, 10, 8, 8}));
        expectDouble("unit/12 average целое", 8.0, TimesheetAnalyzer.average(new int[]{8, 8, 8, 8, 8}));
        expectDouble("unit/13 average дробное", 7.8, TimesheetAnalyzer.average(new int[]{7, 8, 9, 8, 7}));
    }

    // overtimeDays: строго больше 8
    private static void checkOvertimeDays() {
        expectInt("unit/14 overtimeDays нет переработки", 0, TimesheetAnalyzer.overtimeDays(new int[]{8, 8, 8, 8, 8}));
        expectInt("unit/15 overtimeDays два дня", 2, TimesheetAnalyzer.overtimeDays(new int[]{9, 8, 7, 10, 8}));
        expectInt("unit/16 overtimeDays граница 8", 1, TimesheetAnalyzer.overtimeDays(new int[]{8, 9, 8, 8, 8}));
    }

    // weeklyOvertime: max(0, sum - 40)
    private static void checkWeeklyOvertime() {
        expectInt("unit/17 weeklyOvertime сумма 40", 0, TimesheetAnalyzer.weeklyOvertime(new int[]{8, 8, 8, 8, 8}));
        expectInt("unit/18 weeklyOvertime сумма 50", 10, TimesheetAnalyzer.weeklyOvertime(new int[]{10, 10, 10, 10, 10}));
        expectInt("unit/19 weeklyOvertime сумма меньше 40", 0, TimesheetAnalyzer.weeklyOvertime(new int[]{6, 6, 6, 6, 6}));
    }

    // защитные условия методов вычислений (программные контракты)
    private static void checkGuards() {
        expectGuard("unit/20 total(null)", "total", null, "hours is null");
        expectGuard("unit/21 average длина 4", "average", new int[]{8, 8, 8, 8}, "hours length must be 5, got 4");
        expectGuard("unit/22 overtimeDays(null)", "overtimeDays", null, "hours is null");
        expectGuard("unit/23 weeklyOvertime длина 6", "weeklyOvertime", new int[6], "hours length must be 5, got 6");
    }

    // вариант 13, часть А: пустые значения на концах строки игнорируются
    private static void checkVariantPartA() {
        expectArray("var13-A/1 завершающая ; допускается", "8;8;8;8;8;", new int[]{8, 8, 8, 8, 8});
        expectArray("var13-A/2 начальная ; допускается", ";9;8;7;10;8", new int[]{9, 8, 7, 10, 8});
        expectError("var13-A/3 пустое в середине по-прежнему ошибка", "8;;9;10;8", "Ошибка: позиция 2 ");
        expectError("var13-A/4 после обрезки четыре значения", "8;8;8;8;;", "Ошибка: структура");
    }

    // вариант 13, часть Б: addMinWorkday
    private static void checkVariantPartB() {
        expectInt("var13-B/1 min среди ненулевых", 6, TimesheetAnalyzer.addMinWorkday(new int[]{8, 0, 6, 9, 7}));
        expectInt("var13-B/2 все нули", 0, TimesheetAnalyzer.addMinWorkday(new int[]{0, 0, 0, 0, 0}));
        expectInt("var13-B/3 все равны", 8, TimesheetAnalyzer.addMinWorkday(new int[]{8, 8, 8, 8, 8}));
        expectInt("var13-B/4 один день", 3, TimesheetAnalyzer.addMinWorkday(new int[]{0, 0, 3, 0, 0}));
    }

    private static void expectArray(String name, String line, int[] expected) {
        total++;
        try {
            int[] actual = TimesheetAnalyzer.parseHours(line);
            report(name, Arrays.equals(expected, actual), "получено " + Arrays.toString(actual));
        } catch (RuntimeException e) {
            report(name, false, "исключение " + e.getMessage());
        }
    }

    private static void expectError(String name, String line, String messagePrefix) {
        total++;
        try {
            int[] actual = TimesheetAnalyzer.parseHours(line);
            report(name, false, "ожидалось исключение, получено " + Arrays.toString(actual));
        } catch (IllegalArgumentException e) {
            report(name, e.getMessage().startsWith(messagePrefix), "сообщение: " + e.getMessage());
        }
    }

    private static void expectInt(String name, int expected, int actual) {
        total++;
        report(name, expected == actual, "ожидалось " + expected + ", получено " + actual);
    }

    private static void expectDouble(String name, double expected, double actual) {
        total++;
        report(name, Math.abs(expected - actual) < EPS, "ожидалось " + expected + ", получено " + actual);
    }

    private static void expectGuard(String name, String method, int[] hours, String message) {
        total++;
        try {
            invoke(method, hours);
            report(name, false, "ожидалось исключение");
        } catch (IllegalArgumentException e) {
            report(name, message.equals(e.getMessage()), "сообщение: " + e.getMessage());
        }
    }

    private static void invoke(String method, int[] hours) {
        switch (method) {
            case "total":
                TimesheetAnalyzer.total(hours);
                break;
            case "average":
                TimesheetAnalyzer.average(hours);
                break;
            case "overtimeDays":
                TimesheetAnalyzer.overtimeDays(hours);
                break;
            case "weeklyOvertime":
                TimesheetAnalyzer.weeklyOvertime(hours);
                break;
            default:
                TimesheetAnalyzer.addMinWorkday(hours);
        }
    }

    private static void report(String name, boolean ok, String detail) {
        if (!ok) {
            failed++;
        }
        System.out.println((ok ? "PASS " : "FAIL ") + name + (ok ? "" : ": " + detail));
    }

    // ---------- режим 2: data-driven ----------

    private static void runDataDriven() {
        List<String> lines = readSample();
        if (lines == null) {
            return;
        }
        int number = 0;
        int passed = 0;
        for (String line : lines) {
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            number++;
            if (reportDataLine(number, line)) {
                passed++;
            }
        }
        System.out.println("Data-driven: " + passed + "/" + number + " passed");
        total += number;
        failed += number - passed;
    }

    private static List<String> readSample() {
        try {
            return Files.readAllLines(Paths.get(SAMPLE_FILE), StandardCharsets.UTF_8);
        } catch (IOException e) {
            total++;
            report("data/файл", false, "не удалось прочитать " + SAMPLE_FILE);
            return null;
        }
    }

    private static boolean reportDataLine(int number, String line) {
        String problem = checkDataLine(line);
        if (problem == null) {
            System.out.println("PASS data/" + number);
            return true;
        }
        System.out.println("FAIL data/" + number + ": " + problem);
        return false;
    }

    /** Формат строки: вход|ожидание|комментарий. Возвращает null, если проверка прошла. */
    private static String checkDataLine(String line) {
        String[] parts = line.split("\\|", -1);
        if (parts.length != 3) {
            return "ожидалось 3 части через |, получено " + parts.length;
        }
        String input = parts[0].equals("EMPTY") ? "" : parts[0];
        String expected = parts[1];
        try {
            int[] hours = TimesheetAnalyzer.parseHours(input);
            if (!expected.startsWith("OK:")) {
                return "ожидалась ошибка (" + expected + "), строка принята";
            }
            return compareMetrics(hours, expected.substring("OK:".length()));
        } catch (IllegalArgumentException e) {
            return compareError(e.getMessage(), expected);
        }
    }

    private static String compareMetrics(int[] hours, String expectedMetrics) {
        String actual = "sum=" + TimesheetAnalyzer.total(hours)
                + ",avg=" + String.format(Locale.US, "%.2f", TimesheetAnalyzer.average(hours))
                + ",od=" + TimesheetAnalyzer.overtimeDays(hours)
                + ",wo=" + TimesheetAnalyzer.weeklyOvertime(hours);
        return actual.equals(expectedMetrics) ? null : "ожидалось " + expectedMetrics + ", получено " + actual;
    }

    private static String compareError(String message, String expected) {
        if (!expected.startsWith("ERROR:")) {
            return "неожиданная ошибка: " + message;
        }
        String what = expected.substring("ERROR:".length());
        String prefix = what.equals("структура") ? "Ошибка: структура" : "Ошибка: " + what + " ";
        return message.startsWith(prefix) ? null : "ожидалось \"" + what + "\", сообщение: " + message;
    }

    // ---------- режим 3: интерактивный ----------

    private static void runInteractive() {
        System.out.println("Введите строку табеля, например 8;9;10;8;8 (exit или пустая строка - выход)");
        Scanner in = new Scanner(System.in);
        while (in.hasNextLine()) {
            String line = in.nextLine().replace("﻿", "");
            if (line.isEmpty() || line.equals("exit")) {
                break;
            }
            System.out.println(describe(line));
        }
    }

    private static String describe(String line) {
        try {
            return formatReport(TimesheetAnalyzer.parseHours(line));
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }

    // итоговый отчёт: сумма, среднее (2 знака), дни с переработкой, недельная переработка
    private static String formatReport(int[] hours) {
        return "Сумма: " + TimesheetAnalyzer.total(hours) + "\n"
                + "Среднее: " + String.format(Locale.US, "%.2f", TimesheetAnalyzer.average(hours)) + "\n"
                + "Дней с переработкой: " + TimesheetAnalyzer.overtimeDays(hours) + "\n"
                + "Недельная переработка: " + TimesheetAnalyzer.weeklyOvertime(hours) + "\n"
                + "Самый короткий рабочий день: " + TimesheetAnalyzer.addMinWorkday(hours);
    }
}
