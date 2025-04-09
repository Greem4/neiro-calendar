package ru.greemlab.neirocalendar.controller;

import ru.greemlab.neirocalendar.domain.dto.AttendanceRecordDto;
import ru.greemlab.neirocalendar.domain.dto.CalendarDataDto;
import ru.greemlab.neirocalendar.domain.dto.DayCellDto;
import ru.greemlab.neirocalendar.domain.dto.MonthContext;
import ru.greemlab.neirocalendar.service.CalendarService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;

/**
 * Абстрактный базовый контроллер для работы с календарём.
 * Содержит общую логику для построения календарной сетки, получения
 * диапазона дат выбранного месяца и форматирование наименований месяцев.
 */
public abstract class AbstractCalendarController {

    protected final CalendarService calendarService;

    // Константа с разрешёнными днями для записи занятий
    protected static final Set<DayOfWeek> ALLOWED_DAYS = Set.of(
            DayOfWeek.TUESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SUNDAY
    );

    public AbstractCalendarController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    /**
     * Получает контекст выбранного месяца: год, месяц, дату начала и конца месяца.
     * Если год или месяц равны null, используются текущие год и месяц.
     *
     * @param year  выбранный год
     * @param month выбранный месяц
     * @return объект MonthContext с данными выбранного месяца
     */
    public static MonthContext getMonthContext(Integer year, Integer month) {
        var now = LocalDate.now();
        var selectedYear = (year == null) ? now.getYear() : year;
        var selectedMonth = (month == null) ? now.getMonthValue() : month;

        // Границы выбранного месяца
        var startOfMonth = LocalDate.of(selectedYear, selectedMonth, 1);
        var endOfMonth = startOfMonth.withDayOfMonth(startOfMonth.lengthOfMonth());

        return new MonthContext(selectedYear, selectedMonth, startOfMonth, endOfMonth);
    }

    /**
     * Строит 2D-сетку календаря – список недель, каждая неделя представлена списком дней.
     * Для формирования первой недели подбираются предыдущие дни, чтобы неделя начиналась с понедельника.
     *
     * @param year       выбранный год
     * @param month      выбранный месяц
     * @param recordsMap карта записей посещаемости, сгруппированных по дате
     * @return 2D список объектов DayCellDto
     */
    protected List<List<DayCellDto>> buildCalendarGrid(int year, int month,
                                                       Map<LocalDate, List<AttendanceRecordDto>> recordsMap) {
        List<List<DayCellDto>> result = new ArrayList<>();
        var firstOfMonth = LocalDate.of(year, month, 1);
        var firstDayDow = firstOfMonth.getDayOfWeek().getValue();
        var start = firstOfMonth.minusDays(firstDayDow - 1);

        final int WEEKS_TO_SHOW = 6;
        var current = start;
        for (int w = 0; w < WEEKS_TO_SHOW; w++) {
            List<DayCellDto> weekRow = new ArrayList<>(7);
            for (int d = 0; d < 7; d++) {
                var inCurrentMonth = current.getYear() == year && current.getMonthValue() == month;
                var recs = recordsMap.getOrDefault(current, List.of());
                weekRow.add(new DayCellDto(current, inCurrentMonth, recs));
                current = current.plusDays(1);
            }
            result.add(weekRow);
        }
        return result;
    }

    /**
     * Формирует отображение наименований месяцев на русском языке.
     *
     * @return LinkedHashMap, где ключ — номер месяца (1-12), значение — название месяца
     */
    protected LinkedHashMap<Integer, String> getMonthNames() {
        LinkedHashMap<Integer, String> map = new LinkedHashMap<>();
        for (int i = 1; i <= 12; i++) {
            var name = Month.of(i)
                    .getDisplayName(TextStyle.FULL_STANDALONE, Locale.forLanguageTag("ru-RU"));
            name = name.substring(0, 1).toUpperCase() + name.substring(1);
            map.put(i, name);
        }
        return map;
    }

    protected CalendarDataDto prepareCalendarData(Integer year, Integer month) {
        // Текущая дата по умолчанию
        var ctx = getMonthContext(year, month);

        // Получаем все записи за выбранный месяц
        var monthlyRecords = calendarService.getRecordsBetween(ctx.startOfMonth(), ctx.endOfMonth());

        // Группируем записи по датам
        Map<LocalDate, List<AttendanceRecordDto>> recordsByDate = new HashMap<>();
        for (var rec : monthlyRecords) {
            recordsByDate.computeIfAbsent(rec.visitDate(), k -> new ArrayList<>()).add(rec);
        }

        // Строим сетку календаря (максимум 6 недель, 7 дней в неделе)
        List<List<DayCellDto>> weeks = buildCalendarGrid(ctx.year(), ctx.month(), recordsByDate);

        // Итоговые расчёты
        var totalCost = calendarService.calculateTotalCost(ctx.startOfMonth(), ctx.endOfMonth());
        var attendedCount = monthlyRecords.stream().filter(r -> Boolean.TRUE.equals(r.attended())).count();

        return CalendarDataDto.builder()
                .year(ctx.year())
                .month(ctx.month())
                .weeks(weeks)
                .totalCost(totalCost)
                .attendedCount(attendedCount)
                .monthNames(getMonthNames())
                .build();
    }
}
