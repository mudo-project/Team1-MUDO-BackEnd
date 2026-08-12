package com.academy.mudogroupware.lecture.infrastructure.persistence;

import com.academy.mudogroupware.global.infrastructure.persistence.CreatedAtEntity;

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
@Table(name = "classroom")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClassroomEntity extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "classroom_id")
    private Long id;

    // 실제 DB 제약(uk_classroom_name, V1.5.9)과 일치시킨다. ddl-auto=create-drop을 쓰는
    // 테스트 스키마도 이 제약을 그대로 반영해야, 동시 생성 시나리오를 정확히 재현할 수 있다.
    @Column(nullable = false, length = 100, unique = true)
    private String name;

    @Builder
    private ClassroomEntity(String name) {
        this.name = name;
    }
}
