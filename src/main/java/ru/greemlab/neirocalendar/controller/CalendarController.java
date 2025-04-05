package ru.greemlab.neirocalendar.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.greemlab.neirocalendar.domain.dto.AttendanceRecordDto;
import ru.greemlab.neirocalendar.service.CalendarService;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * MVC-контроллер: отвечает за отображение календаря (Thymeleaf).
 * Принимает запросы, вызывает сервис, кладёт данные в модель, возвращает имя шаблона.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/calendar")
public class CalendarController {

    private final CalendarService calendarService;

    /**
     * Показывает календарь за указанный месяц/год (по умолчанию - текущие).
     *
     * @param model - модель для Thymeleaf
     * @param year  - год (необязательный)
     * @param month - месяц (необязательный)
     * @return имя шаблона "calendar"
     */
    @GetMapping
    public String showCalendar(
            Model model,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "month", required = false) Integer month
    ) {
        var today = LocalDate.now();
        var currentYear = (year == null) ? today.getYear() : year;
        var currentMonth = (month == null) ? today.getMonthValue() : month;

        // Вычисляем границы месяца
        var startOfMonth = LocalDate.of(currentYear, currentMonth, 1);
        var endOfMonth = startOfMonth.withDayOfMonth(startOfMonth.getDayOfMonth());

        // Получаем все записи через сервис
        var monthlyRecords = calendarService.getRecordsBetween(startOfMonth, endOfMonth);
        // Сумма за месяц
        var totalCost = calendarService.calculateTotalCost(startOfMonth, endOfMonth);

        // Названия месяцев
        var monthNames = getMonthNames();

        // Кладём данные в модель
        model.addAttribute("year", currentYear);
        model.addAttribute("month", currentMonth);
        model.addAttribute("records", monthlyRecords);
        model.addAttribute("totalCost", totalCost);
        model.addAttribute("monthNames", monthNames);

        return "calendar";
    }

    /**
     * Обработка формы: добавляем новую запись с attended = false.
     *
     * @param personName - имя/фамилия человека
     * @param date       - дата визита
     */
    @PostMapping("/add")
    public String addAttendance(
            @RequestParam("personName") String personName,
            @RequestParam("date") @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate date
    ) {
        log.info("Received request to add attendance for person={} date={}", personName, date);
        var dto = new AttendanceRecordDto(null, personName, date, false);
        calendarService.saveAttendance(dto);
        return "redirect:/calendar";
    }

    /**
     * Проставить "галочку" (attended = true) для записи с заданным ID.
     * @param recordId - идентификатор записи
     */
    @PostMapping("/check")
    public String checkAttendance(@RequestParam("recordId") Long recordId) {
        log.info("Checking attendance for recordId={}", recordId);

        // На практике можно было бы сделать calendarService.findById(recordId).
        // Пока используем вариант, где ищем запись среди текущего месяца.
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = startOfMonth.withDayOfMonth(startOfMonth.lengthOfMonth());

        // Ищем нужную запись (DTO) среди списка за месяц.
        AttendanceRecordDto dto = calendarService.getRecordsBetween(startOfMonth, endOfMonth)
                .stream()
                .filter(r -> r.id().equals(recordId))
                .findFirst()
                .orElse(null);

        if (dto != null) {
            // "Переключаем" attended в true
            AttendanceRecordDto updatedDto = new AttendanceRecordDto(
                    dto.id(),
                    dto.personName(),
                    dto.visitDate(),
                    true
            );
            calendarService.saveAttendance(updatedDto);
        } else {
            log.warn("Record with ID={} not found in the current month range.", recordId);
        }

        return "redirect:/calendar";
    }

    private static LinkedHashMap<Object, Object> getMonthNames() {
        var monthNames = new LinkedHashMap<>();
        for (int i = 1; i <= 12; i++) {
            var name = Month.of(i)
                    .getDisplayName(TextStyle.FULL_STANDALONE, new Locale("ru"));
            name = name.substring(0, 1).toUpperCase() + name.substring(1);
            monthNames.put(i, name);
        }
        return monthNames;
    }
}
