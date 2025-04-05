package ru.greemlab.neirocalendar.domain.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Ячейка календаря (один день), содержащая:
 * - дату,
 * - флаг, принадлежит ли день к текущему месяцу,
 * - список записей (AttendanceRecordDto) на этот день.
 */
public record DayCellDto(
        LocalDate date,
        boolean inCurrentMonth,
        List<AttendanceRecordDto> records
) {
}
