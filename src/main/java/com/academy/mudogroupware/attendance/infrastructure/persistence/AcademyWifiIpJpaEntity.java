package com.academy.mudogroupware.attendance.infrastructure.persistence;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "academy_wifi_ip")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AcademyWifiIpJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wifi_ip_id")
    private Long id;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "note", length = 100)
    private String note;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private AcademyWifiIpJpaEntity(String ipAddress, String note,
                                   LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.ipAddress = ipAddress;
        this.note = note;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
