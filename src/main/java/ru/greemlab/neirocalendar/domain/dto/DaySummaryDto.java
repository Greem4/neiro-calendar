package ru.greemlab.neirocalendar.domain.dto;

import java.time.LocalDate;

/**
 * DTO для сводки по дням: количество посещённых занятий и заработанная сумма
 */
public record DaySummaryDto(
        LocalDate date,
        int attendedCount,
        int earnings
) {
}
