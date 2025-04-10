package ru.greemlab.neirocalendar.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.greemlab.neirocalendar.service.CalendarService;

import java.time.LocalDate;
import java.util.List;

/**
 * MVC-контроллер для отображения полного календаря на выбранный месяц.
 * В нужные дни (Вт, Чт, Пт, Вс) можно добавлять/отмечать/удалять занятия.
 */
@Slf4j
@Controller
@RequestMapping("/calendar")
public class CalendarController extends AbstractCalendarController {

    public CalendarController(CalendarService calendarService) {
        super(calendarService);
    }

    /**
     * Отображает страницу календаря.
     *
     * @param year  выбранный год (если не указан – используется текущий год)
     * @param month выбранный месяц (если не указан – используется текущий месяц)
     * @param model модель для передачи данных в шаблон
     * @return название шаблона для отображения календаря
     */
    @GetMapping
    public String showCalendar(
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "month", required = false) Integer month,
            Model model
    ) {
        var calendarData = prepareCalendarData(year, month);

        // Заполняем модель атрибутами для шаблона
        model.addAttribute("year", calendarData.year());
        model.addAttribute("month", calendarData.month());
        model.addAttribute("weeks", calendarData.weeks());
        model.addAttribute("totalCost", calendarData.totalCost());
        model.addAttribute("attendedCount", calendarData.attendedCount());
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
        calendarService.saveAttendanceFor3Month(personName, date);
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
     * Отменить отсутствие
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
}
