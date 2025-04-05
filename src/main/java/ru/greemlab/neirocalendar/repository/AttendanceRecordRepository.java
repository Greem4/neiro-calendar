package ru.greemlab.neirocalendar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
    @Query("SELECT r FROM AttendanceRecord r WHERE r.visitDate >= :start AND r.visitDate <= :end ORDER BY r.id ASC")
    List<AttendanceRecord> findByVisitDateBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

}
