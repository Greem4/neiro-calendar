package ru.greemlab.neirocalendar.domain.dto;

import java.time.LocalDate;

/**
 * DTO (Data Transfer Object) для передачи записей о посещении
 * между слоями (Controller -> Service -> View).
 * Здесь используем современный Java record.
 */
public record AttendanceRecordDto(
        Long id,
        String personName,
        LocalDate visitDate,
        Boolean attended
) {
}
