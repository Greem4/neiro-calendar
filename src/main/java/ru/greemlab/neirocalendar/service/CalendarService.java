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
    public AttendanceRecordDto saveAttendance(AttendanceRecordDto dto) {
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
        return mapper.toDto(saved);
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
        int sum = 0;
        for (var record : list) {
            if (Boolean.TRUE.equals(record.attended())) {
                sum += COST_PER_ATTENDANCE;
            }
        }
        return sum;
    }
}
