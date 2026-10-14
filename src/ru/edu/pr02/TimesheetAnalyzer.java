package ru.edu.pr02;

import java.util.Arrays;

/**
 * Обработка строки табеля одного сотрудника за рабочую неделю (практическая работа №5).
 * Класс без состояния: все методы public static и без ввода-вывода.
 *
 * Вариант 13. Часть А: parseHours игнорирует пустые значения на концах строки,
 * поэтому завершающая (и начальная) точка с запятой допускается.
 * Часть Б: addMinWorkday возвращает самый короткий отработанный день.
 */
public class TimesheetAnalyzer {
    private static final int DAYS_IN_WEEK = 5;
    private static final int MIN_HOURS = 0;
    private static final int MAX_HOURS = 16;
    private static final int DAILY_NORMAL_HOURS = 8;
    private static final int WEEKLY_NORM_HOURS = 40;
    private static final String SEPARATOR = ";";
    private static final String HOURS_FORMAT = "^(0|[1-9][0-9]*)$";

    /**
     * Единственная точка валидации строкового ввода (правила R1-R9).
     * Fail-fast: первая найденная ошибка прекращает разбор.
     */
    public static int[] parseHours(String line) {
        String[] parts = line == null ? new String[0] : trimEmptyEnds(line.split(SEPARATOR, -1));
        if (parts.length != DAYS_IN_WEEK) {
            throw new IllegalArgumentException("Ошибка: структура – ожидалось " + DAYS_IN_WEEK
                    + " значений, получено " + parts.length);
        }
        int[] result = new int[DAYS_IN_WEEK];
        for (int i = 0; i < DAYS_IN_WEEK; i++) {
            result[i] = parseValue(parts[i], i + 1);
        }
        return result;
    }

    public static int total(int[] hours) {
        requireWeek(hours);
        int sum = 0;
        for (int value : hours) {
            sum += value;
        }
        return sum;
    }

    public static double average(int[] hours) {
        double mean = (double) total(hours) / DAYS_IN_WEEK;
        return Math.round(mean * 100.0) / 100.0;
    }

    public static int overtimeDays(int[] hours) {
        requireWeek(hours);
        int days = 0;
        for (int value : hours) {
            if (value > DAILY_NORMAL_HOURS) {
                days++;
            }
        }
        return days;
    }

    public static int weeklyOvertime(int[] hours) {
        return Math.max(0, total(hours) - WEEKLY_NORM_HOURS);
    }

    /**
     * Часть Б варианта 13: минимальное ненулевое значение (самый короткий рабочий день).
     * Дни с нулём считаются выходными и не участвуют. Если все нули, возвращает 0.
     */
    public static int addMinWorkday(int[] hours) {
        requireWeek(hours);
        int min = 0;
        for (int value : hours) {
            if (value > 0 && (min == 0 || value < min)) {
                min = value;
            }
        }
        return min;
    }

    // --- вспомогательные методы ---

    // Часть А варианта 13: пустые элементы в начале и в конце не считаются значениями
    private static String[] trimEmptyEnds(String[] parts) {
        int from = 0;
        int to = parts.length;
        while (from < to && parts[from].isEmpty()) {
            from++;
        }
        while (to > from && parts[to - 1].isEmpty()) {
            to--;
        }
        return Arrays.copyOfRange(parts, from, to);
    }

    private static int parseValue(String text, int position) {
        if (!text.matches(HOURS_FORMAT)) {
            throw new IllegalArgumentException("Ошибка: позиция " + position + " – " + describeBadFormat(text));
        }
        int value;
        try {
            value = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(rangeMessage(position, text));
        }
        if (value < MIN_HOURS || value > MAX_HOURS) {
            throw new IllegalArgumentException(rangeMessage(position, text));
        }
        return value;
    }

    private static String describeBadFormat(String text) {
        if (text.isEmpty()) {
            return "пустое значение";
        }
        if (text.length() > 1 && text.charAt(0) == '0' && text.matches("^[0-9]+$")) {
            return "значение \"" + text + "\" с ведущим нулём не допускается";
        }
        return "значение \"" + text + "\" не является целым числом";
    }

    private static String rangeMessage(int position, String text) {
        return "Ошибка: позиция " + position + " – значение " + text
                + " вне диапазона [" + MIN_HOURS + ", " + MAX_HOURS + "]";
    }

    // Программный контракт, а не проверка пользовательского ввода
    private static void requireWeek(int[] hours) {
        if (hours == null) {
            throw new IllegalArgumentException("hours is null");
        }
        if (hours.length != DAYS_IN_WEEK) {
            throw new IllegalArgumentException("hours length must be " + DAYS_IN_WEEK + ", got " + hours.length);
        }
    }
}
