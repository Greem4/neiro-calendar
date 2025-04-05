package ru.greemlab.neirocalendar.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.greemlab.neirocalendar.domain.dto.AttendanceRecordDto;
import ru.greemlab.neirocalendar.domain.entity.AttendanceRecord;
import ru.greemlab.neirocalendar.mapper.UserAttendanceRecordMap;
import ru.greemlab.neirocalendar.repository.AttendanceRecordRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Сервисный слой для бизнес-логики.
 * Здесь логика сохранения, поиска, преобразования Entity <-> DTO, подсчёт итогов и т.д.
 * <p>
 * Аннотации:
 * - @Service - Spring Bean сервисного слоя
 * - @RequiredArgsConstructor (Lombok) - генерация конструктора с final полями
 * - @Slf4j - логер (Lombok), если хотим логировать
 * - @Transactional - для операций с БД (гарантирует транзакционность)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarService {
    private static final int COST_PER_ATTENDANCE = 1250;

    private final AttendanceRecordRepository repository;
    private final UserAttendanceRecordMap userAttendanceRecordMap;

    /**
     * Создание или обновление записи о посещении (через DTO).
     *
     * @param dto - данные
     * @return сохранённое DTO
     */
    @Transactional
    public AttendanceRecordDto saveAttendance(AttendanceRecordDto dto) {
        log.info("Saving attendance record: {}", dto);

        var entity = dto.id() != null
                ? repository.findById(dto.id()).orElse(new AttendanceRecord())
                : new AttendanceRecord();

        entity.setPersonName(dto.personName());
        entity.setVisitDate(dto.visitDate());
        entity.setAttended(dto.attended() != null ? dto.attended() : false);
        var saved = repository.save(entity);
        return userAttendanceRecordMap.toDtoAttendance(saved);
    }

    /**
     * Получить все записи (DTO) на конкретную дату.
     */
    @Transactional(readOnly = true)
    public List<AttendanceRecordDto> getRecordsForDay(LocalDate date) {
        return repository.findByVisitDate(date)
                .stream()
                .map(userAttendanceRecordMap::toDtoAttendance)
                .toList();
    }

    /**
     * Получить все записи (DTO) за период (включая границы).
     */
    @Transactional(readOnly = true)
    public List<AttendanceRecordDto> getRecordsBetween(LocalDate start, LocalDate end) {
        return repository.findByVisitDateBetween(start, end)
                .stream()
                .map(userAttendanceRecordMap::toDtoAttendance)
                .toList();
    }

    /**
     * Подсчитать общую сумму (руб.) за период (start..end).
     * Считаем только записи, у которых attended = true.
     */
    @Transactional(readOnly = true)
    public int calculateTotalCost(LocalDate start, LocalDate end) {
        var records = getRecordsBetween(start, end);
        int sum = 0;
        for (var record : records) {
            if (Boolean.TRUE.equals(record.attended())) {
                sum += COST_PER_ATTENDANCE;
            }
        }
        log.info("Calculated total cost from {} to {}: {} rubles", start, end, sum);
        return sum;
    }

}
