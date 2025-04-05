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

    // Константа с разрешёнными днями для записи занятий
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
        // Текущая дата по умолчанию
        LocalDate now = LocalDate.now();
        int selectedYear = (year == null) ? now.getYear() : year;
        int selectedMonth = (month == null) ? now.getMonthValue() : month;

        // Границы выбранного месяца
        LocalDate startOfMonth = LocalDate.of(selectedYear, selectedMonth, 1);
        LocalDate endOfMonth = startOfMonth.withDayOfMonth(startOfMonth.lengthOfMonth());

        // Получаем все записи за выбранный месяц
        var monthlyRecords = calendarService.getRecordsBetween(startOfMonth, endOfMonth);

        // Группируем записи по датам
        Map<LocalDate, List<AttendanceRecordDto>> recordsByDate = new HashMap<>();
        for (var rec : monthlyRecords) {
            recordsByDate.computeIfAbsent(rec.visitDate(), k -> new ArrayList<>()).add(rec);
        }

        // Строим сетку календаря (максимум 6 недель, 7 дней в неделе)
        List<List<DayCellDto>> weeks = buildCalendarGrid(selectedYear, selectedMonth, recordsByDate);

        // Итоговые расчёты
        int totalCost = calendarService.calculateTotalCost(startOfMonth, endOfMonth);
        long attendedCount = monthlyRecords.stream().filter(r -> Boolean.TRUE.equals(r.attended())).count();

        // Заполняем модель атрибутами для шаблона
        model.addAttribute("year", selectedYear);
        model.addAttribute("month", selectedMonth);
        model.addAttribute("weeks", weeks);
        model.addAttribute("totalCost", totalCost);
        model.addAttribute("attendedCount", attendedCount);
        model.addAttribute("monthNames", getMonthNames());
        // Атрибут для фильтрации разрешённых дней в шаблоне
        model.addAttribute("allowedDays", ALLOWED_DAYS);
        // Заголовки для таблицы (только разрешённые дни)
        model.addAttribute("weekDays", List.of("Вт", "Чт", "Пт", "Вс"));

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
     * Отменить присутствие
     */
    @PostMapping("/uncheck")
    public String unCheckAttendance(@RequestParam("recordId") Long recordId) {
        log.info("Uncheck attendance for recordId={}", recordId);
        calendarService.markAttendanceFalse(recordId);
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
     * Каждая "неделя" представлена списком из 7 ячеек (DayCellDto).
     * Некоторые ячейки могут относиться к предыдущему/следующему месяцу.
     */
    private List<List<DayCellDto>> buildCalendarGrid(
            int year, int month,
            Map<LocalDate, List<AttendanceRecordDto>> recordsMap
    ) {
        List<List<DayCellDto>> result = new ArrayList<>();
        LocalDate firstOfMonth = LocalDate.of(year, month, 1);
        int firstDayDow = firstOfMonth.getDayOfWeek().getValue(); // ISO-8601: Пн=1 ... Вс=7
        LocalDate start = firstOfMonth.minusDays(firstDayDow - 1);
        int WEEKS_TO_SHOW = 6;
        LocalDate current = start;
        for (int w = 0; w < WEEKS_TO_SHOW; w++) {
            List<DayCellDto> weekRow = new ArrayList<>(7);
            for (int d = 0; d < 7; d++) {
                boolean inCurrentMonth = (current.getYear() == year && current.getMonthValue() == month);
                var recs = recordsMap.getOrDefault(current, List.of());
                weekRow.add(new DayCellDto(current, inCurrentMonth, recs));
                current = current.plusDays(1);
            }
            result.add(weekRow);
        }
        return result;
    }

    /**
     * Возвращает отображения месяцев (1 -> "Январь", 2 -> "Февраль", ...)
     */
    private static LinkedHashMap<Integer, String> getMonthNames() {
        LinkedHashMap<Integer, String> map = new LinkedHashMap<>();
        for (int i = 1; i <= 12; i++) {
            String name = Month.of(i).getDisplayName(TextStyle.FULL_STANDALONE, new Locale("ru", "RU"));
            name = name.substring(0, 1).toUpperCase() + name.substring(1);
            map.put(i, name);
        }
        return map;
    }
}
