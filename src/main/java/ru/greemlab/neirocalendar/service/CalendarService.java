package ru.greemlab.neirocalendar.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.greemlab.neirocalendar.domain.dto.AttendanceRecordDto;
import ru.greemlab.neirocalendar.domain.dto.DaySummaryDto;
import ru.greemlab.neirocalendar.domain.entity.AttendanceRecord;
import ru.greemlab.neirocalendar.mapper.UserAttendanceRecordMap;
import ru.greemlab.neirocalendar.repository.AttendanceRecordRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalendarService {

    /**
     * Цена одного посещения
     */
    private static final int COST_PER_ATTENDANCE = 1250;

    private final AttendanceRecordRepository repository;
    private final UserAttendanceRecordMap mapper;

    /**
     * Создать / обновить запись
     */
    @Transactional
    public void saveAttendance(AttendanceRecordDto dto) {
        AttendanceRecord entity;
        if (dto.id() != null) {
            // Если уже есть ID, найдём в БД, иначе создаём новый
            entity = repository.findById(dto.id()).orElse(new AttendanceRecord());
        } else {
            entity = new AttendanceRecord();
        }

        entity.setPersonName(dto.personName());
        entity.setVisitDate(dto.visitDate());
        entity.setAttended(dto.attended() != null ? dto.attended() : false);

        var saved = repository.save(entity);
        mapper.toDto(saved);
    }

    /**
     * Создать / обновить запись на 3 месяца
     */
    @Transactional
    public void saveAttendanceFor3Month(String personName, LocalDate startDate) {
        var endDate = startDate.plusMonths(3);
        var current = startDate;

        while (!current.isAfter(endDate)) {
            var dto = new AttendanceRecordDto(null, personName, current, false);
            saveAttendance(dto);
            current = current.plusWeeks(1);
        }
    }

    /**
     * Отметить присутствие (attended = true) по ID
     */
    @Transactional
    public void markAttendanceTrue(Long recordId) {
        repository.findById(recordId).ifPresent(rec -> {
            rec.setAttended(true);
            repository.save(rec);
        });
    }

    /**
     * Отменить присутствие (attended = true) по ID
     */
    @Transactional
    public void markAttendanceFalse(Long recordId) {
        repository.findById(recordId).ifPresent(rec -> {
            rec.setAttended(false);
            repository.save(rec);
        });
    }

    /**
     * Удалить запись по ID
     */
    @Transactional
    public void deleteAttendance(Long recordId) {
        repository.deleteById(recordId);
    }

    /**
     * Найти записи за период [start..end]
     */
    @Transactional(readOnly = true)
    public List<AttendanceRecordDto> getRecordsBetween(LocalDate start, LocalDate end) {
        return repository.findByVisitDateBetween(start, end)
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    /**
     * Посчитать общую сумму (только для attended = true)
     */
    @Transactional(readOnly = true)
    public int calculateTotalCost(LocalDate start, LocalDate end) {
        var list = getRecordsBetween(start, end);
        var sum = 0;
        for (var record : list) {
            if (Boolean.TRUE.equals(record.attended())) {
                sum += COST_PER_ATTENDANCE;
            }
        }
        return sum;
    }

    /**
     * Подсчитает посещения и сумму за день (только для attended = true)
     */
    @Transactional(readOnly = true)
    public List<DaySummaryDto> getDailySummaries(LocalDate start, LocalDate end) {
        // Получаем записи за период [start; end]
        var records = getRecordsBetween(start, end);

        // Группируем записи по дате и фильтруем только attended = true
        var dailyRecords = records.stream()
                .filter(rec -> Boolean.TRUE.equals(rec.attended()))
                .collect(Collectors.groupingBy(AttendanceRecordDto::visitDate));

        // Собираем результаты: для каждого дня считаем количество и заработок
        List<DaySummaryDto> result = new ArrayList<>();
        for (Map.Entry<LocalDate, List<AttendanceRecordDto>> entry : dailyRecords.entrySet()) {
            var count = entry.getValue().size();
            var earnings = count * COST_PER_ATTENDANCE;
            result.add(new DaySummaryDto(entry.getKey(), count, earnings));
        }
        result.sort(Comparator.comparing(DaySummaryDto::date));
        return result;
    }
}
