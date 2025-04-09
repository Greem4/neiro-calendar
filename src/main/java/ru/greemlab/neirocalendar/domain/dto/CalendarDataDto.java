package ru.greemlab.neirocalendar.domain.dto;

import lombok.Builder;

import java.util.List;
import java.util.Map;

@Builder
public record CalendarDataDto(
        int year,
        int month,
        List<List<DayCellDto>> weeks,
        long totalCost,
        long attendedCount,
        Map<Integer, String> monthNames
) {
}
