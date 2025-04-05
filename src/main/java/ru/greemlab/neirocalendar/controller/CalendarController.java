package ru.greemlab.neirocalendar.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.greemlab.neirocalendar.domain.dto.AttendanceRecordDto;
import ru.greemlab.neirocalendar.domain.dto.DayCellDto;
import ru.greemlab.neirocalendar.service.CalendarService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;

/**
 * MVC-контроллер для отображения полного календаря на выбранный месяц.
 * В нужные дни (Вт, Чт, Пт, Вс) можно добавлять/отмечать/удалять занятия.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/calendar")
public class CalendarController {

    private final CalendarService calendarService;

    // Какие дни разрешены для записи занятий
    private static final Set<DayOfWeek> ALLOWED_DAYS = Set.of(
            DayOfWeek.TUESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SUNDAY
    );

    @GetMapping
    public String showCalendar(
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "month", required = false) Integer month,
            Model model
    ) {
        // Текущая дата по умолчанию (если ничего не передали)
        LocalDate now = LocalDate.now();
        int selectedYear = (year == null) ? now.getYear() : year;
        int selectedMonth = (month == null) ? now.getMonthValue() : month;

        // Границы выбранного месяца
        LocalDate startOfMonth = LocalDate.of(selectedYear, selectedMonth, 1);
        LocalDate endOfMonth = startOfMonth.withDayOfMonth(startOfMonth.lengthOfMonth());

        // Все записи за месяц
        var monthlyRecords = calendarService.getRecordsBetween(startOfMonth, endOfMonth);

        // Группируем их по дате для удобного доступа
        Map<LocalDate, List<AttendanceRecordDto>> recordsByDate = new HashMap<>();
        for (var rec : monthlyRecords) {
            recordsByDate
                    .computeIfAbsent(rec.visitDate(), k -> new ArrayList<>())
                    .add(rec);
        }

        // Строим "сетку" календаря (5-6 строк, 7 столбцов)
        List<List<DayCellDto>> weeks = buildCalendarGrid(selectedYear, selectedMonth, recordsByDate);

        // Итоги
        int totalCost = calendarService.calculateTotalCost(startOfMonth, endOfMonth);
        long attendedCount = monthlyRecords.stream()
                .filter(r -> Boolean.TRUE.equals(r.attended()))
                .count();

        model.addAttribute("year", selectedYear);
        model.addAttribute("month", selectedMonth);
        model.addAttribute("weeks", weeks);
        model.addAttribute("totalCost", totalCost);
        model.addAttribute("attendedCount", attendedCount);

        // Для выпадающего списка
        var monthNames = getMonthNames();
        model.addAttribute("monthNames", monthNames);

        // Заголовки для таблицы (Пн, Вт, Ср, Чт, Пт, Сб, Вс)
        model.addAttribute("weekDays", List.of("Пн","Вт","Ср","Чт","Пт","Сб","Вс"));

        return "calendar";
    }

    /**
     * Добавление новой записи (attended = false)
     */
    @PostMapping("/add")
    public String addAttendance(
            @RequestParam("personName") String personName,
            @RequestParam("date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date
    ) {
        log.info("Add attendance: personName={}, date={}", personName, date);
        var dto = new AttendanceRecordDto(null, personName, date, false);
        calendarService.saveAttendance(dto);
        return "redirect:/calendar";
    }

    /**
     * Отметить присутствие
     */
    @PostMapping("/check")
    public String checkAttendance(@RequestParam("recordId") Long recordId) {
        log.info("Check attendance for recordId={}", recordId);
        calendarService.markAttendanceTrue(recordId);
        return "redirect:/calendar";
    }

    /**
     * Удалить запись
     */
    @PostMapping("/delete")
    public String deleteAttendance(@RequestParam("recordId") Long recordId) {
        log.info("Delete attendance recordId={}", recordId);
        calendarService.deleteAttendance(recordId);
        return "redirect:/calendar";
    }

    /**
     * Формируем 2D-список (недели -> дни).
     * Каждая "неделя" - это список из 7 ячеек (DayCellDto).
     * При этом некоторые ячейки могут относиться к предыдущему/следующему месяцу.
     */
    private List<List<DayCellDto>> buildCalendarGrid(
            int year, int month,
            Map<LocalDate, List<AttendanceRecordDto>> recordsMap
    ) {
        List<List<DayCellDto>> result = new ArrayList<>();

        LocalDate firstOfMonth = LocalDate.of(year, month, 1);
        // ISO-8601: Понедельник=1, ... Воскресенье=7
        int firstDayDow = firstOfMonth.getDayOfWeek().getValue();
        // Находим дату, с которой начнём (т.е. понедельник первой недели)
        // если месяц начался во вторник (2), то сместимся назад на (2-1)=1 день.
        LocalDate start = firstOfMonth.minusDays(firstDayDow - 1);

        // Макс. 6 строк (недель), чтобы вместить месяц
        int WEEKS_TO_SHOW = 6;
        int DAYS_IN_GRID = WEEKS_TO_SHOW * 7; // 42 дня макс.

        LocalDate current = start;
        for (int w = 0; w < WEEKS_TO_SHOW; w++) {
            List<DayCellDto> weekRow = new ArrayList<>(7);
            for (int d = 0; d < 7; d++) {
                boolean inCurrentMonth = (current.getYear() == year && current.getMonthValue() == month);
                // Найдём записи для current
                var recs = recordsMap.getOrDefault(current, List.of());
                var cell = new DayCellDto(current, inCurrentMonth, recs);
                weekRow.add(cell);
                current = current.plusDays(1);
            }
            result.add(weekRow);
        }

        return result;
    }

    /**
     * Мапа (1->"Январь", 2->"Февраль", ...)
     */
    private static LinkedHashMap<Integer, String> getMonthNames() {
        var map = new LinkedHashMap<Integer, String>();
        for (int i = 1; i <= 12; i++) {
            var name = Month.of(i).getDisplayName(TextStyle.FULL_STANDALONE, new Locale("ru", "RU"));
            // Первая буква с большой
            name = name.substring(0, 1).toUpperCase() + name.substring(1);
            map.put(i, name);
        }
        return map;
    }
}
