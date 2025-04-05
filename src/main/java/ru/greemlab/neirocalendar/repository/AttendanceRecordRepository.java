package ru.greemlab.neirocalendar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.greemlab.neirocalendar.domain.entity.AttendanceRecord;

import java.time.LocalDate;
import java.util.List;

/**
 * Репозиторий для работы с таблицей "attendance_records".
 * Наследуемся от JpaRepository, чтобы получить базовые CRUD-методы:
 * save, findAll, findById, delete и др.
 */
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {

    /**
     * Находим все записи на конкретную дату.
     */
    List<AttendanceRecord> findByVisitDate(LocalDate date);

    /**
     * Находим все записи в заданном интервале (включительно).
     */
    List<AttendanceRecord> findByVisitDateBetween(LocalDate start, LocalDate end);
}
