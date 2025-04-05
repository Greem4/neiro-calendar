package ru.greemlab.neirocalendar.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Сущность для таблицы attendance_records
 */
@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "attendance_records")
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "person_name", nullable = false)
    private String personName;

    @Column(name = "visit_date", nullable = false)
    private LocalDate visitDate;

    @Column(name = "attended", nullable = false)
    private Boolean attended = false;
}
