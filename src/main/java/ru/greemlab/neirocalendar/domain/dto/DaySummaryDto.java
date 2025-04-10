package ru.greemlab.neirocalendar.domain.dto;

import java.time.LocalDate;

/**
 * DTO для сводки по дням: количество посещённых занятий и заработанная сумма
 */
public record DaySummaryDto(
        LocalDate date,
        int totalCount,     // всего записей
        int attendedCount,  // пришли
        int earnings        // сумма
) {
}
