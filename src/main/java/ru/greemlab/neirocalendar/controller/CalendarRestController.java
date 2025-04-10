package ru.greemlab.neirocalendar.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.greemlab.neirocalendar.domain.dto.CalendarResponseDto;
import ru.greemlab.neirocalendar.domain.dto.DaySummaryDto;
import ru.greemlab.neirocalendar.service.CalendarService;

import java.time.LocalDate;
import java.util.List;

/**
 * REST контроллер для управления календарём посещаемости.
 * Предоставляет API для получения календарных данных, добавления, отметки и удаления записей.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/calendar")
@Tag(name = "Календарь", description = "Операции для управления календарём посещаемости")
public class CalendarRestController extends AbstractCalendarController {


    public CalendarRestController(CalendarService calendarService) {
        super(calendarService);
    }

    /**
     * Возвращает данные календаря для указанного месяца и года.
     *
     * @param year  выбранный год (если не указан – используется текущий год)
     * @param month выбранный месяц (если не указан – используется текущий месяц)
     * @return JSON-структура с календарной сеткой и статистикой посещений
     */
    @GetMapping
    @Operation(summary = "Получить календарь для указанного месяца/года",
            description = "Возвращает JSON со списком дней календаря и данными посещаемости.")
    public ResponseEntity<CalendarResponseDto> getCalendar(
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "month", required = false) Integer month
    ) {
        // Получаем подготовленные календарные данные через общий метод
        var calendarData = prepareCalendarData(year, month);

        var response = CalendarResponseDto.builder()
                .year(calendarData.year())
                .month(calendarData.month())
                .weeks(calendarData.weeks())
                .totalCost(calendarData.totalCost())
                .attendedCount(calendarData.attendedCount())
                .monthNames(getMonthNames())
                .allowedDays(ALLOWED_DAYS)
                .weekDays(List.of("Вт", "Чт", "Пт", "Вс"))
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Возвращает количество посещённых занятий и заработанную сумму по каждому дню за указанный период (только для attended = true).
     */
    @GetMapping("/daily-summary")
    @Operation(summary = "Получить сводку по дням",
            description = "Возвращает количество посещённых занятий и заработанную сумму по каждому дню за указанный период")
    public ResponseEntity<List<DaySummaryDto>> getDailySummaries(
            @RequestParam("start") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate start,
            @RequestParam("end")   @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end
    ) {
        var dailySummaries = calendarService.getDailySummaries(start, end);
        return ResponseEntity.ok(dailySummaries);
    }

    /**
     * Создаёт новую запись посещаемости для указанного человека на заданную дату (attended=false по умолчанию).
     *
     * @param personName имя человека
     * @param date       дата посещения (формат yyyy-MM-dd)
     * @return сообщение об успешном создании записи
     */
    @PostMapping("/add")
    @Operation(summary = "Добавить записи посещаемости на 3 месяца вперёд",
            description = "Создаёт новые записи посещаемости для указанного человека на каждый аналогичный день недели на ближайшие 3 месяца.")
    public ResponseEntity<String> addAttendance(
            @RequestParam("personName") String personName,
            @RequestParam("date") @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date
    ) {
        log.info("Добавление записи посещаемости: personName={}, date={}", personName, date);
        calendarService.saveAttendanceFor3Month(personName, date);
        return ResponseEntity.ok("Запись посещаемости создана");
    }

    /**
     * Помечает запись посещаемости как выполненную (attended=true).
     *
     * @param recordId идентификатор записи
     * @return сообщение об успешном изменении статуса записи
     */
    @PostMapping("/check")
    @Operation(summary = "Отметить запись как посещённую",
            description = "Помечает существующую запись посещаемости как выполненную (attended=true).")
    public ResponseEntity<String> checkAttendance(@RequestParam("recordId") Long recordId) {
        log.info("Отметка посещаемости для записи recordId={}", recordId);
        calendarService.markAttendanceTrue(recordId);
        return ResponseEntity.ok("Запись посещаемости отмечена");
    }

    /**
     * Снимает отметку о посещении (attended=false).
     *
     * @param recordId идентификатор записи
     * @return сообщение об успешном изменении статуса записи
     */
    @PostMapping("/uncheck")
    @Operation(summary = "Снять отметку посещаемости",
            description = "Помечает существующую запись посещаемости как невыполненную (attended=false).")
    public ResponseEntity<String> unCheckAttendance(@RequestParam("recordId") Long recordId) {
        log.info("Снятие отметки посещаемости для записи recordId={}", recordId);
        calendarService.markAttendanceFalse(recordId);
        return ResponseEntity.ok("Запись посещаемости обновлена");
    }

    /**
     * Удаляет указанную запись посещаемости.
     *
     * @param recordId идентификатор записи
     * @return сообщение об успешном удалении записи
     */
    @DeleteMapping("/delete")
    @Operation(summary = "Удалить запись посещаемости",
            description = "Полностью удаляет запись посещаемости из календаря.")
    public ResponseEntity<String> deleteAttendance(@RequestParam("recordId") Long recordId) {
        log.info("Удаление записи посещаемости с recordId={}", recordId);
        calendarService.deleteAttendance(recordId);
        return ResponseEntity.ok("Запись посещаемости удалена");
    }
}
