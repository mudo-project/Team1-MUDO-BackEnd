package com.academy.mudogroupware.revenuereport.infrastructure.persistence;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.academy.mudogroupware.global.infrastructure.persistence.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "revenue_report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RevenueReportEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    @Column(name = "target_month", nullable = false)
    private LocalDate targetMonth;

    @Lob
    @Column(nullable = false)
    private String report;

    @Lob
    @Column(name = "data_snapshot", nullable = false)
    private String dataSnapshot;

    @Setter
    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Builder
    private RevenueReportEntity(Long id, LocalDate targetMonth, String report, String dataSnapshot,
                                LocalDateTime readAt) {
        this.id = id;
        this.targetMonth = targetMonth;
        this.report = report;
        this.dataSnapshot = dataSnapshot;
        this.readAt = readAt;
    }
}
