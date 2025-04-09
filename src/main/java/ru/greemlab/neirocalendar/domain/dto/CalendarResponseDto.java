package ru.greemlab.neirocalendar.domain.dto;

import lombok.Builder;

import java.time.DayOfWeek;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

@Builder
public record CalendarResponseDto(
        int year,
        int month,
        List<List<DayCellDto>> weeks,
        long totalCost,
        long attendedCount,
        LinkedHashMap<Integer, String> monthNames,
        Set<DayOfWeek> allowedDays,
        List<String> weekDays
) {
}
