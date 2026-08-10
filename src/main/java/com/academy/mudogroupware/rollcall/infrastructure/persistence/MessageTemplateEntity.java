package com.academy.mudogroupware.rollcall.infrastructure.persistence;

import com.academy.mudogroupware.global.infrastructure.persistence.BaseTimeEntity;
import com.academy.mudogroupware.rollcall.domain.model.AttendanceStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "message_template")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MessageTemplateEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_id")
    private Long id;

    @Column(name = "academy_id", nullable = false)
    private Long academyId;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceStatus status;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Builder
    private MessageTemplateEntity(Long academyId, String name, AttendanceStatus status, String content,
                                   Long createdBy) {
        this.academyId = academyId;
        this.name = name;
        this.status = status;
        this.content = content;
        this.createdBy = createdBy;
    }

    public void update(String name, String content) {
        this.name = name;
        this.content = content;
    }
}
